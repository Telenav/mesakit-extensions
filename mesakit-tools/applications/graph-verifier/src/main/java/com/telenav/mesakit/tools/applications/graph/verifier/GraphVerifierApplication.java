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

package com.telenav.mesakit.tools.applications.graph.verifier;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.coredata.comparison.Differences;
import com.telenav.kivakit.collections.set.ObjectSet;
import com.telenav.kivakit.language.count.Count;
import com.telenav.kivakit.language.count.MutableCount;
import com.telenav.kivakit.resource.path.Extension;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.GraphProject;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.map.data.formats.library.map.identifiers.MapNodeIdentifier;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfNode;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfWay;
import com.telenav.mesakit.map.data.formats.pbf.model.identifiers.PbfWayIdentifier;
import com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.mesakit.map.data.formats.pbf.processing.readers.SerialPbfReader;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonDocument;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonFeature;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonPolyline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.telenav.kivakit.filesystem.File.fileArgumentParser;
import static com.telenav.kivakit.filesystem.File.fileSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor.Action.ACCEPTED;

/**
 * Converts a PBF input file into a graph then compares the graph against the PBF file. If a graph file is specified as
 * input, it can be compared against a sub-graph specified with the -subgraph switch.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class GraphVerifierApplication extends Application
{
    public static void main(String[] arguments)
    {
        new GraphVerifierApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            fileArgumentParser(this, "The graph to verify")
                    .required()
                    .build();

    private final SwitchParser<File> SUB_GRAPH =
            fileSwitchParser(this, "sub-graph", "The sub-graph to verify against the input graph")
                    .optional()
                    .build();

    private final GeoJsonDocument document = new GeoJsonDocument();

    private GraphVerifierApplication()
    {
        super(GraphProject.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @Override
    protected void onRun()
    {
        var input = argument(INPUT);
        var graph = new SmartGraphLoader(input).load(this);
        if (graph != null)
        {
            if (input.fileName().endsWith(Extension.OSM_PBF))
            {
                var reader = new SerialPbfReader(input);
                verify(graph, reader);
            }
            else
            {
                if (has(SUB_GRAPH))
                {
                    var subgraph = new SmartGraphLoader(get(SUB_GRAPH)).load(this);
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
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(SUB_GRAPH, QUIET);
    }

    private Edge matchingEdge(Graph graph, Edge edge)
    {
        var from = graph.vertexForNodeIdentifier(edge.fromNodeIdentifier());
        var to = graph.vertexForNodeIdentifier(edge.toNodeIdentifier());
        if (from != null && to != null)
        {
            Edge match = null;
            var minimumDifferences = Integer.MAX_VALUE;
            for (var candidate : from.edgesBetween(to))
            {
                var differences = edge.differencesFrom(candidate);
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

    private void verify(Graph subgraph, Graph graph)
    {
        for (var edge : subgraph.edges())
        {
            var worldEdge = matchingEdge(graph, edge);
            if (worldEdge != null)
            {
                var differences = edge.differencesFrom(worldEdge);
                if (differences.isDifferent())
                {
                    warning("$ edge $ is different from $ edge $: $", edge.graph().name(), edge,
                            worldEdge.graph().name(), worldEdge, differences);
                    var feature = new GeoJsonFeature();
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
        var output = File.parse(this, "differences.geojson");
        if (document.size() != 0)
        {
            document.save(output);
        }
        else
        {
            output.delete();
        }
    }

    private void verify(Graph graph, SerialPbfReader reader)
    {
        var nodes = new MutableCount();
        Set<PbfWayIdentifier> ways = new HashSet<>();
        var nodeTagDifferences = new MutableCount();
        var wayTagDifferences = new MutableCount();
        Set<MapNodeIdentifier> nodeIdentifiers = new HashSet<>();
        for (var vertex : graph.vertexes())
        {
            var identifier = vertex.mapIdentifier();
            if (identifier != null)
            {
                nodeIdentifiers.add(identifier);
            }
        }

        reader.process(new PbfDataProcessor()
        {
            @Override
            public Action onNode(PbfNode node)
            {
                var identifier = node.identifier();
                if (nodeIdentifiers.contains(identifier))
                {
                    var vertex = graph.vertexForNodeIdentifier(identifier);
                    if (vertex != null)
                    {
                        var differences = new Differences();
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
            public Action onWay(PbfWay way)
            {
                var wayIdentifier = way.identifier();
                if (!ways.contains(wayIdentifier))
                {
                    ways.add(wayIdentifier);
                    for (var edge : graph.routeForWayIdentifier(wayIdentifier))
                    {
                        var differences = new Differences();
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
        information("Checked " + Count.parseCount(this, ways.size() + " ways against " + graph.edgeCount()
                + " graph edges derived from " + graph.wayCount() + " ways"));

        information("Way tags had " + wayTagDifferences + " differences");
        information("Node tags had " + nodeTagDifferences + " differences");
    }
}
