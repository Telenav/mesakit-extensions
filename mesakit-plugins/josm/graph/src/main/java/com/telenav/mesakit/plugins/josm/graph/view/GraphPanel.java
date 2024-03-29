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

package com.telenav.mesakit.plugins.josm.graph.view;

import com.telenav.kivakit.core.project.ProjectTrait;
import com.telenav.kivakit.core.time.Duration;
import com.telenav.kivakit.ui.desktop.component.Components;
import com.telenav.kivakit.ui.desktop.component.status.StatusDisplay;
import com.telenav.kivakit.ui.desktop.component.status.StatusPanel;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Fonts;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.kivakit.ui.desktop.theme.vanhelsing.KivaKitVanHelsingTheme;
import com.telenav.mesakit.core.MesaKit;
import com.telenav.mesakit.graph.Vertex;
import com.telenav.mesakit.plugins.josm.graph.GraphPlugin;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.query.QueryPanel;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.routing.RoutingPanel;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.search.SearchPanel;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.tags.TagPanel;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.view.ViewPanel;
import com.telenav.mesakit.plugins.josm.library.BaseJosmPanel;
import org.openstreetmap.josm.gui.layer.Layer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.Collections;

/**
 * The JOSM panel showing graph information and providing search functionality.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings({ "ClassEscapesDefinedScope", "unused" })
public class GraphPanel extends BaseJosmPanel implements
        StatusDisplay,
        ProjectTrait
{
    static
    {
        KivaKitTheme.set(new KivaKitVanHelsingTheme());
    }

    private JTabbedPane tabbedPane;

    private ViewPanel viewPanel;

    private SearchPanel searchPanel;

    private QueryPanel queryPanel;

    private RoutingPanel routingPanel;

    private TagPanel tagPanel;

    private GraphLayer layer;

    private StatusPanel statusPanel;

    public GraphPanel(GraphPlugin plugin)
    {
        super(plugin);

        createLayout(container(), false, Collections.emptyList());

        status(project(MesaKit.class).projectVersion() + " - " + project(MesaKit.class).build());

        SwingUtilities.invokeLater(() ->
                Components.children(this, component -> component.setFont(Fonts.component(12))));
    }

    public void html(String message, Object... arguments)
    {
        searchPanel().html(message, arguments);
    }

    @Override
    public GraphLayer layer()
    {
        return layer;
    }

    public void layer(GraphLayer layer)
    {
        this.layer = layer;
        tagPanel().layer(layer);
    }

    public QueryPanel queryPanel()
    {
        if (queryPanel == null)
        {
            queryPanel = new QueryPanel(this);
        }
        return queryPanel;
    }

    public RoutingPanel routingPanel()
    {
        if (routingPanel == null)
        {
            routingPanel = new RoutingPanel(this);
        }
        return routingPanel;
    }

    public SearchPanel searchPanel()
    {
        if (searchPanel == null)
        {
            searchPanel = new SearchPanel(this);
        }
        return searchPanel;
    }

    public Vertex selectedVertex()
    {
        return layer().model().selection().selectedVertex();
    }

    @Override
    public void status(Duration stayFor, String message, Object... arguments)
    {
        statusPanel.status(stayFor, message, arguments);
    }

    @Override
    public void status(String message, Object... arguments)
    {
        statusPanel.status(Duration.seconds(10), message, arguments);
    }

    public JTabbedPane tabbedPane()
    {
        if (tabbedPane == null)
        {
            tabbedPane = new JTabbedPane();
            tabbedPane.setForeground(KivaKitColors.DARK_CHARCOAL.asAwtColor());
            tabbedPane.addTab("home", searchPanel());
            tabbedPane.addTab("query", queryPanel());
            tabbedPane.addTab("view", viewPanel());
            tabbedPane.addTab("tags", tagPanel());
            tabbedPane.addTab("routing", routingPanel());
        }
        return tabbedPane;
    }

    public TagPanel tagPanel()
    {
        if (tagPanel == null)
        {
            tagPanel = new TagPanel(this);
        }
        return tagPanel;
    }

    public void text(String message, Object... arguments)
    {
        searchPanel().text(message, arguments);
    }

    public ViewPanel viewPanel()
    {
        if (viewPanel == null)
        {
            viewPanel = new ViewPanel(this);
        }
        return viewPanel;
    }

    @Override
    protected void onActiveLayerChanged()
    {
        tagPanel().layer(layer().activeLayer());
    }

    @Override
    protected void onLayerRemoving(Layer layer)
    {
        if (layer() == null)
        {
            hidePanel();
        }
        else
        {
            refresh();
        }
    }

    @Override
    protected void onLayerReorder()
    {
        refresh();
    }

    @Override
    protected void onRefresh()
    {
        super.onRefresh();
        layer().forceRepaint();
    }

    private JPanel container()
    {
        var container = new JPanel();
        container.setLayout(new BorderLayout());
        container.add(tabbedPane(), BorderLayout.CENTER);
        statusPanel = new StatusPanel(StatusPanel.Display.NO_HEALTH_PANEL);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        container.add(statusPanel, BorderLayout.SOUTH);
        return container;
    }
}
