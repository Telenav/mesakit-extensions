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

package com.telenav.tdk.josm.plugins.library.tile;

import com.telenav.kivakit.collections.map.CacheMap;
import com.telenav.kivakit.kernel.scalars.counts.Maximum;

import java.util.Map;

public class SlippyTileCache<Request extends AbstractTileRequest, Tile extends AbstractTile>
        extends AbstractTileCache<Request, Tile>
{
    private final Map<Request, Tile> cache = new CacheMap<>(Maximum._100);

    @Override
    public synchronized Tile get(final Request request)
    {
        return cache.get(request);
    }

    @Override
    public synchronized void put(final Request request, final Tile tile)
    {
        cache.put(request, tile);
    }

    @Override
    public synchronized void remove(final Request request)
    {
        cache.remove(request);
    }
}
