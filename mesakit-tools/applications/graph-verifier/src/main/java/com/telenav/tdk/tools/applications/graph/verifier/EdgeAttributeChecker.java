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

import com.telenav.tdk.core.filesystem.Folder;
import com.telenav.tdk.core.kernel.commandline.CommandLineParser;
import com.telenav.tdk.core.kernel.commandline.SwitchParser;
import com.telenav.tdk.core.kernel.logging.Logger;
import com.telenav.tdk.core.kernel.logging.LoggerFactory;
import com.telenav.tdk.core.kernel.scalars.counts.Count;
import com.telenav.tdk.core.kernel.scalars.levels.Percentage;
import com.telenav.tdk.core.kernel.time.Time;
import com.telenav.tdk.core.resource.path.Extension;
import com.telenav.tdk.graph.Edge;
import com.telenav.tdk.graph.Graph;
import com.telenav.tdk.graph.io.load.SmartGraphLoader;
import com.telenav.tdk.graph.specifications.common.edge.EdgeAttributes;
import com.telenav.tdk.graph.specifications.library.attributes.Attribute;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validate validity of edge attributes
 *
 * @author songg
 */
public class EdgeAttributeChecker
{
    static final Set<Attribute<Edge>> scope_all = new HashSet<>();

    static final Set<Attribute<Edge>> scope_output = new HashSet<>();

    private static final Logger LOGGER = LoggerFactory.newLogger();

    private static final SwitchParser<SmartGraphLoader> GRAPH_RESOURCE = SmartGraphLoader.switchParser().required().build();

    private static final SwitchParser<Folder> OUTPUT_FOLDER = Folder.OUTPUT.required().build();

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

    public static void main(final String[] args)
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

        public CheckResult(final Folder outputFolder, final Graph graph)
        {
            this.outputFolder = outputFolder;
            this.graph = graph;
            outputFolder.ensureExists();
        }

        public void addFailed(final Edge edge, final Attribute<Edge> attribute) throws IOException
        {
            var count = attributeTypeCount.get(attribute);
            if (count == null)
            {
                count = Count._0;
            }

            count = count.add(1);
            attributeTypeCount.put(attribute, count);

            if (scope_output.contains(attribute))
            {
                final var writer = writerForType(attribute);
                writer.write(edge.identifierAsLong() + "\n");
            }
        }

        public void close() throws IOException
        {
            for (final var writer : attributeTypeWriters.values())
            {
                writer.close();
            }

            attributeTypeWriters.clear();
        }

        public void save() throws IOException
        {
            final Writer writer = outputFolder.file("failed-statistics.txt").printWriter();

            writer.write("total " + failed + " of " + graph.edgeCount() + " ("
                    + Count.of(failed).percentOf(graph.edgeCount()) + ") failed \n");

            for (final var entry : attributeTypeCount.entrySet())
            {
                final var attribute = entry.getKey();
                final var count = entry.getValue();
                writer.write(attribute + "\t" + count + "\t" + count.percentOf(graph.edgeCount()) + "\n");
            }

            writer.write("\nChecked Attributes:\n");

            for (final var attribute : graph.dataSpecification().attributes(graph.edgeStore().getClass()))
            {
                if (scope_all.contains(attribute))
                {
                    writer.write(attribute + "\t" + graph.supports(attribute) + "\n");
                }
            }

            writer.close();

            close();
        }

        public Writer writerForType(final Attribute<Edge> attribute)
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

    public boolean checkCountry(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.country() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkFreeFlowSpeed(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.freeFlowSpeed() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkLaneCount(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.laneCount() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkLength(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.length() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkOneWayFlag(final Edge edge)
    {
        var isValid = true;
        try
        {
            edge.isOneWay();
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkOsmFromNodeIdentifier(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.fromNodeIdentifier() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkOsmToNodeIdentifier(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.toNodeIdentifier() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkRoadShape(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.roadShape() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkRoadType(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.roadType() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    public boolean checkSpeedLimit(final Edge edge)
    {
        var isValid = true;
        try
        {
            isValid = (edge.speedLimit() != null);
        }
        catch (final Exception ex)
        {
            isValid = false;
        }

        return isValid;
    }

    private boolean check(final Edge edge, final Attribute<Edge> attribute)
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

    private void run(final String[] arguments)
    {
        final var commandLine = new CommandLineParser(GraphVerifierApplication.class)
                .add(GRAPH_RESOURCE)
                .add(OUTPUT_FOLDER)
                .parse(arguments);

        final var graph = commandLine.get(GRAPH_RESOURCE).load();
        final var outputFolder = commandLine.get(OUTPUT_FOLDER);

        final var time = Time.now();
        final var result = new CheckResult(outputFolder, graph);
        final var total = graph.edgeCount();
        try
        {
            var i = 0;
            for (final var edge : graph.edges())
            {
                i++;
                var failed = false;
                for (final var attribute : scope_all)
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
                    LOGGER.information("Finished ${debug} ${debug}", i, new Percentage(i * 100.0 / total.asInt()));
                }
            }

            result.save();
        }
        catch (final Exception ex)
        {
            ex.printStackTrace();
        }

        LOGGER.information("Finish check in ${debug} and output result to ${debug}", time.elapsedSince(), outputFolder);
    }
}
