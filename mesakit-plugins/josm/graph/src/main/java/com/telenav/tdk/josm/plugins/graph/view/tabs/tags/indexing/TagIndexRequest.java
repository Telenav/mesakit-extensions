package com.telenav.tdk.josm.plugins.graph.view.tabs.tags.indexing;

import com.telenav.tdk.graph.Graph;
import com.telenav.tdk.map.geography.rectangle.Rectangle;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author jonathanl (shibo)
 */
public class TagIndexRequest
{
    Graph graph;

    Rectangle bounds;

    Consumer<TagIndex> consumer;

    public TagIndexRequest(final Graph graph, final Rectangle bounds, final Consumer<TagIndex> consumer)
    {
        this.graph = graph;
        this.bounds = bounds;
        this.consumer = consumer;
    }

    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof TagIndexRequest)
        {
            final TagIndexRequest that = (TagIndexRequest) object;
            return graph.equals(that.graph) && bounds.equals(that.bounds);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(graph, bounds);
    }
}
