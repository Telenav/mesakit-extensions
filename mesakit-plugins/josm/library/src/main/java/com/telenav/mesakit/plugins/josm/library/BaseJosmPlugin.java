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

import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Shortcut;

import static com.telenav.kivakit.ensure.Ensure.ensureNotNull;

public abstract class BaseJosmPlugin extends Plugin
{
    // The MesaKit Graph panel
    private BaseJosmPanel panel;

    // The type of layer class
    private final Class<? extends BaseJosmLayer> layerClass;

    protected BaseJosmPlugin(PluginInformation info, Class<? extends BaseJosmLayer> layerClass)
    {
        super(info);
        this.layerClass = ensureNotNull(layerClass);
        MainApplication.getLayerManager().addActiveLayerChangeListener(event -> activeLayerChanged(event.getPreviousActiveLayer()));
        MainApplication.getLayerManager().addLayerChangeListener(new LayerManager.LayerChangeListener()
        {
            @Override
            public void layerAdded(LayerManager.LayerAddEvent event)
            {
                addedLayer(event.getAddedLayer());
            }

            @Override
            public void layerOrderChanged(LayerManager.LayerOrderChangeEvent event)
            {
                reorderedLayers();
            }

            @Override
            public void layerRemoving(LayerManager.LayerRemoveEvent event)
            {
                removingLayer(event.getRemovedLayer());
            }
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    public BaseJosmLayer createLayer()
    {
        return createLayer(name());
    }

    public BaseJosmLayer createLayer(String name)
    {
        if (isMultiLayer() || selectedLayer() == null)
        {
            return newLayer(name);
        }
        return null;
    }

    public void destroyLayer()
    {
        if (selectedLayer() != null)
        {
            selectedLayer().onDestroy();
        }
    }

    @SuppressWarnings("SameReturnValue")
    public abstract String iconName();

    public boolean isLayerOfPlugin(Layer layer)
    {
        return layer != null && layerClass.isAssignableFrom(layer.getClass());
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame)
    {
        if (newFrame != null && oldFrame == null)
        {
            panel().onInitialize();
        }
        if (oldFrame != null && newFrame == null)
        {
            if (panel != null)
            {
                panel = null;
            }
        }
    }

    public MapView mapView()
    {
        if (MainApplication.getMap() != null)
        {
            return MainApplication.getMap().mapView;
        }
        return null;
    }

    public abstract String name();

    public BaseJosmPanel panel()
    {
        if (panel == null)
        {
            panel = newPanel();
            panel.hidePanel();
        }
        return panel;
    }

    public BaseJosmLayer selectedLayer()
    {
        var layer = MainApplication.getLayerManager().getActiveLayer();
        if (layer != null && layerClass.isAssignableFrom(layer.getClass()))
        {
            return (BaseJosmLayer) layer;
        }
        return null;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public Shortcut shortcut()
    {
        return Shortcut.registerShortcut("subwindow:" + name().toLowerCase(), "Toggle: " + name() + " features",
                shortCutKey(), Shortcut.CTRL_SHIFT);
    }

    public String tooltip()
    {
        return name() + " Plugin";
    }

    public String userName()
    {
        return CredentialsManager.getInstance().getUsername();
    }

    public void zoomTo(Rectangle bounds)
    {
        if (mapView() != null && bounds != null)
        {
            mapView().zoomTo(new Bounds(bounds.bottom().asDegrees(), bounds.left().asDegrees(),
                    bounds.top().asDegrees(), bounds.right().asDegrees()));
        }
    }

    @SuppressWarnings("SameReturnValue")
    protected boolean isMultiLayer()
    {
        return true;
    }

    protected abstract BaseJosmLayer newLayer(String name);

    protected abstract BaseJosmPanel newPanel();

    @SuppressWarnings("EmptyMethod")
    protected void onActiveLayerChanged()
    {
    }

    @SuppressWarnings("EmptyMethod")
    protected void onLayerAdded()
    {
    }

    @SuppressWarnings("EmptyMethod")
    protected void onLayerRemoving(Layer layer)
    {
    }

    @SuppressWarnings("SameReturnValue")
    protected int preferredPanelHeight()
    {
        return 150;
    }

    protected abstract int shortCutKey();

    private void activeLayerChanged(Layer layer)
    {
        if (isLayerOfPlugin(layer))
        {
            onActiveLayerChanged();
            if (panel() != null)
            {
                panel().onActiveLayerChanged();
            }
        }
    }

    private void addedLayer(Layer layer)
    {
        if (isLayerOfPlugin(layer))
        {
            onLayerAdded();
            if (panel() != null)
            {
                panel().onLayerAdded();
            }
        }
    }

    private void removingLayer(Layer layer)
    {
        if (isLayerOfPlugin(layer))
        {
            onLayerRemoving(layer);
            if (panel() != null)
            {
                panel().onLayerRemoving(layer);
            }
        }
    }

    private void reorderedLayers()
    {
        if (panel() != null)
        {
            panel().onLayerReorder();
        }
    }
}
