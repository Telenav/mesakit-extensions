open module mesakit.tools.applications.pbf.analyzer
{
    requires transitive kivakit.application;
    requires transitive mesakit.data.formats.pbf;
    requires transitive mesakit.graph.core;

    requires osmosis.core;
    requires mesakit.map.measurements;
    requires kivakit.primitive.collections;

    exports com.telenav.mesakit.tools.applications.pbf.analyzer;
}
