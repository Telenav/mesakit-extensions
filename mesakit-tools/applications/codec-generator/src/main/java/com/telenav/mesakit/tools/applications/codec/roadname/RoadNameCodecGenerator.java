package com.telenav.mesakit.tools.applications.codec.roadname;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.data.compression.codecs.huffman.character.CharacterFrequencies;
import com.telenav.kivakit.data.compression.codecs.huffman.character.HuffmanCharacterCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.string.HuffmanStringCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.string.StringFrequencies;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.language.progress.reporters.Progress;
import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.kernel.language.values.count.Maximum;
import com.telenav.kivakit.kernel.language.values.count.Minimum;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;
import com.telenav.kivakit.kernel.messaging.repeaters.BaseRepeater;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.tools.applications.codec.CodecGeneratorApplication;

/**
 * @author jonathanl (shibo)
 */
public class RoadNameCodecGenerator extends BaseRepeater
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    public void run(CommandLine commandLine)
    {
        var graph = new SmartGraphLoader(commandLine.argument(CodecGeneratorApplication.INPUT)).load();

        var characters = new CharacterFrequencies();
        var strings = new StringFrequencies(Count._10_000_000, Maximum._100_000_000);
        var progress = Progress.create(LOGGER);
        for (var edge : graph.edges())
        {
            for (var name : edge.roadNames())
            {
                characters.add(name.name());
                strings.add(name.name());
                progress.next();
            }
        }
        progress.end();
        if (!characters.frequencies().contains(HuffmanCharacterCodec.ESCAPE))
        {
            characters.frequencies().add(HuffmanCharacterCodec.ESCAPE, Count._1024);
        }

        var characterCodec = HuffmanCharacterCodec.from(characters.symbols(Minimum._1024), Maximum._16);
        characterCodec.asProperties().save(characterCodec.toString(), File.parse("default-road-name-character.codec"));

        var stringCodec = HuffmanStringCodec.from(strings.symbols(Minimum._1024), Maximum._16);
        stringCodec.asProperties().save(stringCodec.toString(), File.parse("string.codec"));
    }
}
