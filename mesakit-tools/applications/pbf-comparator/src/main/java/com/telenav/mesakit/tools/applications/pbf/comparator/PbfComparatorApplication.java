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
import com.telenav.kivakit.collections.set.ObjectSet;
import com.telenav.kivakit.core.language.progress.reporters.Progress;
import com.telenav.kivakit.language.time.Time;
import com.telenav.mesakit.map.data.formats.pbf.PbfProject;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfNode;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfRelation;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfWay;
import com.telenav.mesakit.map.data.formats.pbf.osm.Osm;
import com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.mesakit.map.data.formats.pbf.processing.readers.SerialPbfReader;

import java.util.HashSet;
import java.util.Set;

import static com.telenav.kivakit.commandline.SwitchParsers.booleanSwitchParser;
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
    public static void main(String[] arguments)
    {
        new PbfComparatorApplication().run(arguments);
    }

    private final SwitchParser<File> BEFORE =
            fileSwitchParser(this, "before", "The before PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<File> AFTER =
            fileSwitchParser(this, "after", "The after PBF file to process")
                    .required()
                    .build();

    private final SwitchParser<Boolean> COMPARE_NODES =
            booleanSwitchParser(this, "compareNodes", "True to compare nodes")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> COMPARE_WAYS =
            booleanSwitchParser(this, "compareWays", "True to compare ways")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final SwitchParser<Boolean> COMPARE_RELATIONS =
            booleanSwitchParser(this, "compareRelations", "True to compare relations")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> SHOW_REMOVED =
            booleanSwitchParser(this, "showRemoved", "True to show removed nodes, ways and relations")
                    .optional()
                    .defaultValue(true)
                    .build();

    private final SwitchParser<Boolean> SHOW_ADDED =
            booleanSwitchParser(this, "showAdded", "True to show added nodes, ways and relations")
                    .optional()
                    .defaultValue(true)
                    .build();

    protected PbfComparatorApplication()
    {
        super(PbfProject.get());
    }

    public void compare(File before, File after)
    {
        var start = Time.now();

        Set<Long> beforeNodes = new HashSet<>();
        Set<Long> beforeWays = new HashSet<>();
        Set<Long> beforeRelations = new HashSet<>();

        var beforeNodeProgress = Progress.create(this);
        var beforeWayProgress = Progress.create(this);
        var beforeRelationsProgress = Progress.create(this);

        new SerialPbfReader(before).process(new PbfDataProcessor()
        {
            @Override
            public Action onNode(PbfNode node)
            {
                if (get(COMPARE_NODES))
                {
                    beforeNodes.add(node.identifierAsLong());
                }
                beforeNodeProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onRelation(PbfRelation relation)
            {
                if (get(COMPARE_RELATIONS))
                {
                    beforeRelations.add(relation.identifierAsLong());
                }
                beforeRelationsProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onWay(PbfWay way)
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

        Set<Long> afterNodes = new HashSet<>();
        Set<Long> afterWays = new HashSet<>();
        Set<Long> afterRelations = new HashSet<>();
        var afterNodeProgress = Progress.create(this);
        var afterWayProgress = Progress.create(this);
        var afterRelationsProgress = Progress.create(this);

        new SerialPbfReader(after).process(new PbfDataProcessor()
        {
            @Override
            public Action onNode(PbfNode node)
            {
                if (get(COMPARE_NODES))
                {
                    afterNodes.add(node.identifierAsLong());
                }
                afterNodeProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onRelation(PbfRelation relation)
            {
                if (get(COMPARE_RELATIONS))
                {
                    afterRelations.add(relation.identifierAsLong());
                }
                afterRelationsProgress.next();
                return ACCEPTED;
            }

            @Override
            public Action onWay(PbfWay way)
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

        SetDifferencer<Long> differencer = new SetDifferencer<>(null)
        {
            @Override
            protected void onAdded(Long id)
            {
                if (get(SHOW_ADDED))
                {
                    System.out.println("+" + id);
                }
            }

            @Override
            protected void onRemoved(Long id)
            {
                if (get(SHOW_REMOVED))
                {
                    System.out.println("-" + id);
                }
            }

            @Override
            protected void onUpdated(Long id)
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
        var before = get(BEFORE);
        if (!before.exists())
        {
            exit("Before file does not exist: " + before);
        }
        var after = get(AFTER);
        if (!after.exists())
        {
            exit("After file does not exist: " + after);
        }
        compare(before, after);
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(BEFORE, AFTER, SHOW_REMOVED, SHOW_ADDED, COMPARE_NODES, COMPARE_RELATIONS, COMPARE_WAYS, QUIET);
    }
}
