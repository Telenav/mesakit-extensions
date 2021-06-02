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

package com.telenav.mesakit.plugins.josm.graph.view;

import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.GraphTheme;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.DebugAnnotationRenderer;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.EdgeLabelRenderer;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.EdgeRenderer;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.PlaceRenderer;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.PolylineRenderer;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.RelationRenderer;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.VertexRenderer;

import static com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingPoint.pixels;
import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.CITY;
import static com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale.REGION;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.HIGHLIGHTED;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.INACTIVE;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.RESTRICTED;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.SELECTED;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.UNSELECTED;

/**
 * Paints the {@link ViewModel} for the loaded graph on a {@link MapCanvas}.
 *
 * @author jonathanl (shibo)
 */
class GraphLayerRenderer
{
    private final ViewModel model;

    private final GraphTheme theme = new GraphTheme();

    GraphLayerRenderer(final ViewModel model)
    {
        this.model = model;
    }

    void paint(final MapCanvas canvas)
    {
        final var vertexRenderer = new VertexRenderer(canvas, model);
        final var edgeRenderer = new EdgeRenderer(canvas, model);
        final var edgeLabelRenderer = new EdgeLabelRenderer(canvas, model);
        final var relationRenderer = new RelationRenderer(canvas, model);
        final var placeRenderer = new PlaceRenderer(canvas, model);
        final var lineRenderer = new PolylineRenderer(canvas, model);
        final var debugRenderer = new DebugAnnotationRenderer(canvas, model);

        // Clear any shapes we drew last time
        model.selection().clearShapes();
        model.clearDrawnRectangles();

        // Draw box around graph
        if (!model.isWorldGraph())
        {
            theme.boxGraphBounds(model.graph(), model.isActiveLayer()).draw(canvas);
        }

        // If we're zoomed in enough to draw anything,
        if (canvas.scale().atOrCloserThan(REGION))
        {
            // and we're inactive,
            if (!model.isActiveLayer())
            {
                // just draw the inactive edges
                edgeRenderer.draw(INACTIVE);
            }
            else
            {
                // otherwise, if we're zoomed in
                if (canvas.scale().isZoomedIn(CITY))
                {
                    // draw unselected objects from back to front
                    relationRenderer.draw(UNSELECTED);
                    edgeRenderer.draw(HIGHLIGHTED);
                    placeRenderer.draw(UNSELECTED);
                    edgeRenderer.draw(UNSELECTED);
                    vertexRenderer.draw(UNSELECTED);
                }
                else
                {
                    // or if we're zoomed out, draw them differently
                    relationRenderer.draw(RESTRICTED);
                    edgeRenderer.draw(UNSELECTED);
                    edgeLabelRenderer.draw(UNSELECTED);
                    placeRenderer.draw(UNSELECTED);
                    edgeRenderer.draw(HIGHLIGHTED);
                }

                // then draw selected objects on top
                relationRenderer.draw(SELECTED);
                edgeRenderer.draw(SELECTED);
                vertexRenderer.draw(SELECTED);
                placeRenderer.draw(SELECTED);

                if (model.debug())
                {
                    debugRenderer.drawAnnotations();
                }

                if (canvas.scale().isZoomedIn(CITY))
                {
                    edgeLabelRenderer.draw(UNSELECTED);
                }
            }
        }
        else
        {
            edgeRenderer.draw(HIGHLIGHTED);
        }

        // and finally, draw any user-specified lines
        lineRenderer.drawSelectedPolylines();

        // and the scale label in the upper left
        theme.labelMap(canvas.scale().name())
                .at(pixels(50, 10))
                .draw(canvas);
    }
}
