////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License".
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

package com.telenav.mesakit.serialization.json;

import com.telenav.kivakit.core.language.strings.formatting.Separators;
import com.telenav.kivakit.language.time.LocalTime;
import com.telenav.kivakit.conversion.core.time.UtcDateTimeConverter;
import com.telenav.kivakit.language.count.Bytes;
import com.telenav.kivakit.language.count.Count;
import com.telenav.kivakit.core.language.values.identifier.Identifier;
import com.telenav.kivakit.serialization.json.BaseGsonFactorySource;
import com.telenav.kivakit.serialization.json.DefaultGsonFactory;
import com.telenav.kivakit.serialization.json.GsonFactory;
import com.telenav.kivakit.serialization.json.serializers.CountGsonSerializer;
import com.telenav.mesakit.graph.identifiers.collections.NodeIdentifierList;
import com.telenav.mesakit.graph.identifiers.collections.WayIdentifierList;
import com.telenav.mesakit.graph.map.MapEdgeIdentifier;
import com.telenav.mesakit.graph.metadata.DataBuild;
import com.telenav.mesakit.map.geography.Latitude;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.Longitude;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.measurements.motion.Speed;
import com.telenav.mesakit.map.region.Region;
import com.telenav.mesakit.map.region.regions.Continent;
import com.telenav.mesakit.map.region.regions.Country;
import com.telenav.mesakit.map.region.regions.MetropolitanArea;
import com.telenav.mesakit.map.region.regions.State;
import com.telenav.mesakit.map.road.model.RoadName;
import com.telenav.mesakit.map.road.model.SpeedCategory;
import com.telenav.mesakit.map.road.model.converters.RoadNameConverter;
import com.telenav.mesakit.serialization.json.serializers.IdentifierGsonSerializer;
import com.telenav.mesakit.serialization.json.serializers.LatitudeGsonSerializer;
import com.telenav.mesakit.serialization.json.serializers.LongitudeGsonSerializer;
import com.telenav.mesakit.serialization.json.serializers.SpeedCategoryGsonSerializer;

public class MesaKitGsonFactorySource extends BaseGsonFactorySource
{
    @Override
    public GsonFactory gsonFactory()
    {
        return new DefaultGsonFactory(this)
                .withSerialization(Identifier.class, new IdentifierGsonSerializer())
                .withSerialization(LocalTime.class, serializer(new UtcDateTimeConverter(this)))
                .withSerialization(Count.class, new CountGsonSerializer())
                .withSerialization(Bytes.class, serializer(new Bytes.Converter(this)))
                .withSerialization(Continent.class, serializer(new Continent.Converter<Continent>(this)))
                .withSerialization(Country.class, serializer(new Region.Converter<Country>(this)))
                .withSerialization(State.class, serializer(new State.Converter<State>(this)))
                .withSerialization(MetropolitanArea.class, serializer(new MetropolitanArea.Converter<MetropolitanArea>(this)))
                .withSerialization(DataBuild.class, serializer(new DataBuild.Converter(this)))
                .withSerialization(Location.class, serializer(new Location.DegreesConverter(this)))
                .withSerialization(Latitude.class, new LatitudeGsonSerializer())
                .withSerialization(Longitude.class, new LongitudeGsonSerializer())
                .withSerialization(Rectangle.class, serializer(new Rectangle.Converter(this)))
                .withSerialization(RoadName.class, serializer(new RoadNameConverter(this)))
                .withSerialization(MapEdgeIdentifier.class, serializer(new MapEdgeIdentifier.Converter(this)))
                .withSerialization(Speed.class, serializer(new Speed.Converter(this)))
                .withSerialization(SpeedCategory.class, new SpeedCategoryGsonSerializer())
                .withSerialization(Distance.class, serializer(new Distance.Converter(this)))
                .withSerialization(WayIdentifierList.class, serializer(new WayIdentifierList.Converter(this, Separators.DEFAULT)))
                .withSerialization(NodeIdentifierList.class, serializer(new NodeIdentifierList.Converter(this, Separators.DEFAULT)));
    }
}

