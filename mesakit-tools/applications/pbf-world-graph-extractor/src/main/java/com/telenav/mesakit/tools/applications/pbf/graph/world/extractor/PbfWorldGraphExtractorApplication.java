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

package com.telenav.mesakit.tools.applications.pbf.graph.world.extractor;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.resource.CopyMode;
import com.telenav.kivakit.settings.Deployment;
import com.telenav.mesakit.graph.GraphProject;
import com.telenav.mesakit.graph.Metadata;
import com.telenav.mesakit.graph.specifications.library.pbf.PbfDataSourceFactory;
import com.telenav.mesakit.graph.world.WorldGraph;
import com.telenav.mesakit.graph.world.WorldGraphConfiguration;
import com.telenav.mesakit.graph.world.WorldGraphDeployments;
import com.telenav.mesakit.graph.world.repository.WorldGraphRepository;
import com.telenav.mesakit.graph.world.repository.WorldGraphRepositoryFolder;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.osm.OsmNavigableWayFilter;
import com.telenav.mesakit.tools.applications.pbf.graph.world.extractor.conversion.WorldConversion;

import java.util.List;

import static com.telenav.kivakit.commandline.SwitchParsers.booleanSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParsers.enumSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParsers.threadCountSwitchParser;
import static com.telenav.kivakit.core.collections.set.ObjectSet.objectSet;
import static com.telenav.kivakit.filesystem.Files.fileArgumentParser;
import static com.telenav.kivakit.filesystem.Files.fileSwitchParser;
import static com.telenav.kivakit.resource.Extension.GRAPH;
import static com.telenav.mesakit.graph.specifications.library.pbf.PbfDataSourceFactory.Type.PARALLEL_READER;
import static com.telenav.mesakit.graph.specifications.library.pbf.PbfDataSourceFactory.Type.SERIAL_READER;
import static com.telenav.mesakit.graph.world.repository.WorldGraphRepository.worldGraphRepositorySwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter.relationFilterSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter.wayFilterSwitchParser;

/**
 * Application to split an input PBF file into "world graph", which is a grid of sub-graphs in cells of a given width
 * and height
 *
 * @author jonathanl (shibo)
 */
public class PbfWorldGraphExtractorApplication extends Application
{
    public static void main(String[] arguments)
    {
        new PbfWorldGraphExtractorApplication().run(arguments);
    }

    private enum Mode
    {
        EXTRACT,
        CONVERT
    }

    /** The deployment (see com.telenav.kivakit.graph.world.configuration) */
    private SwitchParser<Deployment> DEPLOYMENT;

    /** The input PBF resource to split into a grid */
    private final ArgumentParser<File> INPUT =
            fileArgumentParser(this, "The PBF file to split into a new world graph")
                    .required()
                    .build();

    private final SwitchParser<Mode> MODE =
            enumSwitchParser(this, "mode", "TimeFormat of operation (extract or convert)", Mode.class)
                    .optional()
                    .defaultValue(Mode.EXTRACT)
                    .build();

    private final SwitchParser<Boolean> OVERWRITE =
            booleanSwitchParser(this, "overwrite", "True to overwrite any existing graph")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> PROFILE_FORCE_LOAD =
            booleanSwitchParser(this, "profile-force-load", "True to profile force-loading")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> WARN =
            booleanSwitchParser(this, "warn", "False to suppress detailed warnings")
                    .optional()
                    .defaultValue(true)
                    .build();

    /** The destination world grid repository to populate */
    private final SwitchParser<WorldGraphRepository> WORLD_GRAPH_REPOSITORY =
            worldGraphRepositorySwitchParser("World graph repository folder in which to install the extracted world graph")
                    .optional()
                    .build();

    public final SwitchParser<File> EXCLUDED_HIGHWAY_TYPES_FILE =
            fileSwitchParser(this, "excluded-highway-types", "A text file containing excluded highway types (one per line)")
                    .optional()
                    .build();

    public final SwitchParser<File> FREE_FLOW_SIDE_FILE =
            fileSwitchParser(this, "free-flow-side-file", "The file to load free flow from")
                    .optional()
                    .build();

    public final SwitchParser<File> INCLUDED_HIGHWAY_TYPES_FILE =
            fileSwitchParser(this, "included-highway-types", "A text file containing included highway types (one per line)")
                    .optional()
                    .build();

    public final SwitchParser<Boolean> REGION_INFORMATION =
            booleanSwitchParser(this, "region-information", "Include region information (expensive in OSM)")
                    .optional()
                    .defaultValue(true)
                    .build();

    /** Filter for relations */
    public final SwitchParser<RelationFilter> RELATION_FILTER =
            relationFilterSwitchParser(this)
                    .required()
                    .build();

    /** Filter for ways */
    public final SwitchParser<WayFilter> WAY_FILTER =
            wayFilterSwitchParser(this)
                    .optional()
                    .defaultValue(new OsmNavigableWayFilter())
                    .build();

    final SwitchParser<Boolean> PARALLEL =
            booleanSwitchParser(this, "parallel", "True to use the parallel PBF reader")
                    .optional()
                    .defaultValue(false)
                    .build();

    /** Number of threads to use when extracting and converting */
    final SwitchParser<Count> THREADS = threadCountSwitchParser(this, Count.count(24));

    public final SwitchParser<Boolean> VERIFY =
            booleanSwitchParser(this, "verify", "True to verify output graphs")
                    .optional()
                    .defaultValue(false)
                    .build();

    /** The input speed pattern file to compile into each cell */
    public final SwitchParser<File> SPEED_PATTERN_FILE =
            fileSwitchParser(this, "speed-pattern", "The speed pattern file to compile into each cell")
                    .optional()
                    .build();

    private PbfWorldGraphExtractorApplication()
    {
        addProject(GraphProject.class);
    }

    @Override
    public void onRun()
    {
        // Start time
        var start = Time.now();

        // Install deployment
        registerSettingsIn(get(DEPLOYMENT));

        // Ensure this works later
        get(WAY_FILTER);

        // Get input resource
        var input = argument(INPUT);
        var metadata = Metadata.metadata(input);
        if (metadata == null)
        {
            exit("Unable get metadata for $", input);
            return;
        }

        // Get number of threads to use
        var threads = get(THREADS);

        // Get mode (extract-and-convert or convert only)
        var mode = get(MODE);

        // Get the repository install folder
        var repositoryInstallFolder = repositoryInstallFolder(commandLine(), metadata, mode);

        // Get the local repository
        var localRepository = configuration().localRepository();

        // Get temporary and local install folders
        var localRepositoryInstallFolder = localRepository.folder(metadata);

        // Create world graph to populate
        var worldGraph = WorldGraph.create(localRepositoryInstallFolder, metadata);
        var worldGrid = worldGraph.worldGrid();

        // If we're extracting
        if (mode == Mode.EXTRACT)
        {
            // If the local install folder exists and it's not empty
            if (localRepositoryInstallFolder.exists() && !localRepositoryInstallFolder.isEmpty())
            {
                // quit with an error because we'd overwrite existing data
                if (get(OVERWRITE))
                {
                    localRepositoryInstallFolder.clearAll();
                }
                else
                {
                    exit("Local install folder $ exists and is not empty", localRepositoryInstallFolder);
                }
            }

            // get the input file to extract
            if (argumentList().size() != 1)
            {
                exit("Input PBF file is required to extract");
            }
            var speedPattern = get(SPEED_PATTERN_FILE);
            if (speedPattern != null && !speedPattern.exists())
            {
                exit("Speed pattern file doesn't exist! File path: " + speedPattern);
            }

            // and a temporary folder in the local repository
            var localRepositoryTemporaryFolder = localRepository.temporaryFolder();
            if (localRepositoryTemporaryFolder != null)
            {
                // and extract PBF cells into the local repository temporary folder
                var factory = listenTo(new PbfDataSourceFactory(input,
                        get(THREADS), get(PARALLEL) ? PARALLEL_READER : SERIAL_READER));
                var extracted = worldGrid.extract(localRepositoryTemporaryFolder,
                        () -> factory.map(metadata));

                // and if anything was extracted,
                if (extracted.isNonZero())
                {
                    // convert the extracted PBF files
                    var conversion = new WorldConversion();
                    var statistics = conversion.convert(this, worldGrid, commandLine(), metadata, localRepositoryTemporaryFolder, threads);

                    // and if we succeeded
                    if (statistics.succeeded().isNonZero())
                    {
                        // then write the world graph index file
                        worldGrid.saveIndex(localRepositoryTemporaryFolder, statistics.metadata(metadata));

                        // and rename the temporary folder in order to install the data locally.
                        information("Renaming $ to $", localRepositoryTemporaryFolder, localRepositoryInstallFolder);
                        localRepositoryInstallFolder.clearAllAndDelete();
                        localRepositoryTemporaryFolder.renameTo(localRepositoryInstallFolder);

                        // If there's a repository folder to copy the installed data to
                        if (repositoryInstallFolder != null)
                        {
                            // then copy the data (both PBFs and graphs)
                            localRepositoryInstallFolder.safeCopyTo(repositoryInstallFolder, CopyMode.UPDATE, ProgressReporter.nullProgressReporter());
                        }
                    }
                }
            }
        }
        else if (mode == Mode.CONVERT)
        {
            // We're only converting PBF to graph files in-place
            var statistics = new WorldConversion().convert(this, worldGrid, commandLine(), metadata, localRepositoryInstallFolder, threads);

            // If some PBFs were converted
            if (statistics.succeeded().isNonZero())
            {
                // then write the world graph index file
                worldGraph.worldGrid().saveIndex(localRepositoryInstallFolder, statistics.metadata(metadata));

                // and there's a repository folder to copy to
                if (repositoryInstallFolder != null)
                {
                    // then copy just the new graphs to the repository install folder.
                    localRepositoryInstallFolder.copyTo(repositoryInstallFolder, CopyMode.UPDATE, GRAPH.matcher(),
                            BroadcastingProgressReporter.createProgressReporter(this, "bytes"));
                    localRepositoryInstallFolder.copyTo(repositoryInstallFolder, CopyMode.UPDATE, WorldGraphRepositoryFolder.WORLD.matcher(),
                            BroadcastingProgressReporter.createProgressReporter(this, "bytes"));
                }
            }
        }

        if (get(PROFILE_FORCE_LOAD))
        {
            var startLoad = Time.now();
            var forceLoad = WorldGraph.load(localRepositoryInstallFolder);
            forceLoad.loadAll();
            information("Force-loaded world graph in $", startLoad.elapsedSince());
        }

        // We're done
        information("Completed in $", start.elapsedSince());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        // The deployments can now be created since the graph core is initialized, which means that
        // environment variable expansions are now possible, like ${mesakit.graph.folder} in particular
        DEPLOYMENT = new WorldGraphDeployments(this).switchParser("deployment")
                .required()
                .build();

        return objectSet(WARN,
                MODE,
                PARALLEL,
                THREADS,
                DEPLOYMENT,
                WORLD_GRAPH_REPOSITORY,
                SPEED_PATTERN_FILE,
                VERIFY,
                PROFILE_FORCE_LOAD,
                EXCLUDED_HIGHWAY_TYPES_FILE,
                INCLUDED_HIGHWAY_TYPES_FILE,
                FREE_FLOW_SIDE_FILE,
                REGION_INFORMATION,
                RELATION_FILTER,
                WAY_FILTER,
                OVERWRITE,
                QUIET);
    }

    private WorldGraphConfiguration configuration()
    {
        return require(WorldGraphConfiguration.class);
    }

    /**
     * Returns the repository install folder specified on the command line
     */
    private WorldGraphRepositoryFolder repositoryInstallFolder(CommandLine commandLine, Metadata metadata,
                                                               Mode mode)
    {
        // Get any repository that was specified
        var repository = commandLine.get(WORLD_GRAPH_REPOSITORY);

        // If a repository was specified
        if (repository != null)
        {
            // then get the install folder for the specified data date
            var installFolder = repository.folder(metadata);
            installFolder.mkdirs();

            // If we're extracting from scratch and the install folder exists and its not empty,
            if (mode == Mode.EXTRACT && !installFolder.isEmpty())
            {
                // we don't want to overwrite existing data, so it is an error
                commandLine.exit("Cannot install in non-empty repository folder " + installFolder);
            }

            // Create the install folder
            installFolder.mkdirs();

            // and return it
            return installFolder;
        }

        return null;
    }
}
