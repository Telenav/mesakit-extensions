package com.telenav.mesakit.serialization.json;

import com.telenav.kivakit.conversion.core.time.local.IsoLocalDateTimeConverter;
import com.telenav.kivakit.conversion.core.time.utc.IsoDateTimeConverter;
import com.telenav.kivakit.conversion.core.value.BytesConverter;
import com.telenav.kivakit.core.project.Project;
import com.telenav.kivakit.core.project.ProjectTrait;
import com.telenav.kivakit.core.string.Separators;
import com.telenav.kivakit.serialization.gson.GsonFactory;
import com.telenav.kivakit.serialization.gson.serializers.value.CountGsonSerializer;
import com.telenav.mesakit.core.BaseMesaKitProject;
import com.telenav.mesakit.graph.identifiers.collections.NodeIdentifierList;
import com.telenav.mesakit.graph.identifiers.collections.WayIdentifierList;
import com.telenav.mesakit.graph.map.MapEdgeIdentifier;
import com.telenav.mesakit.graph.metadata.DataBuild;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.measurements.motion.Speed;
import com.telenav.mesakit.map.region.Region;
import com.telenav.mesakit.map.region.regions.Continent;
import com.telenav.mesakit.map.region.regions.Country;
import com.telenav.mesakit.map.region.regions.MetropolitanArea;
import com.telenav.mesakit.map.region.regions.State;
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
            .addSerializer(new BytesConverter())
            .addSerializer(new Continent.Converter<>(this, Continent.class))
            .addSerializer(new Region.Converter<>(this, Country.class))
            .addSerializer(new DataBuild.Converter(this))
            .addSerializer(new Distance.Converter(this))
            .addSerializer(new IsoDateTimeConverter(this))
            .addSerializer(new IsoLocalDateTimeConverter(this))
            .addSerializer(new Location.DegreesConverter(this))
            .addSerializer(new MapEdgeIdentifier.Converter(this))
            .addSerializer(new MetropolitanArea.Converter<>(this, MetropolitanArea.class))
            .addSerializer(new NodeIdentifierList.Converter(this, Separators.DEFAULT))
            .addSerializer(new Rectangle.Converter(this))
            .addSerializer(new RoadNameConverter(this))
            .addSerializer(new Speed.Converter(this))
            .addSerializer(new State.Converter<>(this, State.class))
            .addSerializer(new WayIdentifierList.Converter(this, Separators.DEFAULT))
            .addSerializer(new CountGsonSerializer())
            .addSerializer(new IdentifierGsonSerializer())
            .addSerializer(new LatitudeGsonSerializer())
            .addSerializer(new LongitudeGsonSerializer())
            .addSerializer(new SpeedCategoryGsonSerializer());
    }
}

