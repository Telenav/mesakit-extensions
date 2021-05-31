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

package com.telenav.kivakit.josm.plugins.graph;

import com.telenav.kivakit.graph.project.KivaKitGraphCore;
import com.telenav.kivakit.josm.plugins.graph.view.GraphLayer;
import com.telenav.kivakit.josm.plugins.graph.view.GraphPanel;
import com.telenav.kivakit.josm.plugins.library.BaseJosmPanel;
import com.telenav.kivakit.josm.plugins.library.BaseJosmPlugin;
import com.telenav.kivakit.map.region.Region;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.PluginInformation;

import java.awt.event.KeyEvent;

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

        ExtensionFileFilter.addImporter(new GraphFileImporter(this));

        // Initialize the graph api
        KivaKitGraphCore.get().install();

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
