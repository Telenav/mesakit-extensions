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

package com.telenav.mesakit.tools.applications.codec;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.language.collections.set.ObjectSet;
import com.telenav.kivakit.kernel.language.values.count.Maximum;
import com.telenav.kivakit.kernel.language.values.count.Minimum;
import com.telenav.mesakit.graph.project.GraphCoreProject;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.mesakit.tools.applications.codec.polyline.HuffmanCodecGenerator;
import com.telenav.mesakit.tools.applications.codec.roadname.RoadNameCodecGenerator;
import com.telenav.mesakit.tools.applications.codec.tag.TagCodecGenerator;

import java.util.List;

import static com.telenav.kivakit.commandline.SwitchParser.enumSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParser.integerSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParser.maximumSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParser.minimumSwitchParser;
import static com.telenav.kivakit.filesystem.File.fileArgumentParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter.relationFilterSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter.wayFilterSwitchParser;

/**
 * Dumps graph resources. A specific edge, vertex, relation, way or place can be shown. The spatial index can also be
 * dumped.
 *
 * @author jonathanl (shibo)
 */
public class CodecGeneratorApplication extends Application
{
    public static final ArgumentParser<File> INPUT =
            fileArgumentParser("The input .graph or .osm.pbf file")
                    .required()
                    .build();

    public static final SwitchParser<WayFilter> WAY_FILTER =
            wayFilterSwitchParser()
                    .optional()
                    .build();

    public static final SwitchParser<RelationFilter> RELATION_FILTER =
            relationFilterSwitchParser()
                    .optional()
                    .build();

    public static final SwitchParser<Integer> TAG_CODEC_SAMPLE_FREQUENCY =
            integerSwitchParser("tag-codec-sample-frequency", "Only use every nth entity to build codecs to improve performance")
                    .optional()
                    .defaultValue(1)
                    .build();

    public static final SwitchParser<Maximum> TAG_CODEC_STRINGS_MAXIMUM =
            maximumSwitchParser("tag-codec-strings-maximum", "The maximum number of strings in a string codec")
                    .optional()
                    .defaultValue(Maximum._256)
                    .build();

    public static final SwitchParser<Maximum> TAG_CODEC_STRINGS_MAXIMUM_BITS =
            maximumSwitchParser("tag-codec-strings-maximum-bits", "The maximum number of bits for a symbol in a string codec")
                    .optional()
                    .defaultValue(Maximum._12)
                    .build();

    public static final SwitchParser<Minimum> TAG_CODEC_STRINGS_MINIMUM_OCCURRENCES =
            minimumSwitchParser("tag-codec-strings-minimum-occurrences", "The minimum number of occurrences for a string to be included in a string codec")
                    .optional()
                    .defaultValue(Minimum._1024)
                    .build();

    public static final SwitchParser<Maximum> TAG_CODEC_CHARACTERS_MAXIMUM_BITS =
            maximumSwitchParser("tag-codec-characters-maximum-bits", "The maximum number of bits for a symbol in an ASCII character codec")
                    .optional()
                    .defaultValue(Maximum._12)
                    .build();

    public static final SwitchParser<Minimum> TAG_CODEC_CHARACTERS_MINIMUM_OCCURRENCES =
            minimumSwitchParser("tag-codec-characters-minimum-occurrences", "The minimum number of occurrences for an ASCII character to be included in a character codec")
                    .optional()
                    .defaultValue(Minimum._1024)
                    .build();

    public static void main(final String[] arguments)
    {
        new CodecGeneratorApplication().run(arguments);
    }

    enum Type
    {
        POLYLINE,
        TAG,
        ROAD_NAME
    }

    private final SwitchParser<Type> TYPE =
            enumSwitchParser("type", "The type of codec(s) to generate", Type.class)
                    .required()
                    .build();

    protected CodecGeneratorApplication()
    {
        super(GraphCoreProject.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @Override
    protected void onRun()
    {
        // show arguments
        information(commandLineDescription("Codec Generator"));

        switch (get(TYPE))
        {
            case TAG:
                if (!has(WAY_FILTER) || !has(RELATION_FILTER))
                {
                    exit("Tag codecs require both the -way-filter and -relation-filter switches");
                }
                listenTo(new TagCodecGenerator()).run(commandLine());
                break;

            case POLYLINE:
                listenTo(new HuffmanCodecGenerator()).run(commandLine());
                break;

            case ROAD_NAME:
                listenTo(new RoadNameCodecGenerator()).run(commandLine());
                break;
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.of
                (
                        TYPE,
                        WAY_FILTER,
                        RELATION_FILTER,
                        TAG_CODEC_SAMPLE_FREQUENCY,
                        TAG_CODEC_STRINGS_MAXIMUM,
                        TAG_CODEC_STRINGS_MAXIMUM_BITS,
                        TAG_CODEC_STRINGS_MINIMUM_OCCURRENCES,
                        TAG_CODEC_CHARACTERS_MAXIMUM_BITS,
                        TAG_CODEC_CHARACTERS_MINIMUM_OCCURRENCES,
                        QUIET
                );
    }
}
