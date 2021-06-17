open module mesakit.serialization.json
{
    requires transitive kivakit.serialization.json;
    requires transitive mesakit.graph.core;

    exports com.telenav.mesakit.serialization.json;
    exports com.telenav.mesakit.serialization.json.serializers;
}
