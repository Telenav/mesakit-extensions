package com.telenav.mesakit.plugins.josm.graph.view.tabs.query;

import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.value.mutable.MutableValue;
import com.telenav.kivakit.ui.desktop.component.icon.search.MagnifyingGlass;
import com.telenav.kivakit.ui.desktop.component.panel.stack.CardPanel;
import com.telenav.kivakit.ui.desktop.component.progress.ProgressPanel;
import com.telenav.kivakit.ui.desktop.layout.Borders;
import com.telenav.kivakit.ui.desktop.layout.HorizontalBox;
import com.telenav.kivakit.ui.desktop.layout.Size;
import com.telenav.kivakit.ui.desktop.layout.Spacing;
import com.telenav.kivakit.ui.desktop.layout.VerticalBoxLayout;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.graph.query.GraphQuery;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.plugins.josm.graph.view.GraphLayer;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.search.UserFeedback;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.event.ActionListener;

/**
 * @author jonathanl (shibo)
 */
public class QueryPanel extends JPanel
{
    private enum Mode
    {
        QUERY_TOOLS,
        PROGRESS_BAR
    }

    private JTextField searchField;

    private MatchesPanel matches;

    private final GraphPanel graphPanel;

    private CardPanel cardPanel;

    private final ProgressReporter searchProgress = BroadcastingProgressReporter.progressReporter();

    private GraphQuery graphQuery;

    public QueryPanel(GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;

        Borders.applyMargin(this, 8);

        // Add the cards panel and the 'matches' panel in a vertical box
        new VerticalBoxLayout(this)
                .add(cardPanel())
                .add(matchesPanel());

        // then start by playing the query tools card
        mode(Mode.QUERY_TOOLS);
    }

    public MatchesPanel matchesPanel()
    {
        if (matches == null)
        {
            matches = new MatchesPanel(graphPanel);
        }
        return matches;
    }

    private CardPanel cardPanel()
    {
        if (cardPanel == null)
        {
            // Add the query tools and progress panel as cards
            cardPanel = new CardPanel();
            cardPanel.addCard(queryTools(), "query-tools");
            cardPanel.addCard(progressPanel(), "progress-bar");
            Size.heightOf(32).maximum(cardPanel);
        }
        return cardPanel;
    }

    private void feedback(UserFeedback feedback)
    {
        if (feedback != null)
        {
            if (feedback.status() != null)
            {
                graphPanel.status(feedback.status());
            }
        }
    }

    private void mode(Mode mode)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (mode == Mode.PROGRESS_BAR)
            {
                cardPanel.show("progress-bar");
            }
            else
            {
                cardPanel.show("query-tools");
            }
        });
    }

    private ProgressPanel progressPanel()
    {
        // Create progress panel that tracks search progress
        searchProgress.reset();
        return new ProgressPanel(searchProgress, 300, completion ->
        {
            // and when progress completes, stop any query and show the search card
            graphQuery.stop();
            mode(Mode.QUERY_TOOLS);
        });
    }

    private HorizontalBox queryTools()
    {
        var theme = KivaKitTheme.get();

        graphPanel.overrideMenuAcceleratorKeys(searchField());

        // Search when the query button is pushed or return is hit in the search field
        ActionListener search = searchAction(graphPanel);
        var searchButton = theme.newButton("query");
        searchButton.setFont(theme.fontNormal());
        searchButton.addActionListener(search);
        searchField().addActionListener(search);

        // Add the search field and button to a query tools box
        return new HorizontalBox(Spacing.MANUAL_SPACING, 24)
                .add(new MagnifyingGlass())
                .add(searchField())
                .add(searchButton);
    }

    private ActionListener searchAction(GraphPanel graphPanel)
    {
        return event ->
        {
            if (graphPanel.layer() != null)
            {
                var searchString = searchField().getText();
                if (!Strings.isNullOrEmpty(searchString))
                {
                    searchField().setSelectionStart(0);
                    searchField().setSelectionEnd(searchString.length());

                    if (searchString.startsWith("select"))
                    {
                        var viewBounds = graphPanel.layer().model().bounds();
                        mode(Mode.PROGRESS_BAR);
                        select(searchString, viewBounds);
                    }
                }
            }
            else
            {
                graphPanel.status("No graph layer is selected. To load a graph use File/Open...");
            }
        };
    }

    private JTextField searchField()
    {
        if (searchField == null)
        {
            searchField = KivaKitTheme.get().newTextField();
            Borders.applyMargin(searchField, 8);
            Size.widthOf(1_000).preferred(cardPanel);
            UIManager.getDefaults().put("TextPane.background", KivaKitColors.DARK_CHARCOAL.asAwtColor());
        }
        return searchField;
    }

    private void select(String query, Rectangle viewBounds)
    {
        matchesPanel().clear();
        new Thread(() ->
        {
            try
            {
                // Get the candidate edges within the view area
                var candidates = graphPanel.layer().graph().edgesIntersecting(viewBounds);

                // and if the number of candidates
                var count = candidates.count();

                // is large enough, then show the progress bar
                SwingUtilities.invokeLater(() -> mode(count.isGreaterThan(Count._10_000) ? Mode.PROGRESS_BAR : Mode.QUERY_TOOLS));

                var error = new MutableValue<String>();
                graphQuery = new GraphQuery();
                searchProgress.reset();
                searchProgress.steps(count.asMaximum());
                var result = graphQuery.execute(searchProgress, candidates, query, Maximum.maximum(1_000), error::set);
                if (error.get() != null)
                {
                    feedback(UserFeedback.status(error.get()));
                }
                else
                {
                    var edges = new EdgeSet();
                    for (var element : result)
                    {
                        if (element != null)
                        {
                            edges.add(element);
                        }
                    }
                    graphPanel.layer().show(edges, GraphLayer.Show.HIGHLIGHT_ONLY);
                    SwingUtilities.invokeLater(() -> matchesPanel().addAll(result));
                    feedback(UserFeedback.html("Found " + edges.size() + " matching edges"));
                }
            }
            catch (Exception e)
            {
                feedback(UserFeedback.status(e.getMessage()));
            }
            mode(Mode.QUERY_TOOLS);
        }, "QuerySelect").start();
    }
}
