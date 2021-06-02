package com.telenav.mesakit.plugins.josm.graph.view.tabs.view;

import com.telenav.kivakit.ui.desktop.layout.Borders;
import com.telenav.kivakit.ui.desktop.layout.HorizontalBoxLayout;
import com.telenav.kivakit.ui.desktop.layout.Margins;
import com.telenav.kivakit.ui.desktop.layout.Spacing;
import com.telenav.mesakit.graph.EdgeRelation;
import com.telenav.mesakit.graph.Place;
import com.telenav.mesakit.map.road.model.RoadType;
import com.telenav.mesakit.plugins.josm.graph.view.GraphPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

/**
 * @author jonathanl (shibo)
 */
public class ViewPanel extends JPanel
{
    /**
     * @author jonathanl (shibo)
     */
    @SuppressWarnings("InnerClassMayBeStatic")
    public class Column extends JPanel
    {
        Column()
        {
            final var layout = new BoxLayout(this, BoxLayout.Y_AXIS);
            setLayout(layout);
        }

        public void add(final JComponent header, final JComponent column)
        {
            header.setAlignmentY(Component.TOP_ALIGNMENT);
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(header);

            add(Box.createRigidArea(new Dimension(0, 6)));

            column.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(column);
        }
    }

    /**
     * @author jonathanl (shibo)
     */
    public class EdgesColumn extends Column
    {
        EdgesColumn()
        {
            final var outer = ViewPanel.this;
            outer.viewEdges = new ViewCheckBox(graphPanel, "edges");

            final var values = RoadType.values();
            Arrays.sort(values, Comparator.comparing(Enum::name));
            outer.viewRoadTypes = new ViewMultiSelectList<>(graphPanel, values);
            outer.viewRoadTypes.getSelectionModel().setSelectionInterval(0, values.length - 1);
            final var scroll = new JScrollPane(outer.viewRoadTypes, VERTICAL_SCROLLBAR_ALWAYS,
                    HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(190, 100));
            scroll.setMinimumSize(new Dimension(150, 150));

            add(outer.viewEdges, scroll);
        }
    }

    /**
     * @author jonathanl (shibo)
     */
    public class PlacesColumn extends Column
    {
        PlacesColumn()
        {
            final var outer = ViewPanel.this;
            outer.viewPlaces = new ViewCheckBox(graphPanel, "places");

            final var values = Place.Type.values();
            Arrays.sort(values, Comparator.comparing(Enum::name));
            outer.viewPlaceTypes = new ViewMultiSelectList<>(graphPanel, values);
            outer.viewPlaceTypes.getSelectionModel().setSelectionInterval(0, values.length - 1);
            final var scroll = new JScrollPane(outer.viewPlaceTypes, VERTICAL_SCROLLBAR_ALWAYS,
                    HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(190, 100));
            scroll.setMinimumSize(new Dimension(150, 150));

            add(outer.viewPlaces, scroll);
        }
    }

    /**
     * @author jonathanl (shibo)
     */
    public class RelationsColumn extends Column
    {
        RelationsColumn()
        {
            final var outer = ViewPanel.this;
            outer.viewRelations = new ViewCheckBox(graphPanel, "relations");

            final var values = EdgeRelation.Type.values();
            Arrays.sort(values, Comparator.comparing(Enum::name));
            outer.viewRelationTypes = new ViewMultiSelectList<>(graphPanel, values);
            outer.viewRelationTypes.getSelectionModel().setSelectionInterval(0, values.length - 1);
            final var scroll = new JScrollPane(outer.viewRelationTypes, VERTICAL_SCROLLBAR_ALWAYS,
                    HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(190, 100));
            scroll.setMinimumSize(new Dimension(150, 150));

            add(outer.viewRelations, scroll);
        }
    }

    private final GraphPanel graphPanel;

    private ViewCheckBox viewEdges;

    private ViewCheckBox viewRelations;

    private ViewCheckBox viewPlaces;

    private ViewMultiSelectList<EdgeRelation.Type> viewRelationTypes;

    private ViewMultiSelectList<RoadType> viewRoadTypes;

    private ViewMultiSelectList<Place.Type> viewPlaceTypes;

    public ViewPanel(final GraphPanel graphPanel)
    {
        this.graphPanel = graphPanel;

        Borders.insideMarginsOf(Margins.of(8)).apply(this);

        new HorizontalBoxLayout(this, Spacing.AUTOMATIC_SPACING)
                .add(new EdgesColumn())
                .add(new RelationsColumn())
                .add(new PlacesColumn());
    }

    public boolean viewAllEdges()
    {
        return viewEdges.isSelected() && viewRoadTypes().size() == RoadType.values().length;
    }

    public boolean viewEdges()
    {
        return viewEdges.isSelected();
    }

    public List<Place.Type> viewPlaceTypes()
    {
        if (viewPlaceTypes.getModel().getSize() > 0)
        {
            return viewPlaceTypes.getSelectedValuesList();
        }
        return Collections.emptyList();
    }

    public boolean viewPlaces()
    {
        return viewPlaces.isSelected();
    }

    public List<EdgeRelation.Type> viewRelationTypes()
    {
        if (viewRelationTypes.getModel().getSize() > 0)
        {
            return viewRelationTypes.getSelectedValuesList();
        }
        return Collections.emptyList();
    }

    public boolean viewRelations()
    {
        return viewRelations.isSelected();
    }

    public List<RoadType> viewRoadTypes()
    {
        if (viewRoadTypes.getModel().getSize() > 0)
        {
            return viewRoadTypes.getSelectedValuesList();
        }
        return Collections.emptyList();
    }
}
