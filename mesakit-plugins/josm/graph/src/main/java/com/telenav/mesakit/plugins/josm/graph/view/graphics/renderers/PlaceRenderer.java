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

import com.telenav.kivakit.language.count.Count;
import com.telenav.mesakit.graph.Place;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.PlaceTheme;

/**
 * Draws dots where places are located
 *
 * @author jonathanl (shibo)
 */
public class PlaceRenderer
{
    private final MapCanvas canvas;

    private final ViewModel model;

    private final PlaceTheme theme = new PlaceTheme();

    public PlaceRenderer(MapCanvas canvas, ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw(Selection.Type type)
    {
        if (model.graphPanel().viewPanel().viewPlaces())
        {
            for (var place : model.graph().placesInside(model.bounds()))
            {
                if (isVisible(place) && model.selection().is(type, place))
                {
                    draw(place);
                }
            }
        }
    }

    private void draw(Place place)
    {
        // If we're zoomed in or the place is important enough
        if (canvas.scale().closerThan(MapScale.STATE) || place.isCity() || place.population().isGreaterThan(Count.count(10_000)))
        {
            var dot = model.selection().isSelected(place)
                    ? theme.dotPlaceSelected(place.population())
                    : theme.dotPlace(place.population());

            var shape = dot
                    .withLabelText(place.name())
                    .withLocation(place.location())
                    .draw(canvas);

            model.selection().shape(place, shape);
        }
    }

    private boolean isVisible(Place place)
    {
        if (place.type() == null)
        {
            return false;
        }
        return model.graphPanel().viewPanel().viewPlaces() && model.graphPanel().viewPanel().viewPlaceTypes().contains(place.type());
    }
}
