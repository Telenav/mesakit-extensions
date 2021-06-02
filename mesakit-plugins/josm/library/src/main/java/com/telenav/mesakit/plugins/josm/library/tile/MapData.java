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

public enum MapData
{
    TOMTOM_NA_13M9("TomTom", "TomTom", "TT_NA_13M9"),
    OSM("OSM", "OSM", "OSM"),
    NT_NA_13Q2("Navteq", "Navteq", "NT_NA_13Q2");

    private final String displayName;

    private final String vectorName;

    private final String trafficName;

    MapData(final String displayName, final String vectorName, final String trafficName)
    {
        this.displayName = displayName;
        this.vectorName = vectorName;
        this.trafficName = trafficName;
    }

    public String displayName()
    {
        return this.displayName;
    }

    public String getVectorName()
    {
        return this.vectorName;
    }

    public String trafficName()
    {
        return this.trafficName;
    }
}
