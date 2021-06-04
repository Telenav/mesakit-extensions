package com.telenav.mesakit.plugins.josm.graph.view.graphics.coordinates;

import com.telenav.kivakit.kernel.language.primitives.Doubles;
import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingPoint;
import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingSize;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.geography.shape.rectangle.Size;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapProjection;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapView;

/**
 * @author jonathanl (shibo)
 */
public class JosmCoordinateMapper implements MapProjection
{
    private final MapView view;

    public JosmCoordinateMapper(final MapView view)
    {
        this.view = view;
    }

    @Override
    public DrawingSize drawingSize()
    {
        final var bounds = view.getBounds();
        return DrawingSize.pixels(bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public Rectangle mapArea()
    {
        final var bounds = view.getBounds();
        final var location = bounds.getLocation();
        final var width = bounds.getWidth();
        final var height = bounds.getHeight();
        return Rectangle.fromLocations(
                Location.degrees(location.getY(), location.getX()),
                Location.degrees(location.getY() + height, location.getX() + width));
    }

    @Override
    public DrawingSize toDrawing(final Size size)
    {
        return null;
    }

    @Override
    public DrawingPoint toDrawing(final Location location)
    {
        return DrawingPoint.point(view.getPoint(new LatLon(location.latitude().asDegrees(), location.longitude().asDegrees())));
    }

    @Override
    public Location toMap(final DrawingPoint point)
    {
        final var latlon = view.getLatLon(point.x(), point.y());
        return Location.degrees(Doubles.inRange(latlon.getY(), -85, 85), Doubles.inRange(latlon.getX(), -180, 180));
    }
}
