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

package com.telenav.mesakit.plugins.josm.graph.model;

import com.telenav.kivakit.core.language.Objects;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingPoint;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.EdgeRelation;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.GraphElement;
import com.telenav.mesakit.graph.Place;
import com.telenav.mesakit.graph.ShapePoint;
import com.telenav.mesakit.graph.Vertex;
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.graph.matching.snapping.GraphSnapper;
import com.telenav.mesakit.map.geography.Location;
import com.telenav.mesakit.map.geography.shape.polyline.Polyline;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import org.openstreetmap.josm.gui.layer.Layer;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds information about the currently selected objects and the stack of objects beneath the selection. Various
 * isSelected() methods can be used to determine if an object is selected. The selection can be changed with the
 * select() methods. The shapes of rendered objects can be stored with the shape() methods and hit testing is performed
 * with the *ForPoint(Point2D) methods.
 *
 * @author jonathanl (shibo)
 */
public class Selection
{
    public enum Type
    {
        SELECTED,
        UNSELECTED,
        INACTIVE,
        HIGHLIGHTED,
        RESTRICTED
    }

    // Selected layer
    private Layer selectedLayer;

    // Selected objects
    private Vertex selectedVertex;

    private Edge selectedEdge;

    private EdgeRelation selectedRelation;

    private Place selectedPlace;

    private ShapePoint selectedShapePoint;

    private Set<Polyline> selectedPolylines = new HashSet<>();

    // Highlighted objects
    private EdgeSet highlightedEdges = new EdgeSet(Estimate._1024);

    // Stacks of overlapping selected objects
    private List<Vertex> selectedVertexStack = new ArrayList<>();

    private List<Edge> selectedEdgeStack = new ArrayList<>();

    private List<EdgeRelation> selectedRelationStack = new ArrayList<>();

    // Maps from each shape as drawn on the canvas to the corresponding object
    private final Map<Shape, Vertex> vertexForShape = new HashMap<>();

    private final Map<Shape, Edge> edgeForShape = new HashMap<>();

    private final Map<Edge, Shape> shapeForEdge = new HashMap<>();

    private final Map<Shape, EdgeRelation> relationForShape = new HashMap<>();

    private final Map<Shape, Location> shapePointLocationForShape = new HashMap<>();

    private final Map<Shape, Place> placeForShape = new HashMap<>();

    private final Map<Shape, ShapePoint> shapeToShapePoint = new HashMap<>();

    public void clear()
    {
        selectedVertex = null;
        selectedEdge = null;
        selectedRelation = null;
        selectedPlace = null;
        selectedShapePoint = null;
        selectedVertexStack.clear();
        selectedEdgeStack.clear();
        selectedRelationStack.clear();
    }

    public void clearPolylines()
    {
        selectedPolylines.clear();
        highlightedEdges.clear();
    }

    public void clearShapes()
    {
        vertexForShape.clear();
        edgeForShape.clear();
        shapeForEdge.clear();
        relationForShape.clear();
        shapePointLocationForShape.clear();
        shapeToShapePoint.clear();
        placeForShape.clear();
    }

    public List<Edge> edgesForPoint(MapCanvas canvas,
                                    Graph graph,
                                    DrawingPoint point,
                                    boolean snapToNearby)
    {
        List<Edge> edges = new ArrayList<>();
        for (var entry : edgeForShape.entrySet())
        {
            var key = entry.getKey();
            if (key != null && key.contains(point.asAwt()))
            {
                var value = entry.getValue();
                if (value != null)
                {
                    edges.add(value);
                }
            }
        }
        if (!edges.isEmpty())
        {
            return edges;
        }
        if (snapToNearby)
        {
            var snap = new GraphSnapper(graph, Distance.meters(200), null, Edge.TransportMode.ANY)
                    .snap(canvas.toMap(point), null);
            if (snap != null)
            {
                edges.add(snap.closestEdge());
            }
        }
        return edges;
    }

    public void highlight(EdgeSet edges)
    {
        highlightedEdges = edges;
    }

    public EdgeSet highlightedEdges()
    {
        return highlightedEdges;
    }

    public boolean is(Edge edge, Type type)
    {
        switch (type)
        {
            case HIGHLIGHTED:
                return isHighlighted(edge);

            case SELECTED:
                return isSelected(edge);

            case UNSELECTED:
                return !isSelected(edge);

            case INACTIVE:
                return true;

            default:
                return false;
        }
    }

    public boolean is(Type type, EdgeRelation relation)
    {
        switch (type)
        {
            case SELECTED:
                return isSelected(relation);

            case UNSELECTED:
                return !isSelected(relation);

            case HIGHLIGHTED:

            default:
                return false;
        }
    }

    public boolean is(Type type, Place place)
    {
        return (type == Type.SELECTED) == isSelected(place);
    }

    public boolean is(Type type, Polyline line)
    {
        return (type == Type.SELECTED) == isSelected(line);
    }

    public boolean isHighlighted(Edge edge)
    {
        return highlightedEdges.contains(edge);
    }

    public boolean isSelected(Edge edge)
    {
        return Objects.isEqual(edge, selectedEdge);
    }

    public boolean isSelected(EdgeRelation relation)
    {
        return Objects.isEqual(relation, selectedRelation);
    }

    public boolean isSelected(Layer layer)
    {
        return Objects.isEqual(layer, selectedLayer);
    }

    public boolean isSelected(Place place)
    {
        return Objects.isEqual(place, selectedPlace);
    }

    public boolean isSelected(Polyline polyline)
    {
        return selectedPolylines.contains(polyline);
    }

    public boolean isSelected(ShapePoint shapePoint)
    {
        return Objects.isEqual(shapePoint, selectedShapePoint);
    }

    public boolean isSelected(Vertex vertex)
    {
        return Objects.isEqual(vertex, selectedVertex);
    }

    public GraphElement next()
    {
        if (selectedVertexStack.size() > 1)
        {
            var index = selectedVertexStack.indexOf(selectedVertex);
            if (index != -1)
            {
                return select(selectedVertexStack.get((index + 1) % selectedVertexStack.size()));
            }
        }
        if (selectedEdgeStack.size() > 1)
        {
            var index = selectedEdgeStack.indexOf(selectedEdge);
            if (index != -1)
            {
                return select(selectedEdgeStack.get((index + 1) % selectedEdgeStack.size()));
            }
        }
        if (selectedRelationStack.size() > 1)
        {
            var index = selectedRelationStack.indexOf(selectedRelation);
            if (index != -1)
            {
                return select(selectedRelationStack.get((index + 1) % selectedRelationStack.size()));
            }
        }
        return null;
    }

    public Place placeForPoint(Point2D point)
    {
        for (var entry : placeForShape.entrySet())
        {
            if (entry.getKey().contains(point))
            {
                return entry.getValue();
            }
        }
        return null;
    }

    public GraphElement previous()
    {
        if (selectedVertexStack.size() > 1)
        {
            var index = selectedVertexStack.indexOf(selectedVertex);
            if (index != -1)
            {
                return select(selectedVertexStack.get(Math.abs(index - 1) % selectedVertexStack.size()));
            }
        }
        if (selectedEdgeStack.size() > 1)
        {
            var index = selectedEdgeStack.indexOf(selectedEdge);
            if (index != -1)
            {
                return select(selectedEdgeStack.get(Math.abs(index - 1) % selectedEdgeStack.size()));
            }
        }
        if (selectedRelationStack.size() > 1)
        {
            var index = selectedRelationStack.indexOf(selectedRelation);
            if (index != -1)
            {
                return select(selectedRelationStack.get((index - 1) % selectedRelationStack.size()));
            }
        }
        return null;
    }

    public List<EdgeRelation> relationsForPoint(Point2D point)
    {
        List<EdgeRelation> relations = new ArrayList<>();
        for (var entry : relationForShape.entrySet())
        {
            if (entry.getKey().contains(point))
            {
                relations.add(entry.getValue());
            }
        }
        return relations;
    }

    public boolean sameEdgeStack(List<Edge> edges)
    {
        return new HashSet<>(selectedEdgeStack).equals(new HashSet<>(edges));
    }

    public boolean sameRelationStack(List<EdgeRelation> relations)
    {
        return new HashSet<>(selectedRelationStack).equals(new HashSet<>(relations));
    }

    public boolean sameVertexStack(List<Vertex> vertexes)
    {
        return new HashSet<>(selectedVertexStack).equals(new HashSet<>(vertexes));
    }

    public Edge select(Edge edge)
    {
        clear();
        selectedEdge = edge;
        return edge;
    }

    public EdgeRelation select(EdgeRelation selectedEdgeRelation)
    {
        clear();
        selectedRelation = selectedEdgeRelation;
        return selectedEdgeRelation;
    }

    public void select(Layer selectedLayer)
    {
        clear();
        this.selectedLayer = selectedLayer;
    }

    public void select(Place selectedPlace)
    {
        clear();
        this.selectedPlace = selectedPlace;
    }

    public void select(Polyline line)
    {
        selectedPolylines.add(line);
    }

    public void select(Set<Polyline> polylines)
    {
        clear();
        selectedPolylines = polylines;
    }

    public void select(ShapePoint selectedShapePoint)
    {
        clear();
        this.selectedShapePoint = selectedShapePoint;
    }

    public Vertex select(Vertex selectedVertex)
    {
        clear();
        this.selectedVertex = selectedVertex;
        return selectedVertex;
    }

    public Edge selectedEdge()
    {
        return selectedEdge;
    }

    public void selectedEdgeStack(List<Edge> edges)
    {
        selectedEdgeStack = edges;
    }

    public Set<Polyline> selectedPolylines()
    {
        return selectedPolylines;
    }

    public EdgeRelation selectedRelation()
    {
        return selectedRelation;
    }

    public void selectedRelationStack(List<EdgeRelation> relations)
    {
        selectedRelationStack = relations;
    }

    public ShapePoint selectedShapePoint()
    {
        return selectedShapePoint;
    }

    public Vertex selectedVertex()
    {
        return selectedVertex;
    }

    public void selectedVertexStack(List<Vertex> vertexes)
    {
        selectedVertexStack = vertexes;
    }

    public Shape shape(Edge edge)
    {
        return shapeForEdge.get(edge);
    }

    public void shape(Edge edge, Shape shape)
    {
        edgeForShape.put(shape, edge);
        shapeForEdge.put(edge, shape);
    }

    public void shape(EdgeRelation relation, Shape shape)
    {
        relationForShape.put(shape, relation);
    }

    public void shape(Place place, Shape shape)
    {
        placeForShape.put(shape, place);
    }

    public void shape(ShapePoint point, Shape shape)
    {
        shapePointLocationForShape.put(shape, point.location());
        shapeToShapePoint.put(shape, point);
    }

    public void shape(Vertex vertex, Shape shape)
    {
        vertexForShape.put(shape, vertex);
    }

    public ShapePoint shapePointForPoint(Graph graph, Point2D point)
    {
        for (var entry : shapePointLocationForShape.entrySet())
        {
            if (entry.getKey().contains(point))
            {
                return shapeToShapePoint.get(entry.getKey());
            }
        }
        return null;
    }

    public List<Vertex> vertexesForPoint(Point2D point)
    {
        List<Vertex> vertexes = new ArrayList<>();
        for (var entry : vertexForShape.entrySet())
        {
            if (entry.getKey().contains(point))
            {
                vertexes.add(entry.getValue());
            }
        }
        return vertexes;
    }
}
