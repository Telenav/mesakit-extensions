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

package com.telenav.mesakit.serialization.json;

import com.google.gson.GsonBuilder;
import com.telenav.kivakit.kernel.language.strings.formatting.Separators;
import com.telenav.kivakit.kernel.language.time.LocalTime;
import com.telenav.kivakit.kernel.language.values.count.Bytes;
import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.kernel.language.values.identifier.Identifier;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;
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

public class GraphCoreGsonFactory extends GsonFactory
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    @Override
    protected GsonBuilder addSerializers(final GsonBuilder builder)
    {
        addSerializer(builder, Identifier.class, new IdentifierGsonSerializer());
        addSerializer(builder, LocalTime.class, serializer(LocalTime.ISO_UTC_TIMESTAMP_CONVERTER));
        addSerializer(builder, Count.class, new CountGsonSerializer());
        addSerializer(builder, Bytes.class, serializer(new Bytes.Converter(LOGGER)));
        addSerializer(builder, Continent.class, serializer(new Continent.Converter<Continent>(LOGGER)));
        addSerializer(builder, Country.class, serializer(new Region.Converter<Country>(LOGGER)));
        addSerializer(builder, State.class, serializer(new State.Converter<State>(LOGGER)));
        addSerializer(builder, MetropolitanArea.class, serializer(new MetropolitanArea.Converter<MetropolitanArea>(LOGGER)));
        addSerializer(builder, DataBuild.class, serializer(new DataBuild.Converter(LOGGER)));
        addSerializer(builder, Location.class, serializer(new Location.DegreesConverter(LOGGER)));
        addSerializer(builder, Latitude.class, new LatitudeGsonSerializer());
        addSerializer(builder, Longitude.class, new LongitudeGsonSerializer());
        addSerializer(builder, Rectangle.class, serializer(new Rectangle.Converter(LOGGER)));
        addSerializer(builder, RoadName.class, serializer(new RoadNameConverter(LOGGER)));
        addSerializer(builder, MapEdgeIdentifier.class, serializer(new MapEdgeIdentifier.Converter(LOGGER)));
        addSerializer(builder, Speed.class, serializer(new Speed.Converter(LOGGER)));
        addSerializer(builder, SpeedCategory.class, new SpeedCategoryGsonSerializer());
        addSerializer(builder, Distance.class, serializer(new Distance.Converter(LOGGER)));
        addSerializer(builder, WayIdentifierList.class, serializer(new WayIdentifierList.Converter(LOGGER, Separators.DEFAULT)));
        addSerializer(builder, NodeIdentifierList.class, serializer(new NodeIdentifierList.Converter(LOGGER, Separators.DEFAULT)));
        return builder;
    }
}
