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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.telenav.kivakit.serialization.gson.serializers.BaseGsonElementSerializer;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;

public class RectangleInDegreesGsonSerializer extends BaseGsonElementSerializer<Rectangle>
{
    public RectangleInDegreesGsonSerializer()
    {
        super(Rectangle.class);
    }

    @Override
    protected JsonElement toJson(Rectangle value)
    {
        var bottomLeft = serialize(value.bottomLeft());
        var topRight = serialize(value.topRight());

        var object = new JsonObject();

        object.add("bottomLeft", bottomLeft);
        object.add("topRight", topRight);

        return object;
    }

    @Override
    protected Rectangle toValue(JsonElement json)
    {
        var bottomLeft = (Location) deserialize(json, "bottomLeft", Location.class);
        var topRight = (Location) deserialize(json, "topRight", Location.class);

        return Rectangle.fromLocations(bottomLeft, topRight);
    }
}
