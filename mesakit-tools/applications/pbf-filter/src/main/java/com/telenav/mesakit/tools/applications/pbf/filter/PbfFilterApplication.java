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

package com.telenav.mesakit.tools.applications.pbf.filter;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.language.collections.set.ObjectSet;
import com.telenav.kivakit.kernel.language.time.Time;
import com.telenav.kivakit.kernel.language.values.count.Estimate;
import com.telenav.kivakit.kernel.language.values.count.MutableCount;
import com.telenav.kivakit.primitive.collections.set.LongSet;
import com.telenav.mesakit.map.data.formats.pbf.PbfProject;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfNode;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfRelation;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfWay;
import com.telenav.mesakit.map.data.formats.pbf.model.tags.PbfTagPatternFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.readers.SerialPbfReader;
import com.telenav.mesakit.map.data.formats.pbf.processing.writers.PbfWriter;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;

import java.util.ArrayList;

import static com.telenav.kivakit.filesystem.File.fileSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.model.tags.PbfTagPatternFilter.tagFilterSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor.Action.ACCEPTED;
import static com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor.Action.FILTERED_OUT;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter.relationFilterSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter.wayFilterSwitchParser;

/**
 * PbfFilters the given PBF input file to the output file with the given tag filter. A way filter and/or relation filter
 * can optionally be applied as well.
 *
 * @author jonathanl (shibo)
 */
public class PbfFilterApplication extends Application
{
    public static void main(String[] arguments)
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
                    nodesAccepted.asCount().plus(nodesRejected.asCount()), nodesAccepted, nodesRejected);
            information("Relations before filtering ($), after filtering ($), removed ($) ",
                    relationsAccepted.asCount().plus(relationsRejected.asCount()), relationsAccepted,
                    relationsRejected);
            information("Ways before filtering ($), after filtering ($), removed ($) ",
                    waysAccepted.asCount().plus(waysRejected.asCount()), waysAccepted, waysRejected);
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
            fileSwitchParser(this, "input", "The PBF file to preprocess")
                    .required()
                    .build();

    private final SwitchParser<File> OUTPUT =
            fileSwitchParser(this, "output", "File to save the new PBF")
                    .required()
                    .build();

    private final SwitchParser<PbfTagPatternFilter> TAG_FILTER =
            tagFilterSwitchParser()
                    .required()
                    .build();

    private final SwitchParser<RelationFilter> RELATION_FILTER =
            relationFilterSwitchParser(this)
                    .optional()
                    .build();

    private final SwitchParser<WayFilter> WAY_FILTER =
            wayFilterSwitchParser(this)
                    .optional()
                    .build();

    private PbfFilterApplication()
    {
        super(PbfProject.get());
    }

    @Override
    protected void onRun()
    {
        var input = get(INPUT);
        var output = get(OUTPUT);
        var osmTagFilter = get(TAG_FILTER);
        var wayFilter = get(WAY_FILTER);
        var referencedNodes = referencedNodes(wayFilter, input);

        var relationFilter = get(RELATION_FILTER);

        var start = Time.now();
        var statistics = new Statistics();

        var writer = new PbfWriter(output, false);
        var reader = new SerialPbfReader(input);
        reader.process(new PbfDataProcessor()
        {
            @Override
            public void onBounds(Bound bound)
            {
                writer.write(bound);
            }

            @Override
            public Action onNode(PbfNode node)
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
            public Action onRelation(PbfRelation relation)
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
            public Action onWay(PbfWay way)
            {
                if (wayFilter != null && wayFilter.accepts(way))
                {
                    var tags = new ArrayList<>(way.tagList().asList());
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

        information("Filtered ($) ($) to $ ($) in $ ", input, input.sizeInBytes(), output, output.sizeInBytes(), start.elapsedSince());

        statistics.log();

        if (osmTagFilter != null)
        {
            var allTags = osmTagFilter.allFilteredTags();
            var rejectedTags = osmTagFilter.rejectedTags();
            information("Osm tag keys before filtering $, after filtering ($), removed ($):  $", allTags.size(),
                    allTags.size() - rejectedTags.size(), rejectedTags.size(), rejectedTags);
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(INPUT, OUTPUT, TAG_FILTER, RELATION_FILTER, WAY_FILTER, QUIET);
    }

    private static LongSet referencedNodes(WayFilter filter, File graphFile)
    {
        if (filter == null)
        {
            return null;
        }

        var referencedNodes = new LongSet("referencedNodes");
        referencedNodes.initialSize(Estimate._1024);
        referencedNodes.initialize();

        SerialPbfReader reader = new SerialPbfReader(graphFile);
        reader.process(new PbfDataProcessor()
        {
            @Override
            public Action onWay(PbfWay way)
            {
                if (filter.accepts(way))
                {
                    for (var node : way.nodes())
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
