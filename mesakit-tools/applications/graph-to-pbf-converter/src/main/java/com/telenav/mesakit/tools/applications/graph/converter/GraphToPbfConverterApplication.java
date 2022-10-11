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
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.mesakit.graph.GraphProject;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.graph.io.save.PbfGraphSaver;

import java.util.ArrayList;
import java.util.List;

import static com.telenav.kivakit.core.collections.set.ObjectSet.set;
import static com.telenav.kivakit.filesystem.Files.fileArgumentParser;
import static com.telenav.kivakit.filesystem.Folders.folderSwitchParser;
import static com.telenav.kivakit.resource.Extension.GRAPH;
import static com.telenav.kivakit.resource.Extension.OSM_PBF;

/**
 * Converts a graph back to a PBF. This is only going to produce the exact original PBF file if the graph contains full
 * node information.
 *
 * @author jonathanl (shibo)
 */
public class GraphToPbfConverterApplication extends Application
{
    public static void main(String[] arguments)
    {
        new GraphToPbfConverterApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            fileArgumentParser(this, "The graph to convert to PBF")
                    .required()
                    .build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            folderSwitchParser(this, "output-folder", "Folder in which the output files are to be saved")
                    .optional()
                    .build();

    private final List<File> converted = new ArrayList<>();

    private final List<File> materialized = new ArrayList<>();

    public GraphToPbfConverterApplication()
    {
        addProject(GraphProject.class);
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @Override
    protected void onRun()
    {
        var input = argument(INPUT);
        var outputFolder = get(OUTPUT_FOLDER);

        // If an output folder was specified,
        if (outputFolder != null)
        {
            // then be sure it exists
            outputFolder.ensureExists();
        }

        // If we're converting just one file
        var start = Time.now();
        if (input.isFile())
        {
            convertOne(outputFolder, input.parent(), input);
        }
        else
        {
            // otherwise we're converting a whole folder
            var folder = input.asFolder();

            // so go through each input file in the folder,
            for (var file : folder.nestedFiles(GRAPH.matcher()))
            {
                convertOne(outputFolder, folder, file);
            }
        }

        // Dematerialize converted files
        for (var file : materialized)
        {
            file.dematerialize();
        }

        information("Built ${debug} PBF file(s) in ${debug}:", converted.size(), start.elapsedSince());
        for (var file : converted)
        {
            information(" - $", file.path().asAbsolute());
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.set(OUTPUT_FOLDER, QUIET);
    }

    private void convertOne(Folder outputFolder, Folder folder, File input)
    {
        // Materialize the input file
        materialized.add(input);

        // Determine the output file
        var output = output(folder, input, outputFolder);

        // Load the graph
        var graph = new SmartGraphLoader(input).load(this);

        // Convert from Graph to PBF
        new PbfGraphSaver().save(graph, output);
        converted.add(output);
    }

    private File output(Folder folder, File input, Folder outputFolder)
    {
        File outputFile;
        if (outputFolder == null)
        {
            outputFile = input.withoutCompoundExtension().withExtension(OSM_PBF);
        }
        else
        {
            outputFile = outputFolder.file(input.relativeTo(folder)).withoutCompoundExtension()
                    .withExtension(OSM_PBF);
        }
        outputFile.parent().ensureExists();
        return outputFile;
    }
}
