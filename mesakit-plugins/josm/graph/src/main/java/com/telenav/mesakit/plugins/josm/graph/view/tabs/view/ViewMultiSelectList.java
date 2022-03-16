package com.telenav.mesakit.plugins.josm.graph.view.tabs.view;

import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;

import javax.swing.JList;

/**
 * @author jonathanl (shibo)
 */
public class ViewMultiSelectList<T> extends JList<T>
{
    public ViewMultiSelectList(GraphPanel graphPanel, T[] values)
    {
        super(values);
        KivaKitTheme.get().applyTo(this);
        getSelectionModel().addListSelectionListener((event) ->
        {
            var layer = graphPanel.layer();
            if (layer != null)
            {
                layer.forceRepaint();
            }
        });
    }
}
