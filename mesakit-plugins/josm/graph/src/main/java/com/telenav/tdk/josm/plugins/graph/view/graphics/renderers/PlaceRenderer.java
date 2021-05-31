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

package com.telenav.tdk.josm.plugins.graph.view.graphics.renderers;

import com.telenav.kivakit.josm.plugins.graph.model.Selection.Type;
import com.telenav.kivakit.josm.plugins.graph.model.ViewModel;
import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.kernel.scalars.levels.Percent;
import com.telenav.mesakit.graph.Place;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.ui.swing.map.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.swing.map.graphics.canvas.Scale;
import com.telenav.mesakit.map.ui.swing.map.graphics.canvas.Width;
import com.telenav.mesakit.map.ui.swing.map.graphics.drawables.Dot;
import com.telenav.mesakit.map.ui.swing.map.theme.Labels;

import static com.telenav.kivakit.map.ui.swing.map.theme.Dots.TRANSLUCENT_YELLOW;
import static com.telenav.kivakit.map.ui.swing.map.theme.MapStyles.Place.PLACE;

/**
 * Draws dots where places are located
 *
 * @author jonathanl (shibo)
 */
public class PlaceRenderer
{
    private enum RenderPass
    {
        DOT,
        LABEL
    }

    private final MapCanvas canvas;

    private final ViewModel model;

    public PlaceRenderer(final MapCanvas canvas, final ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw(final Type type)
    {
        if (model.graphPanel().viewPanel().viewPlaces())
        {
            for (final var pass : RenderPass.values())
            {
                for (final var place : model.graph().placesInside(model.bounds()))
                {
                    if (isVisible(place) && model.selection().is(type, place))
                    {
                        draw(pass, place);
                    }
                }
            }
        }
    }

    private void draw(final RenderPass pass, final Place place)
    {
        // If we're zoomed in or the place is important
        if (canvas.scale().closerThan(Scale.STATE) || place.isCity() || place.population().isGreaterThan(Count.count(10_000)))
        {
            // True if the place is selected
            final var isSelected = model.selection().isSelected(place);

            // Draw dot
            if (pass == RenderPass.DOT)
            {
                // Compute dot diameter
                final var base = Distance.meters(8);
                final var width = Width.of(base
                        .add(base.scaledBy(Math.abs(Math.log10(place.population().asInt())))));

                final var normal = new Dot(width, PLACE, width.scaled(Percent.of(10)), PLACE);
                final var selected = TRANSLUCENT_YELLOW.withWidth(width);

                final var dot = isSelected ? selected : normal;
                final var shape = dot.draw(canvas, place.location());
                model.selection().shape(place, shape);
            }

            // Draw label
            if (pass == RenderPass.LABEL)
            {
                if (isSelected || place.population().isGreaterThan(Count._100_000))
                {
                    final var label = isSelected ? Labels.SELECTED : Labels.NORMAL;
                    label.draw(canvas, place.location(), place.name());
                }
            }
        }
    }

    private boolean isVisible(final Place place)
    {
        if (place.type() == null)
        {
            return false;
        }
        final var panel = model.graphPanel();
        return panel.viewPanel().viewPlaces() && panel.viewPanel().viewPlaceTypes().contains(place.type());
    }
}
