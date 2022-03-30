open module mesakit.josm.plugins.geojson
{
    requires transitive mesakit.plugins.josm.library;
    requires transitive mesakit.map.utilities.geojson;
    requires transitive kivakit.ui.desktop;
    requires transitive mesakit.map.geography;
    requires transitive mesakit.map.data.library;

    requires josm;

    exports com.telenav.mesakit.plugins.josm.geojson;
}
