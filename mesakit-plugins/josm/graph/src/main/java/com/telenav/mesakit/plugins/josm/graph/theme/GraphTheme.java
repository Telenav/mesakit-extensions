package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.kivakit.ui.desktop.theme.KivaKitStyles;
import com.telenav.kivakit.ui.desktop.theme.darcula.KivaKitDarculaTheme;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapBox;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapLabel;

import static com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.measurements.DrawingWidth.pixels;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.BLUE_RIDGE_MOUNTAINS;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.LIGHT_GRAY;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.VALENCIA;

public class GraphTheme extends KivaKitDarculaTheme
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

    public Style styleGraphActive()
    {
        return styleLabel()
                .withFillColor(LIGHT_GRAY)
                .withTextColor(BLUE_RIDGE_MOUNTAINS);
    }

    public Style styleGraphInactive()
    {
        return styleLabel()
                .withFillColor(LIGHT_GRAY)
                .withTextColor(BLUE_RIDGE_MOUNTAINS);
    }

    public Style styleLabelMap()
    {
        return KivaKitStyles.OCEAN_SURF;
    }

    public Style styleSelected()
    {
        return VALENCIA;
    }
}
