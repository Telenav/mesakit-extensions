package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.mesakit.map.ui.desktop.graphics.style.MapStroke;
import com.telenav.mesakit.map.ui.desktop.theme.MapStyles;

import static com.telenav.mesakit.map.measurements.geographic.Distance.meters;

public class PolylineTheme extends GraphTheme
{
    public Style styleUnselected()
    {
        return MapStyles.NORMAL
                .withFillStroke(MapStroke.stroke(meters(8.0)))
                .withDrawStroke(MapStroke.stroke(meters(1.0)));
    }

    public Style styleZoomedIn()
    {
        return MapStyles.NORMAL
                .withFillStroke(MapStroke.stroke(meters(8.0)))
                .withDrawStroke(MapStroke.stroke(meters(1.0)));
    }

    public Style styleZoomedOut()
    {
        return MapStyles.NORMAL
                .withFillStroke(MapStroke.stroke(meters(3.0)))
                .withDrawStroke(MapStroke.stroke(meters(1.0)));
    }
}
