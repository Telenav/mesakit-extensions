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

package com.telenav.mesakit.plugins.josm.library.tile.vector;

import com.telenav.kivakit.collections.map.string.VariableMap;
import com.telenav.kivakit.messaging.logging.Logger;
import com.telenav.kivakit.messaging.logging.LoggerFactory;
import com.telenav.kivakit.network.core.Host;
import com.telenav.kivakit.network.core.QueryParameters;
import com.telenav.kivakit.network.http.HttpNetworkLocation;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.polyline.Polyline;
import com.telenav.mesakit.map.geography.shape.polyline.PolylineBuilder;
import com.telenav.mesakit.plugins.josm.library.tile.AbstractTileDownloader;
import com.telenav.mesakit.plugins.josm.library.tile.SlippyTileCache;

public class VectorTileDownloader extends AbstractTileDownloader<VectorTileRequest, VectorTile>
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    public enum Version
    {
        V3,
        V4
    }

    private final HttpNetworkLocation location;

    private final Version version;

    public VectorTileDownloader(HttpNetworkLocation location, Version version)
    {
        super(new SlippyTileCache<>());
        this.location = location;
        this.version = version;
    }

    @Override
    protected VectorTile onDownload(VectorTileRequest request)
    {
        var tile = request.tile();
        var server = location + request.mapData().getVectorName()
                + (version == Version.V3 ? "/NA/current/version.json" : "");
        var variables = new VariableMap<String>();
        variables.put("zoom", "" + tile.getZoomLevel().level());
        variables.put("x", "" + tile.x());
        variables.put("y", "" + tile.y());
        variables.put("baseurl", server);
        var host = Host.parse(this, "hqd-vectortilefs.telenav.com");
        var location = new HttpNetworkLocation(host.http()
                .path(this, "/tools/bin/vm_decoder" + (version == Version.V4 ? "_v4" : "") + ".py"))
                .withQueryParameters(new QueryParameters(variables));
        var vectorTile = new VectorTile(tile);
        LOGGER.information("Downloaded $ ($)", vectorTile, tile.mapArea());
        return vectorTile;
    }

    // (37.63799,-122.40458) (37.6343,-122.40299) (37.62697,-122.40183)
    private Polyline polyline(String string)
    {
        var builder = new PolylineBuilder();
        var converter = new Location.DegreesConverter(LOGGER);
        for (var location : string.split(" "))
        {
            var value = location.substring(1, location.length() - 1);
            builder.add(converter.convert(value));
        }
        return builder.build();
    }
}
