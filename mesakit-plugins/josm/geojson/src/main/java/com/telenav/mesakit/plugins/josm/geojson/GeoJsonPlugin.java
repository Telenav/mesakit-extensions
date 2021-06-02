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

package com.telenav.mesakit.plugins.josm.geojson;

import com.telenav.mesakit.plugins.josm.library.BaseJosmPlugin;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.plugins.PluginInformation;

import java.awt.event.KeyEvent;

/**
 * This plugin leverages JOSM to import files.
 */
public class GeoJsonPlugin extends BaseJosmPlugin
{
    protected int changeSetIdentifier;

    public GeoJsonPlugin(final PluginInformation info)
    {
        super(info, GeoJsonLayer.class);
        ExtensionFileFilter.addImporter(new GeoJsonFileImporter(this));
        OsmServerWriter.registerPostprocessor((primitives, monitor) ->
        {
            for (final IPrimitive primitive : primitives)
            {
                changeSetIdentifier = primitive.getChangesetId();
            }
        });
    }

    public int changeSetIdentifier()
    {
        return changeSetIdentifier;
    }

    @Override
    public String iconName()
    {
        return "geojson.png";
    }

    @Override
    public String name()
    {
        return "TDK GeoJson";
    }

    @Override
    public GeoJsonPanel panel()
    {
        return (GeoJsonPanel) super.panel();
    }

    @Override
    public GeoJsonLayer selectedLayer()
    {
        return (GeoJsonLayer) super.selectedLayer();
    }

    @Override
    protected GeoJsonLayer newLayer(final String name)
    {
        return new GeoJsonLayer(this, name);
    }

    @Override
    protected GeoJsonPanel newPanel()
    {
        return new GeoJsonPanel(this);
    }

    @Override
    protected int shortCutKey()
    {
        return KeyEvent.VK_J;
    }
}
