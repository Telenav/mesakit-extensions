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

package com.telenav.kivakit.graph.geocoding.reverse.matching;

import com.telenav.kivakit.kernel.language.string.Strings;
import com.telenav.kivakit.kernel.scalars.levels.Percentage;
import com.telenav.kivakit.map.road.model.RoadName;

public class FuzzyRoadNameMatcher implements RoadNameMatcher
{
    @Override
    public Percentage matches(final RoadName candidate, final RoadName desired)
    {
        // If the candidate precisely matches (ignoring case) the name we're looking for
        if (candidate.equals(desired))
        {
            return Percentage._100;
        }

        // If directions were specified and they don't match
        final var candidateDirection = candidate.extractDirection();
        final var desiredDirection = desired.extractDirection();
        if (candidateDirection != null && desiredDirection != null && !candidateDirection.equals(desiredDirection))
        {
            // then the road names don't match
            return Percentage._0;
        }

        // compute the edit distance between the two names
        final var distance = Strings.levenshteinDistance(candidate.name().toLowerCase(), desired.name().toLowerCase());

        // and the two names match if the percentage of characters that would need to be
        // changed is less than the configuration setting,
        final var length = desired.name().length();
        var percentage = (length - distance) * 100.0 / length;
        if (percentage < 0.0)
        {
            percentage = 0;
        }
        if (percentage > 100.0)
        {
            percentage = 100;
        }

        return new Percentage(percentage);
    }
}
