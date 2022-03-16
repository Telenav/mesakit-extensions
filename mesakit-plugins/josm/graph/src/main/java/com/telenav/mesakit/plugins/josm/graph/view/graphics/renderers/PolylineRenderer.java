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

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.mesakit.map.geography.shape.polyline.Polyline;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapPolyline;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.PolylineTheme;

/**
 * Draws any selected polyline
 *
 * @author jonathanl (shibo)
 */
public class PolylineRenderer
{
    private final MapCanvas canvas;

    private final ViewModel model;

    private final PolylineTheme theme = new PolylineTheme();

    public PolylineRenderer(MapCanvas canvas, ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw(Polyline polyline)
    {
        draw(polyline, theme.styleUnselected());
    }

    public void draw(Polyline polyline, Style style)
    {
        MapPolyline.polyline(style)
                .withPolyline(polyline)
                .draw(canvas);
    }

    public void drawSelectedPolylines()
    {
        var style = canvas.scale().isZoomedIn(MapScale.CITY) ? theme.styleZoomedIn() : theme.styleZoomedOut();
        for (var selectedPolyline : model.selection().selectedPolylines())
        {
            if (model.selection().is(Selection.Type.SELECTED, selectedPolyline))
            {
                draw(selectedPolyline, style);
            }
        }
    }
}
