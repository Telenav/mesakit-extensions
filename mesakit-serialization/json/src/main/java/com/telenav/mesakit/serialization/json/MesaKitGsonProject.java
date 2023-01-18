package com.telenav.mesakit.serialization.json;

import com.telenav.kivakit.conversion.core.time.local.IsoLocalDateTimeConverter;
import com.telenav.kivakit.conversion.core.time.utc.IsoDateTimeConverter;
import com.telenav.kivakit.conversion.core.value.BytesConverter;
import com.telenav.kivakit.core.project.Project;
import com.telenav.kivakit.core.project.ProjectTrait;
import com.telenav.kivakit.core.string.Separators;
import com.telenav.kivakit.core.time.LocalTime;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Bytes;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.identifier.Identifier;
import com.telenav.kivakit.serialization.gson.factory.GsonFactory;
import com.telenav.kivakit.serialization.gson.serializers.CountGsonSerializer;
import com.telenav.mesakit.core.BaseMesaKitProject;
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

/**
 * This class defines a KivaKit {@link Project}. It cannot be constructed with the new operator since it has a private
 * constructor. To access the singleton instance of this class, call {@link Project#resolveProject(Class)}, or use
 * {@link ProjectTrait#project(Class)}.
 *
 * @author jonathanl (shibo)
 */
public class MesaKitGsonProject extends BaseMesaKitProject
{
    @Override
    public void onInitialize()
    {
        // Add serializers and deserializers to whatever GsonFactory is registered
        require(GsonFactory.class)
            .addConvertingSerializer(Bytes.class, new BytesConverter(this))
            .addConvertingSerializer(Continent.class, new Continent.Converter<>(this, Continent.class))
            .addConvertingSerializer(Country.class, new Region.Converter<>(this, Country.class))
            .addConvertingSerializer(DataBuild.class, new DataBuild.Converter(this))
            .addConvertingSerializer(Distance.class, new Distance.Converter(this))
            .addConvertingSerializer(Time.class, new IsoDateTimeConverter(this))
            .addConvertingSerializer(LocalTime.class, new IsoLocalDateTimeConverter(this))
            .addConvertingSerializer(Location.class, new Location.DegreesConverter(this))
            .addConvertingSerializer(MapEdgeIdentifier.class, new MapEdgeIdentifier.Converter(this))
            .addConvertingSerializer(MetropolitanArea.class, new MetropolitanArea.Converter<>(this, MetropolitanArea.class))
            .addConvertingSerializer(NodeIdentifierList.class, new NodeIdentifierList.Converter(this, Separators.DEFAULT))
            .addConvertingSerializer(Rectangle.class, new Rectangle.Converter(this))
            .addConvertingSerializer(RoadName.class, new RoadNameConverter(this))
            .addConvertingSerializer(Speed.class, new Speed.Converter(this))
            .addConvertingSerializer(State.class, new State.Converter<>(this, State.class))
            .addConvertingSerializer(WayIdentifierList.class, new WayIdentifierList.Converter(this, Separators.DEFAULT))
            .addJsonSerializerDeserializer(Count.class, new CountGsonSerializer())
            .addJsonSerializerDeserializer(Identifier.class, new IdentifierGsonSerializer())
            .addJsonSerializerDeserializer(Latitude.class, new LatitudeGsonSerializer())
            .addJsonSerializerDeserializer(Longitude.class, new LongitudeGsonSerializer())
            .addJsonSerializerDeserializer(SpeedCategory.class, new SpeedCategoryGsonSerializer());
    }
}

