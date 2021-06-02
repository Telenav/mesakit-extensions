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

import com.telenav.mesakit.map.ui.desktop.tiles.SlippyTile;
import com.telenav.mesakit.plugins.josm.library.tile.AbstractTile;

public class VectorTile extends AbstractTile
{
    private final SlippyTile tile;

    VectorTile(final SlippyTile tile)
    {
        this.tile = tile;
    }

    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof VectorTile)
        {
            final var that = (VectorTile) object;
            return tile.equals(that.tile);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return tile.hashCode();
    }

    @Override
    public boolean isExpired()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "[VectorTile tile = " + tile + "]";
    }
}
