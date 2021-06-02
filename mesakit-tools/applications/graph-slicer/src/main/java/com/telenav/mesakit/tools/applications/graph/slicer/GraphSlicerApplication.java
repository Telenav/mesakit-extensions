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
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.language.progress.ProgressReporter;
import com.telenav.kivakit.resource.compression.codecs.GzipCodec;
import com.telenav.kivakit.resource.path.Extension;
import com.telenav.mesakit.graph.io.archive.GraphArchive;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.graph.project.GraphCoreProject;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;

import java.util.Set;

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
    public static void main(final String[] arguments)
    {
        new GraphSlicerApplication().run(arguments);
    }

    private final SwitchParser<SmartGraphLoader> GRAPH_RESOURCE =
            graphSwitchParser("graph", "The input graph to slice")
                    .required()
                    .build();

    private final SwitchParser<File> OUTPUT =
            fileSwitchParser("output", "The output file")
                    .required()
                    .build();

    private final SwitchParser<Rectangle> BOUNDS =
            rectangleSwitchParser("bounds", "The rectangle to slice to as minimumLatitude,minimumLongitude:maximumLatitude,maximumLongitude")
                    .required()
                    .build();

    protected GraphSlicerApplication()
    {
        super(GraphCoreProject.get());
    }

    @Override
    protected void onRun()
    {
        final var graph = get(GRAPH_RESOURCE).load();
        final var bounds = get(BOUNDS);
        var output = get(OUTPUT);
        if (output.extension().endsWith(Extension.GZIP))
        {
            output = output.withCodec(new GzipCodec());
        }
        final var clipped = graph.clippedTo(bounds);
        information("clipped to $, producing $", bounds, clipped.asString());
        try (final var archive = new GraphArchive(output, WRITE, ProgressReporter.NULL))
        {
            clipped.save(archive);
        }
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(GRAPH_RESOURCE, OUTPUT, BOUNDS, QUIET);
    }
}
