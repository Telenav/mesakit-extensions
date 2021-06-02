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

import com.telenav.kivakit.kernel.interfaces.comparison.Matcher;
import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.kernel.language.values.count.Estimate;
import com.telenav.kivakit.kernel.language.values.count.Maximum;
import com.telenav.mesakit.graph.EdgeRelation;
import com.telenav.mesakit.graph.Route;
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.graph.collections.RelationSet;
import com.telenav.mesakit.map.geography.shape.rectangle.Width;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.map.ui.desktop.theme.shapes.Relations;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.RelationTheme;

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

    public RelationRenderer(final MapCanvas canvas, final ViewModel model)
    {
        this.canvas = canvas;
        this.model = model;
    }

    public void draw(final Selection.Type type)
    {
        switch (type)
        {
            case RESTRICTED:
                drawRestrictions(UNSELECTED);
                drawRestrictions(HIGHLIGHTED);
                drawRestrictions(SELECTED);
                break;

            case UNSELECTED:
                drawNonRestrictions();
                drawRestrictions(type);
                break;

            case SELECTED:
                final var selected = model.selection().selectedRelation();
                if (selected != null)
                {
                    final var viaNodeLocation = selected.viaNodeLocation();
                    if (viaNodeLocation != null)
                    {
                        Relations.VIA_NODE_SELECTED.withWidth(viaNodeDotSize()).draw(canvas, viaNodeLocation);
                    }
                    for (final var route : selected.asRoutes())
                    {
                        Relations.SELECTED_LINE.draw(canvas, route.polyline());
                    }
                }
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    private void drawNonRestriction(final EdgeRelation relation)
    {
        for (final var route : relation.asRoutes())
        {
            final var shape = line(Relation.ROUTE, route).draw(canvas, route.polyline());
            model.selection().shape(relation, shape);
        }
    }

    private void drawNonRestrictions()
    {
        if (canvas.scale().isZoomedIn(MapScale.NEIGHBORHOOD))
        {
            final var relations = relations((relation) -> isVisible(relation) && !relation.isRestriction());
            if (Count.count(relations).isLessThan(MAXIMUM_RENDERED_NON_RESTRICTIONS))
            {
                for (final var relation : relations)
                {
                    if (!model.selection().isSelected(relation))
                    {
                        drawNonRestriction(relation);
                    }
                }
            }
        }
    }

    private void drawRestriction(final EdgeRelation relation)
    {
        // Draw via node
        final var viaNodeLocation = relation.viaNodeLocation();
        if (viaNodeLocation != null)
        {
            final var dot = relation.is(EdgeRelation.Type.BAD_TURN_RESTRICTION) ? Relation.VIA_NODE_BAD : Relation.VIA_NODE;
            dot.withWidth(viaNodeDotSize()).draw(canvas, viaNodeLocation);
        }

        // Draw route
        if (canvas.scale().atOrCloserThan(Scale.NEIGHBORHOOD))
        {
            if (relation.isTurnRestriction())
            {
                final var restriction = relation.turnRestriction();
                for (final var route : restriction.routes())
                {
                    final var shape = line(Relation.RESTRICTION, route).draw(canvas, route.polyline());
                    model.selection().shape(relation, shape);
                }
                final var location = relation.viaNodeLocation();
                if (location != null)
                {
                    final var shape = Relation.ROUTE_NONE.draw(canvas, location);
                    model.selection().shape(relation, shape);
                }
            }
            else
            {
                for (final var route : relation.asRoutes())
                {
                    final var shape = line(Relation.RESTRICTION, route).draw(canvas, route.polyline());
                    model.selection().shape(relation, shape);
                }
            }
        }
    }

    private void drawRestrictions(final Selection.Type type)
    {
        final var relations = relations((relation) -> isVisible(relation) && relation.isRestriction());
        if (Count.count(relations).isLessThan(MAXIMUM_RENDERED_RESTRICTIONS))
        {
            for (final var relation : relations)
            {
                if (model.selection().is(type, relation))
                {
                    drawRestriction(relation);
                }
            }
        }
    }

    private boolean isVisible(final EdgeRelation relation)
    {
        if (relation.type() == null)
        {
            return false;
        }
        final var panel = model.graphPanel();
        return panel.viewPanel().viewRelations() && panel.viewPanel().viewRelationTypes().contains(relation.type());
    }

    private Line line(final Line line, final Route route)
    {
        return EdgeRenderer.fattened(line, route.asEdgeSet().mostImportant());
    }

    private RelationSet relations(final Matcher<EdgeRelation> matcher)
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
        final var relations = new RelationSet(Estimate._2048);
        for (final var edge : edges)
        {
            for (final var relation : edge.relations())
            {
                if (matcher.matches(relation))
                {
                    relations.add(relation);
                }
            }
        }
        return relations;
    }

    private Width viaNodeDotSize()
    {
        switch (canvas.scale())
        {
            case STATE:
            case REGION:
            case CITY:
                return Width.pixels(3f);

            case NEIGHBORHOOD:
            case STREET:
            default:
                return Width.meters(7);
        }
    }
}
