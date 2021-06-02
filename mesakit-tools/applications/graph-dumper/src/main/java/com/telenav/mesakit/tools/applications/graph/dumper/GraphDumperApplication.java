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

package com.telenav.mesakit.tools.applications.graph.dumper;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.language.iteration.Streams;
import com.telenav.kivakit.kernel.language.strings.AsciiArt;
import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.kernel.language.vm.JavaVirtualMachine;
import com.telenav.kivakit.kernel.messaging.Message;
import com.telenav.kivakit.resource.path.Extension;
import com.telenav.mesakit.graph.GraphElement;
import com.telenav.mesakit.graph.identifiers.EdgeIdentifier;
import com.telenav.mesakit.graph.identifiers.PlaceIdentifier;
import com.telenav.mesakit.graph.identifiers.RelationIdentifier;
import com.telenav.mesakit.graph.identifiers.VertexIdentifier;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.graph.project.GraphCore;
import com.telenav.mesakit.map.data.formats.pbf.model.identifiers.PbfWayIdentifier;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;

import java.util.List;
import java.util.Set;

import static com.telenav.kivakit.commandline.SwitchParser.booleanSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParser.countSwitchParser;
import static com.telenav.kivakit.filesystem.File.fileSwitchParser;
import static com.telenav.mesakit.graph.identifiers.EdgeIdentifier.edgeIdentifierSwitchParser;
import static com.telenav.mesakit.graph.identifiers.PlaceIdentifier.placeIdentifierSwitchParser;
import static com.telenav.mesakit.graph.identifiers.RelationIdentifier.relationIdentifierSwitchParser;
import static com.telenav.mesakit.graph.identifiers.VertexIdentifier.vertexIdentifierSwitchParser;
import static com.telenav.mesakit.graph.io.load.SmartGraphLoader.graphArgumentParser;
import static com.telenav.mesakit.map.data.formats.pbf.model.identifiers.PbfWayIdentifier.pbfWayIdentifierSwitchParser;

/**
 * Dumps graph resources. A specific edge, vertex, relation, way or place can be shown. The spatial index can also be
 * dumped.
 *
 * @author jonathanl (shibo)
 */
public class GraphDumperApplication extends Application
{
    private static final ArgumentParser<SmartGraphLoader> INPUT =
            graphArgumentParser("The graph to dump")
                    .required()
                    .build();

    private static final SwitchParser<EdgeIdentifier> EDGE =
            edgeIdentifierSwitchParser("edge", "A specific edge identifier to dump")
                    .optional()
                    .build();

    private static final SwitchParser<VertexIdentifier> VERTEX =
            vertexIdentifierSwitchParser("vertex", "A specific vertex identifier to dump")
                    .optional()
                    .build();

    private static final SwitchParser<RelationIdentifier> RELATION =
            relationIdentifierSwitchParser("relation", "A specific relation identifier to dump")
                    .optional()
                    .build();

    private static final SwitchParser<PbfWayIdentifier> WAY =
            pbfWayIdentifierSwitchParser("way", "A specific way identifier to dump")
                    .optional()
                    .build();

    private static final SwitchParser<PlaceIdentifier> PLACE =
            placeIdentifierSwitchParser("place", "A specific place identifier to dump")
                    .optional()
                    .build();

    private static final SwitchParser<Boolean> SPATIAL_INDEX =
            booleanSwitchParser("spatial-index", "Dump the spatial index")
                    .optional()
                    .build();

    private static final SwitchParser<Boolean> DUMP_EDGE_IDENTIFIERS =
            booleanSwitchParser("all-edge-identifiers", "Dump all edge identifiers to the console")
                    .optional()
                    .defaultValue(false)
                    .build();

    private static final SwitchParser<File> DUMP_HEAP =
            fileSwitchParser("dump-heap-to", "Force load all data and dump graph heap to the given file")
                    .optional()
                    .build();

    private static final SwitchParser<Count> LIMIT =
            countSwitchParser("entity-limit", "The maximum number of edges, vertexes and relations to dump")
                    .optional()
                    .defaultValue(Count.MAXIMUM)
                    .build();

    public static void main(final String[] arguments)
    {
        new GraphDumperApplication().run(arguments);
    }

    protected GraphDumperApplication()
    {
        super(GraphCore.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @SuppressWarnings({ "UseOfSystemOutOrSystemErr", "SpellCheckingInspection" })
    @Override
    protected void onRun()
    {
        final var graph = argument(INPUT).load();

        if (has(DUMP_HEAP))
        {
            graph.loadAll();
            final var file = get(DUMP_HEAP);
            file.delete();
            file.withExtension(Extension.parse(".idom"));
            information("Dumping heap to: $", file);
            JavaVirtualMachine.local().dumpHeap(file.path().asJavaPath());
        }
        else if (get(DUMP_EDGE_IDENTIFIERS))
        {
            for (final var edge : graph.edges())
            {
                System.out.println(edge.identifier());
            }
        }
        else if (has(WAY))
        {
            for (final var edge : graph.routeForWayIdentifier(get(WAY)))
            {
                dump(edge);
            }
        }
        else if (has(EDGE))
        {
            dump(graph.edgeForIdentifier(get(EDGE)));
        }
        else if (has(SPATIAL_INDEX))
        {
            graph.edgeStore().spatialIndex().dump(System.out);
            var number = 1;
            for (final var edge : graph.edgesIntersecting(Rectangle.CONTINENTAL_US))
            {
                Message.println("edge $: $", number++, edge);
            }
        }
        else if (has(VERTEX))
        {
            dump(graph.vertexForIdentifier(get(VERTEX)));
        }
        else if (has(RELATION))
        {
            dump(graph.relationForIdentifier(get(RELATION)));
        }
        else if (has(PLACE))
        {
            dump(graph.placeForIdentifier(get(PLACE)));
        }
        else
        {
            final var limit = get(LIMIT);
            graph.edges().stream().limit(limit.asInt()).forEach(this::dump);
            graph.vertexes().stream().limit(limit.asInt()).forEach(this::dump);
            Streams.stream(graph.relations()).limit(limit.asInt()).forEach(this::dump);
            Streams.stream(graph.places()).limit(limit.asInt()).forEach(this::dump);
        }
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of
                (
                        WAY,
                        EDGE,
                        VERTEX,
                        RELATION,
                        PLACE,
                        LIMIT,
                        SPATIAL_INDEX,
                        DUMP_EDGE_IDENTIFIERS,
                        DUMP_HEAP,
                        QUIET
                );
    }

    private void dump(final GraphElement element)
    {
        information(AsciiArt.line(element.name()));
        information(element.asString());
    }
}
