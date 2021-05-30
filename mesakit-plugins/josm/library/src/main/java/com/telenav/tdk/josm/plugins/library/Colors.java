////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.telenav.tdk.josm.plugins.library;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Colors
{
    private static final Color VERY_LIGHT_GRAY = new Color(245, 245, 245);

    private static final Color TRANSPARENT_BLUE = new Color(0, 0, 255, 150);

    public static final Color LOADING_TEXT = TRANSPARENT_BLUE;

    public static final Color TEXT_BACKGROUND = VERY_LIGHT_GRAY;

    public static final Color TEXT = Color.BLUE;

    public static final Color TEXT_BACKGROUND_OUTLINE = Color.LIGHT_GRAY;

    public static final Color GRID = TRANSPARENT_BLUE;

    public static final Color FLOW_NOT_AVAILABLE = VERY_LIGHT_GRAY;

    public static final Color FLOW_CLOSED = Color.BLACK;

    public static final Color FLOW_SLOW = Color.RED;

    public static final Color FLOW_MODERATE = Color.YELLOW;

    public static final Color FLOW_FAST = Color.GREEN;

    public static final Color FLOW_OUTLINE = Color.BLUE;

    public static final Color FLOW_OUTLINE_SELECTED = Color.BLACK;

    public static final Color PROBE_MARKER = Color.BLUE;

    public static final Color PROBE_MARKER_SELECTED = Color.ORANGE;

    private static final Map<String, Color> colorForSeverity = new HashMap<>();

    static
    {
        colorForSeverity.put("CRITICAL", Color.RED);
        colorForSeverity.put("BLOCKER", Color.RED);
        colorForSeverity.put("MAJOR", Color.ORANGE);
        colorForSeverity.put("MINOR", Color.YELLOW);
        colorForSeverity.put("LOW_IMPACT", Color.GREEN);
    }

    public static Color colorForIncidentSeverity(final String severity)
    {
        final var color = colorForSeverity.get(severity);
        if (color != null)
        {
            return color;
        }
        return Color.LIGHT_GRAY;
    }
}
