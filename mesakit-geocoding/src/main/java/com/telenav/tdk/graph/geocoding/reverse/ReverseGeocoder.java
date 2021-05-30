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

package com.telenav.tdk.graph.geocoding.reverse;

import com.telenav.tdk.core.kernel.scalars.levels.Percentage;
import com.telenav.tdk.graph.geocoding.reverse.matching.FuzzyRoadNameMatcher;
import com.telenav.tdk.graph.geocoding.reverse.matching.RoadNameMatcher;
import com.telenav.tdk.graph.specifications.osm.graph.edge.model.attributes.OsmEdgeAttributes;
import com.telenav.tdk.graph.traffic.roadsection.*;
import com.telenav.tdk.map.geography.Location;
import com.telenav.tdk.map.geography.polyline.*;
import com.telenav.tdk.map.measurements.*;
import com.telenav.tdk.map.road.model.RoadName;
import com.telenav.tdk.map.road.name.standardizer.RoadNameStandardizer;

import java.util.Set;

/**
 * Takes a location and locates the nearest appropriate edge. A road name and a heading can give assistance in finding
 * the right edge.
 *
 * @author jonathanl (shibo)
 */
public class ReverseGeocoder
{
    public static class Configuration
    {
        private Distance within;

        private RoadNameStandardizer roadNameStandardizer;

        private Percentage roadNameCloseness;

        private RoadSectionCodingSystem roadSectionCodingSystem = RoadSectionCodingSystem.TMC;

        private RoadNameMatcher roadNameMatcher = new FuzzyRoadNameMatcher();

        private Angle headingTolerance = Angle.degrees(45);

        /** if compare direction of name or not */
        private boolean compareDirection = true;

        public boolean compareDirection()
        {
            return compareDirection;
        }

        public void compareDirection(final boolean compare)
        {
            compareDirection = compare;
        }

        public Angle headingTolerance()
        {
            return headingTolerance;
        }

        public void headingTolerance(final Angle headingTolerance)
        {
            this.headingTolerance = headingTolerance;
        }

        public Percentage roadNameCloseness()
        {
            return roadNameCloseness;
        }

        /**
         * @param roadNameCloseness The minimum percentage of characters that must be the same between one road name and
         * another for them to be considered equivalent.
         * <p>
         * This is computed using the Levenschtein distance divided by the string length. For example, the Levenschtein
         * distance between "Latona Ave NE" and "Latona Ave" is 3 (because 3 characters must be added to "Latona Ave" to
         * get "Latona Ave NE"), which if we're looking for "Latona Ave" is a distance of 3 / 10 or 30%. A 30% distance
         * is a closeness of 70%.
         * <p>
         * Given this, if your roadNameCloseness in this configuration was 70%, the two names would be considered
         * equivalent.
         */
        public void roadNameCloseness(final Percentage roadNameCloseness)
        {
            this.roadNameCloseness = roadNameCloseness;
        }

        public RoadNameMatcher roadNameMatcher()
        {
            return roadNameMatcher;
        }

        public void roadNameMatcher(final RoadNameMatcher roadNameMatcher)
        {
            this.roadNameMatcher = roadNameMatcher;
        }

        public RoadNameStandardizer roadNameStandardizer()
        {
            return roadNameStandardizer;
        }

        public void roadNameStandardizer(final RoadNameStandardizer standardizer)
        {
            roadNameStandardizer = standardizer;
        }

        public RoadSectionCodingSystem roadSectionCodingSystem()
        {
            return roadSectionCodingSystem;
        }

        public void roadSectionCodingSystem(final RoadSectionCodingSystem roadSectionCodingSystem)
        {
            this.roadSectionCodingSystem = roadSectionCodingSystem;
        }

        public Distance within()
        {
            return within;
        }

        public void within(final Distance within)
        {
            this.within = within;
        }
    }

    public static class Request
    {
        private Location location;

        private Heading heading;

        private RoadName roadName;

        public Heading heading()
        {
            return heading;
        }

        public void heading(final Heading heading)
        {
            this.heading = heading;
        }

        public Location location()
        {
            return location;
        }

        public void location(final Location location)
        {
            this.location = location;
        }

        public RoadName roadName()
        {
            return roadName;
        }

        public void roadName(final RoadName roadName)
        {
            this.roadName = roadName;
        }
    }

    public static class Response
    {
        private final Edge edge;

        private final RoadSectionCode roadSectionCode;

        private final PolylineSnap snap;

        private Percentage percentage = Percentage._0;

        Response(final Edge edge, final RoadSectionCode roadSectionCode, final PolylineSnap snap)
        {
            this.edge = edge;
            this.roadSectionCode = roadSectionCode;
            this.snap = snap;
        }

        Response(final Edge edge, final RoadSectionCode roadSectionCode, final PolylineSnap snap,
                 final Percentage percentage)
        {
            this(edge, roadSectionCode, snap);
            this.percentage = percentage;
        }

        public Edge edge()
        {
            return edge;
        }

        public Percentage percentage()
        {
            return percentage;
        }

        public RoadSectionCode roadSectionCode()
        {
            return roadSectionCode;
        }

        public PolylineSnap snap()
        {
            return snap;
        }
    }

    private final Graph graph;

    private final Configuration configuration;

    public ReverseGeocoder(final Graph graph, final Configuration configuration)
    {
        this.graph = graph;
        this.configuration = configuration;
    }

    public Response locate(final Request request)
    {
        final var snapper = new PolylineSnapper();
        final var standardizer = configuration.roadNameStandardizer();
        final var location = request.location();
        final var heading = request.heading();

        var desired = request.roadName();
        if (standardizer != null)
        {
            desired = request.roadName() != null ? standardizer.standardize(request.roadName()).asRoadName()
                    : null;
        }

        var closestDistance = Distance.MAXIMUM;
        Response response = null;
        var highestRoadNameCloseness = Percentage._0;

        // Go through each edge within the given distance of the requested location
        for (final var edge : graph.edgesIntersecting(location.within(configuration.within())))
        {
            // and if no heading was specified or the edge's heading is close to what we're
            // looking for,
            if (heading == null || edge.heading().isClose(heading, configuration.headingTolerance()))
            {
                var roadNameCloseness = Percentage._100;
                if (desired != null)
                {
                    roadNameCloseness = matches(edge.roadNames(), desired, standardizer, edge.heading());
                }
                if (desired == null || (roadNameCloseness.isGreaterThan(configuration.roadNameCloseness())
                        && roadNameCloseness.isGreaterThanOrEqualTo(highestRoadNameCloseness)))
                {
                    highestRoadNameCloseness = roadNameCloseness;
                    // then snap the location to the edge
                    final var snap = snapper.snap(edge, location);

                    // and if the snap is the closest we've seen so far,
                    if (snap.distanceToSource().isLessThan(closestDistance))
                    {
                        // create a new response
                        closestDistance = snap.distanceToSource();
                        response = new Response(edge, roadSectionCode(edge, snap), snap, roadNameCloseness);
                    }
                }
            }
        }
        return response;
    }

    private Percentage matches(final Set<RoadName> roadNames, final RoadName desired,
                               final RoadNameStandardizer standardizer, final Heading edgeHeading)
    {
        var highestScore = Percentage._0;
        for (var roadName : roadNames)
        {
            // and if the edge is named,
            if (roadName != null)
            {
                if (desired.extractDirection() != null && roadName.extractDirection() == null)
                {
                    roadName = RoadName.forName(roadName.name() + " " + edgeHeading.asDirection());
                }
                // and the standardized road name matches the desired road name,
                final var score = configuration.roadNameMatcher().matches(
                        standardizer != null ? standardizer.standardize(roadName).asRoadName() : roadName, desired);
                if (score.isGreaterThan(highestScore))
                {
                    highestScore = score;
                }
            }
        }
        return highestScore;
    }

    private RoadSectionCode roadSectionCode(final Edge edge, final PolylineSnap snap)
    {
        RoadSectionCode code = null;

        if (configuration.roadSectionCodingSystem().equals(RoadSectionCodingSystem.TMC)
                && edge.supports(OsmEdgeAttributes.get().FORWARD_TMC_IDENTIFIERS))
        {
            var closest = Distance.MAXIMUM;
            for (final var identifier : edge.tmcIdentifiers())
            {
                final var section = identifier.roadSection();
                final var proximity = snap.distanceTo(section.start()).add(snap.distanceTo(section.end()));
                if (proximity.isLessThan(closest))
                {
                    closest = proximity;
                    code = identifier.asCode();
                }
            }
        }
        else if (configuration.roadSectionCodingSystem().equals(RoadSectionCodingSystem.TELENAV_TRAFFIC_LOCATION)
                && edge.supports(OsmEdgeAttributes.get().FORWARD_TELENAV_TRAFFIC_LOCATION_IDENTIFIER))
        {
            if (edge.osmTelenavTrafficLocationIdentifier() != null)
            {
                code = edge.osmTelenavTrafficLocationIdentifier().asCode();
            }
        }
        return code;
    }
}
