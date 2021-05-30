open module tdk.josm.plugins.library
{
    requires transitive tdk.map.utilities.geojson;
    requires transitive tdk.map.ui;
    requires transitive tdk.ui.swing;
    requires transitive tdk.graph.traffic;

    requires transitive josm;
    requires transitive gson;

    exports com.telenav.tdk.josm.plugins.library;
    exports com.telenav.tdk.josm.plugins.library.tile;
    exports com.telenav.tdk.josm.plugins.library.tile.vector;
}
