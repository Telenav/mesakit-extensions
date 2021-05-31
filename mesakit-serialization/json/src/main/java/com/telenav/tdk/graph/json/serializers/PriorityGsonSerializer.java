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

import com.telenav.kivakit.kernel.scalars.levels.Priority;
import com.telenav.kivakit.utilities.json.gson.PrimitiveGsonSerializer;

public class PriorityGsonSerializer extends PrimitiveGsonSerializer<Priority, Double>
{
    public PriorityGsonSerializer()
    {
        super(Double.class);
    }

    @Override
    protected Priority toObject(final Double scalar)
    {
        return Priority.forDouble(scalar);
    }

    @Override
    protected Double toPrimitive(final Priority object)
    {
        return object.value();
    }
}
