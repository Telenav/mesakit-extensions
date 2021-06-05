////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.telenav.mesakit.tools.applications.pbf.converter;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.kernel.language.progress.ProgressReporter;
import com.telenav.kivakit.kernel.language.progress.reporters.Progress;
import com.telenav.kivakit.kernel.language.strings.AsciiArt;
import com.telenav.kivakit.kernel.language.time.Time;
import com.telenav.kivakit.kernel.language.values.count.Maximum;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;
import com.telenav.kivakit.kernel.messaging.repeaters.BaseRepeater;
import com.telenav.kivakit.resource.path.Extension;
import com.telenav.mesakit.graph.Metadata;
import com.telenav.mesakit.graph.io.archive.GraphArchive;
import com.telenav.mesakit.graph.specifications.common.edge.EdgeAttributes;
import com.telenav.mesakit.graph.specifications.common.element.GraphElementAttributes;
import com.telenav.mesakit.graph.specifications.common.graph.loader.PbfGraphLoader;
import com.telenav.mesakit.graph.specifications.common.graph.loader.PbfToGraphConverter;
import com.telenav.mesakit.graph.specifications.osm.graph.converter.OsmPbfToGraphConverter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.osm.OsmNavigableWayFilter;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.region.Region;
import com.telenav.mesakit.map.region.regions.Country;

import static com.telenav.kivakit.resource.compression.archive.ZipArchive.Mode.READ;
import static com.telenav.kivakit.resource.compression.archive.ZipArchive.Mode.WRITE;
import static com.telenav.mesakit.graph.specifications.library.pbf.PbfDataAnalysis.AnalysisType.DEFAULT;
import static com.telenav.mesakit.graph.specifications.library.pbf.PbfDataAnalysis.AnalysisType.FULL_NODE_INFORMATION;

/**
 * Performs a single conversion from PBF to graph.
 *
 * @author jonathanl (shibo)
 */
public class Conversion extends BaseRepeater
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private final PbfToGraphConverterApplication application;

    private final CommandLine commandLine;

    private final Folder outputFolder;

    public Conversion(final PbfToGraphConverterApplication application, final CommandLine commandLine,
                      final Folder outputFolder)
    {
        this.application = application;
        this.commandLine = commandLine;
        this.outputFolder = outputFolder;
    }

    /**
     * @return The output file for the input file
     */
    public File convert(File input)
    {
        assert input != null;

        // Materialize the input resource if it's remote (like an HDFS file),
        input = input.materialized(Progress.create(LOGGER));
        try
        {
            // retrieve its metadata,
            final var metadata = Metadata.from(input);
            if (metadata != null)
            {
                // and if the data specification is UniDb and we are not including tags,
                if (metadata.isUniDb() && !commandLine.get(application.INCLUDE_TAGS))
                {
                    // then force the data specification to exclude them
                    metadata.dataSpecification().excludeAttribute(GraphElementAttributes.get().TAGS);
                }

                // and if the data specification wants to look up countries with a spatial index
                if (metadata.dataSpecification().supports(EdgeAttributes.get().COUNTRY))
                {
                    // then get going on loading the border data for countries.
                    final var thread = new Thread(() -> Region.type(Country.class).loadBorders());
                    thread.setPriority(3);
                    thread.start();
                }

                // Next, determine the output file,
                final var output = output(outputFolder, input);

                // create a configuration for the converter,
                final var configuration = configuration(commandLine, metadata);

                // create the converter
                final var converter = converter(metadata);
                converter.configure(configuration);

                // convert the input file,
                information("Converting $ $ to $", metadata.descriptor(), input, output);
                final var graph = converter.convert(input);
                if (graph == null)
                {
                    problem("Graph conversion failed for $", input);
                }
                else
                {
                    // save the graph to disk,
                    try (final var archive = new GraphArchive(this, output, WRITE, ProgressReporter.NULL))
                    {
                        final var start = Time.now();
                        information(AsciiArt.topLine(20, "Saving $", archive));
                        graph.save(archive);
                        information(AsciiArt.bottomLine(20, "Saved $ in $", archive, start.elapsedSince()));
                    }

                    // and verify it if we're were asked to.
                    if (configuration.verify())
                    {
                        try (final var archive = new GraphArchive(this, output, READ, ProgressReporter.NULL))
                        {
                            final var start = Time.now();
                            information(AsciiArt.topLine("Verifying graph"));
                            final var loaded = archive.load(LOGGER);
                            loaded.loadAll();
                            final var comparison = graph.differencesFrom(loaded, Rectangle.MAXIMUM, Maximum._100);
                            if (comparison.isDifferent())
                            {
                                LOGGER.problem("Graph verification failed:\n$", comparison);
                            }
                            information(AsciiArt.bottomLine("Verified graph in $", start.elapsedSince()));
                        }
                    }
                }
                return output;
            }

            problem("$ does not contain metadata. Use the PbfFileMetadataAnnotator application to add it.", input);
            return null;
        }
        finally
        {
            // Remove any local temporary copy of a remote resource
            input.dematerialize();
        }
    }

    /**
     * @return PbfToGraphConverter configuration for command line
     */
    private PbfToGraphConverter.Configuration configuration(final CommandLine commandLine, final Metadata metadata)
    {
        final var loaderConfiguration = PbfGraphLoader.newConfiguration(metadata);

        loaderConfiguration.cleanCutTo(commandLine.get(application.CLEAN_CUT_TO));
        loaderConfiguration.regionInformation(commandLine.get(application.REGION_INFORMATION));
        loaderConfiguration.wayFilter(wayFilter(commandLine));
        loaderConfiguration.relationFilter(relationFilter(commandLine));

        final var configuration = PbfToGraphConverter.newConfiguration(metadata);

        configuration.loaderConfiguration(loaderConfiguration);
        configuration.freeFlowSideFile(commandLine.get(application.FREE_FLOW_SIDE_FILE));
        configuration.turnRestrictionsSideFile(commandLine.get(application.TURN_RESTRICTIONS_SIDE_FILE));
        configuration.verify(commandLine.get(application.VERIFY));
        configuration.parallel(this.commandLine.get(application.PARALLEL_READER));
        configuration.threads(this.commandLine.get(application.THREADS));

        final var speedPatternFile = commandLine.get(application.SPEED_PATTERN_FILE);
        if (speedPatternFile != null && !speedPatternFile.exists())
        {
            commandLine.exit("Speed pattern file doesn't exist! File path: " + speedPatternFile);
        }
        configuration.speedPatternFile(speedPatternFile);

        if (configuration instanceof OsmPbfToGraphConverter.Configuration)
        {
            ((OsmPbfToGraphConverter.Configuration) configuration)
                    .analysisType(commandLine.get(application.INCLUDE_FULL_NODE_INFORMATION) ? FULL_NODE_INFORMATION : DEFAULT);
        }

        return configuration;
    }

    /**
     * @return A configured {@link PbfToGraphConverter} configured by the given command line for the data specification
     * supplied by the metadata.
     */
    private PbfToGraphConverter converter(final Metadata metadata)
    {
        return listenTo((PbfToGraphConverter) metadata.dataSpecification().newGraphConverter(metadata));
    }

    /**
     * @return The output file to write to
     */
    private File output(final Folder outputFolder, final File input)
    {
        final File outputFile;
        if (outputFolder == null)
        {
            outputFile = input.withoutKnownExtensions().withExtension(Extension.GRAPH);
        }
        else
        {
            outputFile = outputFolder.file(input.relativeTo((input.parent())))
                    .withoutKnownExtensions()
                    .withExtension(Extension.GRAPH);
        }
        outputFile.parent().ensureExists();
        return outputFile;
    }

    /**
     * @return A relation filter for the given command line
     */
    private RelationFilter relationFilter(final CommandLine commandLine)
    {
        RelationFilter relationFilter = null;
        if (commandLine.has(application.RELATION_FILTER))
        {
            relationFilter = commandLine.get(application.RELATION_FILTER);
        }
        return relationFilter;
    }

    /**
     * @return A way filter for the given command lines
     */
    private WayFilter wayFilter(final CommandLine commandLine)
    {
        WayFilter wayFilter = new OsmNavigableWayFilter();
        if (commandLine.has(application.EXCLUDED_HIGHWAY_TYPES_FILE))
        {
            final var file = commandLine.get(application.EXCLUDED_HIGHWAY_TYPES_FILE);
            wayFilter = WayFilter.exclude(file.fileName().name(), file);
        }
        if (commandLine.has(application.INCLUDED_HIGHWAY_TYPES_FILE))
        {
            final var file = commandLine.get(application.INCLUDED_HIGHWAY_TYPES_FILE);
            wayFilter = WayFilter.include(file.fileName().name(), file);
        }
        if (commandLine.has(application.WAY_FILTER))
        {
            wayFilter = commandLine.get(application.WAY_FILTER);
        }

        return wayFilter;
    }
}
