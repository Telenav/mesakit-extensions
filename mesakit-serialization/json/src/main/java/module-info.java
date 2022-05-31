open module mesakit.serialization.json
{
    requires transitive mesakit.graph.core;
    requires transitive mesakit.map.geography;
    requires transitive mesakit.map.road.model;
    requires transitive mesakit.map.region;

    requires transitive kivakit.serialization.gson;

    requires com.google.gson;

    exports com.telenav.mesakit.serialization.json;
    exports com.telenav.mesakit.serialization.json.serializers;
}
