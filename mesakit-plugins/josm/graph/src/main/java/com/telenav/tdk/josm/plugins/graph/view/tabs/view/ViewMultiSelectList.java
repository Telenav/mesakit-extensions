package com.telenav.tdk.josm.plugins.graph.view.tabs.view;

import com.telenav.tdk.josm.plugins.graph.view.GraphPanel;
import com.telenav.tdk.utilities.ui.swing.theme.TdkTheme;

import javax.swing.*;

/**
 * @author jonathanl (shibo)
 */
public class ViewMultiSelectList<T> extends JList<T>
{
    public ViewMultiSelectList(final GraphPanel graphPanel, final T[] values)
    {
        super(values);
        TdkTheme.get().configure(this);
        getSelectionModel().addListSelectionListener((event) ->
        {
            final var layer = graphPanel.layer();
            if (layer != null)
            {
                layer.forceRepaint();
            }
        });
    }
}
