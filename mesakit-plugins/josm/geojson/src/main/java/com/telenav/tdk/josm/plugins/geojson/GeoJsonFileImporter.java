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

import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.map.utilities.geojson.GeoJsonDocument;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

public class GeoJsonFileImporter extends FileImporter
{
    private final GeoJsonPlugin plugin;

    public GeoJsonFileImporter(final GeoJsonPlugin plugin)
    {
        super(new ExtensionFileFilter("geojson", "geojson", "GeoJson Files (*.geojson)"));
        this.plugin = plugin;
    }

    @Override
    public void importData(final java.io.File file, final ProgressMonitor progressMonitor)
    {
        try
        {
            final var input = new File(file);
            final var document = GeoJsonDocument.forJson(input.reader().string());
            if (document != null)
            {
                final var layer = (GeoJsonLayer) plugin
                        .createLayer(plugin.name() + " (" + file.getName() + ")");
                layer.setDocument(document);
                layer.setFile(input);
                layer.add();
                plugin.panel().showPanel();
                plugin.zoomTo(document.bounds());
            }
        }
        catch (final Throwable e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(plugin.mapView(), "Unable to open " + file + ":\n\n" + e.toString());
        }
    }

    @Override
    public boolean isBatchImporter()
    {
        return false;
    }
}
