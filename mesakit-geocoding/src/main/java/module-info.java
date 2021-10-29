open module mesakit.graph.geocoding
{
    requires transitive mesakit.graph.core;

    requires transitive kivakit.component;

    exports com.telenav.mesakit.graph.geocoding.reverse;
    exports com.telenav.mesakit.graph.geocoding.reverse.matching;
}
