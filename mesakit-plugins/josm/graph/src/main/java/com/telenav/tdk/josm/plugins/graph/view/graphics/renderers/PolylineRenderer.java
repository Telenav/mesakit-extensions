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
import com.telenav.mesakit.map.ui.swing.map.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.swing.map.graphics.canvas.Scale;

import static com.telenav.kivakit.map.ui.swing.map.theme.MapStyles.Polyline.ZOOMED_IN;
import static com.telenav.kivakit.map.ui.swing.map.theme.MapStyles.Polyline.ZOOMED_OUT;

/**
 * Draws any selected polyline
 *
 * @author jonathanl (shibo)
 */
public class PolylineRenderer
{
    private final MapCanvas canvas;

    private final ViewModel model;

    public PolylineRenderer(final MapCanvas canvas, final ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw()
    {
        for (final var polyline : model.selection().selectedPolylines())
        {
            if (model.selection().is(Type.SELECTED, polyline))
            {
                final var line = canvas.scale().isZoomedIn(Scale.CITY) ? ZOOMED_IN : ZOOMED_OUT;
                line.draw(canvas, polyline);
            }
        }
    }
}
