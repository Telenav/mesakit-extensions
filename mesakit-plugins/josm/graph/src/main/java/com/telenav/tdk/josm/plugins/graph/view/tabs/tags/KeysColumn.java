package com.telenav.tdk.josm.plugins.graph.view.tabs.tags;

import com.telenav.tdk.core.kernel.conversion.language.IdentityConverter;
import com.telenav.tdk.core.kernel.messaging.Listener;
import com.telenav.tdk.utilities.ui.swing.component.searchlist.SearchList;
import com.telenav.tdk.utilities.ui.swing.layout.*;
import com.telenav.tdk.utilities.ui.swing.theme.TdkTheme;

import javax.swing.*;

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

        tagPanel.keysLabel = TdkTheme.get().configure(new JLabel(""));

        new VerticalBoxLayout(this)
                .add(Layouts.leftJustify(tagPanel.keysLabel))
                .add(tagPanel.keysList);
    }
}
