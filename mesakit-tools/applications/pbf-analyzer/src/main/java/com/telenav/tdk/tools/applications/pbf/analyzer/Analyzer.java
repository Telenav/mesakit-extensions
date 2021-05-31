package com.telenav.tdk.tools.applications.pbf.analyzer;

import com.telenav.kivakit.collections.primitive.map.split.SplitLongToLongMap;
import com.telenav.kivakit.data.formats.pbf.model.tags.*;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.commandline.CommandLine;
import com.telenav.kivakit.kernel.language.string.*;
import com.telenav.kivakit.kernel.scalars.counts.*;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.polyline.PolylineBuilder;
import com.telenav.mesakit.map.measurements.geographic.Distance;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.telenav.kivakit.tools.applications.pbf.analyzer.PbfAnalyzerApplication.INPUT;
import static com.telenav.kivakit.tools.applications.pbf.analyzer.PbfAnalyzerApplication.WAY_FILTER;

/**
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class Analyzer
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

    Analyzer(final CommandLine commandLine)
    {
        input = commandLine.argument(INPUT);
        showWarnings = commandLine.get(PbfAnalyzerApplication.SHOW_WARNINGS);
        computeLengths = commandLine.get(PbfAnalyzerApplication.COMPUTE_LENGTHS);

        final var feedback = new StringList();
        feedback.add("input = $", input);
        feedback.add("way filter = $", commandLine.get(WAY_FILTER));
        feedback.add("show warnings = $", showWarnings);
        feedback.add("compute lengths = $", computeLengths);
        feedback.titledBox(LOGGER, "Analyzing " + input.fileName());

        locationForNode = new SplitLongToLongMap("locationForNode");
        locationForNode.initialSize(Estimate._65536);
        locationForNode.initialize();
    }

    public void addWay(final PbfWay way)
    {
        ways++;
        if (computeLengths)
        {
            for (final var tag : way)
            {
                if ("highway".equalsIgnoreCase(tag.getKey()))
                {
                    final var type = tag.getValue();
                    final var builder = new PolylineBuilder();
                    for (final var node : way.nodes())
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

    void addNode(final PbfNode node)
    {
        nodes++;
        if (computeLengths)
        {
            final var location = Location.degrees(node.latitude(), node.longitude());
            locationForNode.put(node.identifierAsLong(), location.asDm7Long());
        }
        final var tags = node.tagMap();
        if (tags.containsKey("place"))
        {
            if (tags.containsKey("population"))
            {
                placesWithPopulation++;
            }
            places++;
        }
    }

    void addRelation(final PbfRelation relation)
    {
        relations++;
        for (final var tag : relation)
        {
            if (tag.getKey() != null && "restriction".equalsIgnoreCase(tag.getKey()))
            {
                final var value = tag.getValue().toLowerCase();
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
        final var report = new StringList();

        report.add("nodes = " + Count.count(nodes));
        report.add("ways = " + Count.count(ways));
        report.add("relations = " + Count.count(relations));

        report.add(Strings.line());

        report.add("file size = " + input.size());
        report.add("nodes / byte = " + (double) nodes / input.size().asBytes());
        report.add("ways / byte = " + (double) ways / input.size().asBytes());
        report.add("relations / byte = " + (double) relations / input.size().asBytes());

        report.add(Strings.line());
        report.add("places = " + Count.count(places));
        report.add("places with population = " + Count.count(placesWithPopulation));

        report.add(Strings.line());
        report.add("bad turn restrictions = " + badTurnRestrictions);
        report.add("turn restrictions (excluding bad) = " + turnRestrictions);

        report.add(Strings.line());
        report.add("no left turn restrictions = " + noLeft);
        report.add("no right turn restrictions = " + noRight);
        report.add("no straight on turn restrictions = " + noStraightOn);
        report.add("no u-turn restrictions = " + noUTurn);

        report.add(Strings.line());
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
            System.out.println(Strings.line());
            final var keys = new StringList(lengthForHighwayType.keySet());
            keys.sort(Comparator.naturalOrder());
            for (final var type : keys)
            {
                System.out.println("highway['" + type + "'] = " + lengthForHighwayType.get(type));
            }
        }

        report.titledBox(LOGGER, "Statistics");
    }
}
