package com.telenav.tdk.josm.plugins.graph.view.tabs.tags;

import com.telenav.tdk.core.kernel.messaging.Message;
import com.telenav.tdk.graph.Graph;
import com.telenav.tdk.josm.plugins.graph.view.*;
import com.telenav.tdk.josm.plugins.graph.view.tabs.tags.indexing.*;
import com.telenav.tdk.map.geography.rectangle.Rectangle;
import com.telenav.tdk.utilities.ui.swing.component.Components;
import com.telenav.tdk.utilities.ui.swing.component.searchlist.SearchList;
import com.telenav.tdk.utilities.ui.swing.layout.*;
import com.telenav.tdk.utilities.ui.swing.theme.TdkTheme;

import javax.swing.*;
import java.util.Collections;

/**
 * @author jonathanl (shibo)
 */
public class TagPanel extends JPanel
{
    final GraphPanel graphPanel;

    GraphLayer layer;

    final TagIndexer indexer = new TagIndexer();

    SearchList<String> valuesList;

    SearchList<String> keysList;

    JLabel valuesLabel;

    JLabel keysLabel;

    final JCheckBox searchViewAreaOnly;

    TagIndex index;

    public TagPanel(final GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;

        indexer.broadcastTo(message -> graphPanel.say(message.formatted()));

        Components.border(this, 10);

        searchViewAreaOnly = TdkTheme.get().newCheckBox("Index tags in view area only");
        searchViewAreaOnly.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        searchViewAreaOnly.addActionListener(event -> refresh());
        searchViewAreaOnly.setSelected(true);

        final var columns = new JPanel();

        new HorizontalBoxLayout(columns)
                .add(new KeysColumn(this))
                .add(new ValuesColumn(this));

        new VerticalBoxLayout(this)
                .add(columns)
                .add(Layouts.leftJustify(searchViewAreaOnly));
    }

    public void layer(final GraphLayer layer)
    {
        if (layer != null)
        {
            this.layer = layer;
            refresh();
        }
    }

    public void refresh()
    {
        if (layer != null && layer.activeLayer() != null)
        {
            final var selectedKey = keysList.selected();
            final var selectedValue = valuesList.selected();

            keysList.elements(Collections.emptyList());
            valuesList.elements(Collections.emptyList());

            final boolean hasTags = graph().hasTags();
            if (hasTags)
            {
                final var bounds = searchViewAreaOnly.isSelected()
                        ? layer.activeLayer().model().bounds()
                        : Rectangle.MAXIMUM;

                final var outer = this;
                final var request = new TagIndexRequest(graph(), bounds, index ->
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        keysLabel.setText(index.keyCount() + " keys");
                        valuesLabel.setText(index.valueCount() + " values");

                        keysList.elements(index.keys());
                        valuesList.elements(Collections.emptyList());

                        if (selectedKey != null)
                        {
                            keysList.select(selectedKey);
                        }
                        if (selectedValue != null)
                        {
                            valuesList.select(selectedValue);
                        }
                    });

                    outer.index = index;
                });

                indexer.index(request);
            }
            else
            {
                keysLabel.setText("0 keys");
                valuesLabel.setText("0 values");
            }

            Components.children(this, component -> component.setVisible(hasTags));
            repaint();
        }
    }

    void updateValues(final String key)
    {
        final var values = index.values(key);
        if (values != null)
        {
            final var selectedValue = valuesList.selected();
            valuesList.elements(values.copy());
            valuesList.select(selectedValue);
            valuesLabel.setText(Message.format("$ values", values.count()));
        }
    }

    private Graph graph()
    {
        return layer.graph();
    }
}
