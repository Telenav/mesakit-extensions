package com.telenav.mesakit.plugins.josm.graph.view.tabs.tags;

import com.telenav.kivakit.core.messaging.MessageFormat;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.ui.desktop.component.Components;
import com.telenav.kivakit.ui.desktop.component.searchlist.SearchList;
import com.telenav.kivakit.ui.desktop.layout.Borders;
import com.telenav.kivakit.ui.desktop.layout.HorizontalBoxLayout;
import com.telenav.kivakit.ui.desktop.layout.Layouts;
import com.telenav.kivakit.ui.desktop.layout.Margins;
import com.telenav.kivakit.ui.desktop.layout.Spacing;
import com.telenav.kivakit.ui.desktop.layout.VerticalBoxLayout;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.plugins.josm.graph.view.GraphLayer;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.tags.indexing.TagIndex;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.tags.indexing.TagIndexRequest;
import com.telenav.mesakit.plugins.josm.graph.view.tabs.tags.indexing.TagIndexer;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
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

    public TagPanel(GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;

        indexer.addListener(message -> graphPanel.status(message.formatted(MessageFormat.WITH_EXCEPTION)));

        Borders.insideMarginsOf(Margins.of(10)).apply(this);

        searchViewAreaOnly = KivaKitTheme.get().newCheckBox("Index tags in view area only");
        searchViewAreaOnly.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
        searchViewAreaOnly.addActionListener(event -> refresh());
        searchViewAreaOnly.setSelected(true);

        var columns = new JPanel();

        new HorizontalBoxLayout(columns, Spacing.AUTOMATIC_SPACING)
                .add(new KeysColumn(this))
                .add(new ValuesColumn(this));

        new VerticalBoxLayout(this)
                .add(columns)
                .add(Layouts.leftJustify(searchViewAreaOnly));
    }

    public void layer(GraphLayer layer)
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
            var selectedKey = keysList.selected();
            var selectedValue = valuesList.selected();

            keysList.elements(Collections.emptyList());
            valuesList.elements(Collections.emptyList());

            boolean hasTags = graph().hasTags();
            if (hasTags)
            {
                var bounds = searchViewAreaOnly.isSelected()
                        ? layer.activeLayer().model().bounds()
                        : Rectangle.MAXIMUM;

                var outer = this;
                var request = new TagIndexRequest(graph(), bounds, index ->
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

    void updateValues(String key)
    {
        var values = index.values(key);
        if (values != null)
        {
            var selectedValue = valuesList.selected();
            valuesList.elements(values.copy());
            valuesList.select(selectedValue);
            valuesLabel.setText(Strings.format("$ values", values.count()));
        }
    }

    private Graph graph()
    {
        return layer.graph();
    }
}
