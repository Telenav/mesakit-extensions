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

package com.telenav.mesakit.plugins.josm.library.tile;

/**
 * Cache for tiles
 *
 * @author jonathanl (shibo)
 */
public abstract class AbstractTileCache<Request extends AbstractTileRequest, Tile extends AbstractTile>
{
    /**
     * Gets the tile for the given request
     *
     * @param request The request
     * @return The tile
     */
    public abstract Tile get(Request request);

    /**
     * Adds a tile to the cache for the given request
     *
     * @param request The request
     * @param tile The tile to add
     */
    public abstract void put(Request request, Tile tile);

    /**
     * Removes the tile for the given request
     *
     * @param request The request
     */
    public abstract void remove(Request request);
}
