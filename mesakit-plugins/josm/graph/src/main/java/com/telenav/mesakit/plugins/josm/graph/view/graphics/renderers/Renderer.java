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

import com.telenav.kivakit.ui.desktop.graphics.drawing.drawables.Dot;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.polyline.Polyline;
import com.telenav.mesakit.map.geography.shape.rectangle.Width;
import com.telenav.mesakit.map.road.model.RoadFunctionalClass;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import static com.telenav.mesakit.map.road.model.RoadFunctionalClass.FIRST_CLASS;
import static com.telenav.mesakit.map.road.model.RoadFunctionalClass.FOURTH_CLASS;
import static com.telenav.mesakit.map.road.model.RoadFunctionalClass.MAIN;
import static com.telenav.mesakit.map.road.model.RoadFunctionalClass.SECOND_CLASS;
import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.CITY;
import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.NEIGHBORHOOD;
import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.REGION;
import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.STATE;
import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.STREET;

/**
 * Base class for renderers which holds the canvas to draw on and the model to draw.
 *
 * @author jonathanl (shibo)
 */
public abstract class Renderer
{
    private static final Map<MapScale, RoadFunctionalClass> scale = new HashMap<>();

    static
    {
        scale.put(STATE, MAIN);
        scale.put(REGION, MAIN);
        scale.put(CITY, FIRST_CLASS);
        scale.put(NEIGHBORHOOD, SECOND_CLASS);
        scale.put(STREET, FOURTH_CLASS);
    }

    private final MapCanvas canvas;

    private final ViewModel model;

    protected Renderer(final MapCanvas canvas, final ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    protected void callout(final Location at, final Dot dot, final Style style, final String text)
    {
        for (int dx = 30; dx < 100; dx += 10)
        {
            for (int dy = -30; dy > -100; dy -= 10)
            {
                final var screenLocation = canvas().toDrawing(at).plus(dx, dy);

                final var drawAt = canvas().toMap(screenLocation);

                final var margin = 5.0;
                final var bounds = canvas().labelBounds(style, drawAt, text);
                final var planned = new Rectangle2D.Double(bounds.getX() - margin,
                        bounds.getY() - margin,
                        bounds.getWidth() + 2 * margin,
                        bounds.getHeight() + 2 * margin);

                if (!model().intersectsDrawnRectangle(planned))
                {
                    dot.draw(canvas(), at);

                    final var connectPoint = new Point2D.Double(bounds.getX(), bounds.getY() + bounds.getHeight() / 2);
                    final var color = Styles.BASE.withFillColor(style.draw()).withDrawColor(style.draw());
                    canvas().drawPolyline(color, Width.pixels(2f), color,
                            Width.pixels(1f), Polyline.fromLocations(at, canvas().location(connectPoint)));

                    final var drawn = canvas().drawLabel(style, drawAt,
                            Width.pixels(2f), text);

                    model().drawn(drawn);
                    return;
                }
            }
        }
    }

    protected MapCanvas canvas()
    {
        return canvas;
    }

    protected boolean isVisible(final Edge edge)
    {
        if (model.graphPanel().viewPanel().viewEdges() && model.graphPanel().viewPanel().viewRoadTypes().contains(edge.roadType()))
        {
            return edge.roadFunctionalClass().isMoreImportantThanOrEqual(scale.get(canvas().scale()));
        }
        return false;
    }

    protected ViewModel model()
    {
        return model;
    }
}
