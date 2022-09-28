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

package com.telenav.mesakit.plugins.josm.graph;

import com.telenav.kivakit.core.logging.Logger;
import com.telenav.kivakit.core.logging.LoggerFactory;
import com.telenav.kivakit.core.messaging.listeners.MessageList;
import com.telenav.kivakit.core.progress.ProgressListener;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.string.AsciiArt;
import com.telenav.kivakit.core.value.level.Percent;
import com.telenav.kivakit.core.value.mutable.MutableValue;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.resource.compression.archive.ZipArchive;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.plugins.josm.graph.view.GraphLayer;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

import java.io.IOException;

import static com.telenav.kivakit.core.messaging.Message.Status.COMPLETED;

/**
 * Imports a Graph file, creating a GraphLayer in JOSM.
 *
 * @author jonathanl (shibo)
 */
public class GraphFileImporter extends FileImporter
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private final GraphPlugin plugin;

    public GraphFileImporter(GraphPlugin plugin)
    {
        super(new ExtensionFileFilter("graph,txd,txd.gz", "graph", "Graph Files (*.graph, *.txd, *.txd.gz)"));

        this.plugin = plugin;
    }

    @Override
    public void importData(java.io.File file, ProgressMonitor progressMonitor) throws IOException
    {
        try
        {
            var input = File.file(file);
            if (ZipArchive.is(LOGGER, input))
            {
                var messages = new MessageList(message -> message.isWorseThan(COMPLETED));
                var reporter = BroadcastingProgressReporter.createProgressReporter();
                progressMonitor.beginTask("Loading MesaKit graph '" + input.baseName() + "'", 100);
                var previous = new MutableValue<>(0);
                reporter.listener(workListener(progressMonitor, previous));
                var graph = new SmartGraphLoader(input).load(messages, reporter);
                if (graph != null)
                {
                    progressMonitor.worked(100 - previous.get());
                    var metadata = graph.metadata();
                    var layer = (GraphLayer) plugin.createLayer(plugin.name() + " " + metadata.descriptor() + " (" + file.getName() + ")");
                    layer.graph(graph, ProgressReporter.none());
                    LOGGER.information("Loaded graph '$':\n$", graph.name(), metadata);
                    layer.add();

                    plugin.panel().showPanel();
                    plugin.zoomTo(graph.bounds().expanded(Percent.percent(10)));
                    layer.forceRepaint();
                    plugin.panel().layer(layer);
                }
                else
                {
                    LOGGER.warning(AsciiArt.textBox("Unable to Load Graph", "$\n$", input, messages.formatted().bulleted()));
                }
            }
            else
            {
                throw new IOException("Not a graph archive: " + file);
            }
        }
        catch (Throwable e)
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

    private ProgressListener workListener(ProgressMonitor progressMonitor, MutableValue<Integer> previous)
    {
        return at ->
        {
            var current = at.asInt();
            var worked = current - previous.get();
            previous.set(current);
            progressMonitor.worked(worked);
        };
    }
}
