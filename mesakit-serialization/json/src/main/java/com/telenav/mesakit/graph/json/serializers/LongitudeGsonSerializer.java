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

package com.telenav.mesakit.graph.json.serializers;

import com.telenav.kivakit.utilities.json.gson.GsonSerializer;
import com.telenav.mesakit.map.geography.Longitude;

import java.lang.reflect.Type;

public class LongitudeGsonSerializer implements GsonSerializer<Longitude>
{
    @Override
    public Longitude deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException
    {
        return Longitude.degrees(context.deserialize(json, Double.class));
    }

    @Override
    public JsonElement serialize(final Longitude longitude, final Type typeOfSrc,
                                 final JsonSerializationContext context)
    {
        return context.serialize(longitude.asDegrees());
    }
}
