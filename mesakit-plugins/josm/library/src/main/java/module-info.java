open module mesakit.plugins.josm.library
{
    requires transitive mesakit.map.utilities.geojson;
    requires transitive mesakit.map.ui.desktop;

    requires josm;
    requires gson;

    exports com.telenav.mesakit.plugins.josm.library;
    exports com.telenav.mesakit.plugins.josm.library.tile;
    exports com.telenav.mesakit.plugins.josm.library.tile.vector;
}
