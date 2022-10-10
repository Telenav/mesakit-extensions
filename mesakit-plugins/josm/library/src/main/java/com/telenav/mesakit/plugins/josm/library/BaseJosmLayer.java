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

package com.telenav.mesakit.plugins.josm.library;

import com.telenav.mesakit.map.geography.Latitude;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.Longitude;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.measurements.geographic.Angle;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.measurements.geographic.Heading;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseJosmLayer extends Layer
{
    private final BaseJosmPlugin plugin;

    private boolean added;

    private boolean initialized;

    private MapView view;

    private final Map<Integer, Font> fontForWidth = new HashMap<>();

    protected BaseJosmLayer(BaseJosmPlugin plugin)
    {
        this(plugin, plugin.name());
    }

    protected BaseJosmLayer(BaseJosmPlugin plugin, String name)
    {
        super(name);
        this.plugin = plugin;
    }

    public BaseJosmLayer activeLayer()
    {
        return plugin().selectedLayer();
    }

    public void add()
    {
        if (!added)
        {
            added = true;
            MainApplication.getLayerManager().addLayer(this);
        }
    }

    @Override
    public void destroy()
    {
        super.destroy();
        plugin.destroyLayer();
        onDestroy();
    }

    public void forceRepaint()
    {
        if (MainApplication.getMap() != null && MainApplication.getMap().mapView != null)
        {
            SwingUtilities.invokeLater(this::invalidate);
        }
    }

    @Override
    public Icon getIcon()
    {
        return ImageProvider.get("layer", "marker_small");
    }

    @Override
    public Object getInfoComponent()
    {
        return null;
    }

    @Override
    public Action[] getMenuEntries()
    {
        return new Action[] {};
    }

    @Override
    public String getToolTipText()
    {
        return plugin.name().toLowerCase();
    }

    @Override
    public boolean isMergable(Layer other)
    {
        return false;
    }

    @Override
    public void mergeFrom(Layer from)
    {
    }

    public void onInitialize()
    {
    }

    @Override
    public void paint(Graphics2D graphics, MapView view, Bounds bounds)
    {
        this.view = view;

        // If we haven't yet initialized,
        if (!initialized)
        {
            initialize(view);
            initialized = true;
        }
        else
        {
            if (isVisible())
            {
                // Do anti-aliasing
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                var originalStroke = graphics.getStroke();
                var originalFont = graphics.getFont();
                onPaint(graphics, view, bounds);
                graphics.setFont(originalFont);
                graphics.setStroke(originalStroke);
            }
        }
    }

    public BaseJosmPanel panel()
    {
        return plugin().panel();
    }

    public BaseJosmPlugin plugin()
    {
        return plugin;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v)
    {
    }

    protected Rectangle2D awtRectangleForRectangle(Rectangle rectangle)
    {
        var topLeft = pointForLocation(rectangle.topLeft());
        var bottomRight = pointForLocation(rectangle.bottomRight());
        return new Rectangle2D.Double(topLeft.getX(), topLeft.getY(), Math.abs(topLeft.getX() - bottomRight.getX()),
                Math.abs(topLeft.getY() - bottomRight.getY()));
    }

    protected Rectangle bounds()
    {
        return Rectangle.fromLocations(locationForPoint(new Point2D.Double(0, 0)),
                locationForPoint(new Point2D.Double(view.getWidth(), view.getHeight())));
    }

    /**
     * Returns the largest font that will draw the given string within the given pixel width
     */
    @SuppressWarnings("SameParameterValue")
    protected Font font(Graphics2D graphics, String string, int width)
    {
        var font = fontForWidth.get(width);
        if (font == null)
        {
            var base = new Font("Helvetica", Font.BOLD, 8);
            for (var points = 8F; points < 50; points += 0.5f)
            {
                var next = base.deriveFont(points);
                if (graphics.getFontMetrics(next).stringWidth(string) < width)
                {
                    font = next;
                }
                else
                {
                    break;
                }
            }
            fontForWidth.put(width, font);
        }
        return font;
    }

    protected Location location(LatLon latlon)
    {
        var latitudeInDegrees = Math.min(90, Math.max(-90, latlon.getY()));
        var longitudeInDegrees = Math.min(180, Math.max(-180, latlon.getX()));
        return new Location(Latitude.degrees(latitudeInDegrees), Longitude.degrees(longitudeInDegrees));
    }

    protected Location locationForPoint(Point2D point)
    {
        var latitudeLongitude = view.getLatLon(point.getX(), point.getY());
        return new Location(Latitude.angle(Latitude.RANGE.constrained(Angle.degrees(latitudeLongitude.getY()))),
                Longitude.angle(Longitude.RANGE.constrained(Angle.degrees(latitudeLongitude.getX()))));
    }

    protected void onDestroy()
    {
    }

    protected void onNextSelection()
    {
    }

    protected abstract void onPaint(Graphics2D graphics, MapView view, Bounds bounds);

    protected void onPopup(Component parent, int x, int y)
    {
    }

    protected void onPreviousSelection()
    {
    }

    protected boolean onSelect(MouseEvent event)
    {
        return false;
    }

    protected Point2D pointForLocation(Location location)
    {
        return view.getPoint2D(new LatLon(location.latitude().asDegrees(), location.longitude().asDegrees()));
    }

    protected void popup(MouseEvent event)
    {
        if (onSelect(event))
        {
            onPopup(event.getComponent(), event.getX(), event.getY());
        }
    }

    protected Rectangle rectangle(Bounds bounds)
    {
        return Rectangle.fromLocations(location(bounds.getMin()), location(bounds.getMax()));
    }

    protected Stroke stroke()
    {
        var width = strokeWidth();
        if (width > 1)
        {
            return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, width / 2);
        }
        return new BasicStroke(width);
    }

    protected float strokeWidth()
    {
        var origin = locationForPoint(new Point2D.Double(0, 0));
        var movedEast = origin.moved(Heading.EAST, Distance.kilometers(8));
        var pixels = (float) pointForLocation(movedEast).getX() / 1000.0f;
        return pixels < 0.5 ? 0.5f : pixels;
    }

    protected Distance viewWidth()
    {
        var width = view.getWidth();
        var upperLeft = locationForPoint(new Point2D.Double(0, 0));
        @SuppressWarnings(
                "SuspiciousNameCombination") var location = locationForPoint(new Point2D.Double(width, width));
        return location.distanceTo(upperLeft);
    }

    private void initialize(MapView view)
    {
        // Initialize layer
        onInitialize();

        // Force repaint to happen later
        forceRepaint();

        // Add mouse listener
        view.addMouseListener(new MouseAdapter()
        {

            @Override
            public void mousePressed(MouseEvent event)
            {
                if (event.isPopupTrigger())
                {
                    popup(event);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                if (event.isPopupTrigger())
                {
                    popup(event);
                }
                else
                {
                    onSelect(event);
                }
            }
        });

        // Add key listener
        view.addKeyListener(new KeyAdapter()
        {

            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ALT || e.getKeyCode() == KeyEvent.VK_UP)
                {
                    onNextSelection();
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN)
                {
                    onPreviousSelection();
                }
            }
        });
    }
}
