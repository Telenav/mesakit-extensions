package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.core.value.level.Percent;

import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Color;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Fonts;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Style;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.kivakit.ui.desktop.theme.KivaKitStyles;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.map.road.model.RoadType;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapDot;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapPolyline;
import com.telenav.mesakit.map.ui.desktop.graphics.style.MapStroke;
import com.telenav.mesakit.map.ui.desktop.theme.MapColors;
import com.telenav.mesakit.map.ui.desktop.theme.MapStyles;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;

import java.awt.Font;

import static com.telenav.kivakit.ui.desktop.graphics.drawing.style.Color.TRANSPARENT;
import static com.telenav.kivakit.ui.desktop.theme.KivaKitStyles.MOJITO;
import static com.telenav.mesakit.map.measurements.geographic.Distance.meters;

public class EdgeTheme extends BaseTheme
{
    public MapPolyline edgeHighlighted()
    {
        return edgeUnselected()
                .withStyle(KivaKitStyles.NIGHT_SHIFT
                        .withFillStroke(MapStroke.stroke(meters(10)))
                        .withDrawStroke(MapStroke.stroke(meters(1))));
    }

    public MapPolyline edgeInactive()
    {
        return edgeUnselected()
                .withStyle(MapStyles.INACTIVE);
    }

    public MapPolyline edgeSelected()
    {
        return edgeUnselected()
                .withStyle(styleSelected())
                .withToArrowHead(MapDot.dot()
                        .withRadius(meters(2))
                        .withStyle(MapStyles.ARROWHEAD
                                .withFillColor(TRANSPARENT)
                                .withDrawStroke(MapStroke.stroke(meters(0.1)))));
    }

    public MapPolyline edgeUnselected()
    {
        return MapPolyline.polyline()
                .withStyle(MOJITO
                        .withFillStroke(MapStroke.stroke(meters(3)))
                        .withDrawStroke(MapStroke.stroke(meters(0.5))));
    }

    public MapPolyline fattenPolyline(MapPolyline line, Edge edge)
    {
        switch (edge.roadFunctionalClass())
        {
            case MAIN:
                return line.fattened(Percent.percent(200));

            case FIRST_CLASS:
                if (edge.roadType() == RoadType.HIGHWAY)
                {
                    return line.fattened(Percent.percent(100));
                }
                else
                {
                    return line.fattened(Percent.percent(50));
                }

            case SECOND_CLASS:
                return line.fattened(Percent.percent(30));

            case THIRD_CLASS:
                return line.fattened(Percent.percent(5));

            case UNKNOWN:
            case FOURTH_CLASS:
            default:
                return line;
        }
    }

    public MapPolyline polylineEdge(MapCanvas canvas, Selection.Type type, Edge edge)
    {
        switch (type)
        {
            case INACTIVE:
                return fattenPolyline(edgeInactive(), edge);

            case HIGHLIGHTED:
                return fattenPolyline(edgeHighlighted(), edge);

            case SELECTED:
                return fattenPolyline(edgeSelected(), edge);

            case UNSELECTED:
                break;

            default:
                throw new IllegalArgumentException();
        }

        var line = fattenPolyline(edgeUnselected(), edge);

        Color color;

        var zoomedIn = canvas.scale().isZoomedIn(MapScale.CITY);
        switch (edge.roadFunctionalClass())
        {
            case MAIN:
                color = zoomedIn ? MapColors.FREEWAY : MapColors.FREEWAY_ZOOMED_OUT;
                break;

            case FIRST_CLASS:
                if (edge.roadType() == RoadType.HIGHWAY)
                {
                    color = zoomedIn ? MapColors.HIGHWAY : MapColors.HIGHWAY_ZOOMED_OUT;
                }
                else
                {
                    color = zoomedIn ? MapColors.FIRST_CLASS : MapColors.FIRST_CLASS_ZOOMED_OUT;
                }
                break;

            case SECOND_CLASS:
                color = MapColors.SECOND_CLASS;
                break;

            case THIRD_CLASS:
                color = MapColors.THIRD_CLASS;
                break;

            case FOURTH_CLASS:
                color = MapColors.FOURTH_CLASS;
                break;

            case UNKNOWN:
            default:
                color = KivaKitColors.UNSPECIFIED;
                break;
        }
        return line.withFillColor(color);
    }

    public Style styleEdgeCallout()
    {
        return Style.create()
                .withTextFont(Fonts.fixedWidth(Font.BOLD, 12))
                .withFillColor(KivaKitColors.IRON.withAlpha(192))
                .withDrawColor(KivaKitColors.LIME)
                .withTextColor(KivaKitColors.LIME);
    }
}
