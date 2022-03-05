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

package com.telenav.mesakit.plugins.josm.graph.view.tabs.search;

import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.language.Objects;
import com.telenav.kivakit.core.language.primitive.Ints;
import com.telenav.kivakit.core.messaging.Listener;
import com.telenav.kivakit.core.string.Separators;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.mesakit.core.MesaKit;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.Route;
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.graph.identifiers.EdgeIdentifier;
import com.telenav.mesakit.graph.identifiers.VertexIdentifier;
import com.telenav.mesakit.graph.map.MapEdgeIdentifier;
import com.telenav.mesakit.graph.world.WorldEdge;
import com.telenav.mesakit.graph.world.WorldGraph;
import com.telenav.mesakit.map.data.formats.library.map.identifiers.MapRelationIdentifier;
import com.telenav.mesakit.map.data.formats.pbf.model.identifiers.PbfNodeIdentifier;
import com.telenav.mesakit.map.data.formats.pbf.model.identifiers.PbfRelationIdentifier;
import com.telenav.mesakit.map.data.formats.pbf.model.identifiers.PbfWayIdentifier;
import com.telenav.mesakit.map.data.formats.pbf.model.tags.PbfTags;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.polyline.Polyline;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.region.locale.MapLocale;
import com.telenav.mesakit.map.road.model.RoadName;
import com.telenav.mesakit.map.road.name.standardizer.RoadNameStandardizer;
import com.telenav.mesakit.plugins.josm.graph.view.GraphLayer;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;

import java.awt.Desktop;
import java.net.URI;
import java.util.regex.Pattern;

import static com.telenav.mesakit.map.road.name.standardizer.RoadNameStandardizer.Mode.MESAKIT_STANDARDIZATION;
import static com.telenav.mesakit.plugins.josm.graph.view.GraphLayer.Show.HIGHLIGHT_AND_ZOOM_TO;

/**
 * The search portion of the {@link GraphPanel}
 *
 * @author jonathanl (shibo)
 */
public class Searcher extends BaseComponent
{
    @SuppressWarnings("SpellCheckingInspection")
    static UserFeedback help()
    {
        var help = new StringList();

        help.add("<span class='section0'>MesaKit $ Graph Plugin</span>", MesaKit.get().projectVersion());
        help.add("<p class='aqua'>To make a graph active, double click on it in the view area or select it in the Layers panel.</p>" +
                "<p class='aqua'>Select edges, vertexes and relations of an active graph to see their details.</p> " +
                "<p class= 'aqua'>Select a rectangle to zoom in. " +
                "TimeFormat any of the following commands or search item into the search box:</p>");
        help.add("<p class='section1'>Commands</p>");
        help.add("<ul class='aqua'>");
        help.add("    <li>bounds - show the bounds of the visible area</li>");
        help.add("    <li>center - show the center of the visible area</li>");
        help.add("    <li>clear - clear any poly-lines drawn by routing or as geographic locations</li>");
        help.add("    <li>graph - show details about the active graph layer</li>");
        help.add("    <li>help - see this help message again</li>");
        help.add("    <li>query help - see help for query syntax used on query tab</li>");
        help.add("    <li>reset - reset the active (top) graph layer so it is fully visible</li>");
        help.add("    <li>debug - toggle edge identifier and index labels</li>");
        help.add("    <li>tag key=value - show all edges with the given key value pair like: \"tag name='Geary St'\" or \"tag oneway=yes\"</li>");
        help.add("    <li>turns - show the number of turn restrictions in the active graph layer</li>");
        help.add("    <li>version - show the plugin version and build date</li>");
        help.add("    <li>visible-turns - show the number of visible turn restrictions</li>");
        help.add("</ul>");
        help.add("<span class='section1'>Find Graph Elements</span>");
        help.add("<ul class='aqua'>");
        help.add("    <li>[edge-identifier] - select the given edge");
        help.add("    <li>[edge-identifier:edge-identifier:...] - highlight the given route</li>");
        help.add("    <li>[vertex-identifier] - select the given vertex</li>");
        help.add("    <li>[relation-identifier] - select the given edge relation</li>");
        help.add("    <li>[traffic-code] - highlight the tmc, osm, tomtom, navteq, ttl or ngx traffic code</li>");
        help.add("    <li>[road-name] - highlight all roads matching the base part of the given road name</li>");
        help.add("</ul>");
        help.add("<span class='section1'>Find Map Identifiers</span>");
        help.add("<ul class='aqua'>");
        help.add("    <li>[node-identifier] - select any vertex for the node identifier</li>");
        help.add("    <li>[way-identifier] - select any edges for the given way identifier</li>");
        help.add("    <li>[relation-identifier] - select any ways belonging to the given relation identifier</li>");
        help.add("    <li>[way:from-node:to-node] - select the edge specified by the way and from/to node identifiers</li>");
        help.add("</ul>");
        help.add("<span class='section1'>Highlight Locations and Shapes</span>");
        help.add("<ul class='aqua'>");
        help.add("    <li>[latitude,longitude] - highlight the given location</li>");
        help.add("    <li>[location-1:location-2] - highlight the given rectangle</li>");
        help.add("    <li>[location-1:location-2:[...]:location-N] - highlight the given polyline</li>");
        help.add("</ul>");

        return UserFeedback.html(help.join("\n")).withStatus("Showing help screen");
    }

    @SuppressWarnings("SpellCheckingInspection")
    static UserFeedback queryHelp()
    {
        var help = new StringList();

        help.add("<span class='section0'>MesaKit $ Graph Query Help</span>", MesaKit.get().projectVersion());
        help.add("<p class='aqua'>Graph queries can be entered into the search box as 'select [query-string]', " +
                "where the query string is composed<br/>of operators, attributes and scalar constants.</p>");
        help.add("<p class='aqua'>For example, the query \"edge.roadName contains 'Lomas'\" would find " +
                " and select edges whose road name<br/>contained the word 'Lomas' (case-independent).</p>");
        help.add("<p class='aqua'>Another example might be selecting all the roads that are smaller " +
                "than two meters with \"edge.length < 2 meters\"</p>");
        help.add("<p class='section1'>Examples</p>");
        help.add("<ul class='aqua'>");
        help.add("    <li>select edge.roadName contains 'Lomas' and edge.length &lt; 10 meters</li>");
        help.add("    <li>select edge.isOnRamp = true and edge.speedLimit &lt; 40 mph</li>");
        help.add("    <li>select edge.isOffRamp = true and edge.speedLimit &lt; 40 mph</li>");
        help.add("    <li>select edge.isRoundabout = true</li>");
        help.add("    <li>select edge.isRoundabout = true and edge.roadFunctionalClass = 3</li>");
        help.add("    <li>select edge.isRoundabout = true and edge.roadFunctionalClass = 3 and edge.length < 5.1 meters</li>");
        help.add("    <li>select edge.roadType = 'freeway' and edge.outEdges contains (edge.length < 10 meters)</li>");
        help.add("</ul>");
        help.add("<p class='section1'>Logical Operators</p>");
        help.add("<ul class='aqua'>");
        help.add("    <li>'not' or '!' - invert the truth of the query expression</li>");
        help.add("    <li>'and' or '&&' - true if the left and right expressions are both true</li>");
        help.add("    <li>'or' or '||' - true if either the left or right expressions are true</li>");
        help.add("</ul>");
        help.add("<p class='section1'>Comparative Operators</p>");
        help.add("<ul class='aqua'>");
        help.add("    <li>'=' or '==' - true if the left expression equals the right expression</li>");
        help.add("    <li>'!=' - true if the left expression is not equal to the right expression</li>");
        help.add("    <li>'contains' - true if the left string expression contains the right string expression (case-independent)</li>");
        help.add("    <li>'&lt;' - true if the left expression is less than the right expression</li>");
        help.add("    <li>'&gt;' - true if the left expression is greater than the right expression</li>");
        help.add("    <li>'&lt;=' - true if the left expression is less than or equal to the right expression</li>");
        help.add("    <li>'&gt;=' - true if the left expression is greater than or equal to the right expression</li>");
        help.add("</ul>");
        help.add("<p class='section1'>Set Node</p>");
        help.add("<ul class='aqua'>");
        help.add("    <li>edge.[attribute] contains ([query]) - evaluates to true if the given set attribute contains an element that satisfies the query in parenthesis</li>");
        help.add("</ul>");
        help.add("<p class='section1'>Attributes and Constants</p>");
        help.add("<ul class='aqua'>");
        help.add("    <li>edge.[attribute] - evaluates to the given attribute so long as it is a scalar value</li>");
        help.add("    <li>double - a double-precision scalar value, such as 4.3</li>");
        help.add("    <li>int - an integer scalar value, such as 10</li>");
        help.add("    <li>string - a string value such as 'sulphur'</li>");
        help.add("    <li>boolean - a boolean value, either true or false</li>");
        help.add("    <li>double [unit] - a distance value, like '3.7 meters', '6 feet' or '5 miles'</li>");
        help.add("</ul>");
        help.add("<p class='section1'>Some Supported Edge Attributes</p>");
        help.add("<ul class='aqua'>");
        help.add("    <li>edge.identifier</li>");
        help.add("    <li>edge.isForward</li>");
        help.add("    <li>edge.isReverse</li>");
        help.add("    <li>edge.isDrivable</li>");
        help.add("    <li>edge.isOnRamp</li>");
        help.add("    <li>edge.isOffRamp</li>");
        help.add("    <li>edge.isOneWay</li>");
        help.add("    <li>edge.isTwoWay</li>");
        help.add("    <li>edge.isRoundabout</li>");
        help.add("    <li>edge.length</li>");
        help.add("</ul>");

        return UserFeedback.html(help.join("\n")).withStatus("Showing graph query help");
    }

    private final GraphLayer layer;

    public Searcher(GraphLayer layer)
    {
        this.layer = layer;
    }

    public EdgeSet findTagged(Maximum maximum, String searchString)
    {
        var pattern = Pattern.compile("tag\\s+(<?tag>.*)");
        var matcher = pattern.matcher(searchString);
        if (matcher.matches())
        {
            var tag = PbfTags.parse(matcher.group("tag"));
            if (tag != null)
            {
                var found = new EdgeSet(Estimate._1024);
                for (var edge : graph().forwardEdges())
                {
                    if (edge.hasTag(tag.getKey()))
                    {
                        var value = edge.tagValue(tag.getKey());
                        if (Objects.equalIgnoringCase(tag.getValue(), value))
                        {
                            found.add(edge);
                            if (found.count().isGreaterThanOrEqualTo(maximum))
                            {
                                return found;
                            }
                        }
                    }
                }
                return found;
            }
        }
        return null;
    }

    public UserFeedback search(String searchString)
    {
        searchString = searchString.trim();

        if ("help".equals(searchString))
        {
            return help();
        }

        if (searchString.equals("query help"))
        {
            return queryHelp();
        }
        if (searchString.equals("version"))
        {
            return UserFeedback.html(MesaKit.get().projectVersion() + " - " + MesaKit.get().build());
        }

        var viewBounds = layer.model().bounds();
        var graphBounds = graph().bounds();

        switch (searchString)
        {
            case "bounds":
                return UserFeedback.text(viewBounds.toString());

            case "center":
                return UserFeedback.text(viewBounds.center().toString());

            case "clear":
                layer.model().selection().clearPolylines();
                layer.forceRepaint();
                return UserFeedback.status("Cleared highlights and poly-lines");

            case "debug":
            {
                var debug = !layer.model().debug();
                layer.model().debug(debug);
                layer.forceRepaint();
                return UserFeedback.status(debug ? "Debug information on" : "Debug information off");
            }

            case "graph":
                return UserFeedback.text(graph().asString());

            case "open":
                return UserFeedback.status(openInBrowser());

            case "reset":
                layer.zoomTo(graphBounds);
                return UserFeedback.status("Showing " + graphBounds);

            case "turns":
                return UserFeedback.text(turnStatistics(Rectangle.MAXIMUM).bulleted());

            case "visible-turns":
                return UserFeedback.text(turnStatistics(graphBounds).bulleted());
        }

        var edges = findTagged(Maximum._1_000, searchString);
        if (edges != null && !edges.isEmpty())
        {
            layer.show(edges, HIGHLIGHT_AND_ZOOM_TO);
            return UserFeedback.text("Showing " + edges.size() + " matching edges");
        }

        var rectangle = findRectangle(searchString);
        if (rectangle != null)
        {
            layer.show(rectangle.asPolyline());
            layer.forceRepaint();
            return UserFeedback.status("Showing rectangle " + rectangle);
        }

        var line = findPolyline(searchString);
        if (line != null)
        {
            layer.show(line);
            return UserFeedback.status("Showing " + (line.size() == 1 ? "location" : "polyline") + " " + line);
        }

        var location = findLocation(searchString);
        if (location != null)
        {
            layer.show(location.bounds().asPolyline());
            return UserFeedback.status("Showing location " + location);
        }

        var route = findRoute(searchString);
        if (route != null && route.size() > 1)
        {
            layer.show(route);
            return UserFeedback.status("Showing route " + route);
        }

        var mapEdgeIdentifier = findMapEdgeIdentifier(searchString);
        if (mapEdgeIdentifier != null)
        {
            if (layer.show(mapEdgeIdentifier))
            {
                return UserFeedback.status("Showing map edge identifier " + mapEdgeIdentifier);
            }
        }

        var identifierPattern = Pattern.compile(
                "\\s*(?<prefix>e|v|n|w|r|edge|vertex|node|way|relation)\\s*(?<identifier>\\d+)L?");

        var matcher = identifierPattern.matcher(searchString);
        if (matcher.matches())
        {
            var prefix = matcher.group("prefix");
            var identifier = matcher.group("identifier");

            // All of these identifiers can be ambiguous (thus the optional prefix string)

            var edgeIdentifier = findEdgeIdentifier(identifier);
            var vertexIdentifier = findVertexIdentifier(identifier);
            var wayIdentifier = findWayIdentifier(identifier);
            var nodeIdentifier = findNodeIdentifier(identifier);
            var relationIdentifier = findRelationIdentifier(identifier);

            var edgeIndex = Ints.parseInt(this, identifier);
            if (edgeIndex > 0)
            {
                Edge indexedEdge = graph().edgeStore().edgeForIndex(edgeIndex);
                if (indexedEdge != null)
                {
                    edgeIdentifier = indexedEdge.identifier();
                }
            }

            if (prefix != null)
            {
                switch (prefix)
                {
                    case "e":
                    case "edge":
                        vertexIdentifier = null;
                        relationIdentifier = null;
                        nodeIdentifier = null;
                        wayIdentifier = null;
                        break;

                    case "v":
                    case "vertex":
                        edgeIdentifier = null;
                        relationIdentifier = null;
                        nodeIdentifier = null;
                        wayIdentifier = null;
                        break;

                    case "n":
                    case "node":
                        edgeIdentifier = null;
                        vertexIdentifier = null;
                        relationIdentifier = null;
                        wayIdentifier = null;
                        break;

                    case "w":
                    case "way":
                        edgeIdentifier = null;
                        vertexIdentifier = null;
                        relationIdentifier = null;
                        nodeIdentifier = null;
                        break;

                    case "r":
                    case "relation":
                        edgeIdentifier = null;
                        vertexIdentifier = null;
                        nodeIdentifier = null;
                        wayIdentifier = null;
                        break;
                }
            }

            var matches = 0;

            if (edgeIdentifier != null)
            {
                matches++;
            }
            if (vertexIdentifier != null)
            {
                matches++;
            }
            if (nodeIdentifier != null)
            {
                matches++;
            }
            if (wayIdentifier != null)
            {
                matches++;
            }
            if (relationIdentifier != null)
            {
                matches++;
            }

            if (matches > 1)
            {
                return UserFeedback.text("That identifier is ambiguous in this graph."
                        + " Add one of the following prefixes to your search string: "
                        + " e/edge, v/vertex, n/node, w/way or r/relation");
            }

            if (edgeIdentifier != null)
            {
                layer.show(edgeIdentifier);
                return UserFeedback.status("Showing edge identifier $", edgeIdentifier);
            }

            if (vertexIdentifier != null)
            {
                layer.show(vertexIdentifier);
                return UserFeedback.status("Showing vertex identifier $", vertexIdentifier);
            }

            if (nodeIdentifier != null)
            {
                layer.show(nodeIdentifier);
                return UserFeedback.status("Showing node identifier $", nodeIdentifier);
            }

            if (wayIdentifier != null)
            {
                layer.show(wayIdentifier);
                return UserFeedback.status("Showing way identifier $", wayIdentifier);
            }

            if (relationIdentifier != null)
            {
                layer.show(relationIdentifier);
                return UserFeedback.status("Showing relation identifier $", relationIdentifier);
            }
        }

        if (!graph().isComposite())
        {
            var roads = findRoadName(searchString);
            if (!roads.isEmpty())
            {
                layer.show(roads, HIGHLIGHT_AND_ZOOM_TO);
                return UserFeedback.text("Ways matching '" + searchString + "':<br/><br/>" + roads.wayIdentifiers())
                        .withStatus("Showing all ways matching '" + searchString + "'");
            }
        }

        return help().withStatus("Couldn't find '" + searchString + "'");
    }

    private EdgeIdentifier findEdgeIdentifier(String searchString)
    {
        var identifier = new EdgeIdentifier.Converter(Listener.none()).convert(searchString);
        if (identifier != null && !graph().isComposite())
        {
            if (graph().contains(identifier))
            {
                return identifier;
            }
        }
        return null;
    }

    private Location findLocation(String searchString)
    {
        return new Location.DegreesConverter(Listener.none()).convert(searchString);
    }

    private MapEdgeIdentifier findMapEdgeIdentifier(String searchString)
    {
        var mapEdgeIdentifier = new MapEdgeIdentifier.Converter(Listener.none()).convert(searchString);
        if (mapEdgeIdentifier != null && !graph().isComposite())
        {
            if (graph().edgeForIdentifier(mapEdgeIdentifier) != null)
            {
                return mapEdgeIdentifier;
            }
        }
        return null;
    }

    private PbfNodeIdentifier findNodeIdentifier(String searchString)
    {
        var nodeIdentifier = new PbfNodeIdentifier.Converter(Listener.none()).convert(searchString);
        if (nodeIdentifier != null && !graph().isComposite())
        {
            if (graph().contains(nodeIdentifier))
            {
                return nodeIdentifier;
            }
        }
        return null;
    }

    private Polyline findPolyline(String searchString)
    {
        return new Polyline.Converter(Listener.none(), new Separators(":", ",")).convert(searchString);
    }

    private Rectangle findRectangle(String searchString)
    {
        return new Rectangle.Converter(Listener.none()).convert(searchString);
    }

    private MapRelationIdentifier findRelationIdentifier(String searchString)
    {
        var relationIdentifier = new PbfRelationIdentifier.Converter(Listener.none()).convert(searchString);
        if (relationIdentifier != null && !graph().isComposite())
        {
            var relation = graph().relationForMapRelationIdentifier(relationIdentifier);
            if (relation != null)
            {
                return relationIdentifier;
            }
        }
        return null;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private EdgeSet findRoadName(String searchString)
    {

        // Get standardized base road name we're looking for
        var standardizer = RoadNameStandardizer.get(MapLocale.ENGLISH_UNITED_STATES.get(), MESAKIT_STANDARDIZATION);
        var searchFor = standardizer.standardize(RoadName.forName(searchString)).baseName();
        var searchForUpperCase = searchFor.toUpperCase();

        var street = new EdgeSet(Estimate._1024);
        for (var edge : graph().edges())
        {
            var roadName = edge.roadName();
            if (roadName != null && roadName.name().toUpperCase().contains(searchForUpperCase))
            {
                var baseName = standardizer.standardize(roadName).baseName();
                if (baseName != null)
                {
                    if (searchForUpperCase.equals(baseName.toUpperCase()))
                    {
                        street.add(edge);
                    }
                }
            }
        }
        return street;
    }

    private Route findRoute(String searchString)
    {
        Edge.Converter edgeConverter;
        if (searchString.startsWith("cell-") && graph() instanceof WorldGraph)
        {
            edgeConverter = new WorldEdge.Converter((WorldGraph) graph(), Listener.none());
        }
        else
        {
            edgeConverter = new Edge.Converter(graph(), Listener.none());
        }
        var route = new Route.Converter(graph(), new Separators(":"), Listener.none(), edgeConverter)
                .convert(searchString);
        if (route != null)
        {
            if (graph().isComposite())
            {
                return route;
            }
            else
            {
                for (var edge : route)
                {
                    if (graph().contains(edge.identifier()))
                    {
                        return route;
                    }
                }
            }
        }
        return null;
    }

    private VertexIdentifier findVertexIdentifier(String searchString)
    {
        var vertexIdentifier = new VertexIdentifier.Converter(Listener.none()).convert(searchString);
        if (vertexIdentifier != null)
        {
            if (graph().contains(vertexIdentifier))
            {
                return vertexIdentifier;
            }
        }
        return null;
    }

    private PbfWayIdentifier findWayIdentifier(String searchString)
    {
        var wayIdentifier = new PbfWayIdentifier.Converter(Listener.none()).convert(searchString);
        if (wayIdentifier != null && !graph().isComposite())
        {
            if (graph().contains(wayIdentifier))
            {
                return wayIdentifier;
            }
        }
        return null;
    }

    private Graph graph()
    {
        return layer.graph();
    }

    private String openInBrowser()
    {
        var edge = layer.model().selection().selectedEdge();
        if (edge != null)
        {
            try
            {
                Desktop.getDesktop().browse(new URI("https://openstreetmap.org/way/" + edge.wayIdentifier().asLong()));
            }
            catch (Exception ignored)
            {
            }
            return "Showing " + edge.wayIdentifier();
        }
        else
        {
            return "No edge selected";
        }
    }

    private StringList turnStatistics(Rectangle bounds)
    {
        var statistics = new TurnRestrictionStatistics();
        statistics.addAll(graph().restrictionRelationsIntersecting(bounds));
        return statistics.summary();
    }
}
