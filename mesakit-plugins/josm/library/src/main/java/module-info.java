open module mesakit.plugins.josm.library
{
    // Mesakit
    requires transitive mesakit.map.geography;
    requires transitive mesakit.map.ui.desktop;

    // KivaKit
    requires kivakit.component;

    requires josm;

    exports com.telenav.mesakit.plugins.josm.library;
    exports com.telenav.mesakit.plugins.josm.library.tile;
    exports com.telenav.mesakit.plugins.josm.library.tile.vector;
}
