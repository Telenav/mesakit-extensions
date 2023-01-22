package com.telenav.mesakit.serialization.json.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.telenav.kivakit.serialization.gson.serializers.BaseGsonElementSerializer;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.mesakit.map.geography.Location;

/**
 * Serializes {@link Location} objects to and from JSON.
 *
 * @author junwei
 */
@LexakaiJavadoc(complete = true)
public class LocationGsonSerializer extends BaseGsonElementSerializer<Location>
{
    public LocationGsonSerializer()
    {
        super(Location.class);
    }

    @Override
    protected JsonElement toJson(Location value)
    {
        var location = new JsonObject();

        location.add("latitude", new JsonPrimitive(value.latitudeInDegrees()));
        location.add("longitude", new JsonPrimitive(value.longitudeInDegrees()));

        return location;
    }

    @Override
    protected Location toValue(JsonElement json)
    {
        return Location.degrees(
            deserialize(json, "latitude", Double.class),
            deserialize(json, "longitude", Double.class));
    }
}
