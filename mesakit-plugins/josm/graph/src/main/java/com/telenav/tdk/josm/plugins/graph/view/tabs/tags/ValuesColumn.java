package com.telenav.kivakit.josm.plugins.graph.view.tabs.tags;

import com.telenav.kivakit.graph.collections.EdgeSet;
import com.telenav.kivakit.kernel.conversion.language.IdentityConverter;
import com.telenav.kivakit.kernel.language.string.Strings;
import com.telenav.kivakit.kernel.messaging.Listener;
import com.telenav.kivakit.utilities.ui.swing.component.searchlist.SearchList;
import com.telenav.kivakit.utilities.ui.swing.layout.*;
import com.telenav.kivakit.utilities.ui.swing.theme.KivaKitTheme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;

import static com.telenav.kivakit.josm.plugins.graph.view.GraphLayer.Show.HIGHLIGHT_AND_ZOOM_TO;
import static com.telenav.kivakit.josm.plugins.graph.view.GraphLayer.Show.HIGHLIGHT_ONLY;

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

        tagPanel.valuesLabel = KivaKitTheme.get().configure(new JLabel(""));
        tagPanel.valuesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        new VerticalBoxLayout(this)
                .add(Layouts.leftJustify(tagPanel.valuesLabel))
                .add(tagPanel.valuesList);
    }
}
