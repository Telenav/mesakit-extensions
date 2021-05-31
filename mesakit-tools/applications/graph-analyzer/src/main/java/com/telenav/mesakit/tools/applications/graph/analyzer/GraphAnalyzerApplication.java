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

package com.telenav.mesakit.tools.applications.graph.analyzer;

import com.telenav.kivakit.application.KivaKitApplication;
import com.telenav.kivakit.data.formats.pbf.osm.OsmHighwayTag;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.kernel.commandline.*;
import com.telenav.kivakit.kernel.interfaces.comparison.Matcher;
import com.telenav.kivakit.kernel.language.matching.All;
import com.telenav.kivakit.kernel.language.string.*;
import com.telenav.kivakit.kernel.language.vm.JavaVirtualMachine;
import com.telenav.kivakit.kernel.operation.progress.reporters.Progress;
import com.telenav.kivakit.kernel.scalars.counts.*;
import com.telenav.kivakit.kernel.time.Time;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.EdgeRelation;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.graph.project.GraphCore;
import com.telenav.mesakit.graph.specifications.common.edge.EdgeAttributes;
import com.telenav.mesakit.graph.specifications.common.relation.RelationAttributes;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.road.model.RoadType;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyze a Graph, printing out interesting statistics.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class GraphAnalyzerApplication extends KivaKitApplication
{
    public static void main(final String[] arguments)
    {
        new GraphAnalyzerApplication().run(arguments);
    }

    private static class Result
    {
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public void add(final String key, final Object value)
        {
            properties.put(key, value);
        }

        public void write(final PrintWriter writer)
        {
            for (final var property : properties.entrySet())
            {
                writer.write(label(property.getKey()) + property.getValue() + "\n");
            }
            writer.write("\n");
        }

        private String label(final String value)
        {
            return Strings.alignRight(value, 32, ' ') + ": ";
        }
    }

    private final ArgumentParser<SmartGraphLoader> GRAPH_RESOURCE =
            SmartGraphLoader.argumentParser("The graph(s) to analyze")
                    .oneOrMore()
                    .build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            Folder.OUTPUT
                    .optional()
                    .build();

    private final SwitchParser<Boolean> PRINT =
            SwitchParser.booleanSwitch("print", "Print output to the console")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final SwitchParser<Boolean> BY_HIGHWAY_TYPE =
            SwitchParser.booleanSwitch("byHighwayType", "Show lengths by highway type")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final Set<EdgeRelation> restrictions = new HashSet<>();

    private final Set<EdgeRelation> all = new HashSet<>();

    protected GraphAnalyzerApplication()
    {
        super(GraphCore.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(GRAPH_RESOURCE);
    }

    @Override
    protected void onRun()
    {
        final boolean print = get(PRINT);
        final boolean byHighwayType = get(BY_HIGHWAY_TYPE);
        final var output = get(OUTPUT_FOLDER);
        for (final var argument : arguments())
        {
            final var graph = argument.get(GRAPH_RESOURCE).load();
            final var start = Time.now();
            graph.loadAll();
            information("Force-loaded in $", start.elapsedSince());
            information("Rough graph size is $", JavaVirtualMachine.local().sizeOfObjectGraph(graph));
            if (byHighwayType)
            {
                if (graph.supports(EdgeAttributes.get().TAGS))
                {
                    analyzeByHighwayType(graph);
                }
                else
                {
                    information("Graph doesn't support PBF tags");
                }
            }
            else
            {
                analyze(print, output, graph);
            }
        }
        System.exit(0);
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(PRINT, BY_HIGHWAY_TYPE, OUTPUT_FOLDER, QUIET);
    }

    private void analyze(final boolean print, final Folder output, final Graph graph)
    {
        if (graph.supports(EdgeAttributes.get().TAGS))
        {
            final var matcher0Result = analyze(graph, matcher0());
            final var matcher1Result = analyze(graph, matcher1());
            final var matcher2Result = analyze(graph, matcher2());
            final var matcher3Result = analyze(graph, matcher3());
            final var matcher4Result = analyze(graph, matcher4());
            final var matcher5Result = analyze(graph, matcher5());
            if (print)
            {
                final var writer = new PrintWriter(System.out);
                matcher0Result.write(writer);
                matcher1Result.write(writer);
                matcher2Result.write(writer);
                matcher3Result.write(writer);
                matcher4Result.write(writer);
                matcher5Result.write(writer);
                writer.flush();
            }
            if (output != null)
            {
                output.mkdirs();
                final var file = output.file(graph.name() + ".analysis.txt");
                final var writer = file.printWriter();
                matcher0Result.write(writer);
                matcher1Result.write(writer);
                matcher2Result.write(writer);
                matcher3Result.write(writer);
                matcher4Result.write(writer);
                matcher5Result.write(writer);
                writer.close();
            }
        }
        else
        {
            final var matcherResult = analyze(graph, new All<>());
            if (print)
            {
                final var writer = new PrintWriter(System.out);
                matcherResult.write(writer);
                writer.flush();
            }
            if (output != null)
            {
                output.mkdirs();
                final var file = output.file(graph.name() + ".analysis.txt");
                final var writer = file.printWriter();
                matcherResult.write(writer);
                writer.close();
            }
        }
    }

    private Result analyze(final Graph graph, final Matcher<Edge> matcher)
    {
        var total = 0D;
        var serviceWays = 0D;
        var withNonEmptyName = 0D;
        var navigableWithNonEmptyName = 0D;
        var withFreeFlowSpeed = 0D;
        var withSpeedLimit = 0D;
        var withLaneCount = 0D;
        var withSurfaceTag = 0D;
        var oneWays = 0D;
        var navigableOneWays = 0D;
        var oneWayStreets = 0D;
        var noLeftTurn = 0;
        var noRightTurn = 0;
        var noUTurn = 0;
        var noStraightOn = 0;
        var onlyLeftTurn = 0;
        var onlyRightTurn = 0;
        var onlyStraightOn = 0;
        var otherTurnRestriction = 0;
        var turnRestrictionsByEdgeTurnRestrictions = 0;
        var turnRestrictionsByEdgeGetRelations = 0;
        final var progress = Progress.create(this);
        final var ofType = new double[7];
        var other = 0D;
        var ferry = 0D;
        final Set<EdgeRelation> visited = new HashSet<>();
        var turnRestrictions = 0;
        for (final var relation : graph.relations())
        {
            if (relation.isTurnRestriction())
            {
                turnRestrictions++;
            }
        }
        var counter = 0;
        for (final var edge : graph.edges().matching(matcher))
        {
            if (edge.osmIsServiceWay())
            {
                if (edge.isForward())
                {
                    counter++;
                    final var miles = edge.length().asMiles();
                    total += miles;
                    serviceWays += miles;
                }
            }
            else
            {
                final var relations = edge.turnRestrictionsBeginningAt();
                turnRestrictionsByEdgeTurnRestrictions += relations.size();
                restrictions.addAll(relations);
                for (final var relation : edge.relations())
                {
                    if (!visited.contains(relation))
                    {
                        visited.add(relation);
                        if (relation.isTurnRestriction())
                        {
                            turnRestrictionsByEdgeGetRelations++;
                            all.add(relation);
                        }
                        if (relation.supports(RelationAttributes.get().TAGS))
                        {
                            if ("restriction".equalsIgnoreCase(relation.tagValue("type")))
                            {
                                final var type = relation.tagValue("restriction");
                                if (type != null)
                                {
                                    if ("no_left_turn".equalsIgnoreCase(type))
                                    {
                                        noLeftTurn++;
                                    }
                                    else if ("no_right_turn".equalsIgnoreCase(type))
                                    {
                                        noRightTurn++;
                                    }
                                    else if ("no_u_turn".equalsIgnoreCase(type))
                                    {
                                        noUTurn++;
                                    }
                                    else if ("no_straight_on".equalsIgnoreCase(type))
                                    {
                                        noStraightOn++;
                                    }
                                    else if ("only_left_turn".equalsIgnoreCase(type))
                                    {
                                        onlyLeftTurn++;
                                    }
                                    else if ("only_right_turn".equalsIgnoreCase(type))
                                    {
                                        onlyRightTurn++;
                                    }
                                    else if ("only_straight_on".equalsIgnoreCase(type))
                                    {
                                        onlyStraightOn++;
                                    }
                                    else
                                    {
                                        otherTurnRestriction++;
                                    }
                                }
                            }
                        }
                    }
                }
                if (edge.isForward())
                {
                    counter++;
                    final var miles = edge.length().asMiles();
                    total += miles;
                    final var roadTypeIdentifier = edge.roadType().identifier();
                    final var isNavigable = roadTypeIdentifier < RoadType.PRIVATE_ROAD.identifier();
                    if (edge.roadName() != null)
                    {
                        withNonEmptyName += miles;
                        if (isNavigable)
                        {
                            navigableWithNonEmptyName += miles;
                        }
                    }
                    if (edge.isOneWay())
                    {
                        oneWays += miles;
                        if (isNavigable)
                        {
                            navigableOneWays += miles;
                            if (roadTypeIdentifier > RoadType.THROUGHWAY.identifier())
                            {
                                oneWayStreets += miles;
                            }
                        }
                    }
                    if (edge.freeFlowSpeed() != null)
                    {
                        withFreeFlowSpeed += miles;
                    }
                    if (edge.speedLimit() != null)
                    {
                        withSpeedLimit += miles;
                    }
                    if (edge.supports(EdgeAttributes.get().TAGS))
                    {
                        if (edge.tagValue("lanes") != null)
                        {
                            withLaneCount += miles;
                        }
                        if (edge.tagValue("surface") != null)
                        {
                            withSurfaceTag += miles;
                        }
                    }
                    if (roadTypeIdentifier < RoadType.PRIVATE_ROAD.identifier())
                    {
                        ofType[roadTypeIdentifier] += miles;
                    }
                    else
                    {
                        other += miles;
                        if (roadTypeIdentifier == RoadType.FERRY.identifier())
                        {
                            ferry += miles;
                        }
                    }
                }
            }
            progress.next();
        }
        final var result = new Result();
        result.add("GRAPH", graph.name());
        result.add("INCLUDING", matcher);
        result.add("TOTAL", miles(total));
        result.add("SERVICE", miles(serviceWays));
        result.add("NAVIGABLE", miles(total - (other + serviceWays)));
        result.add("OTHER", miles(other, total));
        result.add("FERRY", miles(ferry, total));
        result.add("ONE WAY", miles(oneWays, total));
        result.add("NAVIGABLE ONE WAY", miles(navigableOneWays, total));
        result.add("ONE WAY LOCAL_ROAD + LOW_SPEED_ROAD", miles(oneWayStreets, total));
        result.add("NAVIGABLE with non-empty name", miles(navigableWithNonEmptyName, total));
        result.add("With non-empty name", miles(withNonEmptyName, total));
        result.add("With free flow speed", miles(withFreeFlowSpeed, total));
        result.add("With speed limit", miles(withSpeedLimit, total));
        result.add("With lane count", miles(withLaneCount, total));
        result.add("With surface tag", miles(withSurfaceTag, total));
        for (var i = 0; i < ofType.length; i++)
        {
            result.add(RoadType.forIdentifier(i).toString(), miles(ofType[i], total));
        }
        result.add("FREEWAY + HIGHWAY",
                miles(ofType[RoadType.FREEWAY.identifier()] + ofType[RoadType.HIGHWAY.identifier()], total));
        result.add("FREEWAY + HIGHWAY + THROUGHWAY", miles(ofType[RoadType.FREEWAY.identifier()]
                + ofType[RoadType.HIGHWAY.identifier()] + ofType[RoadType.THROUGHWAY.identifier()], total));
        result.add("FREEWAY + HIGHWAY + URBAN_HIGHWAY + THROUGHWAY",
                miles(ofType[RoadType.FREEWAY.identifier()] + ofType[RoadType.HIGHWAY.identifier()]
                        + ofType[RoadType.URBAN_HIGHWAY.identifier()] + ofType[RoadType.THROUGHWAY.identifier()], total));
        result.add("LOCAL_ROAD + LOW_SPEED_ROAD", miles(
                ofType[RoadType.LOCAL_ROAD.identifier()] + ofType[RoadType.LOW_SPEED_ROAD.identifier()], total));
        result.add("NO LEFT TURN", Count.count(noLeftTurn).toCommaSeparatedString());
        result.add("NO RIGHT TURN", Count.count(noRightTurn).toCommaSeparatedString());
        result.add("NO U-TURN", Count.count(noUTurn).toCommaSeparatedString());
        result.add("NO STRAIGHT ON", Count.count(noStraightOn).toCommaSeparatedString());
        result.add("ONLY LEFT TURN", Count.count(onlyLeftTurn).toCommaSeparatedString());
        result.add("ONLY RIGHT TURN", Count.count(onlyRightTurn).toCommaSeparatedString());
        result.add("ONLY STRAIGHT ON", Count.count(onlyStraightOn).toCommaSeparatedString());
        result.add("OTHER TURN RESTRICTIONS", Count.count(otherTurnRestriction).toCommaSeparatedString());
        result.add("TURN RESTRICTIONS", Count.count(turnRestrictions).toCommaSeparatedString());
        result.add("TURN RESTRICTIONS BY Edge.turnRestrictions()",
                Count.count(turnRestrictionsByEdgeTurnRestrictions).toCommaSeparatedString());
        result.add("TURN RESTRICTIONS BY Edge.getRelations()",
                Count.count(turnRestrictionsByEdgeGetRelations).toCommaSeparatedString());

        all.removeAll(restrictions);
        var i = 0;
        for (final var relation : all)
        {
            warning(i + ". missing restriction = " + relation);
            i++;
        }

        information("No. of edges collected: " + counter);
        return result;
    }

    private void analyzeByHighwayType(final Graph graph)
    {
        final Map<String, Distance> lengthForType = new HashMap<>();
        for (final var edge : graph.forwardEdges())
        {
            final var highwayTag = edge.tagList().get("highway");
            if (highwayTag != null)
            {
                final var type = highwayTag.getValue();
                if (type != null)
                {
                    var length = lengthForType.get(type);
                    if (length == null)
                    {
                        length = Distance.ZERO;
                    }
                    lengthForType.put(type, length.add(edge.length()));
                }
            }
        }
        final var keys = new StringList(Maximum.MAXIMUM, lengthForType.keySet());
        keys.sort(String::compareTo);
        for (final var type : keys)
        {
            information("highway['" + type + "'] = " + lengthForType.get(type));
        }
    }

    private HereEdgeMatcher matcher0()
    {
        return new HereEdgeMatcher(OsmHighwayTag.MOTORWAY, OsmHighwayTag.MOTORWAY_LINK, OsmHighwayTag.TRUNK,
                OsmHighwayTag.TRUNK_LINK, OsmHighwayTag.PRIMARY, OsmHighwayTag.PRIMARY_LINK);
    }

    private HereEdgeMatcher matcher1()
    {
        return new HereEdgeMatcher(OsmHighwayTag.MOTORWAY, OsmHighwayTag.MOTORWAY_LINK, OsmHighwayTag.TRUNK,
                OsmHighwayTag.TRUNK_LINK, OsmHighwayTag.PRIMARY, OsmHighwayTag.PRIMARY_LINK, OsmHighwayTag.SECONDARY,
                OsmHighwayTag.SECONDARY_LINK, OsmHighwayTag.TERTIARY, OsmHighwayTag.TERTIARY_LINK,
                OsmHighwayTag.RESIDENTIAL, OsmHighwayTag.RESIDENTIAL_LINK);
    }

    private HereEdgeMatcher matcher2()
    {
        return new HereEdgeMatcher(OsmHighwayTag.MOTORWAY, OsmHighwayTag.MOTORWAY_LINK, OsmHighwayTag.TRUNK,
                OsmHighwayTag.TRUNK_LINK, OsmHighwayTag.PRIMARY, OsmHighwayTag.PRIMARY_LINK, OsmHighwayTag.SECONDARY,
                OsmHighwayTag.SECONDARY_LINK, OsmHighwayTag.TERTIARY, OsmHighwayTag.TERTIARY_LINK,
                OsmHighwayTag.RESIDENTIAL, OsmHighwayTag.RESIDENTIAL_LINK, OsmHighwayTag.PRIVATE, OsmHighwayTag.DRIVEWAY,
                OsmHighwayTag.UNCLASSIFIED, OsmHighwayTag.SERVICE);
    }

    private HereEdgeMatcher matcher3()
    {
        return new HereEdgeMatcher(OsmHighwayTag.MOTORWAY, OsmHighwayTag.MOTORWAY_LINK, OsmHighwayTag.TRUNK,
                OsmHighwayTag.TRUNK_LINK, OsmHighwayTag.PRIMARY, OsmHighwayTag.PRIMARY_LINK, OsmHighwayTag.SECONDARY,
                OsmHighwayTag.SECONDARY_LINK, OsmHighwayTag.TERTIARY, OsmHighwayTag.TERTIARY_LINK,
                OsmHighwayTag.RESIDENTIAL, OsmHighwayTag.RESIDENTIAL_LINK, OsmHighwayTag.PRIVATE, OsmHighwayTag.DRIVEWAY,
                OsmHighwayTag.UNCLASSIFIED, OsmHighwayTag.SERVICE, OsmHighwayTag.REST_AREA, OsmHighwayTag.ROAD,
                OsmHighwayTag.TRACK, OsmHighwayTag.UNDEFINED, OsmHighwayTag.UNKNOWN, OsmHighwayTag.LIVING_STREET);
    }

    private Matcher<Edge> matcher4()
    {
        return new All<>();
    }

    private Matcher<Edge> matcher5()
    {
        return new HereEdgeMatcher(OsmHighwayTag.MOTORWAY, OsmHighwayTag.MOTORWAY_LINK, OsmHighwayTag.TRUNK,
                OsmHighwayTag.TRUNK_LINK, OsmHighwayTag.PRIMARY, OsmHighwayTag.PRIMARY_LINK, OsmHighwayTag.SECONDARY,
                OsmHighwayTag.SECONDARY_LINK, OsmHighwayTag.TERTIARY, OsmHighwayTag.TERTIARY_LINK,
                OsmHighwayTag.RESIDENTIAL, OsmHighwayTag.RESIDENTIAL_LINK, OsmHighwayTag.UNCLASSIFIED,
                OsmHighwayTag.SERVICE, OsmHighwayTag.TRACK, OsmHighwayTag.FOOTWAY, OsmHighwayTag.PEDESTRIAN,
                OsmHighwayTag.STEPS, OsmHighwayTag.BRIDLEWAY, OsmHighwayTag.CONSTRUCTION, OsmHighwayTag.CYCLEWAY,
                OsmHighwayTag.PATH, OsmHighwayTag.BUS_GUIDEWAY);
    }

    private String miles(final double miles)
    {
        return Count.count((int) Math.round(miles)).toCommaSeparatedString() + " miles";
    }

    private String miles(final double miles, final double total)
    {
        return miles(miles) + " (" + Strings.format(miles / total * 100.0, 1) + "%)";
    }
}
