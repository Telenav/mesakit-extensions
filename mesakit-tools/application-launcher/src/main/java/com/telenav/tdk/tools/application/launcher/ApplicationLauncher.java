////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.telenav.kivakit.tools.application.launcher;

import com.telenav.kivakit.kernel.logging.loggers.LogServiceLoader;
import com.telenav.kivakit.kernel.time.Duration;
import com.telenav.kivakit.logs.server.ServerLog;
import com.telenav.kivakit.tools.applications.codec.CodecGeneratorApplication;
import com.telenav.mesakit.tools.applications.graph.analyzer.GraphAnalyzerApplication;
import com.telenav.kivakit.tools.applications.graph.converter.GraphToPbfConverterApplication;
import com.telenav.kivakit.tools.applications.graph.dumper.GraphDumperApplication;
import com.telenav.kivakit.tools.applications.graph.slicer.GraphSlicerApplication;
import com.telenav.kivakit.tools.applications.graph.verifier.GraphVerifierApplication;
import com.telenav.kivakit.tools.applications.graph.ways.extractor.DoubleDigitizedWaysExtractorApplication;
import com.telenav.kivakit.tools.applications.log.viewer.LogViewerApplication;
import com.telenav.kivakit.tools.applications.pbf.analyzer.PbfAnalyzerApplication;
import com.telenav.kivakit.tools.applications.pbf.converter.PbfToGraphConverterApplication;
import com.telenav.kivakit.tools.applications.pbf.dumper.PbfDumperApplication;
import com.telenav.kivakit.tools.applications.pbf.graph.world.extractor.PbfWorldGraphExtractorApplication;
import com.telenav.kivakit.tools.applications.pbf.metadata.PbfMetadataApplication;
import com.telenav.kivakit.tools.applications.pbf.region.extractor.PbfRegionExtractorApplication;
import com.telenav.kivakit.tools.applications.region.information.RegionInformationApplication;
import com.telenav.kivakit.tools.applications.service.registry.ServiceRegistryViewerApplication;

import java.util.Arrays;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ApplicationLauncher
{
    protected static void help(final String tool)
    {
        System.out.println();
        System.out.println("tdk-tool <tool-name>");
        System.out.println();
        System.out.flush();
        Duration.seconds(0.25).sleep();
        if (tool != null)
        {
            System.err.println("Tool '" + tool + "' not found. Please specify one of the following:");
        }
        else
        {
            System.err.println("No application was specified. Please specify one of the following:");
        }
        System.err.flush();
        Duration.seconds(0.25).sleep();
        System.out.println();
        System.out.println("    // Conversions");
        System.out.println();
        System.out.println("    pbf-to-graph-converter - converts pbf files to graph format");
        System.out.println("    pbf-world-graph-extractor - converts an pbf file into a world graph");
        System.out.println("    graph-to-pbf-converter - converts from graph back to pbf format");
        System.out.println();
        System.out.println("    // Graph Tools");
        System.out.println();
        System.out.println("    graph-analyzer - analyzes and gives statistics on a graph file");
        System.out.println("    graph-double-digitized-ways-extractor - extracts double digitized ways");
        System.out.println("    graph-dumper - dumps specific edges / ways from a graph file");
        System.out.println("    graph-slicer - slices a graph to a bounding rectangle");
        System.out.println("    graph-verifier - verifies the integrity of a graph");
        System.out.println();
        System.out.println("    // PBF Tools");
        System.out.println();
        System.out.println("    pbf-analyzer - analyzes pbf files");
        System.out.println("    pbf-dumper - dumps pbf files");
        System.out.println("    pbf-metadata - views, adds and replaces metadata in pbf files");
        System.out.println("    pbf-region-extractor - extracts countries, states and metro areas from a pbf file");
        System.out.println();
        System.out.println("    // Other Tools");
        System.out.println();
        System.out.println("    service-registry-viewer - shows registered TDK services on the local machine or another host");
        System.out.println("    codec-generator - creates codecs for use in graph specification implementations");
        System.out.println("    region-information - shows information about country, state, county and metro regions");
        System.out.println("    log-viewer - shows server logs on a local or remote machine ");
        System.out.println();
    }

    public static void main(String[] arguments)
    {
        // We add the server log manually here because when the application launcher is converted into an über-jar,
        // it breaks modularity and the log service provider implementations like ServerLog can no longer be found.
        // The Jigsaw team is aware of this issue, but has not set a schedule for fixing it yet. -- Shibo
        LogServiceLoader.logs().add(ServerLog.get());

        if (arguments.length >= 1)
        {
            final var application = arguments[0];
            arguments = shift(arguments);

            switch (application.toLowerCase())
            {
                // Conversions

                case "pbf-to-graph-converter":
                    PbfToGraphConverterApplication.main(arguments);
                    break;

                case "pbf-world-graph-extractor":
                    PbfWorldGraphExtractorApplication.main(arguments);
                    break;

                case "graph-to-pbf-converter":
                    GraphToPbfConverterApplication.main(arguments);
                    break;

                // Graph Tools

                case "graph-analyzer":
                    GraphAnalyzerApplication.main(arguments);
                    break;

                case "graph-double-digitized-ways-extractor":
                    DoubleDigitizedWaysExtractorApplication.main(arguments);
                    break;

                case "graph-dumper":
                    GraphDumperApplication.main(arguments);
                    break;

                case "graph-slicer":
                    GraphSlicerApplication.main(arguments);
                    break;

                case "graph-verifier":
                    GraphVerifierApplication.main(arguments);
                    break;

                // PBF Tools

                case "pbf-analyzer":
                    PbfAnalyzerApplication.main(arguments);
                    break;

                case "pbf-dumper":
                    PbfDumperApplication.main(arguments);
                    break;

                case "pbf-metadata":
                    PbfMetadataApplication.main(arguments);
                    break;

                case "pbf-region-extractor":
                    PbfRegionExtractorApplication.main(arguments);
                    break;

                // Other Tools

                case "service-registry-viewer":
                    ServiceRegistryViewerApplication.main(arguments);
                    break;

                case "codec-generator":
                    CodecGeneratorApplication.main(arguments);
                    break;

                case "region-information":
                    RegionInformationApplication.main(arguments);
                    break;

                case "log-viewer":
                    LogViewerApplication.main(arguments);
                    break;

                default:
                    help(application);
                    break;
            }
        }
        else
        {
            help(null);
        }
    }

    private static String[] shift(final String[] arguments)
    {
        return Arrays.copyOfRange(arguments, 1, arguments.length);
    }
}
