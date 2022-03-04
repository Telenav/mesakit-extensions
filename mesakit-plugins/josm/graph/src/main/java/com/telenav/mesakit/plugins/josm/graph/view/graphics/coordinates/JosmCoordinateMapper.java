package com.telenav.mesakit.plugins.josm.graph.view.graphics.coordinates;

import com.telenav.kivakit.core.language.primitive.Doubles;
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

    public JosmCoordinateMapper(MapView view)
    {
        this.view = view;
    }

    @Override
    public DrawingSize drawingSize()
    {
        var bounds = view.getBounds();
        return DrawingSize.pixels(bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public Rectangle mapArea()
    {
        var bounds = view.getBounds();
        var location = bounds.getLocation();
        var width = bounds.getWidth();
        var height = bounds.getHeight();
        return Rectangle.fromLocations(
                Location.degrees(location.getY(), location.getX()),
                Location.degrees(location.getY() + height, location.getX() + width));
    }

    @Override
    public DrawingSize toDrawing(Size size)
    {
        return null;
    }

    @Override
    public DrawingPoint toDrawing(Location location)
    {
        return DrawingPoint.point(view.getPoint(new LatLon(location.latitude().asDegrees(), location.longitude().asDegrees())));
    }

    @Override
    public Location toMap(DrawingPoint point)
    {
        var latlon = view.getLatLon(point.x(), point.y());
        return Location.degrees(Doubles.inRange(latlon.getY(), -85, 85), Doubles.inRange(latlon.getX(), -180, 180));
    }
}
