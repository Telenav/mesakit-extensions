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

import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.measurements.DrawingWidth;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapPolyline;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.EdgeTheme;

/**
 * Draws edges in the appropriate color for zoom level
 *
 * @author jonathanl (shibo)
 */
public class EdgeRenderer extends Renderer
{
    private final ShapePointRenderer shapePointRenderer;

    private final EdgeTheme theme = new EdgeTheme();

    public EdgeRenderer(final MapCanvas canvas, final ViewModel model)
    {
        super(canvas, model);

        shapePointRenderer = new ShapePointRenderer(canvas, model);
    }

    /**
     * Draws edges of the given selection type
     */
    public void draw(final Selection.Type type)
    {
        // Go through the visible edges of the given selection type
        for (final var edge : model().visibleEdges().edges(type))
        {
            // and if the edge is of that type
            if (model().selection().is(edge, type))
            {
                // draw the 'from' and 'to' vertexes if it's selected,
                if (type == Selection.Type.SELECTED)
                {
                    new VertexRenderer(canvas(), model()).draw(edge);
                }

                // and draw the edge
                draw(edge, type);
            }
        }
    }

    private void draw(final Edge edge, final Selection.Type type)
    {
        // Draw the edge polyline
        final var line = polyline(canvas(), type, edge);
        final var shape = line.draw(canvas());

        // store the shape of the edge in the selection model
        model().selection().shape(edge, shape);

        // and draw the edge's shape points
        final var selectedShapePoint = model().selection().selectedShapePoint();
        if (model().selection().isSelected(edge) || (type == Selection.Type.SELECTED && selectedShapePoint != null))
        {
            if (canvas().scale().isZoomedIn(MapScale.CITY))
            {
                shapePointRenderer.draw(edge);
            }
        }
    }

    private MapPolyline polyline(final MapCanvas canvas, final Selection.Type type, final Edge edge)
    {
        final var polyline = theme.polylineEdge(canvas, type, edge);
        if (canvas.scale().isZoomedOut(MapScale.REGION))
        {
            return polyline.withDrawStrokeWidth(DrawingWidth.pixels(type == Selection.Type.HIGHLIGHTED ? 4 : 2));
        }
        return polyline;
    }
}
