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

package com.telenav.mesakit.tools.applications.graph.analyzer;

import com.telenav.kivakit.graph.Edge;
import com.telenav.kivakit.kernel.interfaces.object.Matcher;
import com.telenav.kivakit.kernel.language.string.StringList;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Matches edges that are usable in "HERE" statistics
 *
 * @author jonathanl (shibo)
 */
public class HereEdgeMatcher implements Matcher<Edge>
{
    private final Set<String> include = new LinkedHashSet<>();

    public HereEdgeMatcher(final String... include)
    {
        Collections.addAll(this.include, include);
    }

    @Override
    public boolean matches(final Edge edge)
    {
        if (!"ferry".equalsIgnoreCase(edge.tagValue("route")))
        {
            final var access = edge.tagValue("access");
            if (!"private".equalsIgnoreCase(access) && !"no".equalsIgnoreCase(access))
            {
                for (final var type : edge.tag("highway").getValue().split("[;:]"))
                {
                    if (include.contains(type))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString()
    {
        return new StringList(include).toString();
    }
}
