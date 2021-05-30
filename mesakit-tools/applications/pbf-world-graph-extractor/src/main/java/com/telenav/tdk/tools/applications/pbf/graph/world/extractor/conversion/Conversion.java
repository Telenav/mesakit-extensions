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

package com.telenav.tdk.tools.applications.pbf.graph.world.extractor.conversion;

import com.telenav.tdk.core.filesystem.*;
import com.telenav.tdk.core.kernel.commandline.CommandLine;
import com.telenav.tdk.core.kernel.debug.Debug;
import com.telenav.tdk.core.kernel.language.string.Strings;
import com.telenav.tdk.core.kernel.language.vm.JavaVirtualMachine;
import com.telenav.tdk.core.kernel.logging.*;
import com.telenav.tdk.core.kernel.messaging.Message;
import com.telenav.tdk.core.kernel.messaging.repeaters.BaseRepeater;
import com.telenav.tdk.core.kernel.operation.progress.ProgressReporter;
import com.telenav.tdk.core.kernel.operation.progress.reporters.Progress;
import com.telenav.tdk.core.kernel.scalars.counts.Maximum;
import com.telenav.tdk.core.kernel.time.Time;
import com.telenav.tdk.core.kernel.validation.Validate;
import com.telenav.tdk.core.resource.path.Extension;
import com.telenav.tdk.data.formats.pbf.processing.filters.*;
import com.telenav.tdk.data.formats.pbf.processing.filters.osm.OsmNavigableWayFilter;
import com.telenav.tdk.graph.*;
import com.telenav.tdk.graph.io.archive.GraphArchive;
import com.telenav.tdk.graph.specifications.common.edge.EdgeAttributes;
import com.telenav.tdk.graph.specifications.common.graph.loader.*;
import com.telenav.tdk.graph.specifications.library.pbf.PbfDataAnalysis;
import com.telenav.tdk.graph.specifications.osm.graph.converter.OsmPbfToGraphConverter;
import com.telenav.tdk.graph.world.grid.WorldCell;
import com.telenav.tdk.map.geography.rectangle.Rectangle;
import com.telenav.tdk.map.region.*;
import com.telenav.tdk.tools.applications.pbf.graph.world.extractor.PbfWorldGraphExtractorApplication;

import static com.telenav.tdk.core.resource.compression.archive.ZipArchive.Mode.*;
import static com.telenav.tdk.graph.specifications.common.graph.loader.PbfToGraphConverter.Configuration;

/**
 * Performs a single conversion from PBF to graph.
 *
 * @author jonathanl (shibo)
 */
public class Conversion extends BaseRepeater<Message>
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private static final Debug DEBUG = new Debug(LOGGER);

    private final Folder outputFolder;

    private final WorldCell worldCell;

    private final PbfWorldGraphExtractorApplication application;

    public Conversion(final PbfWorldGraphExtractorApplication application, final Folder outputFolder,
                      final WorldCell worldCell)
    {
        this.application = application;
        this.outputFolder = outputFolder;
        this.worldCell = worldCell;
    }

    /**
     * @return The graph for the input file
     */
    public Graph convert(File input, final Metadata metadata)
    {
        assert input != null;

        // Materialize the input resource if it's remote (like an HDFS file),
        input = input.materialized(Progress.create(this));
        try
        {
            if (metadata.dataSpecification().supports(EdgeAttributes.get().COUNTRY))
            {
                final var thread = new Thread(() -> Region.type(Country.class).loadBorders());
                thread.setPriority(3);
                thread.start();
            }

            // determine the output file,
            final var output = output(outputFolder, input);

            // create and configure the converter,
            final var configuration = configuration(metadata);
            final var converter = converter(metadata);
            converter.configure(configuration);

            // convert the input file,
            information("Converting $ $ to $", metadata.descriptor(), input, output);
            final var graph = converter.convert(input);
            if (graph == null || graph.edgeCount().isZero())
            {
                warning("Graph conversion failed for $", input);
            }
            else
            {
                // save the graph to disk,
                try (final var archive = new GraphArchive(output, ProgressReporter.NULL, WRITE))
                {
                    final var start = Time.now();
                    information("Saving $", archive);
                    graph.save(archive);
                    information("Saved $ in $", archive, start.elapsedSince());
                }

                // and verify it if we're were asked to.
                if (configuration.verify())
                {
                    try (final var archive = new GraphArchive(output, ProgressReporter.NULL, READ))
                    {
                        final var start = Time.now();
                        information(Strings.topLine("Verifying graph"));
                        final var loaded = archive.load(this);
                        loaded.loadAll();

                        final var comparison = graph.differencesFrom(loaded, Rectangle.MAXIMUM, Maximum._100);
                        if (comparison.isDifferent())
                        {
                            problem("Graph verification failed:\n$", comparison);
                        }
                        information(Strings.bottomLine("Verified graph in $", start.elapsedSince()));
                    }
                }
            }

            return graph;
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
    private Configuration configuration(final Metadata metadata)
    {
        final var loaderConfiguration = PbfGraphLoader.newConfiguration(metadata);

        loaderConfiguration.cleanCutTo(worldCell);
        loaderConfiguration.regionInformation(application.get(application.REGION_INFORMATION));
        loaderConfiguration.wayFilter(wayFilter(application.commandLine()));
        loaderConfiguration.relationFilter(relationFilter(application.commandLine()));

        final var configuration = PbfToGraphConverter.newConfiguration(metadata);

        configuration.loaderConfiguration(loaderConfiguration);
        configuration.freeFlowSideFile(application.get(application.FREE_FLOW_SIDE_FILE));
        configuration.verify(application.get(application.VERIFY));
        configuration.parallel(false);
        configuration.threads(JavaVirtualMachine.local().processors());

        final var speedPatternFile = application.get(application.SPEED_PATTERN_FILE);
        if (speedPatternFile != null && !speedPatternFile.exists())
        {
            application.exit("Speed pattern file doesn't exist! File path: " + speedPatternFile);
        }
        configuration.speedPatternFile(speedPatternFile);

        if (configuration instanceof OsmPbfToGraphConverter.Configuration)
        {
            ((OsmPbfToGraphConverter.Configuration) configuration).analysisType(PbfDataAnalysis.AnalysisType.DEFAULT);
        }

        return configuration;
    }

    /**
     * @return A configured {@link PbfToGraphConverter} configured by the given command line for the data specification
     * supplied by the metadata.
     */
    private PbfToGraphConverter converter(final Metadata metadata)
    {
        final var converter = (PbfToGraphConverter) metadata.dataSpecification().newGraphConverter(metadata);
        final var outer = this;
        converter.broadcastTo(message ->
        {
            // We don't want to show all the validation failures unless DEBUG mode is on
            if (!(message instanceof Validate.ValidateFailure) || DEBUG.isEnabled())
            {
                outer.receive(message);
            }
        });
        return converter;
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
            outputFile = outputFolder.file(input.relativePath(input.parent()))
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
