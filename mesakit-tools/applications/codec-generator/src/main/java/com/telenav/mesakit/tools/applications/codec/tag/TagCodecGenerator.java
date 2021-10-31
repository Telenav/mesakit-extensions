package com.telenav.mesakit.tools.applications.codec.tag;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.component.BaseComponent;
import com.telenav.mesakit.map.data.formats.pbf.model.tags.compression.PbfTagCodecBuilder;
import com.telenav.mesakit.tools.applications.codec.CodecGeneratorApplication;

/**
 * @author jonathanl (shibo)
 */
public class TagCodecGenerator extends BaseComponent
{
    public void run(CommandLine commandLine)
    {
        var codecGeneratorApplication = require(CodecGeneratorApplication.class);

        var input = commandLine.argument(codecGeneratorApplication.INPUT);
        var wayFilter = commandLine.get(codecGeneratorApplication.WAY_FILTER);
        var relationFilter = commandLine.get(codecGeneratorApplication.RELATION_FILTER);
        var sampleFrequency = commandLine.get(codecGeneratorApplication.TAG_CODEC_SAMPLE_FREQUENCY);
        var stringsMaximum = commandLine.get(codecGeneratorApplication.TAG_CODEC_STRINGS_MAXIMUM);
        var stringsMinimumOccurrences = commandLine.get(codecGeneratorApplication.TAG_CODEC_STRINGS_MINIMUM_OCCURRENCES);
        var stringsMaximumBits = commandLine.get(codecGeneratorApplication.TAG_CODEC_STRINGS_MAXIMUM_BITS);
        var charactersMinimumOccurrences = commandLine.get(codecGeneratorApplication.TAG_CODEC_CHARACTERS_MINIMUM_OCCURRENCES);
        var charactersMaximumBits = commandLine.get(codecGeneratorApplication.TAG_CODEC_CHARACTERS_MAXIMUM_BITS);

        var builder = new PbfTagCodecBuilder()
                .wayFilter(wayFilter)
                .relationFilter(relationFilter)
                .sampleFrequency(sampleFrequency)
                .stringsMaximum(stringsMaximum)
                .stringsMinimumOccurrences(stringsMinimumOccurrences)
                .stringsMaximumBits(stringsMaximumBits)
                .charactersMinimumOccurrences(charactersMinimumOccurrences)
                .charactersMaximumBits(charactersMaximumBits);

        builder.build(input);
        builder.output();
    }
}
