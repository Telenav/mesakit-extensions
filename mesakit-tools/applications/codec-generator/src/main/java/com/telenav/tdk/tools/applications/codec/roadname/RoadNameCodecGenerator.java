package com.telenav.tdk.tools.applications.codec.roadname;

import com.telenav.tdk.core.filesystem.File;
import com.telenav.tdk.core.kernel.commandline.CommandLine;
import com.telenav.tdk.core.kernel.logging.*;
import com.telenav.tdk.core.kernel.messaging.Message;
import com.telenav.tdk.core.kernel.messaging.repeaters.BaseRepeater;
import com.telenav.tdk.core.kernel.operation.progress.reporters.Progress;
import com.telenav.tdk.core.kernel.scalars.counts.*;
import com.telenav.tdk.graph.io.load.SmartGraphLoader;
import com.telenav.tdk.tools.applications.codec.CodecGeneratorApplication;
import com.telenav.tdk.utilities.compression.codecs.huffman.character.*;
import com.telenav.tdk.utilities.compression.codecs.huffman.string.*;

/**
 * @author jonathanl (shibo)
 */
public class RoadNameCodecGenerator extends BaseRepeater<Message>
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    public void run(final CommandLine commandLine)
    {
        final var graph = new SmartGraphLoader(commandLine.argument(CodecGeneratorApplication.INPUT)).load();

        final var characters = new CharacterFrequencies();
        final var strings = new StringFrequencies(Count._10_000_000, Maximum._100_000_000);
        final var progress = Progress.create(LOGGER);
        for (final var edge : graph.edges())
        {
            for (final var name : edge.roadNames())
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

        final var characterCodec = HuffmanCharacterCodec.from(characters.symbols(Minimum._1024), Maximum._16);
        characterCodec.asProperties().save(characterCodec.toString(), new File("default-road-name-character.codec"));

        final var stringCodec = HuffmanStringCodec.from(strings.symbols(Minimum._1024), Maximum._16);
        stringCodec.asProperties().save(stringCodec.toString(), new File("string.codec"));
    }
}
