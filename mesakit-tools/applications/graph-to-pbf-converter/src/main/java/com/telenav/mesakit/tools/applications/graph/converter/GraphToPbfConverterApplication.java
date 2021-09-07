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

package com.telenav.mesakit.tools.applications.graph.converter;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.kernel.language.collections.set.ObjectSet;
import com.telenav.kivakit.kernel.language.time.Time;
import com.telenav.kivakit.resource.path.Extension;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.graph.io.save.PbfGraphSaver;
import com.telenav.mesakit.graph.GraphProject;

import java.util.ArrayList;
import java.util.List;

import static com.telenav.kivakit.filesystem.File.fileArgumentParser;
import static com.telenav.kivakit.filesystem.Folder.folderSwitchParser;

/**
 * Converts a graph back to a PBF. This is only going to produce the exact original PBF file if the graph contains full
 * node information.
 *
 * @author jonathanl (shibo)
 */
public class GraphToPbfConverterApplication extends Application
{
    public static void main(final String[] arguments)
    {
        new GraphToPbfConverterApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            fileArgumentParser("The graph to convert to PBF")
                    .required()
                    .build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            folderSwitchParser("output-folder", "Folder in which the output files are to be saved")
                    .optional()
                    .build();

    private final List<File> materialized = new ArrayList<>();

    private final List<File> converted = new ArrayList<>();

    public GraphToPbfConverterApplication()
    {
        super(GraphProject.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @Override
    protected void onRun()
    {
        final var input = argument(INPUT);
        final var outputFolder = get(OUTPUT_FOLDER);

        // If an output folder was specified,
        if (outputFolder != null)
        {
            // then be sure it exists
            outputFolder.ensureExists();
        }

        // If we're converting just one file
        final var start = Time.now();
        if (input.isFile())
        {
            convertOne(outputFolder, input.parent(), input);
        }
        else
        {
            // otherwise we're converting a whole folder
            final var folder = input.asFolder();

            // so go through each input file in the folder,
            for (final var file : folder.nestedFiles(Extension.GRAPH.fileMatcher()))
            {
                convertOne(outputFolder, folder, file);
            }
        }

        // Dematerialize converted files
        for (final var file : materialized)
        {
            file.dematerialize();
        }

        information("Built ${debug} PBF file(s) in ${debug}:", converted.size(), start.elapsedSince());
        for (final var file : converted)
        {
            information(" - $", file.path().absolute());
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.of(OUTPUT_FOLDER, QUIET);
    }

    private void convertOne(final Folder outputFolder, final Folder folder, final File input)
    {
        // Materialize the input file
        materialized.add(input);

        // Determine the output file
        final var output = output(folder, input, outputFolder);

        // Load the graph
        final var graph = new SmartGraphLoader(input).load(this);

        // Convert from Graph to PBF
        new PbfGraphSaver().save(graph, output);
        converted.add(output);
    }

    private File output(final Folder folder, final File input, final Folder outputFolder)
    {
        final File outputFile;
        if (outputFolder == null)
        {
            outputFile = input.withoutCompoundExtension().withExtension(Extension.OSM_PBF);
        }
        else
        {
            outputFile = outputFolder.file(input.relativeTo(folder)).withoutCompoundExtension()
                    .withExtension(Extension.OSM_PBF);
        }
        outputFile.parent().ensureExists();
        return outputFile;
    }
}
