open module mesakit.josm.plugins.graph
{
    requires java.desktop;

    requires tdk.graph.query;
    requires tdk.josm.plugins.library;
    requires tdk.navigation.routing;

    exports com.telenav.kivakit.josm.plugins.graph;
}
