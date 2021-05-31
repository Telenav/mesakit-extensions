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

package com.telenav.tdk.tools.applications.pbf.metadata;

import com.telenav.kivakit.application.KivaKitApplication;
import com.telenav.kivakit.data.formats.pbf.processing.filters.*;
import com.telenav.kivakit.data.formats.pbf.processing.filters.osm.*;
import com.telenav.kivakit.data.formats.pbf.processing.filters.unidb.*;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.kernel.commandline.*;
import com.telenav.kivakit.kernel.language.string.Strings;
import com.telenav.kivakit.kernel.messaging.Message;
import com.telenav.mesakit.graph.Metadata;
import com.telenav.mesakit.graph.project.GraphCore;
import com.telenav.mesakit.graph.specifications.library.pbf.PbfFileMetadataAnnotator;
import com.telenav.mesakit.map.geography.Precision;

import java.util.List;
import java.util.Set;

import static com.telenav.kivakit.graph.specifications.library.pbf.PbfFileMetadataAnnotator.Mode;

/**
 * View or add/replace metadata in a PBF file
 *
 * @author jonathanl (shibo)
 */
public class PbfMetadataApplication extends KivaKitApplication
{
    public static void main(final String[] arguments)
    {
        new PbfMetadataApplication().run(arguments);
    }

    private final ArgumentParser<File> INPUT =
            File.argumentParser("Input PBF file")
                    .required()
                    .build();

    private final SwitchParser<Boolean> VIEW =
            SwitchParser.booleanSwitch("view", "View existing metadata")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Boolean> ADD =
            SwitchParser.booleanSwitch("add", "Add or replace metadata")
                    .optional()
                    .defaultValue(false)
                    .build();

    private final SwitchParser<Mode> MODE =
            SwitchParser.enumSwitch("mode", "Omit nodes that are not places or referenced by a way or relation", Mode.class)
                    .optional()
                    .defaultValue(Mode.STRIP_UNREFERENCED_NODES)
                    .build();

    private final SwitchParser<String> DATA_DESCRIPTOR =
            SwitchParser.stringSwitch("data-descriptor", "Data descriptor such as HERE-UniDb-PBF-North_America-2020Q1")
                    .optional()
                    .build();

    private final SwitchParser<Precision> DATA_PRECISION =
            Precision.switchParser()
                    .optional()
                    .build();

    private final SwitchParser<WayFilter> WAY_FILTER =
            WayFilter.wayFilter()
                    .optional()
                    .build();

    private final SwitchParser<RelationFilter> RELATION_FILTER =
            RelationFilter.relationFilter()
                    .optional()
                    .build();

    private PbfMetadataApplication()
    {
        super(GraphCore.get());
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
        final var input = argument(INPUT);
        final var existingMetadata = Metadata.from(input);
        if (get(VIEW))
        {
            information(commandLineDescription("PBF Metadata Annotator"));
            System.out.println(Strings.textBox("Metadata", existingMetadata == null ? "No metadata found" : existingMetadata.asString()));
        }
        else if (get(ADD))
        {
            if (has(DATA_PRECISION) && has(DATA_DESCRIPTOR))
            {
                final var metadataFromDescriptor = Metadata.parseDescriptor(get(DATA_DESCRIPTOR));
                if (metadataFromDescriptor == null)
                {
                    exit("$ is not a valid metadataFromDescriptor", get(DATA_DESCRIPTOR));
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

                            case UniDb:
                                wayFilter = new UniDbNavigableWayFilter();
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

                            case UniDb:
                                relationFilter = new UniDbRelationsFilter();
                                break;

                            default:
                                exit("No default relation filter for $", metadataFromDescriptor.dataSpecification().type());
                                return;
                        }
                    }

                    commandLine().addSwitchValue("way-filter", wayFilter.name());
                    information(commandLineDescription("PBF Metadata Annotator"));

                    final var annotator = listenTo(new PbfFileMetadataAnnotator(input, get(MODE), wayFilter, relationFilter));

                    final var metadata = annotator.read()
                            .withDataPrecision(get(DATA_PRECISION))
                            .withMetadata(metadataFromDescriptor);

                    final boolean replace = existingMetadata != null;
                    if (annotator.write(metadata))
                    {
                        Message.println(Strings.textBox((replace ? "Replaced Metadata in " : "Added Metadata to ")
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
    protected Set<SwitchParser<?>> switchParsers()
    {
        return Set.of(DATA_DESCRIPTOR, DATA_PRECISION, VIEW, ADD, MODE, WAY_FILTER, RELATION_FILTER, QUIET);
    }
}
