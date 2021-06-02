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

package com.telenav.mesakit.tools.applications.pbf.comparator;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.collections.set.SetDifferencer;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.language.progress.reporters.Progress;
import com.telenav.kivakit.kernel.language.time.Time;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfNode;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfRelation;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfWay;
import com.telenav.mesakit.map.data.formats.pbf.osm.Osm;
import com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.mesakit.map.data.formats.pbf.processing.readers.SerialPbfReader;
import com.telenav.mesakit.map.data.formats.pbf.project.DataFormatsPbfProject;

import java.util.HashSet;
import java.util.Set;

import static com.telenav.kivakit.commandline.SwitchParser.booleanSwitchParser;
import static com.telenav.kivakit.filesystem.File.fileSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor.Action.ACCEPTED;

/**
 * Compares two PBF files
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PbfComparatorApplication extends Application
{
    public static void main(final String[] arguments)
    {
        new PbfComparatorApplication().run(arguments);
    }

    private final SwitchParser<File> BEFORE =
            fileSwitchParser("before", "The before PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<File> AFTER =
            fileSwitchParser("after", "The after PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<Boolean> COMPARE_NODES =
            booleanSwitchParser("compareNodes", "True to compare nodes")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> COMPARE_WAYS =
            booleanSwitchParser("compareWays", "True to compare ways")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final SwitchParser<Boolean> COMPARE_RELATIONS =
            booleanSwitchParser("compareRelations", "True to compare relations")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> SHOW_REMOVED =
            booleanSwitchParser("showRemoved", "True to show removed nodes, ways and relations")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final SwitchParser<Boolean> SHOW_ADDED =
            booleanSwitchParser("showAdded", "True to show added nodes, ways and relations")
                    .optional()
                    .defaultValue(true)
                    .build();

    protected PbfComparatorApplication()
    {
        super(DataFormatsPbfProject.get());
    }

    public void compare(final File before, final File after)
    {
        final var start = Time.now();

        final Set<Long> beforeNodes = new HashSet<>();
        final Set<Long> beforeWays = new HashSet<>();
        final Set<Long> beforeRelations = new HashSet<>();

        final var beforeNodeProgress = Progress.create(this);
        final var beforeWayProgress = Progress.create(this);
        final var beforeRelationsProgress = Progress.create(this);

        new SerialPbfReader(before).process(new PbfDataProcessor()
        {
            @Override
            public Action onNode(final PbfNode node)
            {
                if (get(COMPARE_NODES))
                {
                    beforeNodes.add(node.identifierAsLong());
                }
                beforeNodeProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onRelation(final PbfRelation relation)
            {
                if (get(COMPARE_RELATIONS))
                {
                    beforeRelations.add(relation.identifierAsLong());
                }
                beforeRelationsProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onWay(final PbfWay way)
            {
                if (get(COMPARE_WAYS))
                {
                    if (Osm.isNavigable(way))
                    {
                        beforeWays.add(way.identifierAsLong());
                    }
                }
                beforeWayProgress.next();
                return ACCEPTED;
            }
        });

        final Set<Long> afterNodes = new HashSet<>();
        final Set<Long> afterWays = new HashSet<>();
        final Set<Long> afterRelations = new HashSet<>();
        final var afterNodeProgress = Progress.create(this);
        final var afterWayProgress = Progress.create(this);
        final var afterRelationsProgress = Progress.create(this);

        new SerialPbfReader(after).process(new PbfDataProcessor()
        {
            @Override
            public Action onNode(final PbfNode node)
            {
                if (get(COMPARE_NODES))
                {
                    afterNodes.add(node.identifierAsLong());
                }
                afterNodeProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onRelation(final PbfRelation relation)
            {
                if (get(COMPARE_RELATIONS))
                {
                    afterRelations.add(relation.identifierAsLong());
                }
                afterRelationsProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onWay(final PbfWay way)
            {
                if (get(COMPARE_WAYS))
                {
                    if (Osm.isNavigable(way))
                    {
                        afterWays.add(way.identifierAsLong());
                    }
                }
                afterWayProgress.next();
                return ACCEPTED;
            }
        });

        final SetDifferencer<Long> differencer = new SetDifferencer<>(null)
        {
            @Override
            protected void onAdded(final Long id)
            {
                if (get(SHOW_ADDED))
                {
                    System.out.println("+" + id);
                }
            }

            @Override
            protected void onRemoved(final Long id)
            {
                if (get(SHOW_REMOVED))
                {
                    System.out.println("-" + id);
                }
            }

            @Override
            protected void onUpdated(final Long id)
            {
            }
        };
        if (get(COMPARE_NODES))
        {
            information("Comparing nodes");
            differencer.compare(beforeNodes, afterNodes);
        }
        if (get(COMPARE_WAYS))
        {
            information("Comparing ways");
            differencer.compare(beforeWays, afterWays);
        }
        if (get(COMPARE_RELATIONS))
        {
            information("Comparing relations");
            differencer.compare(beforeRelations, afterRelations);
        }
        information("Comparison took $", start.elapsedSince());
    }

    @Override
    protected void onRun()
    {
        final var before = get(BEFORE);
        if (!before.exists())
        {
            exit("Before file does not exist: " + before);
        }
        final var after = get(AFTER);
        if (!after.exists())
        {
            exit("After file does not exist: " + after);
        }
        compare(before, after);
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(BEFORE, AFTER, SHOW_REMOVED, SHOW_ADDED, COMPARE_NODES, COMPARE_RELATIONS, COMPARE_WAYS, QUIET);
    }
}
