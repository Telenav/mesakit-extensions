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

package com.telenav.kivakit.tools.applications.pbf.comparator;

import com.telenav.kivakit.application.KivaKitApplication;
import com.telenav.kivakit.collections.set.SetDifferencer;
import com.telenav.kivakit.data.formats.pbf.model.tags.*;
import com.telenav.kivakit.data.formats.pbf.osm.Osm;
import com.telenav.kivakit.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.kivakit.data.formats.pbf.processing.readers.SerialPbfReader;
import com.telenav.kivakit.data.formats.pbf.project.KivaKitDataFormatsPbf;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.commandline.SwitchParser;
import com.telenav.kivakit.kernel.operation.progress.reporters.Progress;
import com.telenav.kivakit.kernel.time.Time;

import java.util.HashSet;
import java.util.Set;

import static com.telenav.kivakit.data.formats.pbf.processing.PbfDataProcessor.Result.ACCEPTED;

/**
 * Compares two PBF files
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PbfComparatorApplication extends KivaKitApplication
{
    public static void main(final String[] arguments)
    {
        new PbfComparatorApplication().run(arguments);
    }

    private final SwitchParser<File> BEFORE =
            File.switchParser("before", "The before PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<File> AFTER =
            File.switchParser("after", "The after PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<Boolean> COMPARE_NODES =
            SwitchParser.booleanSwitch("compareNodes", "True to compare nodes")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> COMPARE_WAYS =
            SwitchParser.booleanSwitch("compareWays", "True to compare ways")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final SwitchParser<Boolean> COMPARE_RELATIONS =
            SwitchParser.booleanSwitch("compareRelations", "True to compare relations")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> SHOW_REMOVED =
            SwitchParser.booleanSwitch("showRemoved", "True to show removed nodes, ways and relations")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final SwitchParser<Boolean> SHOW_ADDED =
            SwitchParser.booleanSwitch("showAdded", "True to show added nodes, ways and relations")
                    .optional()
                    .defaultValue(true)
                    .build();

    protected PbfComparatorApplication()
    {
        super(KivaKitDataFormatsPbf.get());
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

        new SerialPbfReader(before).process("Comparing", new PbfDataProcessor()
        {
            @Override
            public Result onNode(final PbfNode node)
            {
                if (get(COMPARE_NODES))
                {
                    beforeNodes.add(node.identifierAsLong());
                }
                beforeNodeProgress.next();
                return ACCEPTED;
            }

            @Override
            public Result onRelation(final PbfRelation relation)
            {
                if (get(COMPARE_RELATIONS))
                {
                    beforeRelations.add(relation.identifierAsLong());
                }
                beforeRelationsProgress.next();
                return ACCEPTED;
            }

            @Override
            public Result onWay(final PbfWay way)
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

        new SerialPbfReader(after).process("Comparing", new PbfDataProcessor()
        {
            @Override
            public Result onNode(final PbfNode node)
            {
                if (get(COMPARE_NODES))
                {
                    afterNodes.add(node.identifierAsLong());
                }
                afterNodeProgress.next();
                return ACCEPTED;
            }

            @Override
            public Result onRelation(final PbfRelation relation)
            {
                if (get(COMPARE_RELATIONS))
                {
                    afterRelations.add(relation.identifierAsLong());
                }
                afterRelationsProgress.next();
                return ACCEPTED;
            }

            @Override
            public Result onWay(final PbfWay way)
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
