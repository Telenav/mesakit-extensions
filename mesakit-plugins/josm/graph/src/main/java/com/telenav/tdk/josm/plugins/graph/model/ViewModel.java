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

package com.telenav.tdk.josm.plugins.graph.model;

import com.telenav.kivakit.josm.plugins.graph.view.GraphPanel;
import com.telenav.kivakit.josm.plugins.graph.view.graphics.renderers.VisibleEdges;
import com.telenav.kivakit.kernel.interfaces.comparison.Matcher;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.collections.EdgeSequence;
import com.telenav.mesakit.graph.world.WorldGraph;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;

import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;

/**
 * The model of the graph to render. Includes information about the bounds of the graph and what is selected as well as
 * the areas that have been drawn.
 *
 * @author jonathanl (shibo)
 */
public class ViewModel
{
    private Graph graph;

    private final GraphPanel graphPanel;

    private Rectangle bounds;

    private final Selection selection = new Selection();

    private final Set<Rectangle2D> drawnRectangles = new HashSet<>();

    private VisibleEdges visibleEdges;

    private boolean debug;

    private boolean activeLayer;

    public ViewModel(final GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;
    }

    public void activeLayer(final boolean activeLayer)
    {
        this.activeLayer = activeLayer;
    }

    public final Rectangle bounds()
    {
        return bounds;
    }

    public void bounds(final Rectangle bounds)
    {
        this.bounds = bounds;
    }

    public void clearDrawnRectangles()
    {
        drawnRectangles.clear();
    }

    public void debug(final boolean debug)
    {
        this.debug = debug;
    }

    public boolean debug()
    {
        return debug;
    }

    public void drawn(final Rectangle2D drawn)
    {
        final var expansion = 20;
        final Rectangle2D expanded = new Rectangle2D.Double(drawn.getX() - expansion, drawn.getY() - expansion,
                drawn.getWidth() + 2 * expansion, drawn.getHeight() + 2 * expansion);
        drawnRectangles.add(expanded);
    }

    public EdgeSequence forwardEdges(final Matcher<Edge> matcher)
    {
        return graph().forwardEdgesIntersecting(bounds(), matcher);
    }

    public Graph graph()
    {
        return graph;
    }

    public void graph(final Graph graph)
    {
        this.graph = graph;
    }

    public GraphPanel graphPanel()
    {
        return graphPanel;
    }

    public boolean intersectsDrawnRectangle(final Rectangle2D rectangle)
    {
        for (final var drawn : drawnRectangles)
        {
            if (drawn.intersects(rectangle))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isActiveLayer()
    {
        return activeLayer;
    }

    public boolean isWorldGraph()
    {
        return graph instanceof WorldGraph;
    }

    public Selection selection()
    {
        return selection;
    }

    public VisibleEdges visibleEdges()
    {
        return visibleEdges;
    }

    public void visibleEdges(final VisibleEdges visibleEdges)
    {
        this.visibleEdges = visibleEdges;
    }
}
