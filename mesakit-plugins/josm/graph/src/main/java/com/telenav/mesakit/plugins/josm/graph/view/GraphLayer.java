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

package com.telenav.mesakit.plugins.josm.graph.view;

import com.telenav.kivakit.core.language.collections.list.StringList;
import com.telenav.kivakit.core.language.progress.ProgressReporter;
import com.telenav.kivakit.language.time.PreciseDuration;
import com.telenav.kivakit.language.count.Count;
import com.telenav.kivakit.language.level.Percent;
import com.telenav.kivakit.messaging.logging.Logger;
import com.telenav.kivakit.messaging.logging.LoggerFactory;
import com.telenav.kivakit.messaging.Message;
import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingPoint;
import com.telenav.kivakit.ui.desktop.graphics.drawing.geometry.objects.DrawingRectangle;
import com.telenav.mesakit.graph.Edge;
import com.telenav.mesakit.graph.EdgeRelation;
import com.telenav.mesakit.graph.Graph;
import com.telenav.mesakit.graph.GraphElement;
import com.telenav.mesakit.graph.Place;
import com.telenav.mesakit.graph.Route;
import com.telenav.mesakit.graph.ShapePoint;
import com.telenav.mesakit.graph.Vertex;
import com.telenav.mesakit.graph.collections.EdgeSet;
import com.telenav.mesakit.graph.identifiers.EdgeIdentifier;
import com.telenav.mesakit.graph.identifiers.VertexIdentifier;
import com.telenav.mesakit.graph.io.load.GraphConstraints;
import com.telenav.mesakit.graph.map.MapEdgeIdentifier;
import com.telenav.mesakit.map.data.formats.library.map.identifiers.MapNodeIdentifier;
import com.telenav.mesakit.map.data.formats.library.map.identifiers.MapRelationIdentifier;
import com.telenav.mesakit.map.data.formats.pbf.model.identifiers.PbfWayIdentifier;
import com.telenav.mesakit.map.geography.shape.polyline.Polyline;
import com.telenav.mesakit.map.geography.shape.rectangle.Rectangle;
import com.telenav.mesakit.map.measurements.geographic.Angle;
import com.telenav.mesakit.map.measurements.geographic.Distance;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapCanvas;
import com.telenav.mesakit.map.ui.desktop.graphics.canvas.MapScale;
import com.telenav.mesakit.navigation.routing.RoutingDebugger;
import com.telenav.mesakit.navigation.routing.RoutingRequest;
import com.telenav.mesakit.navigation.routing.bidijkstra.BiDijkstraRouter;
import com.telenav.mesakit.navigation.routing.bidijkstra.BiDijkstraRoutingRequest;
import com.telenav.mesakit.navigation.routing.cost.functions.heuristic.SpeedCostFunction;
import com.telenav.mesakit.navigation.routing.debuggers.SwingRoutingDebugger;
import com.telenav.mesakit.navigation.routing.limiters.CpuTimeRoutingLimiter;
import com.telenav.mesakit.plugins.josm.graph.GraphPlugin;
import com.telenav.mesakit.plugins.josm.graph.model.Selection;
import com.telenav.mesakit.plugins.josm.graph.model.ViewModel;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.coordinates.JosmCoordinateMapper;
import com.telenav.mesakit.plugins.josm.graph.view.graphics.renderers.VisibleEdges;
import com.telenav.mesakit.plugins.josm.library.BaseJosmLayer;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import static com.telenav.kivakit.interfaces.string.Stringable.Format.HTML;
import static com.telenav.mesakit.map.road.model.RoadFunctionalClass.FIRST_CLASS;
import static com.telenav.mesakit.map.road.model.RoadFunctionalClass.MAIN;

/**
 * Handles painting by rendering the graph's {@link ViewModel} on the {@link MapCanvas} using a {@link
 * GraphLayerRenderer} to do the actual work. Besides the loaded graph, two other decimated graphs are maintained for
 * display when zoomed out. This makes the redraw more efficient.
 *
 * @author jonathanl (shibo)
 */
public class GraphLayer extends BaseJosmLayer
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    public enum Show
    {
        HIGHLIGHT_ONLY,
        HIGHLIGHT_AND_ZOOM_TO
    }

    private MapCanvas canvas;

    private Graph graph;

    private Timer indexingTimer = new Timer();

    private Bounds lastPaintBounds;

    private final ViewModel model;

    private Graph region;

    private Graph state;

    public GraphLayer(GraphPlugin plugin, String name)
    {
        super(plugin, name);
        model = new ViewModel(panel());
    }

    @Override
    public GraphLayer activeLayer()
    {
        var layer = super.activeLayer();
        if (layer instanceof GraphLayer)
        {
            return (GraphLayer) layer;
        }
        return null;
    }

    public void graph(Graph graph, ProgressReporter reporter)
    {
        this.graph = graph;
        if (!graph.isComposite() && graph.edgeCount().isGreaterThan(Count._0))
        {
            region = graph.createConstrained(new GraphConstraints()
                    .withEdgeMatcher((edge) -> edge.roadFunctionalClass().isMoreImportantThanOrEqual(FIRST_CLASS))
                    .withEdgeRelationMatcher((relation) -> false));
            if (region != null)
            {
                region = region.createDecimated(Distance.meters(500), Angle.degrees(10), reporter);
                region.graphStore().bounds(graph.bounds());
                region.name(graph.name());
                GraphLayer.LOGGER.information("Created '$':\n$", region.name(), region.metadata());

                state = region.createConstrained(new GraphConstraints()
                        .withEdgeMatcher((edge) -> edge.roadFunctionalClass().isMoreImportantThanOrEqual(MAIN))
                        .withEdgeRelationMatcher((relation) -> false));
                if (state != null)
                {
                    state = state.createDecimated(Distance.meters(1000), Angle.degrees(20), reporter);
                    state.graphStore().bounds(graph.bounds());
                    state.name(graph.name());
                    GraphLayer.LOGGER.information("Created '$':\n$", state.name(), state.metadata());
                }
            }
        }
    }

    public Graph graph()
    {
        return graph;
    }

    public ViewModel model()
    {
        return model;
    }

    @Override
    public synchronized void onPaint(Graphics2D graphics, MapView mapView, Bounds bounds)
    {
        if (!bounds.equals(lastPaintBounds))
        {
            indexingTimer.cancel();
            indexingTimer.purge();
            indexingTimer = new Timer();
            TimerTask indexingTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    panel().tagPanel().refresh();
                }
            };
            indexingTimer.schedule(indexingTask, 1_000);

            lastPaintBounds = bounds;
        }

        model().activeLayer(activeLayer() == this);

        if (graph() != null)
        {
            var paintArea = DrawingRectangle.pixels(0, 0, mapView.getWidth(), mapView.getHeight());
            canvas = MapCanvas.canvas("graph-layer", graphics, MapScale.of(mapView.getScale()), paintArea, new JosmCoordinateMapper(mapView));

            model().bounds(bounds().expanded(Percent.of(5)));
            model().graph(graph);

            switch (canvas.scale())
            {
                case STATE:
                    if (state != null)
                    {
                        model().graph(state);
                    }
                    break;

                case REGION:
                    if (region != null)
                    {
                        model().graph(region);
                    }
                    break;

                default:
                    break;
            }

            model().visibleEdges(new VisibleEdges(canvas, model(), VisibleEdges.Mode.EDGES));

            new GraphLayerRenderer(model).paint(canvas);
        }
    }

    public void onReady()
    {
        panel().status("Ready");
    }

    @Override
    public GraphPanel panel()
    {
        return (GraphPanel) super.panel();
    }

    public boolean route(Vertex start, Vertex end, boolean visualDebug)
    {
        if (start != null && end != null)
        {
            var router = new BiDijkstraRouter(new SpeedCostFunction());
            RoutingRequest request = new BiDijkstraRoutingRequest(start, end)
                    .withLimiter(new CpuTimeRoutingLimiter(PreciseDuration.seconds(5)))
                    .withDebugger(visualDebug ? new SwingRoutingDebugger("routing") : RoutingDebugger.NULL);
            var result = router.findRoute(request);
            if (result != null)
            {
                var route = result.route();
                if (route != null)
                {
                    var polyline = route.polyline();
                    selection().select(polyline);
                    html("Found $ $-edge route in $:\n\n    $\n\n    $", route.length(), route.size(),
                            result.elapsed(), route.toString("\n    "), route);
                    return true;
                }
            }
        }
        else
        {
            panel().status("A start and end vertex must be chosen before routing");
        }
        return false;
    }

    public void show(PbfWayIdentifier identifier)
    {
        if (graph().contains(identifier))
        {
            var route = graph().routeForWayIdentifier(identifier);
            if (route != null)
            {
                show(route);
            }
        }
    }

    public void show(Polyline polyline)
    {
        select(polyline);
        zoomTo(polyline.bounds());
    }

    public void show(Route route)
    {
        selection().clear();
        selection().highlight(route.asEdgeSet());
        zoomTo(route.bounds());
    }

    public void show(EdgeIdentifier identifier)
    {
        if (graph().contains(identifier))
        {
            var edge = graph().edgeForIdentifier(identifier);
            if (edge != null)
            {
                select(edge);
                zoomInOn(edge);
            }
        }
    }

    public boolean show(MapEdgeIdentifier identifier)
    {
        var edge = graph().edgeForIdentifier(identifier);
        if (edge != null)
        {
            select(edge);
            zoomInOn(edge);
            return true;
        }
        return false;
    }

    public void show(MapNodeIdentifier identifier)
    {
        if (graph().contains(identifier))
        {
            var vertex = graph().vertexForNodeIdentifier(identifier);
            if (vertex != null)
            {
                show(vertex.identifier());
            }
        }
    }

    public void show(MapRelationIdentifier identifier)
    {
        if (graph().contains(identifier))
        {
            var relation = graph().relationForMapRelationIdentifier(identifier);
            if (relation != null)
            {
                show(relation);
            }
        }
    }

    public void show(VertexIdentifier identifier)
    {
        if (graph().contains(identifier))
        {
            var vertex = graph().vertexForIdentifier(identifier);
            if (vertex != null)
            {
                select(vertex);
                zoomTo(vertex.location().within(Distance.meters(30)));
            }
        }
    }

    public void show(EdgeSet edges, Show show)
    {
        selection().highlight(edges);
        if (show == Show.HIGHLIGHT_AND_ZOOM_TO)
        {
            zoomTo(edges.bounds());
        }
        forceRepaint();
    }

    public void zoomTo(Rectangle bounds)
    {
        if (bounds != null)
        {
            var expanded = bounds.expanded(Percent.of(10));
            if (expanded.widthAtBase().isLessThan(Distance._100_METERS))
            {
                expanded = expanded.expandedLeft(Distance.meters(50));
                expanded = expanded.expandedRight(Distance.meters(50));
            }
            if (expanded.heightAsDistance().isLessThan(Distance._100_METERS))
            {
                expanded = expanded.expandedTop(Distance.meters(50));
                expanded = expanded.expandedBottom(Distance.meters(50));
            }
            panel().plugin().zoomTo(expanded);
        }
    }

    @Override
    protected void onNextSelection()
    {
        nextSelection();
    }

    @Override
    protected void onPreviousSelection()
    {
        previousSelection();
    }

    @Override
    protected boolean onSelect(MouseEvent event)
    {
        if (activeLayer() == this)
        {
            if (clickVertex(event))
            {
                return true;
            }
            if (clickShapePoint(event))
            {
                return true;
            }
            if (clickEdge(event, false))
            {
                return true;
            }
            if (clickEdgeRelation(event))
            {
                return true;
            }
            if (clickPlace(event))
            {
                return true;
            }
            return clickEdge(event, true);
        }
        else
        {
            if (event.getClickCount() == 2)
            {
                for (var layer : MainApplication.getLayerManager().getLayers())
                {
                    if (layer instanceof GraphLayer)
                    {
                        var graphLayer = (GraphLayer) layer;
                        var location = canvas.toMap(event.getPoint());
                        if (graphLayer.graph().bounds().contains(location))
                        {
                            MainApplication.getLayerManager().setActiveLayer(layer);
                            MainApplication.getLayerManager().moveLayer(layer, 0);
                        }
                        graphLayer.forceRepaint();
                    }
                }
            }
        }
        return false;
    }

    void show(EdgeRelation relation)
    {
        if (relation != null && relation.bounds() != null)
        {
            select(relation);
            zoomInOn(relation);
        }
    }

    private void clearSelection()
    {
        selection().clear();
        html("");
    }

    private boolean clickEdge(MouseEvent event, boolean snap)
    {
        var edges = selection().edgesForPoint(canvas, model().graph(), DrawingPoint.point(event.getPoint()), snap);
        var selected = false;
        if (!edges.isEmpty())
        {
            if (!selection().sameEdgeStack(edges))
            {
                if (canvas != null && canvas.mapWidth().isLessThan(Distance.miles(50)) && !edges.isEmpty())
                {
                    var edge = edges.get(0);
                    select(edge);
                    if (edge.isTwoWay())
                    {
                        selection().shape(edge.reversed(), selection().shape(edge));
                        edges.add(edge.reversed());
                    }
                    selected = true;
                }
                else
                {
                    clearSelection();
                }
            }
            else
            {
                nextSelection();
                selected = true;
            }
        }

        selection().selectedEdgeStack(edges);
        forceRepaint();
        return selected;
    }

    private boolean clickEdgeRelation(MouseEvent event)
    {
        var selected = false;
        if (!event.isPopupTrigger())
        {
            var relations = selection().relationsForPoint(event.getPoint());
            if (!relations.isEmpty())
            {
                if (!selection().sameRelationStack(relations))
                {
                    if (canvas != null && canvas.mapWidth().isLessThan(Distance.miles(50))
                            && !relations.isEmpty())
                    {
                        select(relations.get(0));
                        selected = true;
                    }
                    else
                    {
                        clearSelection();
                    }
                }
                else
                {
                    nextSelection();
                    selected = true;
                }
            }
            selection().selectedRelationStack(relations);
        }
        if (selected)
        {
            selectSearchTab();
        }
        return selected;
    }

    private boolean clickPlace(MouseEvent event)
    {
        var place = selection().placeForPoint(event.getPoint());
        var exactlyOnEdge = !selection().edgesForPoint(canvas, graph, DrawingPoint.point(event.getPoint()), false).isEmpty();
        if (place != null && !exactlyOnEdge)
        {
            select(place);
            selectSearchTab();
            return true;
        }
        return false;
    }

    private boolean clickShapePoint(MouseEvent event)
    {
        var point = selection().shapePointForPoint(graph, event.getPoint());
        if (point != null)
        {
            select(point);
            selectSearchTab();
            return true;
        }
        return false;
    }

    private boolean clickVertex(MouseEvent event)
    {
        var vertexes = selection().vertexesForPoint(event.getPoint());
        var selected = false;
        if (!vertexes.isEmpty())
        {
            if (!selection().sameVertexStack(vertexes))
            {
                if (canvas != null && canvas.mapWidth().isLessThan(Distance.miles(50))
                        && !vertexes.isEmpty())
                {
                    select(vertexes.get(0));
                    selected = true;
                }
                else
                {
                    clearSelection();
                }
            }
            else
            {
                nextSelection();
                selected = true;
            }
        }
        selection().selectedVertexStack(vertexes);
        if (selected)
        {
            selectSearchTab();
        }
        return selected;
    }

    private void describe(Edge edge)
    {
        var relations = new StringList();
        edge.relations().forEach(relation -> relations.add(relation.asString(HTML)));
        panel().status("Selected edge $", edge.identifier());
        html(edge.asString(HTML)
                + "\n"
                + edge.from().asString(HTML)
                + "\n"
                + edge.to().asString(HTML)
                + "\n"
                + (relations.isEmpty() ? "" : relations.join("<br/>")));
    }

    private void describe(Vertex vertex)
    {
        html(vertex.asString(HTML));
    }

    private void describe(EdgeRelation relation)
    {
        html(relation.asString(HTML));
    }

    private void describe(GraphElement element)
    {
        if (element instanceof Edge)
        {
            describe((Edge) element);
        }
        if (element instanceof Vertex)
        {
            describe((Vertex) element);
        }
        if (element instanceof EdgeRelation)
        {
            describe((EdgeRelation) element);
        }
    }

    private void html(String message, Object... arguments)
    {
        panel().html(Message.format(message + "\n\n\n", arguments));
        forceRepaint();
    }

    private void nextSelection()
    {
        describe(selection().next());
        forceRepaint();
    }

    private void previousSelection()
    {
        describe(selection().previous());
        forceRepaint();
    }

    private void select(Edge edge)
    {
        if (edge != null)
        {
            selection().select(edge);
            describe(edge);
        }
    }

    private void select(EdgeRelation relation)
    {
        if (relation != null)
        {
            selection().select(relation);
            describe(relation);
        }
    }

    private void select(Place place)
    {
        if (place != null)
        {
            selection().select(place);
            html(place.asString(HTML));
        }
    }

    private void select(Polyline line)
    {
        if (line != null)
        {
            selection().select(line);
            html(line.toString());
        }
    }

    private void select(ShapePoint point)
    {
        if (point != null)
        {
            selection().select(point);
            if (point.index() == 1)
            {
                html(point.location().asString(HTML));
            }
            else
            {
                html(point.asString(HTML));
            }
        }
    }

    private void select(Vertex vertex)
    {
        if (vertex != null)
        {
            selection().select(vertex);
            describe(vertex);
        }
    }

    private void selectSearchTab()
    {
        model.graphPanel().tabbedPane().setSelectedIndex(0);
    }

    private Selection selection()
    {
        return model().selection();
    }

    private void zoomInOn(GraphElement element)
    {
        zoomTo(element.bounds());
    }
}
