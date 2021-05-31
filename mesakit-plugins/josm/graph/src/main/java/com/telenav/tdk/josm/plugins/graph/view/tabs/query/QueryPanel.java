package com.telenav.kivakit.josm.plugins.graph.view.tabs.query;

import com.telenav.kivakit.graph.collections.EdgeSet;
import com.telenav.kivakit.graph.query.GraphQuery;
import com.telenav.kivakit.josm.plugins.graph.view.*;
import com.telenav.kivakit.josm.plugins.graph.view.tabs.search.UserFeedback;
import com.telenav.kivakit.kernel.language.string.Strings;
import com.telenav.kivakit.kernel.operation.progress.ProgressReporter;
import com.telenav.kivakit.kernel.operation.progress.reporters.Progress;
import com.telenav.kivakit.kernel.scalars.counts.*;
import com.telenav.kivakit.kernel.scalars.mutable.MutableValue;
import com.telenav.kivakit.map.geography.rectangle.Rectangle;
import com.telenav.kivakit.utilities.ui.swing.component.Components;
import com.telenav.kivakit.utilities.ui.swing.component.icon.search.MagnifyingGlass;
import com.telenav.kivakit.utilities.ui.swing.component.progress.ProgressPanel;
import com.telenav.kivakit.utilities.ui.swing.component.stack.CardPanel;
import com.telenav.kivakit.utilities.ui.swing.graphics.color.KivaKitColors;
import com.telenav.kivakit.utilities.ui.swing.layout.*;
import com.telenav.kivakit.utilities.ui.swing.theme.KivaKitTheme;
import org.jetbrains.annotations.NotNull;

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

    private final ProgressReporter searchProgress = Progress.create();

    private GraphQuery graphQuery;

    public QueryPanel(final GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;

        Components.border(this, 8);

        // Add the cards panel and the matches panel in a vertical box
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
            Components.maximumHeight(cardPanel, 32);
        }
        return cardPanel;
    }

    private void feedback(final UserFeedback feedback)
    {
        if (feedback != null)
        {
            if (feedback.status() != null)
            {
                graphPanel.say(feedback.status());
            }
        }
    }

    private void mode(final Mode mode)
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

    @NotNull
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
        final var theme = KivaKitTheme.get();

        graphPanel.overrideMenuAcceleratorKeys(searchField());

        // Search when the query button is pushed or return is hit in the search field
        final ActionListener search = searchAction(graphPanel);
        final var searchButton = theme.newButton("query");
        searchButton.setFont(theme.componentFont());
        searchButton.addActionListener(search);
        searchField().addActionListener(search);

        // Add the search field and button to a query tools box
        return new HorizontalBox(SpacingStyle.MANUAL_SPACING, 24)
                .add(new MagnifyingGlass())
                .add(searchField())
                .add(searchButton);
    }

    @NotNull
    private ActionListener searchAction(final GraphPanel graphPanel)
    {
        return event ->
        {
            if (graphPanel.layer() != null)
            {
                final var searchString = searchField().getText();
                if (!Strings.isEmpty(searchString))
                {
                    searchField().setSelectionStart(0);
                    searchField().setSelectionEnd(searchString.length());

                    if (searchString.startsWith("select"))
                    {
                        final var viewBounds = graphPanel.layer().model().bounds();
                        mode(Mode.PROGRESS_BAR);
                        select(searchString, viewBounds);
                    }
                }
            }
            else
            {
                graphPanel.say("No graph layer is selected. To load a graph use File/Open...");
            }
        };
    }

    private JTextField searchField()
    {
        if (searchField == null)
        {
            searchField = KivaKitTheme.get().newTextField();
            Components.border(searchField, 8);
            Components.preferredWidth(searchField, 1_000);
            UIManager.getDefaults().put("TextPane.background", KivaKitColors.DARK_GRAY.asAwtColor());
        }
        return searchField;
    }

    private void select(final String query, final Rectangle viewBounds)
    {
        matchesPanel().clear();
        new Thread(() ->
        {
            try
            {
                // Get the candidate edges within the view area
                final var candidates = graphPanel.layer().graph().edgesIntersecting(viewBounds);

                // and if the number of candidates
                final var count = candidates.count();

                // is large enough, then show the progress bar
                SwingUtilities.invokeLater(() -> mode(count.isGreaterThan(Count._10_000) ? Mode.PROGRESS_BAR : Mode.QUERY_TOOLS));

                final var error = new MutableValue<String>();
                graphQuery = new GraphQuery();
                searchProgress.reset();
                searchProgress.steps(count.asMaximum());
                final var result = graphQuery.execute(searchProgress, candidates, query, Maximum.of(1_000), error::set);
                if (error.get() != null)
                {
                    feedback(UserFeedback.status(error.get()));
                }
                else
                {
                    final var edges = new EdgeSet();
                    for (final var element : result)
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
            catch (final Exception e)
            {
                feedback(UserFeedback.status(e.getMessage()));
            }
            mode(Mode.QUERY_TOOLS);
        }, "QuerySelect").start();
    }
}
