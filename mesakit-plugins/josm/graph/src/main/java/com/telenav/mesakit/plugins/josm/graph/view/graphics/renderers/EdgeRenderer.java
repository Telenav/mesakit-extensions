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

import com.telenav.kivakit.kernel.language.values.level.Percent;
import com.telenav.kivakit.ui.desktop.graphics.drawing.drawables.Line;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Road;
import com.telenav.mesakit.map.geography.shape.rectangle.Width;
import com.telenav.mesakit.map.road.model.RoadType;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;

import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.INACTIVE;

/**
 * Draws edges in the appropriate color for zoom level
 *
 * @author jonathanl (shibo)
 */
public class EdgeRenderer extends Renderer
{
    public static Line fattened(final Line line, final Edge edge)
    {
        switch (edge.roadFunctionalClass())
        {
            case MAIN:
                return line.fattened(Percent.of(200));

            case FIRST_CLASS:
                if (edge.roadType() == RoadType.HIGHWAY)
                {
                    return line.fattened(Percent.of(100));
                }
                else
                {
                    return line.fattened(Percent.of(50));
                }

            case SECOND_CLASS:
                return line.fattened(Percent.of(30));

            case THIRD_CLASS:
                return line.fattened(Percent.of(5));

            case UNKNOWN:
            case FOURTH_CLASS:
            default:
                return line;
        }
    }

    public static Line fattenedAndFilled(final MapCanvas canvas, final Selection.Type type, final Edge edge)
    {
        switch (type)
        {
            case INACTIVE:
                return fattened(INACTIVE, edge);

            case HIGHLIGHTED:
                return fattened(HIGHLIGHTED, edge);

            case SELECTED:
                return fattened(SELECTED, edge);

            case UNSELECTED:
                break;

            default:
                throw new IllegalArgumentException();
        }

        final var line = fattened(NORMAL, edge);

        final Color color;

        final var zoomedIn = canvas.scale().isZoomedIn(Scale.CITY);
        switch (edge.roadFunctionalClass())
        {
            case MAIN:
                color = zoomedIn ? Road.FREEWAY : Road.FREEWAY_ZOOMED_OUT;
                break;

            case FIRST_CLASS:
                if (edge.roadType() == RoadType.HIGHWAY)
                {
                    color = zoomedIn ? Road.HIGHWAY : Road.HIGHWAY_ZOOMED_OUT;
                }
                else
                {
                    color = zoomedIn ? Road.FIRST_CLASS : Road.FIRST_CLASS_ZOOMED_OUT;
                }
                break;

            case SECOND_CLASS:
                color = Road.SECOND_CLASS;
                break;

            case THIRD_CLASS:
                color = Road.THIRD_CLASS;
                break;

            case FOURTH_CLASS:
                color = Road.FOURTH_CLASS;
                break;

            case UNKNOWN:
            default:
                color = KivaKitColors.UNSPECIFIED;
                break;
        }
        return line.withFill(color);
    }

    private final ShapePointRenderer shapePointRenderer;

    public EdgeRenderer(final MapCanvas canvas, final ViewModel model)
    {
        super(canvas, model);

        shapePointRenderer = new ShapePointRenderer(canvas, model);
    }

    /**
     * Draws edges of the given selection type
     */
    public void draw(final Type type)
    {
        // Go through the visible edges of the given selection type
        for (final var edge : model().visibleEdges().edges(type))
        {
            // and if the edge is of that type
            if (model().selection().is(edge, type))
            {
                // draw the 'from' and 'to' vertexes if it's selected,
                if (type == Type.SELECTED)
                {
                    new VertexRenderer(canvas(), model()).draw(edge);
                }

                // and draw the edge
                draw(edge, type);
            }
        }
    }

    private static Line line(final MapCanvas canvas, final Type type, final Edge edge)
    {
        final var line = fattenedAndFilled(canvas, type, edge);
        if (canvas.scale().isZoomedOut(Scale.REGION))
        {
            return line.withWidth(Width.pixels(type == Type.HIGHLIGHTED ? 4f : 2f))
                    .withOutlineStyle(Styles.TRANSPARENT)
                    .withOutlineWidth(Width.pixels(0f));
        }
        return line;
    }

    private void draw(final Edge edge, final Type type)
    {
        // Draw the edge polyline
        final var line = line(canvas(), type, edge);
        final var shape = line.draw(canvas(), edge.roadShape());

        // store the shape of the edge in the selection model
        model().selection().shape(edge, shape);

        // and draw the edge's shape points
        final var selectedShapePoint = model().selection().selectedShapePoint();
        if (model().selection().isSelected(edge) || (type == Type.SELECTED && selectedShapePoint != null))
        {
            if (canvas().scale().isZoomedIn(Scale.CITY))
            {
                shapePointRenderer.draw(edge);
            }
        }
    }
}
