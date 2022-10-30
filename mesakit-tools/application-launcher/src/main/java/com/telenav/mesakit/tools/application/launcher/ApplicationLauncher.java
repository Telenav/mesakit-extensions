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

package com.telenav.mesakit.tools.application.launcher;

import com.telenav.kivakit.core.time.Duration;
import com.telenav.mesakit.tools.applications.codec.CodecGeneratorApplication;
import com.telenav.mesakit.tools.applications.graph.analyzer.GraphAnalyzerApplication;
import com.telenav.mesakit.tools.applications.graph.converter.GraphToPbfConverterApplication;
import com.telenav.mesakit.tools.applications.graph.dumper.GraphDumperApplication;
import com.telenav.mesakit.tools.applications.graph.slicer.GraphSlicerApplication;
import com.telenav.mesakit.tools.applications.graph.verifier.GraphVerifierApplication;
import com.telenav.mesakit.tools.applications.graph.ways.extractor.DoubleDigitizedWaysExtractorApplication;
import com.telenav.mesakit.tools.applications.pbf.analyzer.PbfAnalyzerApplication;
import com.telenav.mesakit.tools.applications.pbf.converter.PbfToGraphConverterApplication;
import com.telenav.mesakit.tools.applications.pbf.dumper.PbfDumperApplication;
import com.telenav.mesakit.tools.applications.pbf.graph.world.extractor.PbfWorldGraphExtractorApplication;
import com.telenav.mesakit.tools.applications.pbf.metadata.PbfMetadataApplication;
import com.telenav.mesakit.tools.applications.pbf.region.extractor.PbfRegionExtractorApplication;
import com.telenav.mesakit.tools.applications.region.information.RegionInformationApplication;

import java.util.Arrays;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ApplicationLauncher
{
    protected static void help(String tool)
    {
        System.out.println();
        System.out.println("mesakit-tools <tool-name>");
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
        System.out.println("    // Map Data Conversions");
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
        System.out.println("    codec-generator - creates codecs for use in graph specification implementations");
        System.out.println("    region-information - shows information about country, state, county and metro regions");
        System.out.println();
    }

    public static void main(String[] arguments)
    {
        if (arguments.length >= 1)
        {
            var application = arguments[0];
            arguments = shift(arguments);

            switch (application.toLowerCase())
            {
                // Conversions

                case "pbf-to-graph-converter" -> PbfToGraphConverterApplication.main(arguments);
                case "pbf-world-graph-extractor" -> PbfWorldGraphExtractorApplication.main(arguments);
                case "graph-to-pbf-converter" -> GraphToPbfConverterApplication.main(arguments);

                // Graph Tools

                case "graph-analyzer" -> GraphAnalyzerApplication.main(arguments);
                case "graph-double-digitized-ways-extractor" -> DoubleDigitizedWaysExtractorApplication.main(arguments);
                case "graph-dumper" -> GraphDumperApplication.main(arguments);
                case "graph-slicer" -> GraphSlicerApplication.main(arguments);
                case "graph-verifier" -> GraphVerifierApplication.main(arguments);

                // PBF Tools

                case "pbf-analyzer" -> PbfAnalyzerApplication.main(arguments);
                case "pbf-dumper" -> PbfDumperApplication.main(arguments);
                case "pbf-metadata" -> PbfMetadataApplication.main(arguments);
                case "pbf-region-extractor" -> PbfRegionExtractorApplication.main(arguments);

                // Other Tools

                case "codec-generator" -> CodecGeneratorApplication.main(arguments);
                case "region-information" -> RegionInformationApplication.main(arguments);
                default -> help(application);
            }
        }
        else
        {
            help(null);
        }
    }

    private static String[] shift(String[] arguments)
    {
        return Arrays.copyOfRange(arguments, 1, arguments.length);
    }
}
