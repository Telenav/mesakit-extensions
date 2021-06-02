package com.telenav.mesakit.plugins.josm.graph.theme;

import com.telenav.kivakit.kernel.language.values.level.Percent;
import com.telenav.kivakit.ui.desktop.graphics.drawing.drawables.Line;
import com.telenav.kivakit.ui.desktop.graphics.drawing.style.Color;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.map.road.model.RoadType;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.map.ui.desktop.graphics.drawables.MapPolyline;
import com.telenav.mesakit.map.ui.desktop.theme.MapColors;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;

import static com.telenav.mesakit.plugins.josm.graph.model.Selection.Type.INACTIVE;

public class EdgeTheme extends GraphTheme
{
    public static Line fattenedAndFilled(final MapCanvas canvas, final Selection.Type type, final Edge edge)
    {
        switch (type)
        {
            case INACTIVE:
                return fattenPolyline(INACTIVE, edge);

            case HIGHLIGHTED:
                return fattenPolyline(HIGHLIGHTED, edge);

            case SELECTED:
                return fattenPolyline(SELECTED, edge);

            case UNSELECTED:
                break;

            default:
                throw new IllegalArgumentException();
        }

        final var line = fattenPolyline(NORMAL, edge);

        final Color color;

        final var zoomedIn = canvas.scale().isZoomedIn(MapScale.CITY);
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
        return line.withFill(color);
    }

    public MapPolyline fattenPolyline(final MapPolyline line, final Edge edge)
    {
        switch (edge.roadFunctionalClass())
        {
            case MAIN:
                return line.fattened(Percent.of(200));

            case FIRST_CLASS:
                if (edge.roadType() == RoadType.HIGHWAY)
                {
                    return line.fattened(Percent.of(100));
                }
                else
                {
                    return line.fattened(Percent.of(50));
                }

            case SECOND_CLASS:
                return line.fattened(Percent.of(30));

            case THIRD_CLASS:
                return line.fattened(Percent.of(5));

            case UNKNOWN:
            case FOURTH_CLASS:
            default:
                return line;
        }
    }
}
