package com.telenav.tdk.josm.plugins.graph.view.tabs.tags.indexing;

import com.telenav.kivakit.kernel.language.string.StringList;
import com.telenav.kivakit.kernel.language.thread.KivaKitThread;
import com.telenav.kivakit.kernel.language.thread.latches.CompletionLatch;
import com.telenav.kivakit.kernel.language.time.Time;
import com.telenav.kivakit.kernel.messaging.repeaters.BaseRepeater;
import com.telenav.kivakit.kernel.language.values.count.Count;

import java.util.HashSet;

/**
 * @author jonathanl (shibo)
 */
@SuppressWarnings("InfiniteLoopStatement")
public class TagIndexer extends BaseRepeater
{
    private TagIndexRequest request;

    private final CompletionLatch newRequest = new CompletionLatch();

    private boolean requestPending;

    public TagIndexer()
    {
        listenTo(new KivaKitThread("Tag Indexer", () ->
        {
            while (true)
            {
                newRequest.waitForCompletion();
                newRequest.reset();

                if (request != null)
                {
                    requestPending = false;
                    final var index = buildIndex(request);
                    if (index != null)
                    {
                        synchronized (TagIndexer.this)
                        {
                            request.consumer.accept(index);
                        }
                    }
                }
            }
        })).start();
    }

    public synchronized void index(final TagIndexRequest request)
    {
        if (this.request == null || !this.request.equals(request))
        {
            this.request = request;
            requestPending = true;
            newRequest.completed();
        }
    }

    private TagIndex buildIndex(final TagIndexRequest request)
    {
        final var graph = request.graph;
        if (graph.hasTags())
        {
            final var started = Time.now();
            information("Building tag index for '$'...", graph.name());

            final var index = new TagIndex(request);

            for (final var edge : graph.edgesIntersecting(request.bounds))
            {
                for (final var tag : edge.tagList())
                {
                    if (requestPending)
                    {
                        return null;
                    }
                    final var key = tag.getKey();
                    final var value = tag.getValue();
                    index.keyToValues.add(key, value);
                    index.identifiers.add(key + "=" + value, edge.identifier());
                }
            }

            for (final var key : new HashSet<>(index.keyToValues.keySet()))
            {
                if (requestPending)
                {
                    return null;
                }
                final var values = index.keyToValues.get(key).first(Count.count(500));
                final var sorted = new StringList(values.asSet()).sorted();
                index.keyToValues.put(key, sorted);
            }

            information("Indexed $ keys and $ values in $", index.keyCount(), index.valueCount(), started.elapsedSince());
            return index;
        }

        return null;
    }
}
