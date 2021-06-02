package com.telenav.mesakit.plugins.josm.graph.view.tabs.search;

import com.telenav.kivakit.josm.plugins.graph.view.GraphPanel;
import com.telenav.kivakit.kernel.language.string.Strings;
import com.telenav.kivakit.utilities.ui.swing.component.Components;
import com.telenav.kivakit.utilities.ui.swing.component.console.OutputPanel;
import com.telenav.kivakit.utilities.ui.swing.component.icon.search.MagnifyingGlass;
import com.telenav.kivakit.utilities.ui.swing.graphics.color.KivaKitColors;
import com.telenav.kivakit.utilities.ui.swing.layout.*;
import com.telenav.kivakit.utilities.ui.swing.theme.KivaKitTheme;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.event.ActionListener;

import static com.telenav.kivakit.utilities.ui.swing.component.console.OutputPanel.Type;

/**
 * @author jonathanl (shibo)
 */
public class SearchPanel extends JPanel
{
    private final JTextField searchField = KivaKitTheme.get().newTextField();

    private OutputPanel console;

    private final GraphPanel graphPanel;

    public SearchPanel(final GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;

        Components.border(this, 8);

        graphPanel.overrideMenuAcceleratorKeys(searchField);

        final ActionListener searchAction = event ->
        {
            if (graphPanel.layer() != null)
            {
                final var searchString = searchField.getText();
                if (!Strings.isEmpty(searchString))
                {
                    searchField.setSelectionStart(0);
                    searchField.setSelectionEnd(searchString.length());
                    show(new Searcher(graphPanel.layer()).search(searchString));
                }
            }
            else
            {
                graphPanel.say("No graph layer is selected. To load a graph use File/Open...");
            }
        };

        final var searchButton = new JButton("search");
        searchButton.setFont(KivaKitTheme.get().componentFont());
        searchButton.addActionListener(searchAction);

        searchField.addActionListener(searchAction);
        Components.border(searchField, 8);
        Components.preferredWidth(searchField, 1_000);

        final var searchTools = new HorizontalBox(SpacingStyle.MANUAL_SPACING, 24)
                .add(new MagnifyingGlass())
                .add(searchField)
                .add(searchButton);

        UIManager.getDefaults().put("TextPane.background", KivaKitColors.DARK_GRAY.asAwtColor());

        new VerticalBoxLayout(this)
                .add(searchTools)
                .add(console());
    }

    public OutputPanel console()
    {
        if (console == null)
        {
            console = new OutputPanel(Type.VARIABLE_WIDTH);
            console.html(Searcher.help().html());
        }
        return console;
    }

    public void html(final String message, final Object... arguments)
    {
        console().html(message, arguments);
    }

    public void text(final String message, final Object... arguments)
    {
        console().text(message, arguments);
    }

    private void show(final UserFeedback feedback)
    {
        if (feedback != null)
        {
            if (feedback.html() != null)
            {
                html(feedback.html());
            }
            if (feedback.text() != null)
            {
                text(feedback.text());
            }
            if (feedback.status() != null)
            {
                graphPanel.say(feedback.status());
            }
        }
    }
}
