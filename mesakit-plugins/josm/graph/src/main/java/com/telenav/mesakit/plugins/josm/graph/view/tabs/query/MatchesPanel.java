package com.telenav.mesakit.plugins.josm.graph.view.tabs.query;

import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.graph.Route;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.Set;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;

/**
 * @author jonathanl (shibo)
 */
public class MatchesPanel extends JPanel
{
    private final JList<Route> matches;

    public MatchesPanel(final GraphPanel graphPanel)
    {
        matches = KivaKitTheme.get().applyTo(new JList<>());
        matches.setModel(new DefaultListModel<>());

        final var scrollPane = new JScrollPane(matches, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        matches.addListSelectionListener((event) ->
        {
            final var selected = matches.getSelectedValue();
            if (selected != null)
            {
                graphPanel.layer().show(selected);
                graphPanel.layer().zoomTo(selected.bounds());
            }
        });
    }

    public void addAll(final Set<Route> result)
    {
        model().addAll(result);
    }

    public void clear()
    {
        model().removeAllElements();
    }

    private DefaultListModel<Route> model()
    {
        return (DefaultListModel<Route>) matches.getModel();
    }
}
