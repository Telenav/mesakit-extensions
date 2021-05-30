////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.telenav.tdk.josm.plugins.geojson;

import com.telenav.tdk.core.kernel.language.string.Strings;
import com.telenav.tdk.josm.plugins.library.BaseJosmPanel;
import com.telenav.tdk.map.utilities.geojson.GeoJsonFeature;
import com.telenav.tdk.utilities.ui.swing.component.Components;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.*;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

public class GeoJsonPanel extends BaseJosmPanel
{
    private static final long serialVersionUID = -684114141790912097L;

    private final DefaultListModel<GeoJsonFeature> listModel = new DefaultListModel<>();

    private final JList<GeoJsonFeature> list = new JList<>(listModel);

    private final JLabel status = new JLabel();

    private final JTextField search = new JTextField();

    public GeoJsonPanel(final GeoJsonPlugin plugin)
    {
        super(plugin);

        final var panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        final var constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);

        constraints.anchor = GridBagConstraints.NORTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(status, constraints);

        constraints.anchor = GridBagConstraints.NORTH;
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(search, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;

        final var scrollPane = new JScrollPane();
        scrollPane.setViewportView(list);
        panel.add(scrollPane, constraints);

        addNextShortCut();
        addPreviousShortCut();

        list.setAutoscrolls(true);
        list.addMouseListener(new MouseAdapter()
        {

            @Override
            public void mouseClicked(final MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e))
                {
                    zoomToSelected();
                }
            }

            @Override
            public void mousePressed(final MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    popup(e);
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e)
            {
                if (e.isPopupTrigger())
                {
                    popup(e);
                }
            }
        });
        list.addKeyListener(new KeyAdapter()
        {

            @Override
            public void keyReleased(final KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)
                {
                    zoomToSelected();
                }
            }
        });
        search.getDocument().addDocumentListener(new DocumentListener()
        {

            @Override
            public void changedUpdate(final DocumentEvent e)
            {
                refresh();
            }

            @Override
            public void insertUpdate(final DocumentEvent e)
            {
                refresh();
            }

            @Override
            public void removeUpdate(final DocumentEvent e)
            {
                refresh();
            }
        });

        overrideMenuAcceleratorKeys(search);

        final var font = new Font("Helvetica", Font.PLAIN, 12);
        Components.children(panel, component -> component.setFont(font));

        createLayout(panel, false, Collections.emptyList());
    }

    public boolean isVisible(final GeoJsonFeature feature)
    {
        for (var i = 0; i < listModel.size(); i++)
        {
            if (listModel.getElementAt(i).equals(feature))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public GeoJsonLayer layer()
    {
        final var layer = super.layer();
        if (layer instanceof GeoJsonLayer)
        {
            return (GeoJsonLayer) layer;
        }
        return null;
    }

    public void selectFeature(final GeoJsonFeature feature)
    {
        list.setSelectedValue(feature, true);
        list.repaint();
    }

    @Override
    protected void onActiveLayerChanged()
    {
        refresh();
    }

    @Override
    protected void onHide()
    {
        refresh();
    }

    @Override
    protected void onLayerAdded()
    {
        showPanel();
        refresh();
    }

    @Override
    protected void onLayerRemoving(final Layer layer)
    {
        if (layer() == null)
        {
            hidePanel();
        }
        else
        {
            refresh();
        }
    }

    @Override
    protected void onLayerReorder()
    {
        refresh();
    }

    @Override
    protected void onRefresh()
    {

        if (layer() != null)
        {

            // Get the GeoJson document we're showing
            final var document = layer().getDocument();

            // Get features that match the search text
            final List<GeoJsonFeature> matches = new ArrayList<>();
            if (document != null)
            {
                for (final var feature : document)
                {
                    if (Strings.isEmpty(search.getText())
                            || feature.title().toUpperCase().contains(search.getText().toUpperCase()))
                    {
                        matches.add(feature);
                    }
                }
            }

            // If the features have changed
            if (isNewModel(matches))
            {

                // then update the list
                listModel.removeAllElements();
                for (final var match : matches)
                {
                    listModel.addElement(match);
                }
                if (document != null)
                {
                    say("Showing " + matches.size() + " of " + document.size() + " features from "
                            + layer().getFile().fileName());
                }
                list.clearSelection();
                list.repaint();
                repaint();
                if (layer() != null)
                {
                    layer().forceRepaint();
                }
            }
        }
    }

    @Override
    protected void onShow()
    {
        refresh();
    }

    protected void popup(final MouseEvent e)
    {
        list.setSelectedIndex(list.locationToIndex(e.getPoint()));
        final var selectedValue = list.getSelectedValue();
        final var layer = layer();
        if (layer != null)
        {
            layer.zoomToFeature(selectedValue);
        }
        final var menu = new GeoJsonFeaturePopUpMenu(selectedValue);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void addNextShortCut()
    {
        final var shortcutName = "Next GeoJson Feature";
        final var shortcut = Shortcut.registerShortcut(shortcutName, shortcutName, KeyEvent.VK_DOWN,
                Shortcut.ALT_CTRL_SHIFT);
        final var next = new JosmAction(shortcutName, null, shortcutName, shortcut, false)
        {

            private static final long serialVersionUID = 9209000550528190850L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final var index = list.getSelectedIndex();
                if (index + 1 < list.getModel().getSize())
                {
                    list.setSelectedIndex(index + 1);
                }
                zoomToSelected();
            }
        };
        final var menu = MainApplication.getMenu().toolsMenu;
        MainMenu.add(menu, next, MainMenu.WINDOW_MENU_GROUP.VOLATILE);
    }

    private void addPreviousShortCut()
    {
        final var shortcutName = "Previous GeoJson Feature";
        final var shortcut = Shortcut.registerShortcut(shortcutName, shortcutName, KeyEvent.VK_DOWN,
                Shortcut.ALT_CTRL_SHIFT);
        final var previous = new JosmAction(shortcutName, null, shortcutName, shortcut, false)
        {

            private static final long serialVersionUID = 9209000550528190850L;

            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final var index = list.getSelectedIndex();
                if (index - 1 >= 0)
                {
                    list.setSelectedIndex(index - 1);
                }
                zoomToSelected();
            }
        };
        final var menu = MainApplication.getMenu().toolsMenu;
        MainMenu.add(menu, previous, MainMenu.WINDOW_MENU_GROUP.VOLATILE);
    }

    private boolean isNewModel(final List<GeoJsonFeature> features)
    {
        if (listModel.size() == features.size())
        {
            for (var i = 0; i < listModel.size(); i++)
            {
                if (!listModel.getElementAt(i).equals(features.get(i)))
                {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void say(final String message)
    {
        status.setText(message);
    }

    private void zoomToSelected()
    {
        final var layer = layer();
        if (layer != null)
        {
            layer.zoomToFeature(list.getSelectedValue());
        }
    }
}
