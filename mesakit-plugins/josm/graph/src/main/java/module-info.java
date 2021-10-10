open module mesakit.plugins.josm.graph
{
    requires java.desktop;

    requires mesakit.graph.query;
    requires mesakit.plugins.josm.library;
    requires mesakit.navigation.routing;

    requires josm;

    exports com.telenav.mesakit.plugins.josm.graph;
    exports com.telenav.mesakit.plugins.josm.graph.model;
    exports com.telenav.mesakit.plugins.josm.graph.view;
}
