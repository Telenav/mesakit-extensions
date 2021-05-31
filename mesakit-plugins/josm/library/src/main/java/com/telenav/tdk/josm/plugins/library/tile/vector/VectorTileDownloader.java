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

package com.telenav.tdk.josm.plugins.library.tile.vector;

import com.telenav.kivakit.josm.plugins.library.tile.*;
import com.telenav.kivakit.kernel.language.collections.map.VariableMap;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.polyline.*;
import com.telenav.kivakit.network.http.HttpNetworkLocation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public VectorTileDownloader(final HttpNetworkLocation location, final Version version)
    {
        super(new SlippyTileCache<>());
        this.location = location;
        this.version = version;
    }

    @Override
    protected VectorTile onDownload(final VectorTileRequest request)
    {
        final var tile = request.tile();
        final var server = location + request.mapData().getVectorName()
                + (version == Version.V3 ? "/NA/current/version.json" : "");
        final var variables = new VariableMap<String>();
        variables.put("zoom", "" + tile.getZoomLevel().level());
        variables.put("x", "" + tile.getX());
        variables.put("y", "" + tile.getY());
        variables.put("baseurl", server);
        final var host = new Host("hqd-vectortilefs.telenav.com");
        final var location = new HttpNetworkLocation(host.http()
                .path("/tools/bin/vm_decoder" + (version == Version.V4 ? "_v4" : "") + ".py"))
                .withQueryParameters(new QueryParameters(variables));
        final var vectorTile = new VectorTile(tile, roadFeatures(location.get().reader().lines().iterator()));
        LOGGER.information("Downloaded $ ($)", vectorTile, tile.bounds());
        return vectorTile;
    }

    // (37.63799,-122.40458) (37.6343,-122.40299) (37.62697,-122.40183)
    private Polyline polyline(final String string)
    {
        final var builder = new PolylineBuilder();
        final var converter = new Location.DegreesConverter(LOGGER);
        for (final var location : string.split(" "))
        {
            final var value = location.substring(1, location.length() - 1);
            builder.add(converter.convert(value));
        }
        return builder.build();
    }

    // -RF|RT_HIGHWAY|4||US-101 N|BOTHWAY|0|-1
    // +polyline|(37.63799,-122.40458) (37.6343,-122.40299) (37.62697,-122.40183)
    // +tfc_left|105P04173|
    private List<RoadFeature> roadFeatures(final Iterator<String> lines)
    {
        final List<RoadFeature> features = new ArrayList<>();
        String next;
        String pushback = null;
        while (lines.hasNext())
        {
            if (pushback == null)
            {
                next = lines.next();
            }
            else
            {
                next = pushback;
                pushback = null;
            }
            if (next.startsWith("-RF"))
            {
                next = lines.next();
                if (next.trim().startsWith("+polyline"))
                {
                    final var line = polyline(next.split("\\|")[1]);
                    var feature = new RoadFeature(line);
                    while (lines.hasNext())
                    {
                        next = lines.next();
                        if (next.trim().startsWith("+tfc"))
                        {
                            feature.trafficIdentifier(new RoadSectionCodeInferencer().infer(next.split("\\|")[1]));
                            features.add(feature);
                            feature = new RoadFeature(line);
                        }
                        else
                        {
                            pushback = next;
                            break;
                        }
                    }
                }
                else
                {
                    pushback = next;
                }
            }
        }
        return features;
    }
}
