package com.telenav.mesakit.plugins.josm.graph.view.tabs.view;

import com.telenav.kivakit.josm.plugins.graph.view.GraphPanel;
import com.telenav.kivakit.utilities.ui.swing.theme.KivaKitTheme;

import javax.swing.JCheckBox;
import java.awt.Dimension;

/**
 * @author jonathanl (shibo)
 */
public class ViewCheckBox extends JCheckBox
{
    public ViewCheckBox(final GraphPanel graphPanel, final String label)
    {
        super(label);
        KivaKitTheme.get().configure(this);
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
