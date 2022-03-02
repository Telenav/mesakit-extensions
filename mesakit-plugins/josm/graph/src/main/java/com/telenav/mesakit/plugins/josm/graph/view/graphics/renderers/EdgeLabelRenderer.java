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

import com.telenav.kivakit.messaging.logging.Logger;
import com.telenav.kivakit.messaging.logging.LoggerFactory;
import com.telenav.kivakit.messaging.Debug;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Color;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Route;
import com.telenav.mesakit.graph.RouteBuilder;
import com.telenav.mesakit.graph.navigation.navigators.NamedRoadNavigator;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.road.model.RoadFunctionalClass;
import com.telenav.mesakit.map.road.model.RoadType;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.theme.AnnotationTheme;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.telenav.mesakit.map.ui.desktop.theme.shapes.Roads.HIGHWAY_LABEL;
import static com.telenav.mesakit.map.ui.desktop.theme.shapes.Roads.MINOR_STREET_LABEL;
import static com.telenav.mesakit.map.ui.desktop.theme.shapes.Roads.STREET_LABEL;

/**
 * Renders the labels on edges that show road names
 *
 * @author jonathanl (shibo)
 */
public class EdgeLabelRenderer extends Renderer
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private static final Debug DEBUG = new Debug(LOGGER);

    private static final Map<RoadFunctionalClass, Color> roadFunctionalClassToColor = new HashMap<>();

    static
    {
        roadFunctionalClassToColor.put(RoadFunctionalClass.MAIN, KivaKitColors.WHITE_SMOKE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.FIRST_CLASS, KivaKitColors.WHITE_SMOKE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.SECOND_CLASS, KivaKitColors.WHITE_SMOKE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.THIRD_CLASS, KivaKitColors.WHITE_SMOKE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.FOURTH_CLASS, KivaKitColors.WHITE_SMOKE);
    }

    private final AnnotationTheme annotationTheme = new AnnotationTheme();

    public EdgeLabelRenderer(MapCanvas canvas, ViewModel model)
    {
        super(canvas, model);
    }

    public void draw(Selection.Type type)
    {
        // Get the set of edges of the given time,
        var edges = model().visibleEdges().edges(type);

        // and go through edge from the most important to the least,

        var totalLabels = 0;
        for (var roadType = RoadType.FREEWAY; roadType != null; roadType = roadType.nextLeastImportant())
        {
            DEBUG.trace("Drawing labels for $", roadType);

            // looping through edges
            Set<Edge> labeled = new HashSet<>();
            for (var edge : edges)
            {
                // of the same functional class and if we haven't already labeled the edge,
                if (edge.isForward() && edge.roadType().equals(roadType) && !labeled.contains(edge))
                {
                    // find all the connected edges in a route with the same name
                    var roadName = edge.roadName();
                    if (roadName != null)
                    {
                        var route = edge.route(new NamedRoadNavigator(roadName), Distance.MAXIMUM);
                        if (route != null)
                        {
                            // then trim the route to the edges that are visible
                            var visible = trimToVisible(route);
                            if (visible != null)
                            {
                                DEBUG.trace("Found visible route $ for $", visible, roadName);

                                // then traverse the route
                                Location last = null;
                                for (var at : visible)
                                {
                                    var current = at.roadShape().midpoint();
                                    if (last == null)
                                    {
                                        last = current;
                                    }
                                    var distance = canvas().toDrawing(current.distanceTo(last));
                                    if (distance.units() > 300)
                                    {
                                        // and if the edge is long enough,
                                        if (canvas().toDrawing(at.length()).units() > 75)
                                        {
                                            // label it.
                                            if (!labeled.contains(at.forward()))
                                            {
                                                drawLabel(at);
                                                DEBUG.trace("Labeled $", new HashSet<>(labeled));
                                                totalLabels++;
                                                last = current;
                                            }
                                        }
                                    }
                                    labeled.add(at.forward());
                                }
                            }
                        }
                    }
                }
            }

            if (totalLabels > 30)
            {
                break;
            }

            // Remove all the edges we labeled
            edges.removeAll(labeled);
        }
    }

    private void drawLabel(Edge edge)
    {
        // If the edge has a name to use as the label,
        var roadName = edge.roadName();
        if (roadName != null)
        {
            // get the midpoint of its road shape polygon and the label text
            var at = edge.roadShape().midpoint();
            var text = roadName.name();

            // then draw the label
            DEBUG.trace("Drawing label '$' on edge $", text, edge);
            var textColor = roadFunctionalClassToColor.get(edge.roadFunctionalClass());
            Style style;
            switch (edge.roadType())
            {
                case FREEWAY:
                case HIGHWAY:
                    style = HIGHWAY_LABEL;
                    break;

                case FRONTAGE_ROAD:
                case THROUGHWAY:
                    style = STREET_LABEL;
                    break;

                default:
                    style = MINOR_STREET_LABEL;
                    break;
            }
            callout(at, annotationTheme.dotAnnotationLocation(), style.withTextColor(textColor), text);
        }
    }

    private Route trimToVisible(Route route)
    {
        try
        {
            var builder = new RouteBuilder();
            for (var edge : route)
            {
                if (isVisible(edge) && edge.roadShape().intersects(model().bounds()))
                {
                    builder.append(edge);
                }
            }
            return builder.route();
        }
        catch (Exception e)
        {

            // If we can't build a connected route (probably the route is going in and out of the
            // view area near the edges)
            return null;
        }
    }
}
