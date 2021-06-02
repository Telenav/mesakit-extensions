package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.kivakit.ui.desktop.theme.darcula.KivaKitDarculaTheme;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapDot;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapLabel;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapLine;

import static com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.measurements.DrawingWidth.pixels;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.BLUE_RIDGE_MOUNTAINS;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.LIGHT_GRAY;
import static com.telenav.mesakit.map.measurements.geographic.Distance.meters;

public class AnnotationTheme extends KivaKitDarculaTheme
{
    public MapDot dotAnnotationLocation()
    {
        return MapDot.dot()
                .withStyle(styleAnnotationLocation())
                .withRadius(meters(5));
    }

    public MapLabel labelAnnotation(final String text)
    {
        return MapLabel.label(styleAnnotationLabel())
                .withLabelText(text);
    }

    public MapLine lineCallout()
    {
        return MapLine.line(styleLineCallout())
                .withDrawStrokeWidth(pixels(2));
    }

    public Style styleAnnotationLabel()
    {
        return styleLabel()
                .withFillColor(LIGHT_GRAY)
                .withTextColor(BLUE_RIDGE_MOUNTAINS);
    }

    public Style styleEdgeAnnotationCallout()
    {
        return styleLabel()
                .withFillColor(LIGHT_GRAY)
                .withTextColor(BLUE_RIDGE_MOUNTAINS);
    }

    public Style styleLineCallout()
    {
        return Style.create()
                .withFillColor(LIGHT_GRAY)
                .withTextColor(BLUE_RIDGE_MOUNTAINS);
    }

    public Style styleVertexAnnotationCallout()
    {
        return styleLabel()
                .withFillColor(LIGHT_GRAY)
                .withTextColor(BLUE_RIDGE_MOUNTAINS);
    }

    private Style styleAnnotationLocation()
    {
        return styleLabel()
                .withFillColor(LIGHT_GRAY)
                .withTextColor(BLUE_RIDGE_MOUNTAINS);
    }
}
