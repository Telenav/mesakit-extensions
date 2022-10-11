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

package com.telenav.mesakit.tools.applications.graph.ways.extractor;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.resource.Extension;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.io.archive.GraphArchive;
import com.telenav.mesakit.graph.io.load.GraphConstraints;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.graph.library.osm.change.io.PbfSaver;

import static com.telenav.kivakit.core.collections.set.ObjectSet.set;
import static com.telenav.kivakit.filesystem.Folders.folderSwitchParser;
import static com.telenav.kivakit.filesystem.Folder.parseFolder;
import static com.telenav.kivakit.resource.compression.archive.ZipArchive.AccessMode.WRITE;
import static com.telenav.mesakit.graph.io.load.SmartGraphLoader.graphSwitchParser;

/**
 * Creates a graph containing only the double-digitized ways in the input graph. If full node information is available,
 * a PBF file is also created.
 *
 * @author jonathanl (shibo)
 */
public class DoubleDigitizedWaysExtractorApplication extends Application
{
    public static void main(String[] arguments)
    {
        new DoubleDigitizedWaysExtractorApplication().run(arguments);
    }

    private final SwitchParser<SmartGraphLoader> GRAPH =
            graphSwitchParser(this)
                    .required()
                    .build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            folderSwitchParser(this, "output", "The output folder")
                    .optional()
                    .build();

    @Override
    protected void onRun()
    {
        var graph = get(GRAPH).load(this);
        var outputFolder = get(OUTPUT_FOLDER);

        // Extract double digitized edges
        var filtered = graph.createConstrained(GraphConstraints.ALL.withEdgeMatcher(Edge::osmIsDoubleDigitized));
        if (filtered != null)
        {
            // Get graph resource path
            var path = graph.resource().path();

            // If no output folder was specified
            if (outputFolder == null)
            {
                // default to the same folder as the input graph
                outputFolder = parseFolder(this, path.parent().toString());
            }

            if (outputFolder != null)
            {
                // Base file
                var base = outputFolder.file(path.fileName().withoutCompoundExtension() + "-double-digitized-ways");

                // Save graph file
                try (var archive = new GraphArchive(this, base.withExtension(Extension.GRAPH), WRITE, ProgressReporter.nullProgressReporter()))
                {
                    filtered.save(archive);
                }

                // If we have full OSM information
                if (graph.supportsFullPbfNodeInformation())
                {
                    // save the filtered graph to PBF format
                    new PbfSaver().save(filtered, base.withExtension(Extension.OSM_PBF));
                }
                else
                {
                    warning("Not creating PBF file because source graph was not created with -osmNodeInformation=true");
                }
            }
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.set(GRAPH, OUTPUT_FOLDER, QUIET);
    }
}
