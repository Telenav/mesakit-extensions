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

package com.telenav.kivakit.josm.plugins.geojson;

import com.telenav.kivakit.data.formats.library.map.identifiers.MapIdentifier;
import com.telenav.kivakit.map.utilities.geojson.GeoJsonFeature;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Desktop;
import java.net.URI;

class GeoJsonFeaturePopUpMenu extends JPopupMenu
{
    private static final long serialVersionUID = -680977285896859682L;

    public GeoJsonFeaturePopUpMenu(final GeoJsonFeature feature)
    {
        final var entityType = feature.properties().get("osmEntityType");
        final var type = entityType == null ? MapIdentifier.Type.WAY
                : MapIdentifier.Type.valueOf(entityType.toString().toUpperCase());
        final var typeName = type.name().toLowerCase();
        final var item = new JMenuItem("View OSM " + typeName + " in browser");
        item.addActionListener(event ->
        {
            final var identifier = osmIdentifier(feature);
            if (identifier != null)
            {
                try
                {
                    Desktop.getDesktop()
                            .browse(new URI("http://openstreetmap.org/" + typeName + "/" + identifier));
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        add(item);
    }

    private Long osmIdentifier(final GeoJsonFeature feature)
    {
        var identifier = (Double) feature.properties().get("osmIdentifier");
        if (identifier != null)
        {
            return identifier.longValue();
        }
        identifier = (Double) feature.properties().get("osmid");
        if (identifier != null)
        {
            return identifier.longValue();
        }
        return null;
    }
}
