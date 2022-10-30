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

import com.telenav.kivakit.interfaces.comparison.Matcher;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.mesakit.graph.EdgeRelation;
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.graph.collections.RelationSet;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapDot;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.model.VisibleEdges;
import com.telenav.mesakit.plugins.josm.graph.theme.RelationTheme;

import static com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.measurements.DrawingLength.pixels;
import static com.telenav.mesakit.map.measurements.geographic.Distance.meters;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.HIGHLIGHTED;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.SELECTED;
import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.UNSELECTED;

/**
 * Draws relations and restrictions
 *
 * @author jonathanl (shibo)
 */
public class RelationRenderer
{
    private final Maximum MAXIMUM_RENDERED_NON_RESTRICTIONS = Maximum.maximum(500);

    private final Maximum MAXIMUM_RENDERED_RESTRICTIONS = Maximum.maximum(500);

    private final MapCanvas canvas;

    private final ViewModel model;

    private EdgeSet edges;

    private final RelationTheme theme = new RelationTheme();

    public RelationRenderer(MapCanvas canvas, ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw(Selection.Type type)
    {
        switch (type)
        {
            case RESTRICTED ->
            {
                drawRestrictions(UNSELECTED);
                drawRestrictions(HIGHLIGHTED);
                drawRestrictions(SELECTED);
            }
            case UNSELECTED ->
            {
                drawNonRestrictions();
                drawRestrictions(type);
            }
            case SELECTED ->
            {
                var selected = model.selection().selectedRelation();
                if (selected != null)
                {
                    var viaNodeLocation = selected.viaNodeLocation();
                    if (viaNodeLocation != null)
                    {
                        viaNodeDotSize(theme.dotViaNodeSelected())
                                .withLocation(viaNodeLocation).draw(canvas);
                    }
                    for (var route : selected.asRoutes())
                    {
                        theme.polylineRouteSelected()
                                .withPolyline(route.polyline())
                                .draw(canvas);
                    }
                }
            }
            default -> throw new IllegalArgumentException();
        }
    }

    private void drawNonRestriction(EdgeRelation relation)
    {
        for (var route : relation.asRoutes())
        {
            var shape = theme.polylineRoute(theme.polylineRoute(), route).draw(canvas);
            model.selection().shape(relation, shape);
        }
    }

    private void drawNonRestrictions()
    {
        if (canvas.scale().isZoomedIn(MapScale.NEIGHBORHOOD))
        {
            var relations = relations((relation) -> isVisible(relation) && !relation.isRestriction());
            if (Count.count(relations).isLessThan(MAXIMUM_RENDERED_NON_RESTRICTIONS))
            {
                for (var relation : relations)
                {
                    if (!model.selection().isSelected(relation))
                    {
                        drawNonRestriction(relation);
                    }
                }
            }
        }
    }

    private void drawRestriction(EdgeRelation relation)
    {
        // Draw via node
        var viaNodeLocation = relation.viaNodeLocation();
        if (viaNodeLocation != null)
        {
            var dot = relation.is(EdgeRelation.Type.BAD_TURN_RESTRICTION) ? theme.dotViaNodeBad() : theme.dotViaNodeUnselected();
            viaNodeDotSize(dot)
                    .withLocation(viaNodeLocation)
                    .draw(canvas);
        }

        // Draw route
        if (canvas.scale().atOrCloserThan(MapScale.NEIGHBORHOOD))
        {
            if (relation.isTurnRestriction())
            {
                var restriction = relation.turnRestriction();
                for (var route : restriction.routes())
                {
                    var shape = theme.polylineRoute(theme.polylineRestriction(), route).draw(canvas);
                    model.selection().shape(relation, shape);
                }
                var location = relation.viaNodeLocation();
                if (location != null)
                {
                    var shape = theme.dotNoRoute().withLocation(location).draw(canvas);
                    model.selection().shape(relation, shape);
                }
            }
            else
            {
                for (var route : relation.asRoutes())
                {
                    var shape = theme.polylineRoute(theme.polylineRestriction(), route).draw(canvas);
                    model.selection().shape(relation, shape);
                }
            }
        }
    }

    private void drawRestrictions(Selection.Type type)
    {
        var relations = relations((relation) -> isVisible(relation) && relation.isRestriction());
        if (Count.count(relations).isLessThan(MAXIMUM_RENDERED_RESTRICTIONS))
        {
            for (var relation : relations)
            {
                if (model.selection().is(type, relation))
                {
                    drawRestriction(relation);
                }
            }
        }
    }

    private boolean isVisible(EdgeRelation relation)
    {
        if (relation.type() == null)
        {
            return false;
        }
        var panel = model.graphPanel();
        return panel.viewPanel().viewRelations() && panel.viewPanel().viewRelationTypes().contains(relation.type());
    }

    private RelationSet relations(Matcher<EdgeRelation> matcher)
    {
        if (edges == null)
        {
            if (model.graphPanel().viewPanel().viewAllEdges())
            {
                edges = model.visibleEdges().edges();
            }
            else
            {
                edges = new VisibleEdges(canvas, model, VisibleEdges.Mode.RELATIONS).edges();
            }
        }
        var relations = new RelationSet(Estimate._2048);
        for (var edge : edges)
        {
            for (var relation : edge.relations())
            {
                if (matcher.matches(relation))
                {
                    relations.add(relation);
                }
            }
        }
        return relations;
    }

    private MapDot viaNodeDotSize(MapDot dot)
    {
        switch (canvas.scale())
        {
            case STATE:
            case REGION:
            case CITY:
                return dot.withRadius(pixels(3));

            case NEIGHBORHOOD:
            case STREET:
            default:
                return dot.withRadius(meters(7));
        }
    }
}
