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

import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingRectangle;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.road.model.RoadFunctionalClass;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapDot;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.AnnotationTheme;

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

    private final AnnotationTheme theme = new AnnotationTheme();

    protected Renderer(final MapCanvas canvas, final ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    protected void callout(final Location at, final MapDot dot, final Style style, final String text)
    {
        for (int dx = 30; dx < 100; dx += 10)
        {
            for (int dy = -30; dy > -100; dy -= 10)
            {
                final var drawAt = canvas().toDrawing(at).plus(dx, dy);

                final var margin = 5.0;
                final var bounds = DrawingRectangle.rectangle(drawAt, canvas().textSize(style, text));
                final var planned = new Rectangle2D.Double(bounds.x() - margin,
                        bounds.y() - margin,
                        bounds.width() + 2 * margin,
                        bounds.height() + 2 * margin);

                if (!model().intersectsDrawnRectangle(planned))
                {
                    dot.withLocation(at).draw(canvas());

                    final var connectPoint = new Point2D.Double(bounds.x(), bounds.y() + bounds.height() / 2);
                    theme.lineCallout()
                            .withFrom(at)
                            .withTo(canvas().toMap(connectPoint))
                            .draw(canvas());

                    final var shape = theme.labelAnnotation(text)
                            .withLocation(drawAt)
                            .draw(canvas());
                    model().drawn(shape.getBounds());
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
