package com.telenav.mesakit.plugins.josm.graph.view.tabs.tags;

import com.telenav.kivakit.kernel.data.conversion.string.language.IdentityConverter;
import com.telenav.kivakit.kernel.language.strings.Plural;
import com.telenav.kivakit.kernel.messaging.Listener;
import com.telenav.kivakit.ui.desktop.component.searchlist.SearchList;
import com.telenav.kivakit.ui.desktop.layout.Layouts;
import com.telenav.kivakit.ui.desktop.layout.VerticalBoxLayout;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.graph.collections.EdgeSet;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;

import static com.telenav.mesakit.plugins.josm.graph.view.GraphLayer.Show.HIGHLIGHT_AND_ZOOM_TO;
import static com.telenav.mesakit.plugins.josm.graph.view.GraphLayer.Show.HIGHLIGHT_ONLY;

/**
 * @author jonathanl (shibo)
 */
public class ValuesColumn extends JPanel
{
    ValuesColumn(final TagPanel tagPanel)
    {
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        tagPanel.valuesList = new SearchList<>(new IdentityConverter(Listener.none()));
        tagPanel.valuesList.addSelectionListener((value) ->
        {
            final var key = tagPanel.keysList.selected();
            if (key != null && value != null)
            {
                tagPanel.graphPanel.status("Searching for $='$'", key, value);
                final var edges = tagPanel.index.edges(key, value);
                if (edges == null)
                {
                    SwingUtilities.invokeLater(() -> tagPanel.graphPanel.status("Too many matches to show"));
                    tagPanel.layer.show(new EdgeSet(), HIGHLIGHT_ONLY);
                }
                else
                {
                    SwingUtilities.invokeLater(() -> tagPanel.graphPanel.status("Highlighting $ $", edges.size(), Plural.pluralize(edges.size(), "match")));
                    tagPanel.layer.show(edges, tagPanel.searchViewAreaOnly.isSelected() ? HIGHLIGHT_ONLY : HIGHLIGHT_AND_ZOOM_TO);
                }
            }
        });

        tagPanel.valuesLabel = KivaKitTheme.get().newComponentLabel("");
        tagPanel.valuesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        new VerticalBoxLayout(this)
                .add(Layouts.leftJustify(tagPanel.valuesLabel))
                .add(tagPanel.valuesList);
    }
}
