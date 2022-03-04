package com.telenav.mesakit.tools.applications.pbf.graph.world.extractor.conversion;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.string.AsciiArt;
import com.telenav.kivakit.core.thread.Threads;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Bytes;
import com.telenav.kivakit.core.value.count.ConcurrentMutableCount;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.messaging.logging.Logger;
import com.telenav.kivakit.messaging.logging.LoggerFactory;
import com.telenav.kivakit.messaging.Debug;
import com.telenav.mesakit.graph.Metadata;
import com.telenav.mesakit.graph.world.grid.WorldCell;
import com.telenav.mesakit.graph.world.grid.WorldGrid;
import com.telenav.mesakit.graph.world.repository.WorldGraphRepositoryFolder;
import com.telenav.mesakit.tools.applications.pbf.graph.world.extractor.PbfWorldGraphExtractorApplication;

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

        public Metadata metadata(Metadata metadata)
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
    public Statistics convert(PbfWorldGraphExtractorApplication application,
                              WorldGrid grid,
                              CommandLine commandLine,
                              Metadata metadata,
                              WorldGraphRepositoryFolder outputFolder,
                              Count threads)
    {
        // Start time
        var start = Time.now();

        // Get cells with PBF data to convert
        var cells = grid.cells(outputFolder, WorldCell.DataType.PBF).sortedDescendingByPbfSize();
        LOGGER.information(AsciiArt.box("Converting $ cells", cells.size()));

        // Loop through cells
        var attempted = new ConcurrentMutableCount();
        var completed = new ConcurrentMutableCount();
        var failed = new ConcurrentMutableCount();
        var succeeded = new ConcurrentMutableCount();
        var vertexes = new ConcurrentMutableCount();
        var edges = new ConcurrentMutableCount();
        var forwardEdges = new ConcurrentMutableCount();
        var edgeRelations = new ConcurrentMutableCount();
        var places = new ConcurrentMutableCount();
        var totalSize = new ConcurrentMutableCount();

        var toConvert = new ConcurrentLinkedQueue<>(cells);
        var converterThreads = new CountDownLatch(threads.asInt());
        var executor = Threads.threadPool("Converter", threads);

        threads.loop(() ->
                executor.execute(() ->
                {
                    try
                    {
                        while (!toConvert.isEmpty())
                        {
                            var worldCell = toConvert.poll();
                            if (worldCell != null)
                            {
                                // We are attempting to convert a cell
                                attempted.increment();
                                var attempt = attempted.get();
                                var prefix = "Cell " + attempt + " of " + cells.count() + ": ";

                                // so create the graph converter
                                var conversion = new Conversion(application, outputFolder, worldCell);
                                LOGGER.listenTo(conversion);

                                // convert the cell, clean cutting to the cell boundary
                                var input = worldCell.pbfFile(outputFolder).materialized(ProgressReporter.none());
                                LOGGER.information("$ Converting $", prefix.trim(), input);

                                // then convert the PBF to a graph
                                var cellGraphFile = worldCell.cellGraphFile(outputFolder);
                                var cellGraph = conversion.convert(input, metadata
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
                                        var size = cellGraph.estimatedMemorySize();
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
        catch (InterruptedException ignored)
        {
        }

        // We're done
        var report = new StringList();
        report.add("Succeeded: $", succeeded);
        report.add("Failed: $", failed);
        report.add(AsciiArt.line());
        report.add("Vertexes: $", vertexes);
        report.add("Edges: $", edges);
        report.add("Relations: $", edgeRelations);
        report.add(AsciiArt.line());
        report.add("Total Size: $", Bytes.bytes(totalSize.asLong()));
        report.add("Conversion: $", start.elapsedSince());
        LOGGER.information(report.titledBox("Conversion Completed"));

        // Return the number of successful conversions
        var statistics = new Statistics();
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
