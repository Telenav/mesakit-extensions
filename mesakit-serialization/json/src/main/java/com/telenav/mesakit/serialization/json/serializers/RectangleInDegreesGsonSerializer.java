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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.telenav.kivakit.serialization.json.GsonSerializer;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;

import java.lang.reflect.Type;

public class RectangleInDegreesGsonSerializer implements GsonSerializer<Rectangle>
{
    @Override
    public Rectangle deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException
    {
        var object = json.getAsJsonObject();
        var bottomLeft = (Location) context.deserialize(object.get("bottomLeft"), Location.class);
        var topRight = (Location) context.deserialize(object.get("topRight"), Location.class);
        return Rectangle.fromLocations(bottomLeft, topRight);
    }

    @Override
    public JsonElement serialize(final Rectangle rectangle, final Type typeOfSrc,
                                 final JsonSerializationContext context)
    {
        var bottomLeft = context.serialize(rectangle.bottomLeft());
        var topRight = context.serialize(rectangle.topRight());
        var object = new JsonObject();
        object.add("bottomLeft", bottomLeft);
        object.add("topRight", topRight);
        return context.serialize(object);
    }
}
