package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.kernel.language.values.level.Percent;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapDot;

public class PlaceTheme extends BaseTheme
{
    public MapDot dotPlace(Count population)
    {
        var base = Distance.meters(8);
        var radius = base.add(base.times(Math.abs(Math.log10(population.asInt()))));
        return MapDot.dot()
                .withStyle(styleUnselected())
                .withRadius(radius);
    }

    public MapDot dotPlaceSelected(Count population)
    {
        return dotPlace(population)
                .withStyle(styleSelected())
                .scaledBy(Percent.of(10));
    }

    public Style styleUnselected()
    {
        return styleLabel()
                .withFillColor(KivaKitColors.LIGHT_GRAY)
                .withTextColor(KivaKitColors.BLUE_RIDGE_MOUNTAINS);
    }
}
