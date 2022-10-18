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
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.level.Percent;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.resource.Extension;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.graph.specifications.common.edge.EdgeAttributes;
import com.telenav.mesakit.graph.specifications.library.attributes.Attribute;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.telenav.kivakit.core.collections.set.ObjectSet.set;
import static com.telenav.kivakit.filesystem.Folders.folderSwitchParser;
import static com.telenav.mesakit.graph.io.load.SmartGraphLoader.graphSwitchParser;

/**
 * Validate validity of edge attributes
 *
 * @author songg
 */
public class EdgeAttributeChecker extends Application
{
    static final Set<Attribute<Edge>> scope_all = new HashSet<>();

    static final Set<Attribute<Edge>> scope_output = new HashSet<>();

    static
    {
        scope_all.add(EdgeAttributes.get().COUNTRY);
        scope_all.add(EdgeAttributes.get().LANE_COUNT);
        scope_all.add(EdgeAttributes.get().LENGTH);
        scope_all.add(EdgeAttributes.get().ROAD_STATE);
        scope_all.add(EdgeAttributes.get().FROM_NODE_IDENTIFIER);
        scope_all.add(EdgeAttributes.get().TO_NODE_IDENTIFIER);
        scope_all.add(EdgeAttributes.get().ROAD_SHAPE);
        scope_all.add(EdgeAttributes.get().SPEED_LIMIT);
    }

    static
    {
        scope_output.add(EdgeAttributes.get().COUNTRY);
        scope_output.add(EdgeAttributes.get().LANE_COUNT);
        scope_output.add(EdgeAttributes.get().LENGTH);
        scope_output.add(EdgeAttributes.get().ROAD_STATE);
        scope_output.add(EdgeAttributes.get().FROM_NODE_IDENTIFIER);
        scope_output.add(EdgeAttributes.get().TO_NODE_IDENTIFIER);
        scope_output.add(EdgeAttributes.get().ROAD_SHAPE);
    }

    public static void main(String[] args)
    {
        new EdgeAttributeChecker().run(args);
    }

    public static class CheckResult
    {
        public int failed;

        private final Folder outputFolder;

        private final Graph graph;

        private final Map<Attribute<Edge>, Count> attributeTypeCount = new HashMap<>();

        private final Map<Attribute<Edge>, Writer> attributeTypeWriters = new HashMap<>();

        public CheckResult(Folder outputFolder, Graph graph)
        {
            this.outputFolder = outputFolder;
            this.graph = graph;
            outputFolder.ensureExists();
        }

        public void addFailed(Edge edge, Attribute<Edge> attribute) throws IOException
        {
            var count = attributeTypeCount.get(attribute);
            if (count == null)
            {
                count = Count._0;
            }

            count = count.plus(1);
            attributeTypeCount.put(attribute, count);

            if (scope_output.contains(attribute))
            {
                var writer = writerForType(attribute);
                writer.write(edge.identifierAsLong() + "\n");
            }
        }

        public void close() throws IOException
        {
            for (var writer : attributeTypeWriters.values())
            {
                writer.close();
            }

            attributeTypeWriters.clear();
        }

        public void save() throws IOException
        {
            Writer writer = outputFolder.file("failed-statistics.txt").printWriter();

            writer.write("total " + failed + " of " + graph.edgeCount() + " ("
                    + Count.count(failed).percentOf(graph.edgeCount()) + ") failed \n");

            for (var entry : attributeTypeCount.entrySet())
            {
                var attribute = entry.getKey();
                var count = entry.getValue();
                writer.write(attribute + "\t" + count + "\t" + count.percentOf(graph.edgeCount()) + "\n");
            }

            writer.write("\nChecked Attributes:\n");

            for (var attribute : graph.dataSpecification().attributes(graph.edgeStore().getClass()))
            {
                if (scope_all.contains(attribute))
                {
                    writer.write(attribute + "\t" + graph.supports(attribute) + "\n");
                }
            }

            writer.close();

            close();
        }

        public Writer writerForType(Attribute<Edge> attribute)
        {
            var writer = attributeTypeWriters.get(attribute);
            if (writer == null)
            {
                writer = outputFolder.file(attribute.name()).withExtension(Extension.TXT).printWriter();

                attributeTypeWriters.put(attribute, writer);
            }

            return writer;
        }
    }

    private final SwitchParser<SmartGraphLoader> GRAPH_RESOURCE = graphSwitchParser(this).required().build();

    private final SwitchParser<Folder> OUTPUT_FOLDER =
            folderSwitchParser(this, "output-folder", "Output folder")
                    .required()
                    .build();

    public boolean checkCountry(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.country() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkFreeFlowSpeed(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.freeFlowSpeed() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkLaneCount(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.laneCount() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkLength(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.length() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkOneWayFlag(Edge edge)
    {
        var isValid = true;
        try
        {
            edge.isOneWay();
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkOsmFromNodeIdentifier(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.fromNodeIdentifier() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkOsmToNodeIdentifier(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.toNodeIdentifier() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkRoadShape(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.roadShape() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkRoadType(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.roadType() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkSpeedLimit(Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.speedLimit() != null);
        }
        catch (Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    @Override
    protected void onRun()
    {
        var graph = get(GRAPH_RESOURCE).load();
        var outputFolder = get(OUTPUT_FOLDER);

        var time = Time.now();
        var result = new CheckResult(outputFolder, graph);
        var total = graph.edgeCount();
        try
        {
            var i = 0;
            for (var edge : graph.edges())
            {
                i++;
                var failed = false;
                for (var attribute : scope_all)
                {
                    if (!check(edge, attribute))
                    {
                        result.addFailed(edge, attribute);
                        failed = true;
                    }
                }

                if (failed)
                {
                    result.failed++;
                }

                if (i % 1000000 == 0)
                {
                    information("Finished ${debug} ${debug}", i, Percent.percent(i * 100.0 / total.asInt()));
                }
            }

            result.save();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        information("Finish check in ${debug} and output result to ${debug}", time.elapsedSince(), outputFolder);
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return set(GRAPH_RESOURCE, OUTPUT_FOLDER);
    }

    private boolean check(Edge edge, Attribute<Edge> attribute)
    {
        var isValid = true;
        if (attribute.equals(EdgeAttributes.get().COUNTRY))
        {
            isValid = checkCountry(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().LANE_COUNT))
        {
            isValid = checkLaneCount(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().LENGTH))
        {
            isValid = checkLength(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().ROAD_STATE))
        {
            isValid = checkOneWayFlag(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().FROM_NODE_IDENTIFIER))
        {
            isValid = checkOsmFromNodeIdentifier(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().TO_NODE_IDENTIFIER))
        {
            isValid = checkOsmToNodeIdentifier(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().FREE_FLOW_SPEED_CATEGORY))
        {
            isValid = checkFreeFlowSpeed(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().ROAD_SHAPE))
        {
            isValid = checkRoadShape(edge);
        }
        else if (attribute.equals(EdgeAttributes.get().SPEED_LIMIT))
        {
            isValid = checkSpeedLimit(edge);
        }

        return isValid;
    }
}
