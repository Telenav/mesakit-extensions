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

package com.telenav.mesakit.tools.applications.pbf.region.extractor;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.mesakit.map.cutter.PbfRegionCutter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.readers.SerialPbfReader;
import com.telenav.mesakit.map.region.RegionProject;
import com.telenav.mesakit.map.region.RegionSet;

import java.util.List;

import static com.telenav.kivakit.commandline.SwitchParsers.threadCountSwitchParser;
import static com.telenav.kivakit.core.project.Project.resolveProject;
import static com.telenav.kivakit.filesystem.File.fileArgumentParser;
import static com.telenav.kivakit.filesystem.Folder.outputFolderSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter.wayFilterSwitchParser;
import static com.telenav.mesakit.map.region.Region.regionListSwitchParser;

/**
 * Extracts regions from a PBF file into an output folder.
 *
 * @author jonathanl (shibo)
 */
public class PbfRegionExtractorApplication extends Application
{
    public static void main(String[] arguments)
    {
        new PbfRegionExtractorApplication().run(arguments);
    }

    private final SwitchParser<Count> THREADS = threadCountSwitchParser(this, Count._16);

    private final ArgumentParser<File> INPUT =
            fileArgumentParser(this, "The input PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            outputFolderSwitchParser(this)
                    .optional()
                    .build();

    private final SwitchParser<RegionSet> EXTRACT =
            regionListSwitchParser(this, "extract", "Comma separated list of region patterns to extract")
                    .optional()
                    .build();

    private final SwitchParser<RegionSet> EXTRACT_UNDER =
            regionListSwitchParser(this, "extractUnder", "Comma separated list of region patterns to extract under (includes the region itself)")
                    .optional()
                    .build();

    private final SwitchParser<WayFilter> WAY_FILTER =
            wayFilterSwitchParser(this)
                    .required()
                    .build();

    protected PbfRegionExtractorApplication()
    {
        super(resolveProject(RegionProject.class));
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
        var input = argument(0, INPUT);
        if (!input.exists())
        {
            exit("Input file or folder does not exist: " + input);
        }

        // Get arguments
        var outputFolder = get(OUTPUT_FOLDER, input.parent());
        var wayFilter = get(WAY_FILTER);
        var extract = get(EXTRACT);
        var extractUnder = get(EXTRACT_UNDER);

        // Validate consistency
        if (has(EXTRACT) && has(EXTRACT_UNDER))
        {
            exit("The switches -extractOnly and -extractUnder are mutually exclusive");
        }

        // Extract the regions
        var reader = listenTo(new SerialPbfReader(input));
        var extractor = new PbfRegionCutter(() -> reader, outputFolder, wayFilter);
        if (extract != null)
        {
            extractor.regionsToExtract(extract);
        }
        if (extractUnder != null)
        {
            extractor.regionsToExtract(extractUnder.under());
        }
        extractor.cut();
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(OUTPUT_FOLDER, EXTRACT, EXTRACT_UNDER, WAY_FILTER, THREADS, QUIET);
    }
}
