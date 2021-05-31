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

package com.telenav.tdk.josm.plugins.geojson;

import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.josm.plugins.library.BaseJosmLayer;
import com.telenav.kivakit.kernel.interfaces.naming.NamedObject;
import com.telenav.kivakit.kernel.language.values.Count;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;
import com.telenav.kivakit.kernel.scalars.levels.Percent;
import com.telenav.kivakit.utilities.ui.swing.graphics.color.ColorConverter;
import com.telenav.mesakit.map.geography.indexing.rtree.RTreeSettings;
import com.telenav.mesakit.map.geography.indexing.rtree.RTreeSpatialIndex;
import com.telenav.mesakit.map.measurements.Distance;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonDocument;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonFeature;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonGeometry;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonPoint;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonPolyline;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoJsonLayer extends BaseJosmLayer implements NamedObject
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private GeoJsonDocument document;

    private GeoJsonFeature selectedFeature;

    private List<GeoJsonFeature> selectedFeatures;

    private final Map<Shape, GeoJsonFeature> featureForShape = new HashMap<>();

    private final ColorConverter colorConverter = new ColorConverter(LOGGER);

    private RTreeSpatialIndex<GeoJsonFeature> spatialIndex;

    private File file;

    public GeoJsonLayer(final GeoJsonPlugin plugin, final String name)
    {
        super(plugin, name);
    }

    public GeoJsonDocument getDocument()
    {
        return document;
    }

    public void setDocument(final GeoJsonDocument document)
    {
        spatialIndex = new RTreeSpatialIndex<>(objectName() + ".spatialIndex", new RTreeSettings());
        final List<GeoJsonFeature> features = new ArrayList<>();
        for (final var feature : document.features())
        {
            if (feature.bounds() != null)
            {
                features.add(feature);
            }
            else
            {
                LOGGER.warning("Not indexing GeoJson feature with missing bounds: $", feature);
            }
        }
        spatialIndex.bulkLoad(features);
        this.document = document;
        if (panel() != null)
        {
            panel().refresh();
        }
    }

    public File getFile()
    {
        return file;
    }

    public void setFile(final File file)
    {
        this.file = file;
    }

    @Override
    public void onPaint(final Graphics2D graphics, final MapView view, final Bounds bounds)
    {
        if (document != null)
        {
            // Clear out feature map
            featureForShape.clear();

            // If the document is small enough, or the view is narrow enough
            if (document.size() < 10000 || viewWidth().isLessThan(Distance.miles(10)))
            {

                // we can draw each feature in the document
                final var panel = panel();
                if (panel != null)
                {

                    // draw lines first so we can see points
                    drawFeatures(graphics, bounds, panel, false);

                    // draw points on top
                    drawFeatures(graphics, bounds, panel, true);
                }
                if (selectedFeature != null)
                {
                    final var arrowIndex = arrowGeometryIndex(selectedFeature);
                    var index = 0;
                    for (final var geometry : selectedFeature)
                    {
                        if (index++ != arrowIndex)
                        {
                            drawGeometry(graphics, selectedFeature, geometry,
                                    colorForGeometry(selectedFeature, geometry, true), Color.ORANGE, Color.BLUE, true);
                        }
                    }
                }
            }
            else
            {
                drawHeatMap(graphics);
            }
        }
    }

    @Override
    public GeoJsonPanel panel()
    {
        return (GeoJsonPanel) super.panel();
    }

    @Override
    public GeoJsonPlugin plugin()
    {
        return (GeoJsonPlugin) super.plugin();
    }

    @Override
    public void visitBoundingBox(final BoundingXYVisitor v)
    {
        if (document != null)
        {
            final var bounds = document.bounds();
            if (bounds != null)
            {
                v.visit(new Bounds(bounds.bottom().asDegrees(), bounds.left().asDegrees(), bounds.top().asDegrees(),
                        bounds.right().asDegrees()));
            }
        }
    }

    public void zoomToFeature(final GeoJsonFeature feature)
    {
        if (feature != null)
        {
            final var bounds = feature.bounds();
            if (bounds != null)
            {
                plugin().zoomTo(bounds.expanded(new Percent(50)));
            }
            selectedFeature = feature;
        }
    }

    @Override
    protected void onDestroy()
    {
        if (panel() != null)
        {
            panel().refresh();
        }
    }

    @Override
    protected void onInitialize()
    {
        if (panel() != null)
        {
            panel().refresh();
        }
    }

    @Override
    protected void onNextSelection()
    {
        if (selectedFeatures != null && selectedFeatures.size() > 1)
        {
            final var index = selectedFeatures.indexOf(selectedFeature);
            if (index != -1)
            {
                selectFeature(selectedFeatures.get((index + 1) % selectedFeatures.size()));
            }
        }
    }

    @Override
    protected void onPopup(final Component parent, final int x, final int y)
    {
        final var menu = new GeoJsonFeaturePopUpMenu(selectedFeature);
        menu.show(parent, x, y);
    }

    @Override
    protected void onPreviousSelection()
    {
        if (selectedFeatures != null && selectedFeatures.size() > 1)
        {
            final var index = selectedFeatures.indexOf(selectedFeature);
            if (index != -1)
            {
                selectFeature(selectedFeatures.get(Math.abs(index - 1) % selectedFeatures.size()));
            }
        }
    }

    @Override
    protected boolean onSelect(final MouseEvent event)
    {
        final var features = featuresForPoint(event.getPoint());
        if (!features.isEmpty())
        {
            selectFeature(features.get(0));
            selectedFeatures = features;
            return true;
        }
        return false;
    }

    private int arrowGeometryIndex(final GeoJsonFeature feature)
    {
        try
        {
            final var value = feature.properties().get("arrowGeometryIndex");
            if (value != null)
            {
                if (value instanceof Double)
                {
                    return ((Double) value).intValue();
                }
                return Integer.parseInt(value.toString());
            }
        }
        catch (final Exception ignored)
        {
        }
        return -1;
    }

    private Color colorForGeometry(final GeoJsonFeature feature, final GeoJsonGeometry geometry,
                                   final boolean highlight)
    {
        if (isInactiveLayer())
        {
            return Color.GRAY;
        }
        final var colorProperty = feature.properties().get("color");
        if (colorProperty != null)
        {
            final var color = colorConverter.convert(colorProperty.toString());
            if (color != null)
            {
                return highlight ? color.invert().asAwtColor() : color.asAwtColor();
            }
            else
            {
                return null;
            }
        }
        else
        {
            if (geometry instanceof GeoJsonPoint)
            {
                return highlight ? Color.GREEN.brighter() : Color.ORANGE.darker();
            }
            if (geometry instanceof GeoJsonPolyline)
            {
                return highlight ? Color.YELLOW : Color.BLUE.darker();
            }
        }
        return null;
    }

    private void drawFeatures(final Graphics2D graphics, final Bounds bounds, final GeoJsonPanel panel,
                              final boolean drawPoints)
    {
        for (final var feature : spatialIndex.intersecting(rectangle(bounds)))
        {
            final var arrowIndex = arrowGeometryIndex(feature);
            if ((isInactiveLayer() || panel.isVisible(feature))
                    && (selectedFeature == null || feature != selectedFeature))
            {
                var index = 0;
                for (final var geometry : feature)
                {
                    if (index++ != arrowIndex)
                    {
                        if (!drawPoints || geometry instanceof GeoJsonPoint)
                        {
                            drawGeometry(graphics, feature, geometry, colorForGeometry(feature, geometry, false),
                                    null, null, viewWidth().isLessThan(Distance.miles(500)));
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "SuspiciousNameCombination" })
    private void drawGeometry(final Graphics2D graphics, final GeoJsonFeature feature,
                              final GeoJsonGeometry geometry, final Color color, final Color annotationBackground,
                              final Color annotationText,
                              final boolean drawTitle)
    {
        final var width = (int) strokeWidth();
        if (feature.title() != null && drawTitle)
        {
            graphics.setColor(color);
            final var area = awtRectangleForRectangle(feature.bounds());
            graphics.drawRect((int) area.getX(), (int) area.getY(), (int) area.getWidth(), (int) area.getHeight());
            final var textBounds = graphics.getFontMetrics().getStringBounds(feature.title(), graphics);
            graphics.drawString(feature.title(), (int) area.getX(),
                    (int) area.getY() - (int) textBounds.getHeight());
        }
        if (geometry instanceof GeoJsonPoint)
        {
            final var location = ((GeoJsonPoint) geometry).location();
            final var point = pointForLocation(location);
            graphics.setColor(color);
            final var x = (int) point.getX();
            final var y = (int) point.getY();
            graphics.fillOval(x, y, width, width);
            graphics.setColor(color.darker());
            final var outlineWidth = Math.max(2, (width / 8) * 2 / 2);
            graphics.setStroke(new BasicStroke(outlineWidth + 2));
            graphics.drawOval(x, y, width, width);
        }
        if (geometry instanceof GeoJsonPolyline)
        {
            graphics.setColor(color);
            final var line = ((GeoJsonPolyline) geometry).polyline();
            var first = true;
            final Path2D path = new Path2D.Float();
            for (final var to : line.locationSequence())
            {
                final var point = pointForLocation(to);
                if (first)
                {
                    path.moveTo(point.getX(), point.getY());
                    first = false;
                }
                else
                {
                    path.lineTo(point.getX(), point.getY());
                }
            }
            final var stroke = stroke();
            final var shape = stroke.createStrokedShape(path);
            graphics.fill(shape);
            featureForShape.put(shape, feature);
            if (annotationBackground != null)
            {
                var index = 1;
                final var font = font(graphics, "99", width);
                graphics.setFont(font);
                for (final var location : line.locationSequence())
                {
                    final var point = pointForLocation(location);
                    final var centerX = (int) point.getX();
                    final var centerY = (int) point.getY();
                    final var x = centerX - width / 2;
                    final var y = centerY - width / 2;
                    graphics.setColor(annotationText);
                    graphics.fillOval(x - 1, y - 1, width + 2, width + 2);
                    graphics.setColor(annotationBackground);
                    //noinspection SuspiciousNameCombination
                    graphics.fillOval(x, y, width, width);
                    if (font != null)
                    {
                        final var string = "" + index;
                        final var metrics = graphics.getFontMetrics();
                        final var bounds = metrics.getStringBounds(string, graphics);
                        graphics.setColor(annotationText);
                        graphics.drawString(string, centerX - (int) bounds.getWidth() / 2,
                                centerY + metrics.getAscent() / 2);
                    }
                    index++;
                }
            }
        }
    }

    private void drawHeatMap(final Graphics2D graphics)
    {
        final var size = viewWidth().scaledBy(0.02);
        var maximum = Count._0;
        final Map<Rectangle, Count> counts = new HashMap<>();
        for (final var cell : bounds().cells(size))
        {
            final var count = Count.count(spatialIndex.intersecting(cell));
            maximum = maximum.maximum(count);
            counts.put(cell, count);
        }
        for (final var cell : bounds().cells(size))
        {
            final var count = counts.get(cell);
            graphics.setColor(new Color(255, 0, 0,
                    Math.min(255, (int) (255 * ((double) count.asInt() / (double) maximum.asInt())))));
            final var topLeft = pointForLocation(cell.topLeft());
            final var bottomRight = pointForLocation(cell.bottomRight());
            final var x = (int) topLeft.getX();
            final var y = (int) topLeft.getY();
            final var width = (int) (bottomRight.getX() - x);
            final var height = (int) (bottomRight.getY() - y);
            graphics.fillRect(x, y, width, height);
        }
    }

    private List<GeoJsonFeature> featuresForPoint(final Point point)
    {
        final List<GeoJsonFeature> features = new ArrayList<>();
        for (final var entry : featureForShape.entrySet())
        {
            if (entry.getKey().contains(point))
            {
                features.add(entry.getValue());
            }
        }
        return features;
    }

    private boolean isInactiveLayer()
    {
        return activeLayer() != this;
    }

    private void selectFeature(final GeoJsonFeature feature)
    {
        selectedFeature = feature;
        if (panel() != null)
        {
            panel().selectFeature(selectedFeature);
        }
        forceRepaint();
    }
}
