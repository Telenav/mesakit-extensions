package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapBox;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapLabel;

import static com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.measurements.DrawingWidth.pixels;

public class GraphTheme extends BaseTheme
{
    public MapBox boxGraphBounds(final Graph graph, final boolean active)
    {
        return MapBox.box(active ? styleGraphActive() : styleGraphInactive())
                .withLabelText(graph.name())
                .withRectangle(graph.bounds())
                .withDrawStrokeWidth(pixels(3));
    }

    public MapLabel labelMap(final String text)
    {
        return MapLabel.label(styleLabelMap())
                .withLabelText(text)
                .withDrawStrokeWidth(pixels(2));
    }
}
