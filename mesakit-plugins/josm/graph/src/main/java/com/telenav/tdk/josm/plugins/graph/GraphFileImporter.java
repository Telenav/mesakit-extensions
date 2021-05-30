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

package com.telenav.tdk.josm.plugins.graph;

import com.telenav.tdk.core.filesystem.File;
import com.telenav.tdk.core.kernel.language.string.Strings;
import com.telenav.tdk.core.kernel.logging.*;
import com.telenav.tdk.core.kernel.messaging.messages.MessageList;
import com.telenav.tdk.core.kernel.operation.progress.*;
import com.telenav.tdk.core.kernel.operation.progress.reporters.Progress;
import com.telenav.tdk.core.kernel.scalars.counts.Maximum;
import com.telenav.tdk.core.kernel.scalars.levels.Percentage;
import com.telenav.tdk.core.kernel.scalars.mutable.MutableValue;
import com.telenav.tdk.core.resource.compression.archive.ZipArchive;
import com.telenav.tdk.graph.io.load.SmartGraphLoader;
import com.telenav.tdk.josm.plugins.graph.view.GraphLayer;
import org.jetbrains.annotations.NotNull;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

import java.io.IOException;

import static com.telenav.tdk.core.kernel.messaging.Message.Status.COMPLETED;

/**
 * Imports a Graph file, creating a GraphLayer in JOSM.
 *
 * @author jonathanl (shibo)
 */
public class GraphFileImporter extends FileImporter
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private final GraphPlugin plugin;

    public GraphFileImporter(final GraphPlugin plugin)
    {
        super(new ExtensionFileFilter("graph,txd,txd.gz", "graph", "Graph Files (*.graph, *.txd, *.txd.gz)"));

        this.plugin = plugin;
    }

    @Override
    public void importData(final java.io.File file, final ProgressMonitor progressMonitor) throws IOException
    {
        try
        {
            final var input = new File(file);
            if (ZipArchive.is(input))
            {
                final var messages = new MessageList<>(Maximum._100, message -> message.isWorseThan(COMPLETED));
                final var reporter = Progress.create();
                progressMonitor.beginTask("Loading TDK graph '" + input.baseName() + "'", 100);
                final var previous = new MutableValue<>(0);
                reporter.listener(workListener(progressMonitor, previous));
                final var graph = new SmartGraphLoader(input).load(messages, reporter);
                if (graph != null)
                {
                    progressMonitor.worked(100 - previous.get());
                    final var metadata = graph.metadata();
                    final var layer = (GraphLayer) plugin.createLayer(plugin.name() + " " + metadata.descriptor() + " (" + file.getName() + ")");
                    layer.graph(graph, ProgressReporter.NULL);
                    LOGGER.information("Loaded graph '$':\n$", graph.name(), metadata);
                    layer.add();

                    plugin.panel().showPanel();
                    plugin.zoomTo(graph.bounds().expanded(new Percentage(10)));
                    layer.forceRepaint();
                    plugin.panel().layer(layer);
                }
                else
                {
                    LOGGER.warning(Strings.textBox("Unable to Load Graph", "$\n$", input, messages.formatted().bulleted()));
                }
            }
            else
            {
                throw new IOException("Not a graph archive: " + file);
            }
        }
        catch (final Throwable e)
        {
            e.printStackTrace();
            throw new IOException("Unable to open " + file, e);
        }
    }

    @Override
    public boolean isBatchImporter()
    {
        return false;
    }

    @NotNull
    private ProgressListener workListener(final ProgressMonitor progressMonitor, final MutableValue<Integer> previous)
    {
        return at ->
        {
            final var current = at.asInt();
            final var worked = current - previous.get();
            previous.set(current);
            progressMonitor.worked(worked);
        };
    }
}
