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

package com.telenav.kivakit.tools.applications.pbf.converter;

import com.telenav.kivakit.application.KivaKitApplication;
import com.telenav.kivakit.data.formats.pbf.processing.filters.*;
import com.telenav.kivakit.graph.project.KivaKitGraphCore;
import com.telenav.kivakit.kernel.commandline.*;
import com.telenav.kivakit.kernel.language.collections.list.ObjectList;
import com.telenav.kivakit.kernel.language.string.Strings;
import com.telenav.kivakit.kernel.messaging.Message;
import com.telenav.kivakit.kernel.scalars.counts.Count;
import com.telenav.kivakit.kernel.time.Time;
import com.telenav.kivakit.map.region.Region;
import com.telenav.kivakit.map.region.locale.MapLocale;
import com.telenav.kivakit.map.road.name.standardizer.RoadNameStandardizer;
import com.telenav.kivakit.resource.path.Extension;

import java.util.List;
import java.util.Set;

/**
 * Converts PBF files under some data specification to .graph archives. Usage help is available by running the program.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("rawtypes")
public class PbfToGraphConverterApplication extends KivaKitApplication
{
    /**
     * Program entrypoint. Parses command line arguments and switches, constructs application object and calls the run
     * method with {@link CommandLine}
     */
    public static void main(final String[] arguments)
    {
        new PbfToGraphConverterApplication().run(arguments);
    }

    final ArgumentParser<FileList> INPUT_FILES =
            FileList.argumentParser("The comma separated list of input PBF file(s) and/or folders to process", Extension.OSM_PBF)
                    .required()
                    .build();

    final SwitchParser<WayFilter> WAY_FILTER =
            WayFilter.wayFilter()
                    .required()
                    .build();

    final SwitchParser<RelationFilter> RELATION_FILTER =
            RelationFilter.relationFilter()
                    .required()
                    .build();

    final SwitchParser<? extends Region> CLEAN_CUT_TO =
            Region.regionSwitchParser("clean-cut-to", "True to cut edges cleanly at the given border")
                    .optional()
                    .build();

    final SwitchParser<File> EXCLUDED_HIGHWAY_TYPES_FILE =
            File.switchParser("excluded-highway-types", "A text file containing excluded highway types (one per line)")
                    .optional()
                    .build();

    final SwitchParser<File> FREE_FLOW_SIDE_FILE =
            File.switchParser("free-flow-side-file", "The file to load free flow from")
                    .optional()
                    .build();

    final SwitchParser<File> INCLUDED_HIGHWAY_TYPES_FILE =
            File.switchParser("included-highway-types", "A text file containing included highway types (one per line)")
                    .optional()
                    .build();

    final SwitchParser<Boolean> INCLUDE_TAGS =
            SwitchParser.booleanSwitch("include-tags", "True to include tags even if the data specification doesn't normally include them")
                    .optional()
                    .defaultValue(false)
                    .build();

    final SwitchParser<Boolean> INCLUDE_FULL_NODE_INFORMATION =
            SwitchParser.booleanSwitch("include-full-node-information", "True to include shape point information for Cygnus")
                    .optional()
                    .defaultValue(false)
                    .build();

    final SwitchParser<Folder> OUTPUT_FOLDER =
            Folder.switchParser("output-folder", "Folder in which the output files are to be saved")
                    .optional()
                    .build();

    final SwitchParser<Boolean> PARALLEL_READER =
            SwitchParser.booleanSwitch("parallel", "True to use the parallel PBF reader")
                    .optional()
                    .defaultValue(false)
                    .build();

    final SwitchParser<Count> THREADS =
            Count.switchParser("threads", "The number of threads to use when using the parallel PBF reader")
                    .optional()
                    .defaultValue(Count._4)
                    .build();

    final SwitchParser<Boolean> REGION_INFORMATION =
            SwitchParser.booleanSwitch("region-information", "Include region information (expensive in OSM)")
                    .optional()
                    .defaultValue(true)
                    .build();

    final SwitchParser<File> SPEED_PATTERN_FILE =
            File.switchParser("speed-pattern-file", "The file to load speed pattern from")
                    .optional()
                    .build();

    final SwitchParser<File> TRACE_COUNTS_SIDE_FILE =
            File.switchParser("trace-counts-side-file", "The file to load probe counts from")
                    .optional()
                    .build();

    final SwitchParser<File> TURN_RESTRICTIONS_SIDE_FILE =
            File.switchParser("turn-restrictions-side-file", "The file to load turn restrictions from")
                    .optional()
                    .build();

    final SwitchParser<Boolean> VERIFY =
            SwitchParser.booleanSwitch("verify", "Verify the written graph against the in memory graph")
                    .optional()
                    .defaultValue(false)
                    .build();

    private PbfToGraphConverterApplication()
    {
        super(KivaKitGraphCore.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT_FILES);
    }

    @Override
    protected void onProjectInitialized()
    {
        // Load G2 for english in the background
        RoadNameStandardizer.loadInBackground(MapLocale.ENGLISH_UNITED_STATES.get(), RoadNameStandardizer.Mode.G2_STANDARDIZATION);
    }

    @Override
    protected void onRun()
    {
        // Start the clock,
        final var start = Time.now();

        // show application arguments
        announce();

        // check the input files,
        final var inputFiles = argument(INPUT_FILES);
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
        final var outputFiles = new FileList();
        final var conversion = listenTo(new Conversion(this, commandLine(), outputFolder));
        for (final var input : inputFiles)
        {
            // if it's a file,
            if (input.isFile())
            {
                // convert just that one file,
                final var outputFile = conversion.convert(input);
                if (outputFile != null)
                {
                    outputFiles.add(outputFile);
                }
            }
            else
            {
                // otherwise we're converting a whole folder
                final var folder = input.asFolder();

                // so go through each file in the folder,
                for (final var nestedFile : folder.nestedFiles(Extension.OSM_PBF.fileMatcher()))
                {
                    // and convert those files.
                    final var outputFile = conversion.convert(nestedFile);
                    if (outputFile != null)
                    {
                        outputFiles.add(outputFile);
                    }
                }
            }
        }

        // Finally, show what new files were built.
        final var built = new ObjectList<>();
        for (final var graphFile : outputFiles)
        {
            built.append(graphFile.path().asAbsolute());
        }

        announce(Strings.textBox(Message.format("Built ${debug} graph file(s) in ${debug}:",
                outputFiles.size(), start.elapsedSince()), built.bulleted()));
        information("Successfully converted $ file(s)", outputFiles.size());
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(CLEAN_CUT_TO, EXCLUDED_HIGHWAY_TYPES_FILE, FREE_FLOW_SIDE_FILE,
                INCLUDED_HIGHWAY_TYPES_FILE, INCLUDE_TAGS, INCLUDE_FULL_NODE_INFORMATION, OUTPUT_FOLDER,
                PARALLEL_READER, REGION_INFORMATION, RELATION_FILTER, SPEED_PATTERN_FILE,
                TRACE_COUNTS_SIDE_FILE, TURN_RESTRICTIONS_SIDE_FILE, VERIFY, WAY_FILTER, QUIET);
    }

    /**
     * Ensures that input files exist
     */
    private void checkInputFiles(final CommandLine commandLine, final FileList inputFiles)
    {
        for (final var input : inputFiles)
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
    private void showConversionInformation(final FileList inputFiles)
    {
        information(Strings.textBox("Converting PBF Files", inputFiles.bulleted()));
    }
}
