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

import com.telenav.kivakit.josm.plugins.graph.model.ViewModel;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.ShapePoint;
import com.telenav.mesakit.graph.specifications.common.shapepoint.HeavyWeightShapePoint;
import com.telenav.mesakit.map.ui.swing.map.graphics.canvas.MapCanvas;

import static com.telenav.kivakit.map.ui.swing.map.theme.MapStyles.ShapePoint.NORMAL;
import static com.telenav.kivakit.map.ui.swing.map.theme.MapStyles.ShapePoint.SELECTED;

/**
 * Draws the shape points of an edge. Shape points are selectable objects with details when a graph supports full node
 * information. When such information is not available, draws the locations in the road shape, but they are not
 * selectable and have no interesting information.
 *
 * @author jonathanl (shibo)
 * @see Graph#supportsFullPbfNodeInformation()
 */
public class ShapePointRenderer
{
    private final MapCanvas canvas;

    private final ViewModel model;

    public ShapePointRenderer(final MapCanvas canvas, final ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw(final Edge edge)
    {
        if (edge.graph().supportsFullPbfNodeInformation())
        {
            var at = 0;
            final var shapePoints = edge.shapePointsWithoutVertexes();
            for (final var point : shapePoints)
            {
                if (at != 0 && at != shapePoints.size() - 1)
                {
                    drawShapePoint(point);
                }
                at++;
            }
        }
        else
        {
            var at = 0;
            final var shape = edge.roadShape();
            for (final var location : shape.locationSequence())
            {
                if (at != 0 && at != shape.size() - 1)
                {
                    final var point = new HeavyWeightShapePoint(model.graph(), 1L);
                    point.index(1);
                    point.location(location);
                    drawShapePoint(point);
                }
                at++;
            }
        }
    }

    private void drawShapePoint(final ShapePoint point)
    {
        final var selected = model.selection().isSelected(point);
        final var dot = selected ? SELECTED : NORMAL;
        final var shape = dot.draw(canvas, point.location());
        if (model.graph().supportsFullPbfNodeInformation())
        {
            model.selection().shape(point, shape);
        }
    }
}
