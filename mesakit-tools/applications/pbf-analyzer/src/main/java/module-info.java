open module mesakit.tools.applications.pbf.analyzer
{
    requires transitive kivakit.application;
    requires transitive mesakit.data.formats.pbf;
    requires mesakit.map.geography;

    requires mesakit.map.measurements;
    requires kivakit.primitive.collections;

    exports com.telenav.mesakit.tools.applications.pbf.analyzer;
}
