open module mesakit.tools.applications.pbf.region.extractor
{
    requires transitive mesakit.data.formats.pbf;
    requires transitive mesakit.map.cutter;
    requires mesakit.map.region;

    requires transitive kivakit.application;

    exports com.telenav.mesakit.tools.applications.pbf.region.extractor;
}
