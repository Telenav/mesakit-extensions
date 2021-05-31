package com.telenav.tdk.tools.applications.pbf.graph.world.extractor.conversion;

import com.telenav.kivakit.kernel.commandline.CommandLine;
import com.telenav.kivakit.kernel.debug.Debug;
import com.telenav.kivakit.kernel.language.string.StringList;
import com.telenav.kivakit.kernel.language.string.Strings;
import com.telenav.kivakit.kernel.language.thread.Threads;
import com.telenav.kivakit.kernel.language.time.Time;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;
import com.telenav.kivakit.kernel.operation.progress.ProgressReporter;
import com.telenav.kivakit.kernel.scalars.bytes.Bytes;
import com.telenav.kivakit.kernel.scalars.counts.ConcurrentMutableCount;
import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.tools.applications.pbf.graph.world.extractor.PbfWorldGraphExtractorApplication;
import com.telenav.mesakit.graph.Metadata;
import com.telenav.mesakit.graph.world.grid.WorldCell;
import com.telenav.mesakit.graph.world.grid.WorldGrid;
import com.telenav.mesakit.graph.world.repository.WorldGraphRepositoryFolder;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * @author jonathanl (shibo)
 */
public class WorldConversion
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private static final Debug DEBUG = new Debug(LOGGER);

    public static class Statistics
    {
        Count attempted;

        Count completed;

        Count failed;

        Count succeeded;

        Count vertexes;

        Count edges;

        Count places;

        Count forwardEdges;

        Count edgeRelations;

        Bytes totalSize;

        public Count attempted()
        {
            return attempted;
        }

        public Count completed()
        {
            return completed;
        }

        public Count edgeRelations()
        {
            return edgeRelations;
        }

        public Count edges()
        {
            return edges;
        }

        public Count failed()
        {
            return failed;
        }

        public Count forwardEdges()
        {
            return forwardEdges;
        }

        public Metadata metadata(final Metadata metadata)
        {
            return metadata
                    .withVertexCount(vertexes())
                    .withEdgeCount(edges())
                    .withForwardEdgeCount(forwardEdges())
                    .withEdgeRelationCount(edgeRelations())
                    .withShapePointCount(Count._0)
                    .withPlaceCount(places());
        }

        public Count places()
        {
            return places;
        }

        public Count succeeded()
        {
            return succeeded;
        }

        public Bytes totalSize()
        {
            return totalSize;
        }

        public Count vertexes()
        {
            return vertexes;
        }
    }

    /**
     * @return The number of graph files created by converting the pbf files of cells in the given grid folder
     */
    public Statistics convert(final PbfWorldGraphExtractorApplication application, final WorldGrid grid,
                              final CommandLine commandLine, final Metadata metadata,
                              final WorldGraphRepositoryFolder outputFolder, final Count threads)
    {
        // Start time
        final var start = Time.now();

        // Get cells with PBF data to convert
        final var cells = grid.cells(outputFolder, WorldCell.DataType.PBF).sortedDescendingByPbfSize();
        LOGGER.information(Strings.box("Converting $ cells", cells.size()));

        // Loop through cells
        final var attempted = new ConcurrentMutableCount();
        final var completed = new ConcurrentMutableCount();
        final var failed = new ConcurrentMutableCount();
        final var succeeded = new ConcurrentMutableCount();
        final var vertexes = new ConcurrentMutableCount();
        final var edges = new ConcurrentMutableCount();
        final var forwardEdges = new ConcurrentMutableCount();
        final var edgeRelations = new ConcurrentMutableCount();
        final var places = new ConcurrentMutableCount();
        final var totalSize = new ConcurrentMutableCount();

        final var toConvert = new ConcurrentLinkedQueue<>(cells);
        final var converterThreads = new CountDownLatch(threads.asInt());
        final var executor = Threads.threadPool("Converter", threads);

        threads.run(() ->
                executor.execute(() ->
                {
                    try
                    {
                        while (!toConvert.isEmpty())
                        {
                            final var worldCell = toConvert.poll();
                            if (worldCell != null)
                            {
                                // We are attempting to convert a cell
                                attempted.increment();
                                final var attempt = attempted.get();
                                final var prefix = "Cell " + attempt + " of " + cells.count() + ": ";

                                // so create the graph converter
                                final var conversion = new Conversion(application, outputFolder, worldCell);
                                LOGGER.listenTo(conversion);

                                // convert the cell, clean cutting to the cell boundary
                                final var input = worldCell.pbfFile(outputFolder).materialized(ProgressReporter.NULL);
                                LOGGER.information("$ Converting $", prefix.trim(), input);

                                // then convert the PBF to a graph
                                final var cellGraphFile = worldCell.cellGraphFile(outputFolder);
                                final var cellGraph = conversion.convert(input, metadata
                                        .withName(metadata.name() + "_" + worldCell.gridCell().name().replaceAll("-", "_")));
                                completed.increment();
                                LOGGER.information("$ Converted", prefix.trim());

                                // If we failed to convert the cell,
                                if (cellGraph == null)
                                {
                                    // then warn
                                    LOGGER.warning("Unable to convert $ to $", worldCell.pbfFile(outputFolder), cellGraphFile);
                                    failed.increment();
                                }
                                else
                                {
                                    // otherwise increment converted count
                                    succeeded.increment();

                                    // add to statistics
                                    vertexes.add(cellGraph.vertexCount());
                                    edges.add(cellGraph.edgeCount());
                                    edgeRelations.add(cellGraph.relationCount());
                                    forwardEdges.add(cellGraph.forwardEdgeCount());
                                    places.add(cellGraph.placeCount());

                                    // index this cell
                                    worldCell.worldGrid().index().index(worldCell, cellGraph);

                                    // Add to total size
                                    if (DEBUG.isDebugOn())
                                    {
                                        final var size = cellGraph.estimatedMemorySize();
                                        if (size != null)
                                        {
                                            totalSize.add(size);
                                        }
                                    }
                                }

                                // Show overall progress
                                LOGGER.information("OVERALL PROGRESS: $ of $ (succeeded = $, failed = $)",
                                        completed, cells.count(), succeeded, failed);
                            }
                        }
                    }
                    finally
                    {
                        converterThreads.countDown();
                    }
                }));
        executor.shutdown();
        try
        {
            converterThreads.await();
        }
        catch (final InterruptedException ignored)
        {
        }

        // We're done
        final var report = new StringList();
        report.add("Succeeded: $", succeeded);
        report.add("Failed: $", failed);
        report.add(Strings.line());
        report.add("Vertexes: $", vertexes);
        report.add("Edges: $", edges);
        report.add("Relations: $", edgeRelations);
        report.add(Strings.line());
        report.add("Total Size: $", Bytes.bytes(totalSize.asLong()));
        report.add("Conversion: $", start.elapsedSince());
        report.titledBox(LOGGER, "Conversion Completed");

        // Return the number of successful conversions
        final var statistics = new Statistics();
        statistics.attempted = attempted.asCount();
        statistics.completed = completed.asCount();
        statistics.failed = failed.asCount();
        statistics.succeeded = succeeded.asCount();
        statistics.vertexes = vertexes.asCount();
        statistics.edges = edges.asCount();
        statistics.forwardEdges = forwardEdges.asCount();
        statistics.edgeRelations = edgeRelations.asCount();
        statistics.places = places.asCount();
        statistics.totalSize = Bytes.bytes(totalSize.asLong());
        return statistics;
    }
}
