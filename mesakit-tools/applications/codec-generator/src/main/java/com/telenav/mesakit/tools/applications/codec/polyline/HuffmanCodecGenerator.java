package com.telenav.mesakit.tools.applications.codec.polyline;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.conversion.core.language.primitive.IntegerConverter;
import com.telenav.kivakit.core.collections.map.CountMap;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.value.count.Minimum;
import com.telenav.kivakit.data.compression.codecs.huffman.HuffmanCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.tree.Symbols;
import com.telenav.kivakit.filesystem.File;
import com.telenav.mesakit.graph.io.load.SmartGraphLoader;
import com.telenav.mesakit.tools.applications.codec.CodecGeneratorApplication;

/**
 * @author jonathanl (shibo)
 */
public class HuffmanCodecGenerator extends BaseComponent
{
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

    public void run(CommandLine commandLine)
    {
        var graph = new SmartGraphLoader(commandLine.argument(require(CodecGeneratorApplication.class).INPUT)).load();

        var frequencies = new CountMap<Integer>();

        // Go through edges,
        var progress = BroadcastingProgressReporter.progressReporter(this);
        for (var edge : graph.edges())
        {
            // and if the edge is not a segment,
            if (edge.isShaped())
            {
                // get the road shape
                var polyline = edge.roadShape();

                // and go through the locations
                var last = polyline.get(0);
                for (var index = 1; index < polyline.size(); index++)
                {
                    // find the latitude and longitude offsets from the last location in DM5
                    var latitudeInDm5 = polyline.get(index).latitude().asDm5();
                    var latitudeOffsetInDm5 = latitudeInDm5 - last.latitude().asDm5();

                    // and the longitude offset
                    var longitudeInDm5 = polyline.get(index).longitude().asDm5();
                    var longitudeOffsetInDm5 = longitudeInDm5 - last.longitude().asDm5();

                    // and add them to the frequencies array
                    addOffsets(offsetsInDm5, frequencies, latitudeOffsetInDm5);
                    addOffsets(offsetsInDm5, frequencies, longitudeOffsetInDm5);
                }
            }
            progress.next();
        }

        // then build and save the codec
        var symbols = new Symbols<>(frequencies, Minimum._1);
        var codec = HuffmanCodec.huffmanCodec(symbols, Maximum._24);
        codec.asProperties(new IntegerConverter(this)).save(File.parseFile(this, "polyline.codec"), codec.toString());
    }

    private void addOffsets(int[] offsetsInDm5, CountMap<Integer> frequencies, int destinationInDm5)
    {
        // While we have not arrived at the destination offset,
        var sign = destinationInDm5 < 0 ? -1 : 1;
        destinationInDm5 = Math.abs(destinationInDm5);
        while (destinationInDm5 > 0)
        {
            // search for the largest offset we can find that we can apply to it
            for (var index = 0; index < offsetsInDm5.length; index++)
            {
                var offset = offsetsInDm5[index];
                var newDestination = destinationInDm5 - offset;
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
