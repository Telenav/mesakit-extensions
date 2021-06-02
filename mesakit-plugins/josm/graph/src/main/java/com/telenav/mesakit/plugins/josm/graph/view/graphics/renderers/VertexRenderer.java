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

import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;

import static com.telenav.mesakit.map.ui.desktop.theme.shapes.Vertexes.NORMAL;
import static com.telenav.mesakit.map.ui.desktop.theme.shapes.Vertexes.SELECTED;

/**
 * Draws the vertexes of edges when they are selected.
 *
 * @author jonathanl (shibo)
 */
public class VertexRenderer
{
    private final MapCanvas canvas;

    private final ViewModel model;

    public VertexRenderer(final MapCanvas canvas, final ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw(final Edge selected)
    {
        if (selected != null)
        {
            // Draw from vertex
            final var from = NORMAL.withLocation(selected.fromLocation()).draw(canvas);
            model.selection().shape(selected.from(), from);

            // Draw to vertex
            final var to = NORMAL.withLocation(selected.toLocation()).draw(canvas);
            model.selection().shape(selected.to(), to);
        }
    }

    public void draw(final Selection.Type type)
    {
        if (type == Selection.Type.SELECTED)
        {
            final var vertex = model.selection().selectedVertex();
            if (vertex != null)
            {
                SELECTED.withLocation(vertex.location()).draw(canvas);
            }
        }
    }
}
