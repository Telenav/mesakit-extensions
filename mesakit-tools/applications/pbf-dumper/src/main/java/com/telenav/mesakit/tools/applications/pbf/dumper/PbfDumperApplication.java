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

package com.telenav.mesakit.tools.applications.pbf.dumper;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.ArgumentParsers;
import com.telenav.kivakit.filesystem.File;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfNode;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfRelation;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfWay;
import com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor;
import com.telenav.mesakit.map.data.formats.pbf.processing.readers.SerialPbfReader;

import java.util.List;

import static com.telenav.mesakit.map.data.formats.pbf.processing.PbfDataProcessor.Action.ACCEPTED;

/**
 * Dumps the given identifier from the given input file
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class PbfDumperApplication extends Application
{
    public static void main(String[] arguments)
    {
        new PbfDumperApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            File.fileArgumentParser(this, "The PBF file to preprocess")
                    .required()
                    .build();

    private final ArgumentParser<Long> IDENTIFIER =
            ArgumentParsers.longArgumentParser(this, "The node, way or relation identifier to find")
                    .required()
                    .build();

    private int turnRestrictions;

    private int noLeft;

    private int noRight;

    private int noStraightOn;

    private int noUTurn;

    private int onlyLeft;

    private int onlyRight;

    private int onlyStraightOn;

    private int other;

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT, IDENTIFIER);
    }

    @Override
    protected void onRun()
    {
        var file = argument(0, INPUT);
        var identifier = argument(1, IDENTIFIER);

        listenTo(new SerialPbfReader(file)).process(new PbfDataProcessor()
        {
            @Override
            public Action onNode(PbfNode node)
            {
                if (node.identifierAsLong() == identifier)
                {
                    System.out.println(node + " = " + node);
                }
                return ACCEPTED;
            }

            @SuppressWarnings("UseOfSystemOutOrSystemErr")
            @Override
            public Action onRelation(PbfRelation relation)
            {
                var found = false;
                if (relation.identifierAsLong() == identifier || identifier == -1L)
                {
                    found = true;
                }
                else
                {
                    for (var member : relation.members())
                    {
                        if (member.getMemberId() == identifier)
                        {
                            found = true;
                        }
                    }
                }
                if (found)
                {
                    for (var tag : relation)
                    {
                        if (tag.getKey() != null && "restriction".equalsIgnoreCase(tag.getKey()))
                        {
                            var value = tag.getValue().toLowerCase();
                            if (value.startsWith("no_"))
                            {
                                if ("no_left_turn".equalsIgnoreCase(value))
                                {
                                    noLeft++;
                                    turnRestrictions++;
                                }
                                else if ("no_right_turn".equalsIgnoreCase(value))
                                {
                                    noRight++;
                                    turnRestrictions++;
                                }
                                else if ("no_straight_on".equalsIgnoreCase(value))
                                {
                                    noStraightOn++;
                                    turnRestrictions++;
                                }
                                else if ("no_u_turn".equalsIgnoreCase(value))
                                {
                                    noUTurn++;
                                    turnRestrictions++;
                                }
                                else
                                {
                                    System.out.println(tag.getKey() + " = " + value);
                                    other++;
                                }
                            }
                            else if (value.startsWith("only_"))
                            {
                                if ("only_left_turn".equalsIgnoreCase(value))
                                {
                                    onlyLeft++;
                                    turnRestrictions++;
                                }
                                else if ("only_right_turn".equalsIgnoreCase(value))
                                {
                                    onlyRight++;
                                    turnRestrictions++;
                                }
                                else if ("only_straight_on".equalsIgnoreCase(value))
                                {
                                    onlyStraightOn++;
                                    turnRestrictions++;
                                }
                                else
                                {
                                    System.out.println(tag.getKey() + " = " + value);
                                    other++;
                                }
                            }
                            else
                            {
                                System.out.println(tag.getKey() + " = " + value);
                                // PbfDumperApplication.this.other++;
                            }
                        }
                    }
                }
                if (found)
                {
                    System.out.println(relation + " = " + relation);
                }
                return ACCEPTED;
            }

            @Override
            public Action onWay(PbfWay way)
            {
                if (way.identifierAsLong() == identifier)
                {
                    System.out.println(way + " = " + way);
                    for (var node : way.nodes())
                    {
                        System.out.println(" - node " + node.getNodeId() + " (" + node.getLatitude() + ", "
                                + node.getLongitude() + ")");
                    }
                }
                return ACCEPTED;
            }
        });
        if (identifier == -1L)
        {
            System.out.println("turn restrictions (excluding other) = " + turnRestrictions);
            System.out.println("no left turn restrictions = " + noLeft);
            System.out.println("no right turn restrictions = " + noRight);
            System.out.println("no straight on turn restrictions = " + noStraightOn);
            System.out.println("no u-turn restrictions = " + noUTurn);
            System.out.println("only left turn restrictions = " + onlyLeft);
            System.out.println("only right turn restrictions = " + onlyRight);
            System.out.println("only straight on turn restrictions = " + onlyStraightOn);
            System.out.println("others = " + other);
            if (turnRestrictions == (noLeft + noRight + noStraightOn + noUTurn + onlyLeft
                    + onlyRight + onlyStraightOn))
            {
                System.out.println("restrictions add up okay");
            }
            else
            {
                System.err.println("turn restrictions don't add up");
            }
        }
    }
}
