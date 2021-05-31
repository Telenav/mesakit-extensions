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

import com.telenav.kivakit.josm.plugins.graph.model.ViewModel;
import com.telenav.kivakit.kernel.messaging.Message;
import com.telenav.mesakit.graph.Vertex;
import com.telenav.mesakit.map.ui.swing.map.graphics.canvas.MapCanvas;

import java.util.HashSet;
import java.util.Set;

import static com.telenav.kivakit.kernel.language.string.conversion.StringFormat.USER_LABEL;
import static com.telenav.kivakit.map.ui.swing.map.theme.MapStyles.Debug.*;

/**
 * Draws edges in the appropriate color for zoom level
 *
 * @author jonathanl (shibo)
 */
public class DebugAnnotationRenderer extends Renderer
{
    private final Set<Vertex> drawn = new HashSet<>();

    public DebugAnnotationRenderer(final MapCanvas canvas, final ViewModel model)
    {
        super(canvas, model);
    }

    public void drawAnnotations()
    {
        // Go through all visible edges
        for (final var edge : model().visibleEdges().edges())
        {
            if (edge.isForward())
            {
                final var text = Message.format("e${long} [v${long} \u2192 v${long}] id${long}",
                        edge.index(),
                        edge.fromVertexIdentifier(),
                        edge.toVertexIdentifier(),
                        edge.identifierAsLong());

                callout(edge.roadShape().midpoint(), LOCATION, EDGE_CALLOUT, text);

                drawVertexAnnotation(edge.from());
                drawVertexAnnotation(edge.to());
            }
        }
    }

    private void drawVertexAnnotation(final Vertex vertex)
    {
        if (!drawn.contains(vertex))
        {
            final var text = Message.format("v${long} in[$] out[$] n${long}",
                    vertex.index(),
                    vertex.inEdges().asString(USER_LABEL),
                    vertex.outEdges().asString(USER_LABEL),
                    vertex.nodeIdentifier());

            callout(vertex.location(), LOCATION, VERTEX_CALLOUT, text);

            drawn.add(vertex);
        }
    }
}
