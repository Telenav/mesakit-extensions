package com.telenav.mesakit.tools.applications.codec.roadname;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.value.count.Minimum;
import com.telenav.kivakit.data.compression.codecs.huffman.character.CharacterFrequencies;
import com.telenav.kivakit.data.compression.codecs.huffman.character.HuffmanCharacterCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.string.HuffmanStringCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.string.StringFrequencies;
import com.telenav.kivakit.filesystem.File;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.tools.applications.codec.CodecGeneratorApplication;

/**
 * @author jonathanl (shibo)
 */
public class RoadNameCodecGenerator extends BaseComponent
{
    public void run(CommandLine commandLine)
    {
        var graph = new SmartGraphLoader(commandLine.argument(require(CodecGeneratorApplication.class).INPUT)).load();

        var characters = new CharacterFrequencies();
        var strings = new StringFrequencies(Count._10_000_000, Maximum._100_000_000);
        var progress = BroadcastingProgressReporter.create(this);
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
        if (!characters.frequencies().containsKey(HuffmanCharacterCodec.ESCAPE))
        {
            characters.frequencies().plus(HuffmanCharacterCodec.ESCAPE, Count._1024);
        }

        var characterCodec = HuffmanCharacterCodec.from(characters.symbols(Minimum._1024), Maximum._16);
        characterCodec.asProperties().save(characterCodec.toString(), File.parseFile(this, "default-road-name-character.codec"));

        var stringCodec = HuffmanStringCodec.from(strings.symbols(Minimum._1024), Maximum._16);
        stringCodec.asProperties().save(stringCodec.toString(), File.parseFile(this, "string.codec"));
    }
}
