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

import com.telenav.kivakit.messaging.Message;
import com.telenav.mesakit.graph.Vertex;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.AnnotationTheme;

import java.util.HashSet;
import java.util.Set;

import static com.telenav.kivakit.interfaces.string.Stringable.Format.USER_LABEL;

/**
 * Draws edges in the appropriate color for zoom level
 *
 * @author jonathanl (shibo)
 */
public class DebugAnnotationRenderer extends Renderer
{
    private final Set<Vertex> drawn = new HashSet<>();

    private final AnnotationTheme theme = new AnnotationTheme();

    public DebugAnnotationRenderer(MapCanvas canvas, ViewModel model)
    {
        super(canvas, model);
    }

    public void drawAnnotations()
    {
        // Go through all visible edges
        for (var edge : model().visibleEdges().edges())
        {
            if (edge.isForward())
            {
                var text = Message.format("e${long} [v${long} \u2192 v${long}] id${long}",
                        edge.index(),
                        edge.fromVertexIdentifier(),
                        edge.toVertexIdentifier(),
                        edge.identifierAsLong());

                callout(edge.roadShape().midpoint(), theme.dotAnnotationLocation(), theme.styleEdgeAnnotationCallout(), text);

                drawVertexAnnotation(edge.from());
                drawVertexAnnotation(edge.to());
            }
        }
    }

    private void drawVertexAnnotation(Vertex vertex)
    {
        if (!drawn.contains(vertex))
        {
            var text = Message.format("v${long} in[$] out[$] n${long}",
                    vertex.index(),
                    vertex.inEdges().asString(USER_LABEL),
                    vertex.outEdges().asString(USER_LABEL),
                    vertex.nodeIdentifier());

            callout(vertex.location(), theme.dotAnnotationLocation(), theme.styleVertexAnnotationCallout(), text);

            drawn.add(vertex);
        }
    }
}
