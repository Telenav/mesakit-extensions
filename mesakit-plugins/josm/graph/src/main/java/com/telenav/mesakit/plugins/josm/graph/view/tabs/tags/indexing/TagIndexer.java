package com.telenav.mesakit.plugins.josm.graph.view.tabs.tags.indexing;

import com.telenav.kivakit.kernel.language.collections.list.StringList;
import com.telenav.kivakit.kernel.language.threading.KivaKitThread;
import com.telenav.kivakit.kernel.language.threading.latches.CompletionLatch;
import com.telenav.kivakit.kernel.language.time.Time;
import com.telenav.kivakit.kernel.language.values.count.Count;
import com.telenav.kivakit.kernel.messaging.repeaters.BaseRepeater;

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
                    var index = buildIndex(request);
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

    public synchronized void index(TagIndexRequest request)
    {
        if (this.request == null || !this.request.equals(request))
        {
            this.request = request;
            requestPending = true;
            newRequest.completed();
        }
    }

    private TagIndex buildIndex(TagIndexRequest request)
    {
        var graph = request.graph;
        if (graph.hasTags())
        {
            var started = Time.now();
            information("Building tag index for '$'...", graph.name());

            var index = new TagIndex(request);

            for (var edge : graph.edgesIntersecting(request.bounds))
            {
                for (var tag : edge.tagList())
                {
                    if (requestPending)
                    {
                        return null;
                    }
                    var key = tag.getKey();
                    var value = tag.getValue();
                    index.keyToValues.add(key, value);
                    index.identifiers.add(key + "=" + value, edge.identifier());
                }
            }

            for (var key : new HashSet<>(index.keyToValues.keySet()))
            {
                if (requestPending)
                {
                    return null;
                }
                var values = index.keyToValues.get(key).first(Count.count(500));
                var sorted = new StringList(values.asSet()).sorted();
                index.keyToValues.put(key, sorted);
            }

            information("Indexed $ keys and $ values in $", index.keyCount(), index.valueCount(), started.elapsedSince());
            return index;
        }

        return null;
    }
}
