package com.telenav.mesakit.plugins.josm.graph.view.tabs.view;

import com.telenav.kivakit.josm.plugins.graph.view.GraphPanel;
import com.telenav.kivakit.utilities.ui.swing.theme.KivaKitTheme;

import javax.swing.JList;

/**
 * @author jonathanl (shibo)
 */
public class ViewMultiSelectList<T> extends JList<T>
{
    public ViewMultiSelectList(final GraphPanel graphPanel, final T[] values)
    {
        super(values);
        KivaKitTheme.get().configure(this);
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
