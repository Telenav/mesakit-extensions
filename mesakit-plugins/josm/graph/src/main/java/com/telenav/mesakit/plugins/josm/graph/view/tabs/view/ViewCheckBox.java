package com.telenav.mesakit.plugins.josm.graph.view.tabs.view;

import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;

import javax.swing.JCheckBox;
import java.awt.Dimension;

/**
 * @author jonathanl (shibo)
 */
public class ViewCheckBox extends JCheckBox
{
    public ViewCheckBox(GraphPanel graphPanel, String label)
    {
        super(label);
        KivaKitTheme.get().applyTo(this);
        setSelected(true);
        setPreferredSize(new Dimension(150, 20));
        addItemListener(e ->
        {
            var layer = graphPanel.layer();
            if (layer != null)
            {
                layer.forceRepaint();
            }
        });
    }
}
