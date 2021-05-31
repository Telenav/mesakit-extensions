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

package com.telenav.kivakit.josm.plugins.library.tile.vector;

import com.telenav.kivakit.graph.traffic.roadsection.RoadSectionCode;
import com.telenav.kivakit.map.geography.polyline.Polyline;
import com.telenav.kivakit.map.measurements.Speed;

import java.awt.Color;

public class RoadFeature
{
    private final Polyline line;

    private RoadSectionCode trafficIdentifier;

    private Speed speed;

    private Color color;

    private int zOrder;

    public RoadFeature(final Polyline line)
    {
        this.line = line;
    }

    public Color color()
    {
        return color;
    }

    public void color(final Color color)
    {
        this.color = color;
    }

    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof RoadFeature)
        {
            final var that = (RoadFeature) object;
            return trafficIdentifier == that.trafficIdentifier;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return trafficIdentifier.hashCode();
    }

    public Polyline line()
    {
        return line;
    }

    public Speed speed()
    {
        return speed;
    }

    public void speed(final Speed speed)
    {
        this.speed = speed;
    }

    @Override
    public String toString()
    {
        return trafficIdentifier + " = " + (speed == null ? "no speed" : speed);
    }

    public RoadSectionCode trafficIdentifier()
    {
        return trafficIdentifier;
    }

    public void trafficIdentifier(final RoadSectionCode trafficIdentifier)
    {
        this.trafficIdentifier = trafficIdentifier;
    }

    public int zOrder()
    {
        return zOrder;
    }

    public void zOrder(final int zOrder)
    {
        this.zOrder = zOrder;
    }
}
