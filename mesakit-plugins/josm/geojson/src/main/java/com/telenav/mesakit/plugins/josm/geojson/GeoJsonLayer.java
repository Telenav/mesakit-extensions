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

package com.telenav.mesakit.plugins.josm.geojson;

import com.telenav.kivakit.core.logging.Logger;
import com.telenav.kivakit.core.logging.LoggerFactory;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.level.Percent;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.interfaces.naming.NamedObject;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Color;
import com.telenav.mesakit.map.geography.indexing.rtree.RTreeSettings;
import com.telenav.mesakit.map.geography.indexing.rtree.RTreeSpatialIndex;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonDocument;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonFeature;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonGeometry;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonPoint;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonPolyline;
import com.telenav.mesakit.plugins.josm.library.BaseJosmLayer;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
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

    private final Color.ColorConverter colorConverter = new Color.ColorConverter(LOGGER);

    private RTreeSpatialIndex<GeoJsonFeature> spatialIndex;

    private File file;

    public GeoJsonLayer(GeoJsonPlugin plugin, String name)
    {
        super(plugin, name);
    }

    public GeoJsonDocument getDocument()
    {
        return document;
    }

    public File getFile()
    {
        return file;
    }

    @Override
    public void onInitialize()
    {
        if (panel() != null)
        {
            panel().refresh();
        }
    }

    @Override
    public void onPaint(Graphics2D graphics, MapView view, Bounds bounds)
    {
        if (document != null)
        {
            // Clear out feature map
            featureForShape.clear();

            // If the document is small enough, or the view is narrow enough
            if (document.size() < 10000 || viewWidth().isLessThan(Distance.miles(10)))
            {

                // we can draw each feature in the document
                var panel = panel();
                if (panel != null)
                {

                    // draw lines first so we can see points
                    drawFeatures(graphics, bounds, panel, false);

                    // draw points on top
                    drawFeatures(graphics, bounds, panel, true);
                }
                if (selectedFeature != null)
                {
                    var arrowIndex = arrowGeometryIndex(selectedFeature);
                    var index = 0;
                    for (var geometry : selectedFeature)
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

    public void setDocument(GeoJsonDocument document)
    {
        spatialIndex = new RTreeSpatialIndex<>(objectName() + ".spatialIndex", new RTreeSettings());
        List<GeoJsonFeature> features = new ArrayList<>();
        for (var feature : document.features())
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

    public void setFile(File file)
    {
        this.file = file;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v)
    {
        if (document != null)
        {
            var bounds = document.bounds();
            if (bounds != null)
            {
                v.visit(new Bounds(bounds.bottom().asDegrees(), bounds.left().asDegrees(), bounds.top().asDegrees(),
                        bounds.right().asDegrees()));
            }
        }
    }

    public void zoomToFeature(GeoJsonFeature feature)
    {
        if (feature != null)
        {
            var bounds = feature.bounds();
            if (bounds != null)
            {
                plugin().zoomTo(bounds.expanded(Percent.of(50)));
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
    protected void onNextSelection()
    {
        if (selectedFeatures != null && selectedFeatures.size() > 1)
        {
            var index = selectedFeatures.indexOf(selectedFeature);
            if (index != -1)
            {
                selectFeature(selectedFeatures.get((index + 1) % selectedFeatures.size()));
            }
        }
    }

    @Override
    protected void onPopup(Component parent, int x, int y)
    {
        var menu = new GeoJsonFeaturePopUpMenu(selectedFeature);
        menu.show(parent, x, y);
    }

    @Override
    protected void onPreviousSelection()
    {
        if (selectedFeatures != null && selectedFeatures.size() > 1)
        {
            var index = selectedFeatures.indexOf(selectedFeature);
            if (index != -1)
            {
                selectFeature(selectedFeatures.get(Math.abs(index - 1) % selectedFeatures.size()));
            }
        }
    }

    @Override
    protected boolean onSelect(MouseEvent event)
    {
        var features = featuresForPoint(event.getPoint());
        if (!features.isEmpty())
        {
            selectFeature(features.get(0));
            selectedFeatures = features;
            return true;
        }
        return false;
    }

    private int arrowGeometryIndex(GeoJsonFeature feature)
    {
        try
        {
            var value = feature.properties().get("arrowGeometryIndex");
            if (value != null)
            {
                if (value instanceof Double)
                {
                    return ((Double) value).intValue();
                }
                return Integer.parseInt(value.toString());
            }
        }
        catch (Exception ignored)
        {
        }
        return -1;
    }

    private Color colorForGeometry(GeoJsonFeature feature,
                                   GeoJsonGeometry geometry,
                                   boolean highlight)
    {
        if (isInactiveLayer())
        {
            return Color.GRAY;
        }
        var colorProperty = feature.properties().get("color");
        if (colorProperty != null)
        {
            var color = colorConverter.convert(colorProperty.toString());
            if (color != null)
            {
                return highlight ? color.invert() : color;
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

    private void drawFeatures(Graphics2D graphics, Bounds bounds, GeoJsonPanel panel,
                              boolean drawPoints)
    {
        for (var feature : spatialIndex.intersecting(rectangle(bounds)))
        {
            var arrowIndex = arrowGeometryIndex(feature);
            if ((isInactiveLayer() || panel.isVisible(feature))
                    && (selectedFeature == null || feature != selectedFeature))
            {
                var index = 0;
                for (var geometry : feature)
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
    private void drawGeometry(Graphics2D graphics, GeoJsonFeature feature,
                              GeoJsonGeometry geometry, Color color, Color annotationBackground,
                              Color annotationText,
                              boolean drawTitle)
    {
        var width = (int) strokeWidth();
        if (feature.title() != null && drawTitle)
        {
            graphics.setColor(color.asAwtColor());
            var area = awtRectangleForRectangle(feature.bounds());
            graphics.drawRect((int) area.getX(), (int) area.getY(), (int) area.getWidth(), (int) area.getHeight());
            var textBounds = graphics.getFontMetrics().getStringBounds(feature.title(), graphics);
            graphics.drawString(feature.title(), (int) area.getX(),
                    (int) area.getY() - (int) textBounds.getHeight());
        }
        if (geometry instanceof GeoJsonPoint)
        {
            var location = ((GeoJsonPoint) geometry).location();
            var point = pointForLocation(location);
            graphics.setColor(color.asAwtColor());
            var x = (int) point.getX();
            var y = (int) point.getY();
            graphics.fillOval(x, y, width, width);
            graphics.setColor(color.darker().asAwtColor());
            var outlineWidth = Math.max(2, (width / 8) * 2 / 2);
            graphics.setStroke(new BasicStroke(outlineWidth + 2));
            graphics.drawOval(x, y, width, width);
        }
        if (geometry instanceof GeoJsonPolyline)
        {
            graphics.setColor(color.asAwtColor());
            var line = ((GeoJsonPolyline) geometry).polyline();
            var first = true;
            Path2D path = new Path2D.Float();
            for (var to : line.locationSequence())
            {
                var point = pointForLocation(to);
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
            var stroke = stroke();
            var shape = stroke.createStrokedShape(path);
            graphics.fill(shape);
            featureForShape.put(shape, feature);
            if (annotationBackground != null)
            {
                var index = 1;
                var font = font(graphics, "99", width);
                graphics.setFont(font);
                for (var location : line.locationSequence())
                {
                    var point = pointForLocation(location);
                    var centerX = (int) point.getX();
                    var centerY = (int) point.getY();
                    var x = centerX - width / 2;
                    var y = centerY - width / 2;
                    graphics.setColor(annotationText.asAwtColor());
                    graphics.fillOval(x - 1, y - 1, width + 2, width + 2);
                    graphics.setColor(annotationBackground.asAwtColor());
                    //noinspection SuspiciousNameCombination
                    graphics.fillOval(x, y, width, width);
                    if (font != null)
                    {
                        var string = "" + index;
                        var metrics = graphics.getFontMetrics();
                        var bounds = metrics.getStringBounds(string, graphics);
                        graphics.setColor(annotationText.asAwtColor());
                        graphics.drawString(string, centerX - (int) bounds.getWidth() / 2,
                                centerY + metrics.getAscent() / 2);
                    }
                    index++;
                }
            }
        }
    }

    private void drawHeatMap(Graphics2D graphics)
    {
        var size = viewWidth().times(0.02);
        var maximum = Count._0;
        Map<Rectangle, Count> counts = new HashMap<>();
        for (var cell : bounds().cells(size))
        {
            var count = Count.count(spatialIndex.intersecting(cell));
            maximum = maximum.maximum(count);
            counts.put(cell, count);
        }
        for (var cell : bounds().cells(size))
        {
            var count = counts.get(cell);
            graphics.setColor(Color.rgba(255, 0, 0,
                    Math.min(255, (int) (255 * ((double) count.asInt() / (double) maximum.asInt())))).asAwtColor());
            var topLeft = pointForLocation(cell.topLeft());
            var bottomRight = pointForLocation(cell.bottomRight());
            var x = (int) topLeft.getX();
            var y = (int) topLeft.getY();
            var width = (int) (bottomRight.getX() - x);
            var height = (int) (bottomRight.getY() - y);
            graphics.fillRect(x, y, width, height);
        }
    }

    private List<GeoJsonFeature> featuresForPoint(Point point)
    {
        List<GeoJsonFeature> features = new ArrayList<>();
        for (var entry : featureForShape.entrySet())
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

    private void selectFeature(GeoJsonFeature feature)
    {
        selectedFeature = feature;
        if (panel() != null)
        {
            panel().selectFeature(selectedFeature);
        }
        forceRepaint();
    }
}
