open module mesakit.tools.applications.pbf.analyzer
{
    requires transitive mesakit.data.formats.pbf;
    requires transitive mesakit.graph.core;

    requires transitive kivakit.application;

    requires osmosis.core;

    exports com.telenav.mesakit.tools.applications.pbf.analyzer;
}
