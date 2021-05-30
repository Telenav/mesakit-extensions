package com.telenav.tdk.josm.plugins.graph.view.tabs.tags;

import com.telenav.tdk.core.kernel.conversion.language.IdentityConverter;
import com.telenav.tdk.core.kernel.language.string.Strings;
import com.telenav.tdk.core.kernel.messaging.Listener;
import com.telenav.tdk.graph.collections.EdgeSet;
import com.telenav.tdk.utilities.ui.swing.component.searchlist.SearchList;
import com.telenav.tdk.utilities.ui.swing.layout.*;
import com.telenav.tdk.utilities.ui.swing.theme.TdkTheme;

import javax.swing.*;
import java.awt.*;

import static com.telenav.tdk.josm.plugins.graph.view.GraphLayer.Show.*;

/**
 * @author jonathanl (shibo)
 */
public class ValuesColumn extends JPanel
{
    ValuesColumn(final TagPanel tagPanel)
    {
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        tagPanel.valuesList = new SearchList<>(new IdentityConverter(Listener.NULL));
        tagPanel.valuesList.addSelectionListener((value) ->
        {
            final var key = tagPanel.keysList.selected();
            if (key != null && value != null)
            {
                tagPanel.graphPanel.say("Searching for $='$'", key, value);
                final var edges = tagPanel.index.edges(key, value);
                if (edges == null)
                {
                    SwingUtilities.invokeLater(() -> tagPanel.graphPanel.say("Too many matches to show"));
                    tagPanel.layer.show(new EdgeSet(), HIGHLIGHT_ONLY);
                }
                else
                {
                    SwingUtilities.invokeLater(() -> tagPanel.graphPanel.say("Highlighting $ $", edges.size(), Strings.pluralize(edges.size(), "match")));
                    tagPanel.layer.show(edges, tagPanel.searchViewAreaOnly.isSelected() ? HIGHLIGHT_ONLY : HIGHLIGHT_AND_ZOOM_TO);
                }
            }
        });

        tagPanel.valuesLabel = TdkTheme.get().configure(new JLabel(""));
        tagPanel.valuesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        new VerticalBoxLayout(this)
                .add(Layouts.leftJustify(tagPanel.valuesLabel))
                .add(tagPanel.valuesList);
    }
}
