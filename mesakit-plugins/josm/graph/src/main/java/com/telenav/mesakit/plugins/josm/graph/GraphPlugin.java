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

import com.telenav.mesakit.graph.project.GraphCoreProject;
import com.telenav.mesakit.map.region.Region;
import com.telenav.mesakit.plugins.josm.graph.view.GraphLayer;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;
import com.telenav.mesakit.plugins.josm.library.BaseJosmPanel;
import com.telenav.mesakit.plugins.josm.library.BaseJosmPlugin;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.PluginInformation;

import java.awt.event.KeyEvent;

import static org.openstreetmap.josm.actions.ExtensionFileFilter.addImporter;

/**
 * The main plugin class. Adds the file importer, initializes the Graph API and creates new layers and panels.
 *
 * @author jonathanl (shibo)
 */
public class GraphPlugin extends BaseJosmPlugin
{
    public GraphPlugin(final PluginInformation info)
    {

        super(info, GraphLayer.class);

        addImporter(new GraphFileImporter(this));

        // Initialize the graph api
        GraphCoreProject.get().initialize();

        // Force boundaries to load or it causes UI pauses later
        Region.loadBordersInBackground();
    }

    @Override
    public String iconName()
    {
        return "graph.png";
    }

    @Override
    public String name()
    {
        return "TDK Graph";
    }

    @Override
    public GraphPanel panel()
    {
        return (GraphPanel) super.panel();
    }

    @Override
    public GraphLayer selectedLayer()
    {
        return (GraphLayer) super.selectedLayer();
    }

    @Override
    protected GraphLayer newLayer(final String name)
    {
        return new GraphLayer(this, name);
    }

    @Override
    protected BaseJosmPanel newPanel()
    {
        return new GraphPanel(this);
    }

    @Override
    protected void onLayerRemoving(final Layer layer)
    {
        super.onLayerRemoving(layer);
        if (layer instanceof GraphLayer)
        {
            final var graphLayer = (GraphLayer) layer;
            graphLayer.graph().close();
        }
    }

    @Override
    protected int shortCutKey()
    {
        return KeyEvent.VK_G;
    }
}
