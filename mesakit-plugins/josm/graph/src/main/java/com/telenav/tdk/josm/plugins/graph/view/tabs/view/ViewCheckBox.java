package com.telenav.tdk.josm.plugins.graph.view.tabs.view;

import com.telenav.tdk.josm.plugins.graph.view.GraphPanel;
import com.telenav.tdk.utilities.ui.swing.theme.TdkTheme;

import javax.swing.*;
import java.awt.*;

/**
 * @author jonathanl (shibo)
 */
public class ViewCheckBox extends JCheckBox
{
    public ViewCheckBox(final GraphPanel graphPanel, final String label)
    {
        super(label);
        TdkTheme.get().configure(this);
        setSelected(true);
        setPreferredSize(new Dimension(150, 20));
        addItemListener(e ->
        {
            final var layer = graphPanel.layer();
            if (layer != null)
            {
                layer.forceRepaint();
            }
        });
    }
}
