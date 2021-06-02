package com.telenav.mesakit.tools.applications.codec.tag;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.kernel.messaging.repeaters.BaseRepeater;
import com.telenav.mesakit.map.data.formats.pbf.model.tags.compression.PbfTagCodecBuilder;
import com.telenav.mesakit.tools.applications.codec.CodecGeneratorApplication;

/**
 * @author jonathanl (shibo)
 */
public class TagCodecGenerator extends BaseRepeater
{
    public void run(final CommandLine commandLine)
    {
        final var input = commandLine.argument(CodecGeneratorApplication.INPUT);
        final var wayFilter = commandLine.get(CodecGeneratorApplication.WAY_FILTER);
        final var relationFilter = commandLine.get(CodecGeneratorApplication.RELATION_FILTER);
        final var sampleFrequency = commandLine.get(CodecGeneratorApplication.TAG_CODEC_SAMPLE_FREQUENCY);
        final var stringsMaximum = commandLine.get(CodecGeneratorApplication.TAG_CODEC_STRINGS_MAXIMUM);
        final var stringsMinimumOccurrences = commandLine.get(CodecGeneratorApplication.TAG_CODEC_STRINGS_MINIMUM_OCCURRENCES);
        final var stringsMaximumBits = commandLine.get(CodecGeneratorApplication.TAG_CODEC_STRINGS_MAXIMUM_BITS);
        final var charactersMinimumOccurrences = commandLine.get(CodecGeneratorApplication.TAG_CODEC_CHARACTERS_MINIMUM_OCCURRENCES);
        final var charactersMaximumBits = commandLine.get(CodecGeneratorApplication.TAG_CODEC_CHARACTERS_MAXIMUM_BITS);

        final var builder = new PbfTagCodecBuilder()
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
