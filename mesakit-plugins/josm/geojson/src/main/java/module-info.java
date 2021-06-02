open module mesakit.josm.plugins.geojson
{
    requires transitive mesakit.plugins.josm.library;
    requires transitive mesakit.map.utilities.geojson;
    requires transitive mesakit.map.ui.desktop;
    requires transitive mesakit.map.data.library;

    exports com.telenav.mesakit.plugins.josm.geojson;
}
