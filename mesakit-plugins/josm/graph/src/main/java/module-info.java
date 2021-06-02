open module mesakit.plugins.josm.graph
{
    requires java.desktop;

    requires mesakit.graph.query;
    requires mesakit.plugins.josm.library;
    requires mesakit.navigation.routing;

    exports com.telenav.mesakit.plugins.josm.graph;
}
