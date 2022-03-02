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
import com.telenav.kivakit.collections.set.ObjectSet;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.string.IndentingStringBuilder;
import com.telenav.kivakit.core.language.patterns.Pattern;
import com.telenav.kivakit.core.language.patterns.SimplifiedPattern;
import com.telenav.kivakit.core.language.strings.AsciiArt;
import com.telenav.kivakit.messaging.logging.Logger;
import com.telenav.kivakit.messaging.logging.LoggerFactory;
import com.telenav.mesakit.map.region.Region;
import com.telenav.mesakit.map.region.RegionProject;
import com.telenav.mesakit.map.region.RegionSet;
import com.telenav.mesakit.map.region.regions.Continent;

import java.util.List;

import static com.telenav.kivakit.commandline.ArgumentParser.stringArgumentParser;
import static com.telenav.kivakit.commandline.SwitchParsers.booleanSwitchParser;
import static com.telenav.kivakit.core.string.IndentingStringBuilder.Indentation;
import static com.telenav.kivakit.core.string.IndentingStringBuilder.Style;

/**
 * Shows information about a region matching the pattern given as an argument
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings({ "rawtypes", "ConstantConditions", "UseOfSystemOutOrSystemErr" })
public class RegionInformationApplication extends Application
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    public static void main(String[] arguments)
    {
        new RegionInformationApplication().run(arguments);
    }

    private final ArgumentParser<String> REGION =
            stringArgumentParser(this, "A pattern matching the MesaKit code of the region to give information on")
                    .required()
                    .build();

    private final SwitchParser<Boolean> PARENT =
            booleanSwitchParser(this, "parent", "Show the parent of the given region")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> CODE =
            booleanSwitchParser(this, "code", "Show only the region code")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> FOLDER =
            booleanSwitchParser(this, "folder", "Show only the region repository folder")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> URI =
            booleanSwitchParser(this, "uri", "The URI on Geofabrik where this region can be downloaded")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> ALL =
            booleanSwitchParser(this, "all", "Show all attributes for the region")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> RECURSE =
            booleanSwitchParser(this, "recurse", "Show sub-regions")
                    .optional()
                    .defaultValue(false)
                    .build();

    public RegionInformationApplication()
    {
        super(RegionProject.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(REGION);
    }

    @Override
    protected void onRun()
    {
        var region = region(commandLine(), argumentList().first().get(REGION));
        if (get(PARENT))
        {
            region = region.parent();
        }
        boolean showAll = get(ALL);
        boolean showCode = get(CODE);
        if (showCode || showAll)
        {
            System.out.println(region.identity().mesakit().code());
        }
        boolean showFolder = get(FOLDER);
        if (showFolder || showAll)
        {
            System.out.println(region.folder());
        }
        boolean showUri = get(URI);
        if (showUri || showAll)
        {
            System.out.println(region.geofabrikUri());
        }
        if (!showCode && !showFolder && !showUri && !showAll)
        {
            var builder = new IndentingStringBuilder(Style.TEXT, Indentation.of(4));
            regionCodes(region, builder, get(RECURSE));
            System.out.println(builder);
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(PARENT, CODE, FOLDER, RECURSE, URI, ALL, QUIET);
    }

    private Region region(CommandLine commandLine, String name)
    {
        Pattern pattern = new SimplifiedPattern(name.toLowerCase());
        var matches = new RegionSet();
        for (var continent : Continent.all())
        {
            if (pattern.matches(continent.identity().mesakit().code().toLowerCase()))
            {
                matches.add(continent);
            }
            for (Region nested : continent.nestedChildren())
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

    private void regionCodes(Region region, IndentingStringBuilder builder, boolean recurse)
    {
        builder.appendLine(region.identity().mesakit().code() + " (" + region.type() + ")");
        if (recurse)
        {
            for (Region<?> child : region.children())
            {
                builder.indent();
                regionCodes(child, builder, recurse);
                builder.unindent();
            }
        }
    }
}
