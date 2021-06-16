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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.telenav.kivakit.serialization.json.GsonSerializer;
import com.telenav.mesakit.map.geography.Latitude;

import java.lang.reflect.Type;

public class LatitudeGsonSerializer implements GsonSerializer<Latitude>
{
    @Override
    public Latitude deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException
    {
        return Latitude.degrees(context.deserialize(json, Double.class));
    }

    @Override
    public JsonElement serialize(final Latitude latitude, final Type typeOfSrc, final JsonSerializationContext context)
    {
        return context.serialize(latitude.asDegrees());
    }
}
