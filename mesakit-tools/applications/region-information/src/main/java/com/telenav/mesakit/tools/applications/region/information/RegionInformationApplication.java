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

package com.telenav.mesakit.tools.applications.region.information;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.kernel.language.patterns.Pattern;
import com.telenav.kivakit.kernel.language.patterns.SimplifiedPattern;
import com.telenav.kivakit.kernel.language.strings.AsciiArt;
import com.telenav.kivakit.kernel.language.strings.formatting.IndentingStringBuilder;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;
import com.telenav.mesakit.map.region.Region;
import com.telenav.mesakit.map.region.RegionSet;
import com.telenav.mesakit.map.region.project.MapRegionProject;
import com.telenav.mesakit.map.region.regions.Continent;

import java.util.List;
import java.util.Set;

import static com.telenav.kivakit.commandline.ArgumentParser.stringArgumentParser;
import static com.telenav.kivakit.commandline.SwitchParser.booleanSwitchParser;
import static com.telenav.kivakit.kernel.language.strings.formatting.IndentingStringBuilder.Indentation;
import static com.telenav.kivakit.kernel.language.strings.formatting.IndentingStringBuilder.Style;

/**
 * Shows information about a region matching the pattern given as an argument
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings({ "rawtypes", "ConstantConditions", "UseOfSystemOutOrSystemErr" })
public class RegionInformationApplication extends Application
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    public static void main(final String[] arguments)
    {
        new RegionInformationApplication().run(arguments);
    }

    private final ArgumentParser<String> REGION =
            stringArgumentParser("A pattern matching the TDK code of the region to give information on")
                    .required()
                    .build();

    private final SwitchParser<Boolean> PARENT =
            booleanSwitchParser("parent", "Show the parent of the given region")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> CODE =
            booleanSwitchParser("code", "Show only the region code")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> FOLDER =
            booleanSwitchParser("folder", "Show only the region repository folder")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> URI =
            booleanSwitchParser("uri", "The URI on Geofabrik where this region can be downloaded")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> ALL =
            booleanSwitchParser("all", "Show all attributes for the region")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> RECURSE =
            booleanSwitchParser("recurse", "Show sub-regions")
                    .optional()
                    .defaultValue(false)
                    .build();

    public RegionInformationApplication()
    {
        super(MapRegionProject.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(REGION);
    }

    @Override
    protected void onRun()
    {
        var region = region(commandLine(), arguments().first().get(REGION));
        if (get(PARENT))
        {
            region = region.parent();
        }
        final boolean showAll = get(ALL);
        final boolean showCode = get(CODE);
        if (showCode || showAll)
        {
            System.out.println(region.identity().mesakit().code());
        }
        final boolean showFolder = get(FOLDER);
        if (showFolder || showAll)
        {
            System.out.println(region.folder());
        }
        final boolean showUri = get(URI);
        if (showUri || showAll)
        {
            System.out.println(region.geofabrikUri());
        }
        if (!showCode && !showFolder && !showUri && !showAll)
        {
            final var builder = new IndentingStringBuilder(Style.TEXT, Indentation.of(4));
            regionCodes(region, builder, get(RECURSE));
            System.out.println(builder);
        }
    }

    @Override
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(PARENT, CODE, FOLDER, RECURSE, URI, ALL, QUIET);
    }

    private Region region(final CommandLine commandLine, final String name)
    {
        final Pattern pattern = new SimplifiedPattern(name.toLowerCase());
        final var matches = new RegionSet();
        for (final var continent : Continent.all())
        {
            if (pattern.matches(continent.identity().mesakit().code().toLowerCase()))
            {
                matches.add(continent);
            }
            for (final Region nested : continent.nestedChildren())
            {
                if (pattern.matches(nested.identity().mesakit().code().toLowerCase()))
                {
                    matches.add(nested);
                }
            }
        }
        if (matches.isEmpty())
        {
            commandLine.exit("No region found for '" + name + "'");
        }
        if (matches.size() > 1)
        {
            commandLine.exit(
                    "The pattern '" + name + "' was ambiguous. It matches:\n*" + AsciiArt.bulleted(matches, "*   - "));
        }
        return matches.first();
    }

    private void regionCodes(final Region region, final IndentingStringBuilder builder, final boolean recurse)
    {
        builder.appendLine(region.identity().mesakit().code() + " (" + region.type() + ")");
        if (recurse)
        {
            for (final Region<?> child : region.children())
            {
                builder.indent();
                regionCodes(child, builder, recurse);
                builder.unindent();
            }
        }
    }
}
