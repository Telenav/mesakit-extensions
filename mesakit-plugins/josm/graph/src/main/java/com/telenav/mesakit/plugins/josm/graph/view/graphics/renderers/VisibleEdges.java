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

package com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers;

import com.telenav.kivakit.language.count.Maximum;
import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingPoint;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.road.model.RoadType;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;

import java.util.List;

import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.CITY;
import static com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.VisibleEdges.Mode.RELATIONS;

/**
 * @author jonathanl (shibo)
 */
public class VisibleEdges
{
    private static final int MAXIMUM_RENDERED_EDGES = 2_000;

    public enum Mode
    {
        EDGES,
        RELATIONS
    }

    private final MapCanvas canvas;

    private final ViewModel model;

    private final List<Edge> edges;

    private final Mode mode;

    private final Distance smallest;

    public VisibleEdges(MapCanvas canvas, ViewModel model, Mode mode)
    {
        this.canvas = canvas;
        this.model = model;
        this.mode = mode;

        // We don't want to draw any tiny edges. An edge should be at least a few pixels wide.
        smallest = canvas.toMap(DrawingPoint.pixels(0, 0)).distanceTo(canvas.toMap(DrawingPoint.pixels(4, 0)));

        if (canvas.scale().atOrFurtherThan(MapScale.REGION))
        {
            edges = model.forwardEdges(edge -> edge.roadType() == RoadType.FREEWAY
                    || edge.roadType() == RoadType.URBAN_HIGHWAY).asList();
        }
        else
        {
            // Get all of the forward edges that could possibly be drawn
            edges = model.forwardEdges(this::couldBeVisible).asList();

            // While there are too many edges,
            var type = RoadType.WALKWAY;
            while (edges.size() > MAXIMUM_RENDERED_EDGES && type != null)
            {
                // remove the least important ones
                var remove = type;
                edges.removeIf(edge -> edge.roadType().isEqualOrLessImportantThan(remove));
                type = type.nextMostImportant();
            }
        }
    }

    public EdgeSet edges()
    {
        return EdgeSet.forCollection(Maximum.MAXIMUM, edges);
    }

    public EdgeSet edges(Selection.Type type)
    {
        var selected = model.selection().selectedEdge();
        switch (type)
        {
            case UNSELECTED:
            case INACTIVE:
            {
                var edges = edges();
                if (selected != null && selected.isTwoWay())
                {
                    edges.add(selected.reversed());
                }
                return edges;
            }

            case HIGHLIGHTED:
            {
                return EdgeSet.forCollection(Maximum.MAXIMUM, model.selection().highlightedEdges());
            }

            case SELECTED:
            {
                if (selected == null)
                {
                    return EdgeSet.EMPTY;
                }
                else
                {
                    var edges = EdgeSet.of(selected);
                    if (selected.isTwoWay())
                    {
                        edges.add(selected.reversed());
                    }
                    return edges;
                }
            }

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * @return True if the edge could be visible
     */
    private boolean couldBeVisible(Edge edge)
    {
        // If edges are enabled and the specific kind of edge is enable,
        if (mode == RELATIONS || model.graphPanel().viewPanel().viewEdges() && model.graphPanel().viewPanel().viewRoadTypes().contains(edge.roadType()))
        {
            // and the edge is long enough
            if (isLongEnough(edge))
            {
                // then the edge is visible
                return true;
            }

            // If the edge is small but we're zoomed out
            if (canvas.scale().isZoomedOut(CITY))
            {
                // we should include the edge anyway if the neighbors are also too small to avoid gaps.
                return !isLongEnough(edge.next()) && !isLongEnough(edge.previous());
            }
        }
        return false;
    }

    private boolean isLongEnough(Edge edge)
    {
        return edge != null && edge.length().isGreaterThan(smallest);
    }
}
