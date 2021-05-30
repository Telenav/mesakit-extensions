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

package com.telenav.tdk.tools.applications.graph.verifier;

import com.telenav.tdk.core.application.TdkApplication;
import com.telenav.tdk.core.filesystem.File;
import com.telenav.tdk.core.kernel.commandline.*;
import com.telenav.tdk.core.kernel.comparison.Differences;
import com.telenav.tdk.core.kernel.scalars.counts.*;
import com.telenav.tdk.core.resource.path.Extension;
import com.telenav.tdk.data.formats.library.map.identifiers.*;
import com.telenav.tdk.data.formats.pbf.model.tags.*;
import com.telenav.tdk.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.tdk.data.formats.pbf.processing.readers.SerialPbfReader;
import com.telenav.tdk.graph.*;
import com.telenav.tdk.graph.io.load.SmartGraphLoader;
import com.telenav.tdk.graph.project.TdkGraphCore;
import com.telenav.tdk.map.utilities.geojson.*;

import java.util.*;

import static com.telenav.tdk.data.formats.pbf.processing.PbfDataProcessor.Result.ACCEPTED;

/**
 * Converts a PBF input file into a graph then compares the graph against the PBF file. If a graph file is specified as
 * input, it can be compared against a sub-graph specified with the -subgraph switch.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class GraphVerifierApplication extends TdkApplication
{
    public static void main(final String[] arguments)
    {
        new GraphVerifierApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            File.argumentParser("The graph to verify")
                    .required()
                    .build();

    private final SwitchParser<File> SUB_GRAPH =
            File.switchParser("sub-graph", "The sub-graph to verify against the input graph")
                    .optional()
                    .build();

    private final GeoJsonDocument document = new GeoJsonDocument();

    private GraphVerifierApplication()
    {
        super(TdkGraphCore.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @Override
    protected void onRun()
    {
        final var input = argument(INPUT);
        final var graph = new SmartGraphLoader(input).load(this);
        if (graph != null)
        {
            if (input.fileName().endsWith(Extension.OSM_PBF))
            {
                final var reader = new SerialPbfReader(input);
                verify(graph, reader);
            }
            else
            {
                if (has(SUB_GRAPH))
                {
                    final var subgraph = new SmartGraphLoader(get(SUB_GRAPH)).load(this);
                    verify(subgraph, graph);
                }
                else
                {
                    exit("If input file is not a PBF, -sub-graph must be used to specify a graph to compare against");
                }
            }
        }
        else
        {
            exit("Unable to load $", argument(INPUT));
        }
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(SUB_GRAPH, QUIET);
    }

    private Edge matchingEdge(final Graph graph, final Edge edge)
    {
        final var from = graph.vertexForNodeIdentifier(edge.fromNodeIdentifier());
        final var to = graph.vertexForNodeIdentifier(edge.toNodeIdentifier());
        if (from != null && to != null)
        {
            Edge match = null;
            var minimumDifferences = Integer.MAX_VALUE;
            for (final var candidate : from.edgesBetween(to))
            {
                final var differences = edge.differencesFrom(candidate);
                if (differences.isIdentical())
                {
                    return candidate;
                }
                if (differences.size() < minimumDifferences)
                {
                    minimumDifferences = differences.size();
                    match = candidate;
                }
            }
            return match;
        }
        return null;
    }

    private void verify(final Graph subgraph, final Graph graph)
    {
        for (final var edge : subgraph.edges())
        {
            final var worldEdge = matchingEdge(graph, edge);
            if (worldEdge != null)
            {
                final var differences = edge.differencesFrom(worldEdge);
                if (differences.isDifferent())
                {
                    warning("$ edge $ is different from $ edge $: $", edge.graph().name(), edge,
                            worldEdge.graph().name(), worldEdge, differences);
                    final var feature = new GeoJsonFeature();
                    feature.title(
                            "Edge " + edge.identifierAsLong() + " != " + worldEdge.identifierAsLong() + ": " + differences);
                    feature.add(new GeoJsonPolyline(edge.roadShape()));
                    feature.add(new GeoJsonPolyline(worldEdge.roadShape()));
                    document.add(feature);
                }
            }
            else
            {
                warning("$ edge $ not found in $", subgraph.name(), edge, graph.name());
            }
        }
        final var output = new File("differences.geojson");
        if (document.size() != 0)
        {
            document.save(output);
        }
        else
        {
            output.delete();
        }
    }

    private void verify(final Graph graph, final SerialPbfReader reader)
    {
        final var nodes = new MutableCount();
        final Set<WayIdentifier> ways = new HashSet<>();
        final var nodeTagDifferences = new MutableCount();
        final var wayTagDifferences = new MutableCount();
        final Set<NodeIdentifier> nodeIdentifiers = new HashSet<>();
        for (final var vertex : graph.vertexes())
        {
            final var identifier = vertex.mapIdentifier();
            if (identifier != null)
            {
                nodeIdentifiers.add(identifier);
            }
        }

        reader.process("Verifying", new PbfDataProcessor()
        {
            @Override
            public Result onNode(final PbfNode node)
            {
                final var identifier = node.identifier();
                if (nodeIdentifiers.contains(identifier))
                {
                    final var vertex = graph.vertexForNodeIdentifier(identifier);
                    if (vertex != null)
                    {
                        final var differences = new Differences();
                        if (!differences.compare("tags", vertex.tagList(), node.tagList()))
                        {
                            System.out.println("node " + node.identifier() + " (index = " + vertex.index()
                                    + ", id = " + vertex.mapIdentifier() + ") differs: " + differences);
                            nodeTagDifferences.increment();
                        }
                        nodes.increment();
                    }
                }
                return ACCEPTED;
            }

            @Override
            public Result onWay(final PbfWay way)
            {
                final var wayIdentifier = way.identifier();
                if (!ways.contains(wayIdentifier))
                {
                    ways.add(wayIdentifier);
                    for (final var edge : graph.routeForWayIdentifier(wayIdentifier))
                    {
                        final var differences = new Differences();
                        if (!differences.compare("tags", edge.tagList(), way.tagList()))
                        {
                            System.out.println("way " + way.identifier() + ": " + differences);
                            wayTagDifferences.increment();
                        }
                    }
                }
                return ACCEPTED;
            }
        });

        information("Checked " + nodes + " nodes against " + graph.vertexCount() + " graph vertexes");
        information("Checked " + Count.parse(ways.size() + " ways against " + graph.edgeCount()
                + " graph edges derived from " + graph.wayCount() + " ways"));

        information("Way tags had " + wayTagDifferences + " differences");
        information("Node tags had " + nodeTagDifferences + " differences");
    }
}
