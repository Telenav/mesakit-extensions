package com.telenav.mesakit.tools.applications.pbf.analyzer;

import com.telenav.kivakit.commandline.CommandLine;
import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.string.AsciiArt;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.messaging.logging.Logger;
import com.telenav.kivakit.messaging.logging.LoggerFactory;
import com.telenav.kivakit.primitive.collections.map.split.SplitLongToLongMap;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfNode;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfRelation;
import com.telenav.mesakit.map.data.formats.pbf.model.entities.PbfWay;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.polyline.PolylineBuilder;
import com.telenav.mesakit.map.measurements.geographic.Distance;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class Analyzer extends BaseComponent
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private long ways;

    private long nodes;

    private long relations;

    private long places;

    private long placesWithPopulation;

    private int turnRestrictions;

    private int badTurnRestrictions;

    private int noLeft;

    private int noRight;

    private int noStraightOn;

    private int noUTurn;

    private int onlyLeft;

    private int onlyRight;

    private int onlyStraightOn;

    private final Map<String, Distance> lengthForHighwayType = new HashMap<>();

    private final SplitLongToLongMap locationForNode;

    private final File input;

    private final boolean showWarnings;

    private final boolean computeLengths;

    Analyzer(CommandLine commandLine)
    {
        var application = require(PbfAnalyzerApplication.class);
        input = commandLine.argument(application.INPUT);
        showWarnings = commandLine.get(application.SHOW_WARNINGS);
        computeLengths = commandLine.get(application.COMPUTE_LENGTHS);

        var feedback = new StringList();
        feedback.add("input = $", input);
        feedback.add("way filter = $", commandLine.get(application.WAY_FILTER));
        feedback.add("show warnings = $", showWarnings);
        feedback.add("compute lengths = $", computeLengths);
        LOGGER.information(feedback.titledBox("Analyzing " + input.fileName()));

        locationForNode = new SplitLongToLongMap("locationForNode");
        locationForNode.initialSize(Estimate._65536);
        locationForNode.initialize();
    }

    public void addWay(PbfWay way)
    {
        ways++;
        if (computeLengths)
        {
            for (var tag : way)
            {
                if ("highway".equalsIgnoreCase(tag.getKey()))
                {
                    var type = tag.getValue();
                    var builder = new PolylineBuilder();
                    for (var node : way.nodes())
                    {
                        builder.add(Location.dm7(locationForNode.get(node.getNodeId())));
                    }
                    if (builder.size() > 1)
                    {
                        var length = lengthForHighwayType.get(type);
                        if (length == null)
                        {
                            length = Distance.ZERO;
                        }
                        lengthForHighwayType.put(type, length.add(builder.build().length()));
                    }
                }
            }
        }
    }

    void addNode(PbfNode node)
    {
        nodes++;
        if (computeLengths)
        {
            var location = Location.degrees(node.latitude(), node.longitude());
            locationForNode.put(node.identifierAsLong(), location.asDm7Long());
        }
        var tags = node.tagMap();
        if (tags.containsKey("place"))
        {
            if (tags.containsKey("population"))
            {
                placesWithPopulation++;
            }
            places++;
        }
    }

    void addRelation(PbfRelation relation)
    {
        relations++;
        for (var tag : relation)
        {
            if (tag.getKey() != null && "restriction".equalsIgnoreCase(tag.getKey()))
            {
                var value = tag.getValue().toLowerCase();
                if (value.startsWith("no_"))
                {
                    if (value.startsWith("no_left_turn"))
                    {
                        noLeft++;
                        turnRestrictions++;
                    }
                    else if (value.startsWith("no_right_turn"))
                    {
                        noRight++;
                        turnRestrictions++;
                    }
                    else if (value.startsWith("no_straight_on"))
                    {
                        noStraightOn++;
                        turnRestrictions++;
                    }
                    else if (value.startsWith("no_u_turn"))
                    {
                        noUTurn++;
                        turnRestrictions++;
                    }
                    else
                    {
                        if (showWarnings)
                        {
                            System.err.println(tag.getKey() + " = " + value);
                        }
                        badTurnRestrictions++;
                    }
                }
                else if (value.startsWith("only_"))
                {
                    if (value.startsWith("only_left_turn"))
                    {
                        onlyLeft++;
                        turnRestrictions++;
                    }
                    else if (value.startsWith("only_right_turn"))
                    {
                        onlyRight++;
                        turnRestrictions++;
                    }
                    else if (value.startsWith("only_straight_on"))
                    {
                        onlyStraightOn++;
                        turnRestrictions++;
                    }
                    else
                    {
                        if (showWarnings)
                        {
                            System.out.println(tag.getKey() + " = " + value);
                        }
                        badTurnRestrictions++;
                    }
                }
                else
                {
                    if (showWarnings)
                    {
                        System.out.println(tag.getKey() + " = " + value);
                    }
                    badTurnRestrictions++;
                }
            }
        }
    }

    void report()
    {
        var report = new StringList();

        report.add("nodes = " + Count.count(nodes));
        report.add("ways = " + Count.count(ways));
        report.add("relations = " + Count.count(relations));

        report.add(AsciiArt.line());

        report.add("file size = " + input.sizeInBytes());
        report.add("nodes / byte = " + (double) nodes / input.sizeInBytes().asBytes());
        report.add("ways / byte = " + (double) ways / input.sizeInBytes().asBytes());
        report.add("relations / byte = " + (double) relations / input.sizeInBytes().asBytes());

        report.add(AsciiArt.line());
        report.add("places = " + Count.count(places));
        report.add("places with population = " + Count.count(placesWithPopulation));

        report.add(AsciiArt.line());
        report.add("bad turn restrictions = " + badTurnRestrictions);
        report.add("turn restrictions (excluding bad) = " + turnRestrictions);

        report.add(AsciiArt.line());
        report.add("no left turn restrictions = " + noLeft);
        report.add("no right turn restrictions = " + noRight);
        report.add("no straight on turn restrictions = " + noStraightOn);
        report.add("no u-turn restrictions = " + noUTurn);

        report.add(AsciiArt.line());
        report.add("only left turn restrictions = " + onlyLeft);
        report.add("only right turn restrictions = " + onlyRight);
        report.add("only straight on turn restrictions = " + onlyStraightOn);

        if (turnRestrictions != (noLeft + noRight + noStraightOn + noUTurn + onlyLeft
                + onlyRight + onlyStraightOn))
        {
            System.err.println("INTERNAL ERROR: turn restrictions don't add up");
        }

        if (computeLengths)
        {
            System.out.println(AsciiArt.line());
            var keys = new StringList(lengthForHighwayType.keySet());
            keys.sort(Comparator.naturalOrder());
            for (var type : keys)
            {
                System.out.println("highway['" + type + "'] = " + lengthForHighwayType.get(type));
            }
        }

        LOGGER.information(report.titledBox("Statistics"));
    }
}
