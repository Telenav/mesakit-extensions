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

package com.telenav.mesakit.plugins.josm.library;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;

import javax.swing.AbstractAction;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public abstract class BaseJosmPanel extends ToggleDialog
{
    private static final long serialVersionUID = -684114141790912097L;

    private final BaseJosmPlugin plugin;

    private boolean destroyed;

    protected BaseJosmPanel(BaseJosmPlugin plugin)
    {
        super(plugin.name(), plugin.iconName(), plugin.tooltip(), plugin.shortcut(), plugin.preferredPanelHeight());
        this.plugin = plugin;
    }

    @Override
    public void destroy()
    {
        if (!destroyed)
        {
            super.destroy();
            destroyed = true;
            onDestroyed();
        }
    }

    @Override
    public void hideNotify()
    {
        onHide();
    }

    public void hidePanel()
    {
        if (isDialogShowing())
        {
            getToggleAction().actionPerformed(null);
            if (layer() != null)
            {
                layer().forceRepaint();
            }
        }
    }

    public BaseJosmLayer layer()
    {
        return plugin().selectedLayer();
    }

    public void onInitialize()
    {
        MainApplication.getMap().addToggleDialog(this);
        hidePanel();
    }

    public void overrideMenuAcceleratorKeys(JTextField field)
    {
        for (var key = KeyEvent.VK_COMMA; key < KeyEvent.VK_CLOSE_BRACKET; key++)
        {

            // Override VK menu accelerator
            field.getInputMap().put(KeyStroke.getKeyStroke(key, 0), "pressed");
            field.getActionMap().put("pressed", new AbstractAction()
            {

                private static final long serialVersionUID = 6898833815633986680L;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                }
            });

            // Add normal keystroke handler
            var keyChar = (char) key;
            field.getInputMap().put(KeyStroke.getKeyStroke(keyChar), key + "pressed");
            field.getActionMap().put(key + "pressed", new AbstractAction()
            {

                private static final long serialVersionUID = 6898833815633986680L;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    field.replaceSelection(Character.toString(keyChar));
                }
            });
        }
    }

    public BaseJosmPlugin plugin()
    {
        return plugin;
    }

    public final void refresh()
    {
        onRefresh();
    }

    @Override
    public void showNotify()
    {
        onShow();
    }

    public void showPanel()
    {
        if (!isDialogShowing())
        {
            getToggleAction().actionPerformed(null);
            if (layer() != null)
            {
                layer().forceRepaint();
            }
        }
    }

    protected void onActiveLayerChanged()
    {
    }

    @SuppressWarnings("EmptyMethod")
    protected void onDestroyed()
    {
    }

    protected void onHide()
    {
    }

    protected void onLayerAdded()
    {
    }

    protected void onLayerRemoving(Layer layer)
    {
    }

    protected void onLayerReorder()
    {
    }

    protected void onRefresh()
    {
    }

    protected void onShow()
    {
    }
}
