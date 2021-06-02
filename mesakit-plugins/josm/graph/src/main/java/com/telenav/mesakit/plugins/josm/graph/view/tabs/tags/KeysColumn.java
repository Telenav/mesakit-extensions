package com.telenav.mesakit.plugins.josm.graph.view.tabs.tags;

import com.telenav.kivakit.kernel.conversion.language.IdentityConverter;
import com.telenav.kivakit.kernel.messaging.Listener;
import com.telenav.kivakit.utilities.ui.swing.component.searchlist.SearchList;
import com.telenav.kivakit.utilities.ui.swing.layout.*;
import com.telenav.kivakit.utilities.ui.swing.theme.KivaKitTheme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author jonathanl (shibo)
 */
public class KeysColumn extends JPanel
{
    KeysColumn(final TagPanel tagPanel)
    {
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        tagPanel.keysList = new SearchList<>(new IdentityConverter(Listener.NULL));
        tagPanel.keysList.addSelectionListener(tagPanel::updateValues);

        tagPanel.keysLabel = KivaKitTheme.get().configure(new JLabel(""));

        new VerticalBoxLayout(this)
                .add(Layouts.leftJustify(tagPanel.keysLabel))
                .add(tagPanel.keysList);
    }
}
