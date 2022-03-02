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

import com.telenav.kivakit.core.language.Hash;
import com.telenav.mesakit.map.ui.desktop.tiles.SlippyTile;
import com.telenav.mesakit.plugins.josm.library.tile.AbstractTileRequest;
import com.telenav.mesakit.plugins.josm.library.tile.MapData;

public class VectorTileRequest extends AbstractTileRequest
{
    private MapData mapData;

    private SlippyTile tile;

    public VectorTileRequest()
    {
    }

    private VectorTileRequest(VectorTileRequest that)
    {
        mapData = that.mapData;
        tile = that.tile;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object instanceof VectorTileRequest)
        {
            var that = (VectorTileRequest) object;
            return mapData.equals(that.mapData) && tile.equals(that.tile);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Hash.many(mapData, tile);
    }

    public MapData mapData()
    {
        return mapData;
    }

    public SlippyTile tile()
    {
        return tile;
    }

    public VectorTileRequest withForceUpdate(boolean forceUpdate)
    {
        var request = new VectorTileRequest(this);
        request.forceUpdate(forceUpdate);
        return request;
    }

    public VectorTileRequest withMapData(MapData mapData)
    {
        var request = new VectorTileRequest(this);
        request.mapData = mapData;
        return request;
    }

    public VectorTileRequest withTile(SlippyTile tile)
    {
        var request = new VectorTileRequest(this);
        request.tile = tile;
        return request;
    }
}
