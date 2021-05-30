package com.telenav.tdk.josm.plugins.graph.view.tabs.query;

import com.telenav.tdk.graph.Route;
import com.telenav.tdk.josm.plugins.graph.view.GraphPanel;
import com.telenav.tdk.utilities.ui.swing.theme.TdkTheme;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static javax.swing.ScrollPaneConstants.*;

/**
 * @author jonathanl (shibo)
 */
public class MatchesPanel extends JPanel
{
    private final JList<Route> matches;

    public MatchesPanel(final GraphPanel graphPanel)
    {
        matches = TdkTheme.get().configure(new JList<>());
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
