package com.telenav.mesakit.plugins.josm.graph.view.tabs.routing;

import com.telenav.kivakit.ui.desktop.layout.Borders;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;
import com.telenav.mesakit.graph.Vertex;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridLayout;

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
        final var theme = KivaKitTheme.get();
        final var from = theme.newButton("set from vertex");
        final var to = theme.newButton("set to vertex");
        final var route = theme.newButton("find route");

        Borders.applyMargin(this, 16);

        final var layout = new JPanel();
        layout.setLayout(new GridLayout(3, 3, 8, 8));

        fromLabel = theme.newComponentLabel("from-vertex: N/A");
        layout.add(fromLabel);
        layout.add(from);
        layout.add(theme.newComponentLabel(""));

        toLabel = theme.newComponentLabel("to-vertex: N/A");
        layout.add(toLabel);
        layout.add(to);
        layout.add(theme.newComponentLabel(""));

        debug = theme.newCheckBox("debugger");
        layout.add(theme.newComponentLabel(""));
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
