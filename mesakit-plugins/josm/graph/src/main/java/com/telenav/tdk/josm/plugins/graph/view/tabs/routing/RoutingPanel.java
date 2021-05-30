package com.telenav.tdk.josm.plugins.graph.view.tabs.routing;

import com.telenav.tdk.graph.Vertex;
import com.telenav.tdk.josm.plugins.graph.view.GraphPanel;
import com.telenav.tdk.utilities.ui.swing.component.Components;
import com.telenav.tdk.utilities.ui.swing.theme.TdkTheme;

import javax.swing.*;
import java.awt.*;

/**
 * @author jonathanl (shibo)
 */
public class RoutingPanel extends JPanel
{
    private final JLabel fromLabel;

    private final JLabel toLabel;

    private final JCheckBox debug;

    private Vertex from;

    private Vertex to;

    public RoutingPanel(final GraphPanel graphPanel)
    {
        final var theme = TdkTheme.get();
        final var from = theme.newButton("set from vertex");
        final var to = theme.newButton("set to vertex");
        final var route = theme.newButton("find route");

        Components.border(this, 16);

        final var layout = new JPanel();
        layout.setLayout(new GridLayout(3, 3, 8, 8));

        fromLabel = theme.newLabel("from-vertex: N/A");
        layout.add(fromLabel);
        layout.add(from);
        layout.add(theme.newLabel(""));

        toLabel = theme.newLabel("to-vertex: N/A");
        layout.add(toLabel);
        layout.add(to);
        layout.add(theme.newLabel(""));

        debug = theme.newCheckBox("debugger");
        layout.add(theme.newLabel(""));
        layout.add(route);
        layout.add(debug);

        add(layout);

        from.addActionListener(e ->
        {
            this.from = graphPanel.selectedVertex();
            fromLabel.setText("from vertex: " + (this.from == null ? "N/A" : this.from.identifier()));
        });

        to.addActionListener(e ->
        {
            this.to = graphPanel.selectedVertex();
            toLabel.setText("to vertex: " + (this.to == null ? "N/A" : this.to.identifier()));
        });

        route.addActionListener(e -> new Thread(() ->
        {
            if (!graphPanel.layer().route(this.from, this.to,
                    debug.isSelected()))
            {
                graphPanel.say("Couldn't find route");
            }
        }).start());
    }
}
