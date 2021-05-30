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

package com.telenav.tdk.josm.plugins.graph.view;

import com.telenav.tdk.josm.plugins.graph.model.ViewModel;
import com.telenav.tdk.josm.plugins.graph.view.graphics.renderers.*;
import com.telenav.tdk.map.measurements.Heading;
import com.telenav.tdk.map.ui.swing.map.graphics.canvas.*;
import com.telenav.tdk.map.ui.swing.map.theme.Styles;

import static com.telenav.tdk.josm.plugins.graph.model.Selection.Type.SELECTED;
import static com.telenav.tdk.josm.plugins.graph.model.Selection.Type.*;
import static com.telenav.tdk.map.ui.swing.map.theme.Labels.*;

/**
 * Paints the {@link ViewModel} for the loaded graph on a {@link MapCanvas}.
 *
 * @author jonathanl (shibo)
 */
class GraphLayerRenderer
{
    private final ViewModel model;

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
            final var style = model.isActiveLayer() ? STYLE_BORDERED : STYLE_GRAYED;
            canvas.drawLabeledRectangle(style, Width.pixels(3f), model.graph().bounds(), model.graph().name());
        }

        // If we're zoomed in enough to draw anything,
        if (canvas.scale().atOrCloserThan(Scale.REGION))
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
                if (canvas.scale().isZoomedIn(Scale.CITY))
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

                if (canvas.scale().isZoomedIn(Scale.CITY))
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
        lineRenderer.draw();

        final var south = canvas.distance(50);
        final var east = canvas.distance(10);
        canvas.drawLabel(Styles.OCEAN_AND_WHITE, canvas.bounds().topLeft()
                        .moved(Heading.SOUTH, south)
                        .moved(Heading.EAST, east),
                Width.pixels(2f), canvas.scale().name());
    }
}
