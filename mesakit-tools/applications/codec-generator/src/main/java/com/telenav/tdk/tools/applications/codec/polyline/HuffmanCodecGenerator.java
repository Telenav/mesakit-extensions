package com.telenav.kivakit.tools.applications.codec.polyline;

import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.commandline.CommandLine;
import com.telenav.kivakit.kernel.conversion.primitive.IntegerConverter;
import com.telenav.kivakit.kernel.language.collections.map.CountMap;
import com.telenav.kivakit.kernel.logging.*;
import com.telenav.kivakit.kernel.messaging.Message;
import com.telenav.kivakit.kernel.messaging.repeaters.BaseRepeater;
import com.telenav.kivakit.kernel.operation.progress.reporters.Progress;
import com.telenav.kivakit.kernel.scalars.counts.*;
import com.telenav.kivakit.graph.io.load.SmartGraphLoader;
import com.telenav.kivakit.map.geography.polyline.compression.huffman.HuffmanPolylineCodec;
import com.telenav.kivakit.tools.applications.codec.CodecGeneratorApplication;
import com.telenav.kivakit.utilities.compression.codecs.huffman.HuffmanCodec;
import com.telenav.kivakit.utilities.compression.codecs.huffman.tree.Symbols;

/**
 * @author jonathanl (shibo)
 */
public class HuffmanCodecGenerator extends BaseRepeater<Message>
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    static final int[] offsetsInDm5 = new int[40];

    static
    {
        offsetsInDm5[0] = 1;
        for (var i = 1; i < offsetsInDm5.length; i++)
        {
            if (i < 5)
            {
                offsetsInDm5[i] = offsetsInDm5[i - 1] + 1;
            }
            else if (i < 10)
            {
                offsetsInDm5[i] = offsetsInDm5[i - 1] + 5;
            }
            else if (i < 15)
            {
                offsetsInDm5[i] = offsetsInDm5[i - 1] + 10;
            }
            else if (i < 20)
            {
                offsetsInDm5[i] = offsetsInDm5[i - 1] + 50;
            }
            else if (i < 35)
            {
                offsetsInDm5[i] = offsetsInDm5[i - 1] + 100;
            }
            else
            {
                offsetsInDm5[i] = offsetsInDm5[i - 1] * 2;
            }
        }
    }

    public void run(final CommandLine commandLine)
    {
        final var graph = new SmartGraphLoader(commandLine.argument(CodecGeneratorApplication.INPUT)).load();

        final var frequencies = new CountMap<Integer>();

        // Go through edges,
        final var progress = Progress.create(LOGGER);
        for (final var edge : graph.edges())
        {
            // and if the edge is not a segment,
            if (edge.isShaped())
            {
                // get the road shape
                final var polyline = edge.roadShape();

                // and go through the locations
                final var last = polyline.get(0);
                for (var index = 1; index < polyline.size(); index++)
                {
                    // find the latitude and longitude offsets from the last location in DM5
                    final var latitudeInDm5 = polyline.get(index).latitude().asDm5();
                    final var latitudeOffsetInDm5 = latitudeInDm5 - last.latitude().asDm5();

                    // and the longitude offset
                    final var longitudeInDm5 = polyline.get(index).longitude().asDm5();
                    final var longitudeOffsetInDm5 = longitudeInDm5 - last.longitude().asDm5();

                    // and add them to the frequencies array
                    addOffsets(offsetsInDm5, frequencies, latitudeOffsetInDm5);
                    frequencies.add(HuffmanPolylineCodec.END_OF_OFFSETS, 2);
                    addOffsets(offsetsInDm5, frequencies, longitudeOffsetInDm5);
                    frequencies.add(HuffmanPolylineCodec.END_OF_OFFSETS, 2);
                }
            }
            progress.next();
        }

        // then build and save the codec
        final var symbols = new Symbols<>(frequencies, Minimum._1);
        final var codec = HuffmanCodec.from(symbols, Maximum._24);
        codec.asProperties(new IntegerConverter(LOGGER)).save(codec.toString(), new File("polyline.codec"));
    }

    private void addOffsets(final int[] offsetsInDm5, final CountMap<Integer> frequencies, int destinationInDm5)
    {
        // While we have not arrived at the destination offset,
        final var sign = destinationInDm5 < 0 ? -1 : 1;
        destinationInDm5 = Math.abs(destinationInDm5);
        while (destinationInDm5 > 0)
        {
            // search for the largest offset we can find that we can apply to it
            for (var index = 0; index < offsetsInDm5.length; index++)
            {
                var offset = offsetsInDm5[index];
                final var newDestination = destinationInDm5 - offset;
                if (newDestination == 0)
                {
                    destinationInDm5 -= offset;
                    frequencies.increment(sign * offset);
                    break;
                }
                else if (newDestination < 0 || index == offsetsInDm5.length - 1)
                {
                    offset = offsetsInDm5[index - 1];
                    destinationInDm5 -= offset;
                    frequencies.increment(sign * offset);
                    break;
                }
            }
        }
    }
}
