package com.telenav.mesakit.plugins.josm.graph.view.tabs.tags;

import com.telenav.kivakit.kernel.data.conversion.string.language.IdentityConverter;
import com.telenav.kivakit.kernel.messaging.Listener;
import com.telenav.kivakit.ui.desktop.component.searchlist.SearchList;
import com.telenav.kivakit.ui.desktop.layout.Layouts;
import com.telenav.kivakit.ui.desktop.layout.VerticalBoxLayout;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * @author jonathanl (shibo)
 */
public class KeysColumn extends JPanel
{
    KeysColumn(final TagPanel tagPanel)
    {
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        tagPanel.keysList = new SearchList<>(new IdentityConverter(Listener.none()));
        tagPanel.keysList.addSelectionListener(tagPanel::updateValues);

        tagPanel.keysLabel = KivaKitTheme.get().newComponentLabel("");

        new VerticalBoxLayout(this)
                .add(Layouts.leftJustify(tagPanel.keysLabel))
                .add(tagPanel.keysList);
    }
}
