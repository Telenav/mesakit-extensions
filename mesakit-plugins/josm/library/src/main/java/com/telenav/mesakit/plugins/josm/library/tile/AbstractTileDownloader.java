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

package com.telenav.mesakit.plugins.josm.library.tile;

import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.core.collections.set.ConcurrentHashSet;
import com.telenav.kivakit.core.thread.Threads;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.vm.JavaVirtualMachine;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractTileDownloader<Request extends AbstractTileRequest, Tile extends AbstractTile> extends BaseComponent
{
    private static final Count THREADS = JavaVirtualMachine.javaVirtualMachine().processors();

    private final ExecutorService executor = Threads.threadPool("Downloader", THREADS);

    private final BlockingQueue<Request> requestQueue = new ArrayBlockingQueue<>(1024, true);

    private final Collection<Request> processing = new ConcurrentHashSet<>();

    private volatile boolean frozen;

    private volatile boolean stop;

    private volatile boolean running;

    private final AtomicInteger runningThreads = new AtomicInteger();

    private final AbstractTileCache<Request, Tile> cache;

    protected AbstractTileDownloader(AbstractTileCache<Request, Tile> cache)
    {
        this.cache = cache;
    }

    public Tile get(Request request)
    {
        var tile = cache.get(request);
        if (tile != null && tile.isExpired())
        {
            if (!frozen)
            {
                cache.remove(request);
            }
            return null;
        }
        return tile;
    }

    public void request(Request request)
    {
        // A request is already satisfied if we can get data for it from the cache
        var isAlreadySatisfied = get(request) != null;

        // A request is already requested if the queue contains it or it is being processed
        var isAlreadyRequested = requestQueue.contains(request) || processing.contains(request);

        // If the request is forcing an update OR it is not already satisfied and not
        // already requested
        if (request.forceUpdate() || (!isAlreadySatisfied && !isAlreadyRequested))
        {
            // then add it to the queue
            //noinspection ResultOfMethodCallIgnored
            requestQueue.offer(request);
        }
    }

    public void start(TileDownloadedListener<? super Request, ? super Tile> listener)
    {
        if (!running)
        {
            stop = false;
            frozen = false;
            running = true;
            information("Starting ${class}", getClass());
            THREADS.loop(() ->
                    executor.execute(() ->
                    {
                        runningThreads.incrementAndGet();
                        while (!stop)
                        {
                            try
                            {
                                // NOTE: there's a very slight synchronization issue here in that
                                // the request ought to be taken from the queue and put in the
                                // processing set atomically, but the window is very small for this
                                // problem to occur and the worst that will happen as a result is
                                // that once in a blue moon a request will be processed twice, which
                                // nobody will even notice. Since take() is a blocking call, simple
                                // synchronization would fail and so solving this perfectly is just
                                // not worth the effort.
                                var request = requestQueue.poll(1, TimeUnit.SECONDS);
                                if (request != null && !processing.contains(request))
                                {
                                    processing.add(request);
                                    try
                                    {
                                        download(listener, request);
                                    }
                                    finally
                                    {
                                        processing.remove(request);
                                    }
                                }
                            }
                            catch (InterruptedException ignored)
                            {
                            }
                        }
                        running = false;
                        if (runningThreads.decrementAndGet() == 0)
                        {
                            information("Stopped ${class}", getClass());
                        }
                    }));
        }
    }

    public void stop()
    {
        information("Stopping ${class}", getClass());
        freeze(true);
        stop = true;
        if (runningThreads.get() == 0)
        {
            information("Stopped ${class}", getClass());
        }
    }

    protected abstract Tile onDownload(Request request);

    private void download(TileDownloadedListener<? super Request, ? super Tile> listener, Request request)
    {
        for (var attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                if (shouldDownload(request))
                {
                    var newTile = onDownload(request);
                    if (newTile != null)
                    {
                        if (shouldDownload(request))
                        {
                            var oldTile = cache.get(request);
                            cache.put(request, newTile);
                            if (listener != null)
                            {
                                listener.onDownloaded(request, oldTile, newTile);
                            }
                        }
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                problem(e, "Unable to retrieve $", request);
            }
        }
    }

    private void freeze(boolean freeze)
    {
        frozen = freeze;
    }

    private boolean shouldDownload(Request request)
    {
        return request.forceUpdate() || get(request) == null || !frozen;
    }
}
