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

package com.telenav.mesakit.tools.applications.graph.slicer;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.resource.compression.codecs.GzipCodec;
import com.telenav.kivakit.resource.path.Extension;
import com.telenav.mesakit.graph.GraphProject;
import com.telenav.mesakit.graph.io.archive.GraphArchive;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;

import static com.telenav.kivakit.core.project.Project.resolveProject;
import static com.telenav.kivakit.filesystem.File.fileSwitchParser;
import static com.telenav.kivakit.resource.compression.archive.ZipArchive.Mode.WRITE;
import static com.telenav.mesakit.graph.io.load.SmartGraphLoader.graphSwitchParser;
import static com.telenav.mesakit.map.geography.shape.rectangle.Rectangle.rectangleSwitchParser;

/**
 * Slice a graph to a given rectangle
 *
 * @author matthieun
 * @author jonathanl (shibo)
 */
public class GraphSlicerApplication extends Application
{
    public static void main(String[] arguments)
    {
        new GraphSlicerApplication().run(arguments);
    }

    private final SwitchParser<SmartGraphLoader> GRAPH_RESOURCE =
            graphSwitchParser(this, "graph", "The input graph to slice")
                    .required()
                    .build();

    private final SwitchParser<File> OUTPUT =
            fileSwitchParser(this, "output", "The output file")
                    .required()
                    .build();

    private final SwitchParser<Rectangle> BOUNDS =
            rectangleSwitchParser(this, "bounds", "The rectangle to slice to as minimumLatitude,minimumLongitude:maximumLatitude,maximumLongitude")
                    .required()
                    .build();

    protected GraphSlicerApplication()
    {
        super(resolveProject(GraphProject.class));
    }

    @Override
    protected void onRun()
    {
        var graph = get(GRAPH_RESOURCE).load();
        var bounds = get(BOUNDS);
        var output = get(OUTPUT);
        if (output.extension().endsWith(Extension.GZIP))
        {
            output = output.withCodec(new GzipCodec());
        }
        var clipped = graph.clippedTo(bounds);
        information("clipped to $, producing $", bounds, clipped.asString());
        try (var archive = new GraphArchive(this, output, WRITE, ProgressReporter.none()))
        {
            clipped.save(archive);
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(GRAPH_RESOURCE, OUTPUT, BOUNDS, QUIET);
    }
}
