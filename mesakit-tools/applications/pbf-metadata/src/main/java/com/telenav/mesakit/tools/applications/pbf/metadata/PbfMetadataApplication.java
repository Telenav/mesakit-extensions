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

package com.telenav.mesakit.tools.applications.pbf.metadata;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.ArgumentParser;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.string.AsciiArt;
import com.telenav.kivakit.messaging.Message;
import com.telenav.mesakit.graph.GraphProject;
import com.telenav.mesakit.graph.Metadata;
import com.telenav.mesakit.graph.specifications.library.pbf.PbfFileMetadataAnnotator;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.osm.OsmNavigableWayFilter;
import com.telenav.mesakit.map.data.formats.pbf.processing.filters.osm.OsmRelationsFilter;
import com.telenav.mesakit.map.geography.Precision;

import java.util.List;

import static com.telenav.kivakit.commandline.SwitchParsers.booleanSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParsers.enumSwitchParser;
import static com.telenav.kivakit.commandline.SwitchParsers.stringSwitchParser;
import static com.telenav.kivakit.filesystem.File.fileArgumentParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.RelationFilter.relationFilterSwitchParser;
import static com.telenav.mesakit.map.data.formats.pbf.processing.filters.WayFilter.wayFilterSwitchParser;
import static com.telenav.mesakit.map.geography.Precision.precisionSwitchParser;

/**
 * View or add/replace metadata in a PBF file
 *
 * @author jonathanl (shibo)
 */
public class PbfMetadataApplication extends Application
{
    public static void main(String[] arguments)
    {
        new PbfMetadataApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            fileArgumentParser(this, "Input PBF file")
                    .required()
                    .build();

    private final SwitchParser<Boolean> VIEW =
            booleanSwitchParser(this, "view", "View existing metadata")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> ADD =
            booleanSwitchParser(this, "add", "Add or replace metadata")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<PbfFileMetadataAnnotator.Mode> MODE =
            enumSwitchParser(this, "mode", "Omit nodes that are not places or referenced by a way or relation", PbfFileMetadataAnnotator.Mode.class)
                    .optional()
                    .defaultValue(PbfFileMetadataAnnotator.Mode.STRIP_UNREFERENCED_NODES)
                    .build();

    private final SwitchParser<String> DATA_DESCRIPTOR =
            stringSwitchParser(this, "data-descriptor", "Data descriptor such as HERE-UniDb-PBF-North_America-2020Q1")
                    .optional()
                    .build();

    private final SwitchParser<Precision> DATA_PRECISION =
            precisionSwitchParser()
                    .optional()
                    .build();

    private final SwitchParser<WayFilter> WAY_FILTER =
            wayFilterSwitchParser(this)
                    .optional()
                    .build();

    private final SwitchParser<RelationFilter> RELATION_FILTER =
            relationFilterSwitchParser(this)
                    .optional()
                    .build();

    private PbfMetadataApplication()
    {
        super(GraphProject.get());
    }

    @Override
    protected List<ArgumentParser<?>> argumentParsers()
    {
        return List.of(INPUT);
    }

    @SuppressWarnings({ "UseOfSystemOutOrSystemErr" })
    @Override
    protected void onRun()
    {
        var input = argument(INPUT);
        var existingMetadata = Metadata.from(input);
        if (get(VIEW))
        {
            information(commandLineDescription("PBF Metadata Annotator"));
            System.out.println(AsciiArt.textBox("Metadata", existingMetadata == null ? "No metadata found" : existingMetadata.asString()));
        }
        else if (get(ADD))
        {
            if (has(DATA_PRECISION) && has(DATA_DESCRIPTOR))
            {
                var metadataFromDescriptor = Metadata.parseDescriptor(get(DATA_DESCRIPTOR));
                if (metadataFromDescriptor == null)
                {
                    exit("$ is not a valid metadata descriptor", get(DATA_DESCRIPTOR));
                }
                else
                {
                    var wayFilter = get(WAY_FILTER);
                    if (wayFilter == null)
                    {
                        switch (metadataFromDescriptor.dataSpecification().type())
                        {
                            case OSM:
                                wayFilter = new OsmNavigableWayFilter();
                                break;

                            default:
                                exit("No default way filter for $", metadataFromDescriptor.dataSpecification().type());
                                return;
                        }
                    }

                    var relationFilter = get(RELATION_FILTER);
                    if (relationFilter == null)
                    {
                        switch (metadataFromDescriptor.dataSpecification().type())
                        {
                            case OSM:
                                relationFilter = new OsmRelationsFilter();
                                break;

                            default:
                                exit("No default relation filter for $", metadataFromDescriptor.dataSpecification().type());
                                return;
                        }
                    }

                    commandLine().addSwitch("way-filter", wayFilter.name());
                    information(commandLineDescription("PBF Metadata Annotator"));

                    var annotator = listenTo(new PbfFileMetadataAnnotator(input, get(MODE), wayFilter, relationFilter));

                    var metadata = annotator.read()
                            .withDataPrecision(get(DATA_PRECISION))
                            .withMetadata(metadataFromDescriptor);

                    boolean replace = existingMetadata != null;
                    if (annotator.write(metadata))
                    {
                        Message.println(AsciiArt.textBox((replace ? "Replaced Metadata in " : "Added Metadata to ")
                                + input.fileName(), "$", metadata));
                    }
                    else
                    {
                        problem("Unable to $ $", replace ? "replace metadata in" : "add metadata to", input);
                    }
                }
            }
            else
            {
                exit("Must supply both -data-descriptor and -data-precision when using -add");
            }
        }
        else
        {
            exit("Must supply either -view or -add");
        }
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(DATA_DESCRIPTOR, DATA_PRECISION, VIEW, ADD, MODE, WAY_FILTER, RELATION_FILTER, QUIET);
    }
}
