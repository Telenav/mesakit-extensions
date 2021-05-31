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

package com.telenav.tdk.tools.applications.pbf.filter;

import com.telenav.kivakit.application.KivaKitApplication;
import com.telenav.kivakit.collections.primitive.set.LongSet;
import com.telenav.kivakit.data.formats.pbf.model.tags.*;
import com.telenav.kivakit.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.kivakit.data.formats.pbf.processing.filters.*;
import com.telenav.kivakit.data.formats.pbf.processing.readers.*;
import com.telenav.kivakit.data.formats.pbf.processing.writers.PbfWriter;
import com.telenav.kivakit.data.formats.pbf.project.KivaKitDataFormatsPbf;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.commandline.SwitchParser;
import com.telenav.kivakit.kernel.scalars.counts.*;
import com.telenav.kivakit.kernel.language.time.Time;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import java.util.ArrayList;
import java.util.Set;

import static com.telenav.kivakit.data.formats.pbf.processing.PbfDataProcessor.Result.*;

/**
 * PbfFilters the given PBF input file to the output file with the given tag filter. A way filter and/or relation filter
 * can optionally be applied as well.
 *
 * @author jonathanl (shibo)
 */
public class PbfFilterApplication extends KivaKitApplication
{
    public static void main(final String[] arguments)
    {
        new PbfFilterApplication().run(arguments);
    }

    private class Statistics
    {
        private final MutableCount nodesAccepted = new MutableCount();

        private final MutableCount relationsAccepted = new MutableCount();

        private final MutableCount waysAccepted = new MutableCount();

        private final MutableCount nodesRejected = new MutableCount();

        private final MutableCount relationsRejected = new MutableCount();

        private final MutableCount waysRejected = new MutableCount();

        void acceptNode()
        {
            nodesAccepted.increment();
        }

        void acceptRelation()
        {
            relationsAccepted.increment();
        }

        void acceptWay()
        {
            waysAccepted.increment();
        }

        void log()
        {
            information("Nodes before filtering ($), after filtering ($), removed ($) ",
                    nodesAccepted.asCount().add(nodesRejected.asCount()), nodesAccepted, nodesRejected);
            information("Relations before filtering ($), after filtering ($), removed ($) ",
                    relationsAccepted.asCount().add(relationsRejected.asCount()), relationsAccepted,
                    relationsRejected);
            information("Ways before filtering ($), after filtering ($), removed ($) ",
                    waysAccepted.asCount().add(waysRejected.asCount()), waysAccepted, waysRejected);
        }

        void rejectNode()
        {
            nodesRejected.increment();
        }

        void rejectRelation()
        {
            relationsRejected.increment();
        }

        void rejectWay()
        {
            waysRejected.increment();
        }
    }

    private final SwitchParser<File> INPUT =
            File.switchParser("input", "The PBF file to preprocess")
                    .required()
                    .build();

    private final SwitchParser<File> OUTPUT =
            File.switchParser("output", "File to save the new PBF")
                    .required()
                    .build();

    private final SwitchParser<PbfTagPatternFilter> TAG_FILTER =
            PbfTagPatternFilter.TAG_FILTER
                    .required()
                    .build();

    private final SwitchParser<RelationFilter> RELATION_FILTER =
            RelationFilter.relationFilter()
                    .optional()
                    .build();

    private final SwitchParser<WayFilter> WAY_FILTER =
            WayFilter.wayFilter()
                    .optional()
                    .build();

    private PbfFilterApplication()
    {
        super(KivaKitDataFormatsPbf.get());
    }

    @Override
    protected void onRun()
    {
        final var input = get(INPUT);
        final var output = get(OUTPUT);
        final var osmTagFilter = get(TAG_FILTER);
        final var wayFilter = get(WAY_FILTER);
        final var referencedNodes = referencedNodes(wayFilter, input);

        final var relationFilter = get(RELATION_FILTER);

        final var start = Time.now();
        final var statistics = new Statistics();

        final var writer = new PbfWriter(output, false);
        final var reader = new SerialPbfReader(input);
        reader.process("Filtering", new PbfDataProcessor()
        {
            @Override
            public void onBounds(final Bound bound)
            {
                writer.write(bound);
            }

            @Override
            public Result onNode(final PbfNode node)
            {
                if (referencedNodes == null || referencedNodes.contains(node.identifierAsLong()))
                {
                    writer.write(node);
                    statistics.acceptNode();
                    return ACCEPTED;
                }
                else
                {
                    statistics.rejectNode();
                    return FILTERED_OUT;
                }
            }

            @Override
            public Result onRelation(final PbfRelation relation)
            {
                if (relationFilter != null && relationFilter.accepts(relation))
                {
                    writer.write(relation);
                    statistics.acceptRelation();
                    return ACCEPTED;
                }
                else
                {
                    statistics.rejectRelation();
                    return FILTERED_OUT;
                }
            }

            @Override
            public Result onWay(final PbfWay way)
            {
                if (wayFilter != null && wayFilter.accepts(way))
                {
                    final var tags = new ArrayList<>(way.tagList().asList());
                    tags.removeIf(tag -> !osmTagFilter.accepts(tag));
                    writer.write(way.withTags(tags));
                    statistics.acceptWay();
                    return ACCEPTED;
                }
                else
                {
                    statistics.rejectWay();
                    return FILTERED_OUT;
                }
            }
        });

        writer.close();

        information("Filtered ($) ($) to $ ($) in $ ", input, input.size(), output, output.size(), start.elapsedSince());

        statistics.log();

        if (osmTagFilter != null)
        {
            final var allTags = osmTagFilter.allFilteredTags();
            final var rejectedTags = osmTagFilter.rejectedTags();
            information("Osm tag keys before filtering $, after filtering ($), removed ($):  $", allTags.size(),
                    allTags.size() - rejectedTags.size(), rejectedTags.size(), rejectedTags);
        }
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(INPUT, OUTPUT, TAG_FILTER, RELATION_FILTER, WAY_FILTER, QUIET);
    }

    private static LongSet referencedNodes(final WayFilter filter, final File graphFile)
    {
        if (filter == null)
        {
            return null;
        }

        final var referencedNodes = new LongSet("referencedNodes");
        referencedNodes.initialSize(Estimate._1024);
        referencedNodes.initialize();

        final PbfReader reader = new SerialPbfReader(graphFile);
        reader.process("Analyzing", new PbfDataProcessor()
        {
            @Override
            public Result onWay(final PbfWay way)
            {
                if (filter.accepts(way))
                {
                    for (final var node : way.nodes())
                    {
                        referencedNodes.add(node.getNodeId());
                    }
                    return ACCEPTED;
                }
                return FILTERED_OUT;
            }
        });

        return referencedNodes;
    }
}
