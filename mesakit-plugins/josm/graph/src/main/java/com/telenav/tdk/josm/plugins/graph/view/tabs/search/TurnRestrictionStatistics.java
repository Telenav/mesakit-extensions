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

package com.telenav.tdk.josm.plugins.graph.view.tabs.search;

import com.telenav.tdk.core.kernel.language.string.StringList;
import com.telenav.tdk.core.kernel.scalars.counts.Count;
import com.telenav.tdk.graph.EdgeRelation;

public class TurnRestrictionStatistics
{
    private int restrictions;

    private int noLefts;

    private int noRights;

    private int noStraightOns;

    private int noUs;

    private int onlyLefts;

    private int onlyRights;

    private int onlyStraightOns;

    private int badRestrictions;

    private int badNoLefts;

    private int badNoRights;

    private int badNoStraightOns;

    private int badNoUs;

    private int badOnlyLefts;

    private int badOnlyRights;

    private int badOnlyStraightOns;

    public void add(final EdgeRelation relation)
    {
        if (relation.isTurnRestriction())
        {
            final var restriction = relation.turnRestriction();
            add(relation, restriction.isBad());
        }
    }

    public void add(final EdgeRelation relation, final boolean bad)
    {
        if (relation.isTurnRestriction())
        {
            switch (relation.turnRestrictionType())
            {
                case NO_LEFT:
                    if (bad)
                    {
                        badNoLefts++;
                    }
                    else
                    {
                        noLefts++;
                    }
                    break;

                case NO_RIGHT:
                    if (bad)
                    {
                        badNoRights++;
                    }
                    else
                    {
                        noRights++;
                    }
                    break;

                case NO_STRAIGHT_ON:
                    if (bad)
                    {
                        badNoStraightOns++;
                    }
                    else
                    {
                        noStraightOns++;
                    }
                    break;

                case NO_U:
                    if (bad)
                    {
                        badNoUs++;
                    }
                    else
                    {
                        noUs++;
                    }
                    break;

                case ONLY_LEFT:
                    if (bad)
                    {
                        badOnlyLefts++;
                    }
                    else
                    {
                        onlyLefts++;
                    }
                    break;

                case ONLY_RIGHT:
                    if (bad)
                    {
                        badOnlyRights++;
                    }
                    else
                    {
                        onlyRights++;
                    }
                    break;

                case ONLY_STRAIGHT_ON:
                    if (bad)
                    {
                        badOnlyStraightOns++;
                    }
                    else
                    {
                        onlyStraightOns++;
                    }
                    break;

                default:
                    break;
            }
            if (bad)
            {
                badRestrictions++;
            }
            restrictions++;
        }
    }

    public void addAll(final Iterable<EdgeRelation> relations)
    {
        relations.forEach(this::add);
    }

    public int badRestrictions()
    {
        return badRestrictions;
    }

    public int restrictions()
    {
        return restrictions;
    }

    public StringList summary()
    {
        final var turns = new StringList();
        turns.add("restrictions = " + Count.of(restrictions));
        turns.add("no lefts = " + Count.of(noLefts));
        turns.add("no rights = " + Count.of(noRights));
        turns.add("no straight ons = " + Count.of(noStraightOns));
        turns.add("no u-turns = " + Count.of(noUs));
        turns.add("only lefts = " + Count.of(onlyLefts));
        turns.add("only rights = " + Count.of(onlyRights));
        turns.add("only straight ons = " + Count.of(onlyStraightOns));
        turns.add("bad restrictions = " + Count.of(badRestrictions));
        turns.add("bad no lefts = " + Count.of(badNoLefts));
        turns.add("bad no rights = " + Count.of(badNoRights));
        turns.add("bad no straight ons = " + Count.of(badNoStraightOns));
        turns.add("bad no u-turns = " + Count.of(badNoUs));
        turns.add("bad only lefts = " + Count.of(badOnlyLefts));
        turns.add("bad only rights = " + Count.of(badOnlyRights));
        turns.add("bad only straight ons = " + Count.of(badOnlyStraightOns));
        return turns;
    }

    @Override
    public String toString()
    {
        return summary().bulleted();
    }
}
