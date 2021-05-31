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

package com.telenav.kivakit.josm.plugins.library.tile.vector;

import com.telenav.kivakit.josm.plugins.library.tile.AbstractTile;
import com.telenav.kivakit.map.ui.swing.map.tiles.SlippyTile;

import java.util.List;

public class VectorTile extends AbstractTile
{
    private final SlippyTile tile;

    private final List<RoadFeature> features;

    VectorTile(final SlippyTile tile, final List<RoadFeature> features)
    {
        this.tile = tile;
        this.features = features;
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

    public List<RoadFeature> features()
    {
        return features;
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
        return "[VectorTile tile = " + tile + ", features = " + features.size() + "]";
    }
}
