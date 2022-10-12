package com.telenav.mesakit.plugins.josm.graph.view.tabs.search;

import com.telenav.kivakit.component.ComponentMixin;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.ui.desktop.component.icon.search.MagnifyingGlass;
import com.telenav.kivakit.ui.desktop.component.panel.output.OutputPanel;
import com.telenav.kivakit.ui.desktop.layout.Borders;
import com.telenav.kivakit.ui.desktop.layout.HorizontalBox;
import com.telenav.kivakit.ui.desktop.layout.Size;
import com.telenav.kivakit.ui.desktop.layout.VerticalBoxLayout;
import com.telenav.kivakit.ui.desktop.theme.KivaKitColors;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.event.ActionListener;

import static com.telenav.kivakit.ui.desktop.layout.Spacing.MANUAL_SPACING;

/**
 * @author jonathanl (shibo)
 */
public class SearchPanel extends JPanel implements ComponentMixin
{
    private final JTextField searchField = KivaKitTheme.get().newTextField();

    private OutputPanel console;

    private final GraphPanel graphPanel;

    public SearchPanel(GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;

        Borders.applyMargin(this, 8);

        graphPanel.overrideMenuAcceleratorKeys(searchField);

        ActionListener searchAction = event ->
        {
            if (graphPanel.layer() != null)
            {
                var searchString = searchField.getText();
                if (!Strings.isNullOrBlank(searchString))
                {
                    searchField.setSelectionStart(0);
                    searchField.setSelectionEnd(searchString.length());
                    show(listenTo(new Searcher(graphPanel.layer())).search(searchString));
                }
            }
            else
            {
                graphPanel.status("No graph layer is selected. To load a graph use File/Open...");
            }
        };

        var searchButton = new JButton("search");
        searchButton.setFont(KivaKitTheme.get().fontNormal());
        searchButton.addActionListener(searchAction);

        searchField.addActionListener(searchAction);
        Borders.applyMargin(searchField, 8);
        Size.widthOf(1_000).preferred(searchField);

        var searchTools = new HorizontalBox(MANUAL_SPACING, 24)
                .add(new MagnifyingGlass())
                .add(searchField)
                .add(searchButton);

        UIManager.getDefaults().put("TextPane.background", KivaKitColors.DARK_CHARCOAL.asAwtColor());

        new VerticalBoxLayout(this)
                .add(searchTools)
                .add(console());
    }

    public OutputPanel console()
    {
        if (console == null)
        {
            console = new OutputPanel(OutputPanel.Type.VARIABLE_WIDTH);
            console.html(Searcher.help().html());
        }
        return console;
    }

    public void html(String message, Object... arguments)
    {
        console().html(message, arguments);
    }

    public void text(String message, Object... arguments)
    {
        console().text(message, arguments);
    }

    private void show(UserFeedback feedback)
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
                graphPanel.status(feedback.status());
            }
        }
    }
}
