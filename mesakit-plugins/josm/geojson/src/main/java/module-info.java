open module tdk.josm.plugins.geojson
{
    requires transitive tdk.josm.plugins.library;
    requires transitive tdk.map.utilities.geojson;
    requires transitive tdk.ui.swing;
    requires transitive tdk.data.formats.library;

    exports com.telenav.tdk.josm.plugins.geojson;
}
