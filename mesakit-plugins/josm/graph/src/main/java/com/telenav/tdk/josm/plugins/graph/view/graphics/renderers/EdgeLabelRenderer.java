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

import com.telenav.tdk.core.kernel.debug.Debug;
import com.telenav.tdk.core.kernel.logging.*;
import com.telenav.tdk.graph.*;
import com.telenav.tdk.graph.navigation.navigators.NamedRoadNavigator;
import com.telenav.tdk.josm.plugins.graph.model.Selection.Type;
import com.telenav.tdk.josm.plugins.graph.model.ViewModel;
import com.telenav.tdk.map.geography.Location;
import com.telenav.tdk.map.measurements.Distance;
import com.telenav.tdk.map.road.model.*;
import com.telenav.tdk.map.ui.swing.map.graphics.canvas.*;
import com.telenav.tdk.utilities.ui.swing.graphics.color.*;

import java.util.*;

import static com.telenav.tdk.map.ui.swing.map.theme.MapStyles.Road.*;

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
        roadFunctionalClassToColor.put(RoadFunctionalClass.MAIN, TdkColors.BRIGHT_WHITE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.FIRST_CLASS, TdkColors.BRIGHT_WHITE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.SECOND_CLASS, TdkColors.BRIGHT_WHITE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.THIRD_CLASS, TdkColors.BRIGHT_WHITE);
        roadFunctionalClassToColor.put(RoadFunctionalClass.FOURTH_CLASS, TdkColors.BRIGHT_WHITE);
    }

    public EdgeLabelRenderer(final MapCanvas canvas, final ViewModel model)
    {
        super(canvas, model);
    }

    public void draw(final Type type)
    {
        // Get the set of edges of the given time,
        final var edges = model().visibleEdges().edges(type);

        // and go through edge from the most important to the least,

        var totalLabels = 0;
        for (var roadType = RoadType.FREEWAY; roadType != null; roadType = roadType.nextLeastImportant())
        {
            DEBUG.trace("Drawing labels for $", roadType);

            // looping through edges
            final Set<Edge> labeled = new HashSet<>();
            for (final var edge : edges)
            {
                // of the same functional class and if we haven't already labeled the edge,
                if (edge.isForward() && edge.roadType().equals(roadType) && !labeled.contains(edge))
                {
                    // find all the connected edges in a route with the same name
                    final var roadName = edge.roadName();
                    if (roadName != null)
                    {
                        final var route = edge.route(new NamedRoadNavigator(roadName), Distance.MAXIMUM);
                        if (route != null)
                        {
                            // then trim the route to the edges that are visible
                            final var visible = trimToVisible(route);
                            if (visible != null)
                            {
                                DEBUG.trace("Found visible route $ for $", visible, roadName);

                                // then traverse the route
                                Location last = null;
                                for (final var at : visible)
                                {
                                    final var current = at.roadShape().midpoint();
                                    if (last == null)
                                    {
                                        last = current;
                                    }
                                    final var distance = canvas().awtDistance(current.distanceTo(last));
                                    if (distance > 300f)
                                    {
                                        // and if the edge is long enough,
                                        if (canvas().awtDistance(at.length()) > 75)
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

    private void drawLabel(final Edge edge)
    {
        // If the edge has a name to use as the label,
        final var roadName = edge.roadName();
        if (roadName != null)
        {
            // get the midpoint of its road shape polygon and the label text
            final var at = edge.roadShape().midpoint();
            final var text = roadName.name();

            // then draw the label
            DEBUG.trace("Drawing label '$' on edge $", text, edge);
            final var textColor = roadFunctionalClassToColor.get(edge.roadFunctionalClass());
            final Style style;
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
            callout(at, ROAD_NAME_CALLOUT_LOCATION, style.withTextColor(textColor), text);
        }
    }

    private Route trimToVisible(final Route route)
    {
        try
        {
            final var builder = new RouteBuilder();
            for (final var edge : route)
            {
                if (isVisible(edge) && edge.roadShape().intersects(model().bounds()))
                {
                    builder.append(edge);
                }
            }
            return builder.route();
        }
        catch (final Exception e)
        {

            // If we can't build a connected route (probably the route is going in and out of the
            // view area near the edges)
            return null;
        }
    }
}
