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

import com.telenav.kivakit.core.messaging.Listener;
import com.telenav.kivakit.filesystem.File;
import com.telenav.mesakit.map.utilities.geojson.GeoJsonDocument;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

import javax.swing.JOptionPane;

public class GeoJsonFileImporter extends FileImporter
{
    private final GeoJsonPlugin plugin;

    public GeoJsonFileImporter(GeoJsonPlugin plugin)
    {
        super(new ExtensionFileFilter("geojson", "geojson", "GeoJson Files (*.geojson)"));
        this.plugin = plugin;
    }

    @Override
    public void importData(java.io.File file, ProgressMonitor progressMonitor)
    {
        try
        {
            var input = File.file(Listener.consoleListener(), file);
            var document = GeoJsonDocument.forJson(input.reader().asString());
            if (document != null)
            {
                var layer = (GeoJsonLayer) plugin
                        .createLayer(plugin.name() + " (" + file.getName() + ")");
                layer.setDocument(document);
                layer.setFile(input);
                layer.add();
                plugin.panel().showPanel();
                plugin.zoomTo(document.bounds());
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(plugin.mapView(), "Unable to open " + file + ":\n\n" + e);
        }
    }

    @Override
    public boolean isBatchImporter()
    {
        return false;
    }
}
