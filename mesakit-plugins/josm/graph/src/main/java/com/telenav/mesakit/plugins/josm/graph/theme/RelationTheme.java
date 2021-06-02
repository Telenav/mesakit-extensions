package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.mesakit.graph.Route;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapDot;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapPolyline;
import com.telenav.mesakit.map.ui.desktop.theme.MapStyles;

import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.OCEAN;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitColors.TRANSLUCENT_OCEAN;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.MANHATTAN;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.SUNNY;
import static com.telenav.mesakit.map.measurements.geographic.Distance.meters;
import static com.telenav.mesakit.map.ui.desktop.theme.MapStyles.ARROWHEAD;
import static com.telenav.mesakit.map.ui.desktop.theme.MapStyles.ERROR;

public class RelationTheme extends EdgeTheme
{
    public MapDot dotNoRoute()
    {
        return MapDot.dot(ERROR);
    }

    public MapDot dotViaNodeBad()
    {
        return MapDot.dot()
                .withStyle(ERROR);
    }

    public MapDot dotViaNodeSelected()
    {
        return MapDot.dot()
                .withStyle(MapStyles.SELECTED_ROUTE);
    }

    public MapDot dotViaNodeUnselected()
    {
        return MapDot.dot()
                .withStyle(MANHATTAN);
    }

    public MapPolyline polylineRestriction()
    {
        return MapPolyline.polyline()
                .withStyle(ERROR)
                .withToArrowHead(MapDot.dot(ARROWHEAD));
    }

    public MapPolyline polylineRoute()
    {
        return MapPolyline.polyline()
                .withStyle(stylePolylineRoute())
                .withFillStrokeWidth(meters(5))
                .withDrawStrokeWidth(meters(0.5))
                .withToArrowHead(MapDot.dot(ARROWHEAD));
    }

    public MapPolyline polylineRoute(final MapPolyline polyline, final Route route)
    {
        return fattenPolyline(polyline, route.asEdgeSet().mostImportant()).withPolyline(route.polyline());
    }

    public MapPolyline polylineRouteSelected()
    {
        return MapPolyline.polyline()
                .withStyle(styleSelectedRoute())
                .withFillStrokeWidth(meters(5))
                .withDrawStrokeWidth(meters(0.5))
                .withToArrowHead(MapDot.dot(ARROWHEAD));
    }

    public Style stylePolylineRoute()
    {
        return Style.create()
                .withFillColor(TRANSLUCENT_OCEAN)
                .withDrawColor(OCEAN);
    }

    public Style styleSelectedRoute()
    {
        return SUNNY;
    }
}
