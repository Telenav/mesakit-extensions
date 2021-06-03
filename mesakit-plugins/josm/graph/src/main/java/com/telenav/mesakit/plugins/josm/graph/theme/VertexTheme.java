package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Fonts;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapDot;
import com.telenav.mesakit.map.ui.desktop.graphics.style.MapStroke;

import java.awt.Font;

import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.IRON;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.ABSINTHE;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.LIFEBUOY;
import static com.telenav.mesakit.map.measurements.geographic.Distance.meters;

public class VertexTheme extends BaseTheme
{
    public MapDot dotVertexDisabled()
    {
        return dotVertexUnselected().withStyle(styleDisabled());
    }

    public MapDot dotVertexInvalid()
    {
        return dotVertexUnselected().withStyle(styleError());
    }

    public MapDot dotVertexSelected()
    {
        return dotVertexUnselected().withStyle(styleSelected());
    }

    public MapDot dotVertexUnselected()
    {
        return MapDot.dot()
                .withRadius(meters(5))
                .withStyle(LIFEBUOY
                        .withFillStroke(MapStroke.stroke(meters(0.5))));
    }

    public Style styleVertexCallout()
    {
        return ABSINTHE
                .withTextFont(Fonts.fixedWidth(Font.BOLD, 12))
                .withFillColor(IRON.withAlpha(192));
    }
}
