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

package com.telenav.tdk.tools.applications.pbf.region.extractor;

import com.telenav.tdk.core.application.TdkApplication;
import com.telenav.tdk.core.filesystem.*;
import com.telenav.tdk.core.kernel.commandline.*;
import com.telenav.tdk.core.kernel.scalars.counts.Count;
import com.telenav.tdk.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.tdk.data.formats.pbf.processing.readers.SerialPbfReader;
import com.telenav.tdk.map.cutter.PbfRegionCutter;
import com.telenav.tdk.map.region.*;
import com.telenav.tdk.map.region.project.TdkMapRegion;

import java.util.*;

/**
 * Extracts regions from a PBF file into an output folder.
 *
 * @author jonathanl (shibo)
 */
public class PbfRegionExtractorApplication extends TdkApplication
{
    private static final SwitchParser<Count> THREADS = Count.threadCountSwitchParser(Count._16);

    public static void main(final String[] arguments)
    {
        new PbfRegionExtractorApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            File.argumentParser("The input PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            Folder.OUTPUT
                    .optional()
                    .build();

    private final SwitchParser<RegionSet> EXTRACT =
            Region.regionListSwitchParser("extract", "Comma separated list of region patterns to extract")
                    .optional()
                    .build();

    private final SwitchParser<RegionSet> EXTRACT_UNDER =
            Region.regionListSwitchParser("extractUnder", "Comma separated list of region patterns to extract under (includes the region itself)")
                    .optional()
                    .build();

    private final SwitchParser<WayFilter> WAY_FILTER =
            WayFilter.wayFilter()
                    .required()
                    .build();

    protected PbfRegionExtractorApplication()
    {
        super(TdkMapRegion.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @Override
    protected void onRun()
    {
        // Get input file
        final var input = argument(0, INPUT);
        if (!input.exists())
        {
            exit("Input file or folder does not exist: " + input);
        }

        // Get arguments
        final var outputFolder = get(OUTPUT_FOLDER, input.parent());
        final var wayFilter = get(WAY_FILTER);
        final var extract = get(EXTRACT);
        final var extractUnder = get(EXTRACT_UNDER);

        // Validate consistency
        if (has(EXTRACT) && has(EXTRACT_UNDER))
        {
            exit("The switches -extractOnly and -extractUnder are mutually exclusive");
        }

        // Extract the regions
        final var reader = listenTo(new SerialPbfReader(input));
        final var extractor = new PbfRegionCutter(() -> reader, outputFolder, wayFilter);
        if (extract != null)
        {
            extractor.regionsToExtract(extract);
        }
        if (extractUnder != null)
        {
            extractor.regionsToExtract(extractUnder.under());
        }
        extractor.extract();
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(OUTPUT_FOLDER, EXTRACT, EXTRACT_UNDER, WAY_FILTER, THREADS, QUIET);
    }
}
