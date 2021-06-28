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

import com.telenav.kivakit.serialization.json.PrimitiveGsonSerializer;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;

/**
 * Serializes {@link Rectangle} objects to and from JSON
 *
 * @author jonathanl (shibo)
 */
@LexakaiJavadoc(complete = true)
public class RectangleGsonSerializer extends PrimitiveGsonSerializer<Rectangle, String>
{
    public RectangleGsonSerializer()
    {
        super(String.class);
    }

    @Override
    protected Rectangle toObject(final String rectangle)
    {
        return Rectangle.parse(rectangle);
    }

    @Override
    protected String toPrimitive(final Rectangle bounds)
    {
        return bounds.toString();
    }
}
