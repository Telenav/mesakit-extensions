package com.telenav.mesakit.plugins.josm.graph.view.graphics.coordinates;

import com.telenav.kivakit.kernel.language.primitives.Doubles;
import com.telenav.mesakit.map.geography.Latitude;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.Longitude;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;

import java.awt.geom.Point2D;

/**
 * @author jonathanl (shibo)
 */
public class JosmCoordinateMapper implements CoordinateMapper
{
    private final MapView view;

    public JosmCoordinateMapper(final MapView view)
    {
        this.view = view;
    }

    @Override
    public Location locationForPoint(final Point2D point)
    {
        final var latitudeLongitude = view.getLatLon(point.getX(), point.getY());
        return new Location(Latitude.degrees(Doubles.inRange(latitudeLongitude.getY(), -90, 90)),
                Longitude.degrees(Doubles.inRange(latitudeLongitude.getX(), -180, 180)));
    }

    @Override
    public Point2D pointForLocation(final Location location)
    {
        return view.getPoint(new LatLon(location.latitude().asDegrees(), location.longitude().asDegrees()));
    }
}
