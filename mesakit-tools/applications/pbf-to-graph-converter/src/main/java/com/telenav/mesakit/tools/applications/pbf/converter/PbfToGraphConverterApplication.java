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

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.collections.list.ObjectList;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.string.AsciiArt;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.filesystem.FileList;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.resource.Extension;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.mesakit.map.region.Region;
import com.telenav.mesakit.map.region.locale.MapLocale;
import com.telenav.mesakit.map.road.name.standardizer.RoadNameStandardizer;

import java.util.List;

import static com.telenav.kivakit.commandline.SwitchParsers.booleanSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParsers.countSwitchParser;
import static com.telenav.kivakit.core.collections.set.ObjectSet.objectSet;
import static com.telenav.kivakit.filesystem.File.fileListArgumentParser;
import static com.telenav.kivakit.filesystem.File.fileSwitchParser;
import static com.telenav.kivakit.filesystem.Folder.folderSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter.relationFilterSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter.wayFilterSwitchParser;
import static com.telenav.mesakit.map.region.Region.regionSwitchParser;

/**
 * Converts PBF files under some data specification to .graph archives. Usage help is available by running the program.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("rawtypes")
public class PbfToGraphConverterApplication extends Application
{
    /**
     * Program entrypoint. Parses command line arguments and switches, constructs application object and calls the run
     * method with {@link CommandLine}
     */
    public static void main(String[] arguments)
    {
        new PbfToGraphConverterApplication().run(arguments);
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT_FILES);
    }

    @Override
    protected void onProjectsInitialized()
    {
        // Load G2 for english in the background
        RoadNameStandardizer.loadInBackground(MapLocale.ENGLISH_UNITED_STATES.get(), RoadNameStandardizer.Mode.MESAKIT_STANDARDIZATION);
    }

    @Override
    protected void onRun()
    {
        // Start the clock,
        var start = Time.now();

        // show application arguments
        showCommandLine();

        // check the input files,
        var inputFiles = argument(INPUT_FILES);
        checkInputFiles(commandLine(), inputFiles);

        // show what will be converted
        showConversionInformation(inputFiles);

        // and if an output folder was specified,
        var outputFolder = get(OUTPUT_FOLDER);
        if (outputFolder != null)
        {
            // then be sure it exists.
            outputFolder.ensureExists();
        }
        else
        {
            outputFolder = inputFiles.first().parent();
        }

        // For each input file,
        var outputFiles = new FileList();
        var conversion = listenTo(new Conversion(this, commandLine(), outputFolder));
        for (var input : inputFiles)
        {
            // if it's a file,
            if (input.isFile())
            {
                // convert just that one file,
                var outputFile = conversion.convert(input);
                if (outputFile != null)
                {
                    outputFiles.add(outputFile);
                }
            }
            else
            {
                // otherwise, we're converting a whole folder
                var folder = input.asFolder();

                // so go through each file in the folder,
                for (var nestedFile : folder.nestedFiles(Extension.OSM_PBF.matcher()))
                {
                    // and convert those files.
                    var outputFile = conversion.convert(nestedFile);
                    if (outputFile != null)
                    {
                        outputFiles.add(outputFile);
                    }
                }
            }
        }

        // Finally, show what new files were built.
        var built = new ObjectList<>();
        for (var graphFile : outputFiles)
        {
            built.append(graphFile.path().absolute());
        }

        announce(AsciiArt.textBox(Strings.format("Built ${debug} graph file(s) in ${debug}:",
                outputFiles.size(), start.elapsedSince()), built.bulleted()));
        information("Successfully converted $ file(s)", outputFiles.size());
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return objectSet(CLEAN_CUT_TO, EXCLUDED_HIGHWAY_TYPES_FILE, FREE_FLOW_SIDE_FILE,
                INCLUDED_HIGHWAY_TYPES_FILE, INCLUDE_TAGS, INCLUDE_FULL_NODE_INFORMATION, OUTPUT_FOLDER,
                PARALLEL_READER, REGION_INFORMATION, RELATION_FILTER, SPEED_PATTERN_FILE,
                TRACE_COUNTS_SIDE_FILE, TURN_RESTRICTIONS_SIDE_FILE, VERIFY, WAY_FILTER, QUIET);
    }

    /**
     * Ensures that input files exist
     */
    private void checkInputFiles(CommandLine commandLine, FileList inputFiles)
    {
        for (var input : inputFiles)
        {
            // and make sure each exists
            if (!input.exists())
            {
                // or give up
                commandLine.exit("Input file or folder does not exist: " + input);
            }
        }
    }

    /**
     * Shows what files will be converted
     */
    private void showConversionInformation(FileList inputFiles)
    {
        information(AsciiArt.textBox("Converting PBF Files", AsciiArt.bulleted(inputFiles)));
    }

    final ArgumentParser<FileList> INPUT_FILES =
            fileListArgumentParser(this, "The comma separated list of input PBF file(s) and/or folders to process", Extension.OSM_PBF)
                    .required()
                    .build();

    final SwitchParser<WayFilter> WAY_FILTER =
            wayFilterSwitchParser(this)
                    .required()
                    .build();

    final SwitchParser<RelationFilter> RELATION_FILTER =
            relationFilterSwitchParser(this)
                    .required()
                    .build();

    final SwitchParser<? extends Region> CLEAN_CUT_TO =
            regionSwitchParser(this, "clean-cut-to", "True to cut edges cleanly at the given border")
                    .optional()
                    .build();

    final SwitchParser<File> EXCLUDED_HIGHWAY_TYPES_FILE =
            fileSwitchParser(this, "excluded-highway-types", "A text file containing excluded highway types (one per line)")
                    .optional()
                    .build();

    final SwitchParser<File> FREE_FLOW_SIDE_FILE =
            fileSwitchParser(this, "free-flow-side-file", "The file to load free flow from")
                    .optional()
                    .build();

    final SwitchParser<File> INCLUDED_HIGHWAY_TYPES_FILE =
            fileSwitchParser(this, "included-highway-types", "A text file containing included highway types (one per line)")
                    .optional()
                    .build();

    final SwitchParser<Boolean> INCLUDE_TAGS =
            booleanSwitchParser(this, "include-tags", "True to include tags even if the data specification doesn't normally include them")
                    .optional()
                    .defaultValue(false)
                    .build();

    final SwitchParser<Boolean> INCLUDE_FULL_NODE_INFORMATION =
            booleanSwitchParser(this, "include-full-node-information", "True to include shape point information for Cygnus")
                    .optional()
                    .defaultValue(false)
                    .build();

    final SwitchParser<Folder> OUTPUT_FOLDER =
            folderSwitchParser(this, "output-folder", "Folder in which the output files are to be saved")
                    .optional()
                    .build();

    final SwitchParser<Boolean> PARALLEL_READER =
            booleanSwitchParser(this, "parallel", "True to use the parallel PBF reader")
                    .optional()
                    .defaultValue(false)
                    .build();

    final SwitchParser<Count> THREADS =
            countSwitchParser(this, "threads", "The number of threads to use when using the parallel PBF reader")
                    .optional()
                    .defaultValue(Count._4)
                    .build();

    final SwitchParser<Boolean> REGION_INFORMATION =
            booleanSwitchParser(this, "region-information", "Include region information (expensive in OSM)")
                    .optional()
                    .defaultValue(true)
                    .build();

    final SwitchParser<File> SPEED_PATTERN_FILE =
            fileSwitchParser(this, "speed-pattern-file", "The file to load speed pattern from")
                    .optional()
                    .build();

    final SwitchParser<File> TRACE_COUNTS_SIDE_FILE =
            fileSwitchParser(this, "trace-counts-side-file", "The file to load probe counts from")
                    .optional()
                    .build();

    final SwitchParser<File> TURN_RESTRICTIONS_SIDE_FILE =
            fileSwitchParser(this, "turn-restrictions-side-file", "The file to load turn restrictions from")
                    .optional()
                    .build();

    final SwitchParser<Boolean> VERIFY =
            booleanSwitchParser(this, "verify", "Verify the written graph against the in memory graph")
                    .optional()
                    .defaultValue(false)
                    .build();
}
