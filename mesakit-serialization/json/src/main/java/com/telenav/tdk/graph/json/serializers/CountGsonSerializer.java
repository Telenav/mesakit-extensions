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

package com.telenav.kivakit.graph.json.serializers;

import com.telenav.kivakit.kernel.scalars.counts.Count;
import com.telenav.kivakit.utilities.json.gson.PrimitiveGsonSerializer;

public class CountGsonSerializer extends PrimitiveGsonSerializer<Count, Integer>
{
    public CountGsonSerializer()
    {
        super(Integer.class);
    }

    @Override
    protected Count toObject(final Integer scalar)
    {
        return Count.of(scalar);
    }

    @Override
    protected Integer toPrimitive(final Count object)
    {
        return object.asInt();
    }
}
