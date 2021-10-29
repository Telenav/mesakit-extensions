package com.telenav.mesakit.serialization.json.serializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.telenav.kivakit.serialization.json.GsonSerializer;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.mesakit.map.geography.Location;

import java.lang.reflect.Type;

/**
 * Serializes {@link Location} objects to and from JSON.
 *
 * @author junwei
 */
@LexakaiJavadoc(complete = true)
public class LocationGsonSerializer implements GsonSerializer<Location>
{
    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException
    {
        var location = json.getAsJsonObject();
        return Location.degrees(
                location.get("latitude").getAsDouble(),
                location.get("longitude").getAsDouble());
    }

    @Override
    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context)
    {
        var location = new JsonObject();
        location.add("latitude", new JsonPrimitive(src.latitudeInDegrees()));
        location.add("longitude", new JsonPrimitive(src.longitudeInDegrees()));
        return location;
    }
}
