open module mesakit.tools.applications.pbf.analyzer
{
    requires transitive kivakit.application;
    requires transitive mesakit.data.formats.pbf;
    requires transitive mesakit.graph.core;

    requires osmosis.core;

    exports com.telenav.mesakit.tools.applications.pbf.analyzer;
}
