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

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.language.primitive.Doubles;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.string.Align;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.interfaces.comparison.Filter;
import com.telenav.kivakit.interfaces.comparison.Matcher;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.EdgeRelation;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.GraphProject;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
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

import static com.telenav.kivakit.commandline.SwitchParsers.booleanSwitchParser;
import static com.telenav.kivakit.core.collections.set.ObjectSet.objectSet;
import static com.telenav.kivakit.filesystem.Folder.folderSwitchParser;
import static com.telenav.mesakit.graph.io.load.SmartGraphLoader.graphArgumentParser;

/**
 * Analyze a Graph, printing out interesting statistics.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class GraphAnalyzerApplication extends Application
{
    public static void main(String[] arguments)
    {
        new GraphAnalyzerApplication().run(arguments);
    }

    private static class Result
    {
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public void add(String key, Object value)
        {
            properties.put(key, value);
        }

        public void write(PrintWriter writer)
        {
            for (var property : properties.entrySet())
            {
                writer.write(label(property.getKey()) + property.getValue() + "\n");
            }
            writer.write("\n");
        }

        private String label(String value)
        {
            return Align.right(value, 32, ' ') + ": ";
        }
    }

    private final SwitchParser<Boolean> BY_HIGHWAY_TYPE =
            booleanSwitchParser(this, "byHighwayType", "Show lengths by highway type")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final ArgumentParser<SmartGraphLoader> GRAPH_RESOURCE =
            graphArgumentParser(this, "The graph(s) to analyze")
                    .oneOrMore()
                    .build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            folderSwitchParser(this, "output-folder", "Output folder")
                    .optional()
                    .build();

    private final SwitchParser<Boolean> PRINT =
            booleanSwitchParser(this, "print", "Print output to the console")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final Set<EdgeRelation> all = new HashSet<>();

    private final Set<EdgeRelation> restrictions = new HashSet<>();

    protected GraphAnalyzerApplication()
    {
        addProject(GraphProject.class);
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(GRAPH_RESOURCE);
    }

    @Override
    protected void onRun()
    {
        boolean print = get(PRINT);
        boolean byHighwayType = get(BY_HIGHWAY_TYPE);
        var output = get(OUTPUT_FOLDER);
        for (var argument : argumentList())
        {
            var graph = argument.get(GRAPH_RESOURCE).load();
            var start = Time.now();
            graph.loadAll();
            information("Force-loaded in $", start.elapsedSince());
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
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return objectSet(PRINT, BY_HIGHWAY_TYPE, OUTPUT_FOLDER, QUIET);
    }

    private void analyze(boolean print, Folder output, Graph graph)
    {
        var matcherResult = analyze(graph, Filter.acceptAll());
        if (print)
        {
            var writer = new PrintWriter(System.out);
            matcherResult.write(writer);
            writer.flush();
        }
        if (output != null)
        {
            output.mkdirs();
            var file = output.file(graph.name() + ".analysis.txt");
            var writer = file.printWriter();
            matcherResult.write(writer);
            writer.close();
        }
    }

    private Result analyze(Graph graph, Matcher<Edge> matcher)
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
        var progress = BroadcastingProgressReporter.createProgressReporter(this);
        var ofType = new double[7];
        var other = 0D;
        var ferry = 0D;
        Set<EdgeRelation> visited = new HashSet<>();
        var turnRestrictions = 0;
        for (var relation : graph.relations())
        {
            if (relation.isTurnRestriction())
            {
                turnRestrictions++;
            }
        }
        var counter = 0;
        for (var edge : graph.edges().matching(matcher))
        {
            if (edge.osmIsServiceWay())
            {
                if (edge.isForward())
                {
                    counter++;
                    var miles = edge.length().asMiles();
                    total += miles;
                    serviceWays += miles;
                }
            }
            else
            {
                var relations = edge.turnRestrictionsBeginningAt();
                turnRestrictionsByEdgeTurnRestrictions += relations.size();
                restrictions.addAll(relations);
                for (var relation : edge.relations())
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
                                var type = relation.tagValue("restriction");
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
                    var miles = edge.length().asMiles();
                    total += miles;
                    var roadTypeIdentifier = edge.roadType().identifier();
                    var isNavigable = roadTypeIdentifier < RoadType.PRIVATE_ROAD.identifier();
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
        var result = new Result();
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
        result.add("NO LEFT TURN", Count.count(noLeftTurn).asCommaSeparatedString());
        result.add("NO RIGHT TURN", Count.count(noRightTurn).asCommaSeparatedString());
        result.add("NO U-TURN", Count.count(noUTurn).asCommaSeparatedString());
        result.add("NO STRAIGHT ON", Count.count(noStraightOn).asCommaSeparatedString());
        result.add("ONLY LEFT TURN", Count.count(onlyLeftTurn).asCommaSeparatedString());
        result.add("ONLY RIGHT TURN", Count.count(onlyRightTurn).asCommaSeparatedString());
        result.add("ONLY STRAIGHT ON", Count.count(onlyStraightOn).asCommaSeparatedString());
        result.add("OTHER TURN RESTRICTIONS", Count.count(otherTurnRestriction).asCommaSeparatedString());
        result.add("TURN RESTRICTIONS", Count.count(turnRestrictions).asCommaSeparatedString());
        result.add("TURN RESTRICTIONS BY Edge.turnRestrictions()",
                Count.count(turnRestrictionsByEdgeTurnRestrictions).asCommaSeparatedString());
        result.add("TURN RESTRICTIONS BY Edge.getRelations()",
                Count.count(turnRestrictionsByEdgeGetRelations).asCommaSeparatedString());

        all.removeAll(restrictions);
        var i = 0;
        for (var relation : all)
        {
            warning(i + ". missing restriction = " + relation);
            i++;
        }

        information("No. of edges collected: " + counter);
        return result;
    }

    private void analyzeByHighwayType(Graph graph)
    {
        Map<String, Distance> lengthForType = new HashMap<>();
        for (var edge : graph.forwardEdges())
        {
            var highwayTag = edge.tagList().get("highway");
            if (highwayTag != null)
            {
                var type = highwayTag.getValue();
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
        var keys = new StringList(Maximum.MAXIMUM, lengthForType.keySet());
        keys.sort(String::compareTo);
        for (var type : keys)
        {
            information("highway['" + type + "'] = " + lengthForType.get(type));
        }
    }

    private String miles(double miles)
    {
        return Count.count((int) Math.round(miles)).asCommaSeparatedString() + " miles";
    }

    private String miles(double miles, double total)
    {
        return miles(miles) + " (" + Doubles.format(miles / total * 100.0, 1) + "%)";
    }
}
