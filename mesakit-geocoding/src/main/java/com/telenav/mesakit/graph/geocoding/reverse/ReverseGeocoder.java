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

package com.telenav.mesakit.graph.geocoding.reverse;

import com.telenav.kivakit.language.level.Percent;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.geocoding.reverse.matching.FuzzyRoadNameMatcher;
import com.telenav.mesakit.graph.geocoding.reverse.matching.RoadNameMatcher;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.polyline.PolylineSnap;
import com.telenav.mesakit.map.geography.shape.polyline.PolylineSnapper;
import com.telenav.mesakit.map.measurements.geographic.Angle;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.measurements.geographic.Heading;
import com.telenav.mesakit.map.road.model.RoadName;
import com.telenav.mesakit.map.road.name.standardizer.RoadNameStandardizer;

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

        private Percent roadNameCloseness;

        private RoadNameMatcher roadNameMatcher = new FuzzyRoadNameMatcher();

        private Angle headingTolerance = Angle.degrees(45);

        /** if compare direction of name or not */
        private boolean compareDirection = true;

        public boolean compareDirection()
        {
            return compareDirection;
        }

        public void compareDirection(boolean compare)
        {
            compareDirection = compare;
        }

        public Angle headingTolerance()
        {
            return headingTolerance;
        }

        public void headingTolerance(Angle headingTolerance)
        {
            this.headingTolerance = headingTolerance;
        }

        public Percent roadNameCloseness()
        {
            return roadNameCloseness;
        }

        /**
         * @param roadNameCloseness The minimum percentage of characters that must be the same between one road name and
         * another for them to be considered equivalent.
         * <p>
         * This is computed using the Levenshtein distance divided by the string length. For example, the Levenshtein
         * distance between "Latona Ave NE" and "Latona Ave" is 3 (because 3 characters must be added to "Latona Ave" to
         * get "Latona Ave NE"), which if we're looking for "Latona Ave" is a distance of 3 / 10 or 30%. A 30% distance
         * is a closeness of 70%.
         * <p>
         * Given this, if your roadNameCloseness in this configuration was 70%, the two names would be considered
         * equivalent.
         */
        public void roadNameCloseness(Percent roadNameCloseness)
        {
            this.roadNameCloseness = roadNameCloseness;
        }

        public RoadNameMatcher roadNameMatcher()
        {
            return roadNameMatcher;
        }

        public void roadNameMatcher(RoadNameMatcher roadNameMatcher)
        {
            this.roadNameMatcher = roadNameMatcher;
        }

        public RoadNameStandardizer roadNameStandardizer()
        {
            return roadNameStandardizer;
        }

        public void roadNameStandardizer(RoadNameStandardizer standardizer)
        {
            roadNameStandardizer = standardizer;
        }

        public Distance within()
        {
            return within;
        }

        public void within(Distance within)
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

        public void heading(Heading heading)
        {
            this.heading = heading;
        }

        public Location location()
        {
            return location;
        }

        public void location(Location location)
        {
            this.location = location;
        }

        public RoadName roadName()
        {
            return roadName;
        }

        public void roadName(RoadName roadName)
        {
            this.roadName = roadName;
        }
    }

    public static class Response
    {
        private final Edge edge;

        private final PolylineSnap snap;

        private Percent percentage = Percent._0;

        Response(Edge edge, PolylineSnap snap)
        {
            this.edge = edge;
            this.snap = snap;
        }

        Response(Edge edge, PolylineSnap snap, Percent percentage)
        {
            this(edge, snap);
            this.percentage = percentage;
        }

        public Edge edge()
        {
            return edge;
        }

        public Percent percentage()
        {
            return percentage;
        }

        public PolylineSnap snap()
        {
            return snap;
        }
    }

    private final Graph graph;

    private final Configuration configuration;

    public ReverseGeocoder(Graph graph, Configuration configuration)
    {
        this.graph = graph;
        this.configuration = configuration;
    }

    public Response locate(Request request)
    {
        var snapper = new PolylineSnapper();
        var standardizer = configuration.roadNameStandardizer();
        var location = request.location();
        var heading = request.heading();

        var desired = request.roadName();
        if (standardizer != null)
        {
            desired = request.roadName() != null ? standardizer.standardize(request.roadName()).asRoadName()
                    : null;
        }

        var closestDistance = Distance.MAXIMUM;
        Response response = null;
        var highestRoadNameCloseness = Percent._0;

        // Go through each edge within the given distance of the requested location
        for (var edge : graph.edgesIntersecting(location.within(configuration.within())))
        {
            // and if no heading was specified or the edge's heading is close to what we're
            // looking for,
            if (heading == null || edge.heading().isClose(heading, configuration.headingTolerance()))
            {
                var roadNameCloseness = Percent._100;
                if (desired != null)
                {
                    roadNameCloseness = matches(edge.roadNames(), desired, standardizer, edge.heading());
                }
                if (desired == null || (roadNameCloseness.isGreaterThan(configuration.roadNameCloseness())
                        && roadNameCloseness.isGreaterThanOrEqualTo(highestRoadNameCloseness)))
                {
                    highestRoadNameCloseness = roadNameCloseness;
                    // then snap the location to the edge
                    var snap = snapper.snap(edge, location);

                    // and if the snap is the closest we've seen so far,
                    if (snap.distanceToSource().isLessThan(closestDistance))
                    {
                        // create a new response
                        closestDistance = snap.distanceToSource();
                        response = new Response(edge, snap, roadNameCloseness);
                    }
                }
            }
        }
        return response;
    }

    private Percent matches(Set<RoadName> roadNames,
                            RoadName desired,
                            RoadNameStandardizer standardizer,
                            Heading edgeHeading)
    {
        var highestScore = Percent._0;
        for (var roadName : roadNames)
        {
            // and if the edge is named,
            if (roadName != null)
            {
                if (desired.extractDirection() != null && roadName.extractDirection() == null)
                {
                    roadName = RoadName.forName(roadName.name() + " " + edgeHeading.asApproximateDirection());
                }
                // and the standardized road name matches the desired road name,
                var score = configuration.roadNameMatcher().matches(
                        standardizer != null ? standardizer.standardize(roadName).asRoadName() : roadName, desired);
                if (score.isGreaterThan(highestScore))
                {
                    highestScore = score;
                }
            }
        }
        return highestScore;
    }
}
