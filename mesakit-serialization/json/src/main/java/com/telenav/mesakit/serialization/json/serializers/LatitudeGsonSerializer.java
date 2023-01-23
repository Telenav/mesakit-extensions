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

package com.telenav.mesakit.serialization.json.serializers;

import com.telenav.kivakit.serialization.gson.serializers.BaseGsonSerializer;
import com.telenav.mesakit.map.geography.Latitude;

public class LatitudeGsonSerializer extends BaseGsonSerializer<Latitude, Double>
{
    public LatitudeGsonSerializer()
    {
        super(Latitude.class, Double.class);
    }

    @Override
    protected Latitude onDeserialize(Double serialized)
    {
        return Latitude.degrees(serialized);
    }

    @Override
    protected Double onSerialize(Latitude value)
    {
        return value.asDegrees();
    }
}
