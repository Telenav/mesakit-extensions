package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.kivakit.ui.desktop.theme.KivaKitStyles;
import com.telenav.kivakit.ui.desktop.theme.darcula.KivaKitDarculaTheme;

import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.BLUE_RIDGE_MOUNTAINS;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.LIGHT_GRAY;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.MANHATTAN;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.SEATTLE;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.VALENCIA;

public class BaseTheme extends KivaKitDarculaTheme
{
    public Style styleDisabled()
    {
        return SEATTLE;
    }

    public Style styleError()
    {
        return MANHATTAN;
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
