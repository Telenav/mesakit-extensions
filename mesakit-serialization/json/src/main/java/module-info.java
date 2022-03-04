open module mesakit.serialization.json
{
    requires transitive kivakit.serialization.json;
    requires transitive mesakit.graph.core;
    requires transitive kivakit.component;

    requires gson;
    requires mesakit.map.geography;
    requires mesakit.map.road.model;
    requires mesakit.map.region;

    exports com.telenav.mesakit.serialization.json;
    exports com.telenav.mesakit.serialization.json.serializers;
}
