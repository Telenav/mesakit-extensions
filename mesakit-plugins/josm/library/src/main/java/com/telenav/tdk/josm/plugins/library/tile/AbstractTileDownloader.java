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

package com.telenav.tdk.josm.plugins.library.tile;

import com.telenav.kivakit.collections.set.ConcurrentHashSet;
import com.telenav.kivakit.kernel.language.thread.Threads;
import com.telenav.kivakit.kernel.language.values.Count;
import com.telenav.kivakit.kernel.language.vm.JavaVirtualMachine;
import com.telenav.kivakit.kernel.logging.Logger;
import com.telenav.kivakit.kernel.logging.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractTileDownloader<Request extends AbstractTileRequest, Tile extends AbstractTile>
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private static final Count THREADS = JavaVirtualMachine.local().processors();

    private final ExecutorService executor = Threads.threadPool("Downloader", THREADS);

    private final BlockingQueue<Request> requestQueue = new ArrayBlockingQueue<>(1024, true);

    private final Collection<Request> processing = new ConcurrentHashSet<>();

    private volatile boolean frozen;

    private volatile boolean stop;

    private volatile boolean running;

    private final AtomicInteger runningThreads = new AtomicInteger();

    private final AbstractTileCache<Request, Tile> cache;

    protected AbstractTileDownloader(final AbstractTileCache<Request, Tile> cache)
    {
        this.cache = cache;
    }

    public Tile get(final Request request)
    {
        final var tile = this.cache.get(request);
        if (tile != null && tile.isExpired())
        {
            if (!this.frozen)
            {
                this.cache.remove(request);
            }
            return null;
        }
        return tile;
    }

    public void request(final Request request)
    {
        // A request is already satisfied if we can get data for it from the cache
        final var isAlreadySatisfied = get(request) != null;

        // A request is already requested if the queue contains it or it is being processed
        final var isAlreadyRequested = this.requestQueue.contains(request) || this.processing.contains(request);

        // If the request is forcing an update OR it is not already satisfied and not
        // already requested
        if (request.forceUpdate() || (!isAlreadySatisfied && !isAlreadyRequested))
        {
            // then add it to the queue
            this.requestQueue.offer(request);
        }
    }

    public void start(final TileDownloadedListener<? super Request, ? super Tile> listener)
    {
        if (!this.running)
        {
            this.stop = false;
            this.frozen = false;
            this.running = true;
            LOGGER.information("Starting ${class}", getClass());
            THREADS.loop(() ->
                    this.executor.execute(() ->
                    {

                        AbstractTileDownloader.this.runningThreads.incrementAndGet();
                        while (!AbstractTileDownloader.this.stop)
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
                                final var request = AbstractTileDownloader.this.requestQueue.poll(1, TimeUnit.SECONDS);
                                if (request != null && !AbstractTileDownloader.this.processing.contains(request))
                                {
                                    AbstractTileDownloader.this.processing.add(request);
                                    try
                                    {
                                        download(listener, request);
                                    }
                                    finally
                                    {
                                        AbstractTileDownloader.this.processing.remove(request);
                                    }
                                }
                            }
                            catch (final InterruptedException ignored)
                            {
                            }
                        }
                        AbstractTileDownloader.this.running = false;
                        if (AbstractTileDownloader.this.runningThreads.decrementAndGet() == 0)
                        {
                            LOGGER.information("Stopped ${class}", AbstractTileDownloader.this.getClass());
                        }
                    }));
        }
    }

    public void stop()
    {
        LOGGER.information("Stopping ${class}", getClass());
        freeze(true);
        this.stop = true;
        if (this.runningThreads.get() == 0)
        {
            LOGGER.information("Stopped ${class}", getClass());
        }
    }

    protected abstract Tile onDownload(final Request request);

    private void download(final TileDownloadedListener<? super Request, ? super Tile> listener, final Request request)
    {
        for (var attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                if (shouldDownload(request))
                {
                    final var newTile = onDownload(request);
                    if (newTile != null)
                    {
                        if (shouldDownload(request))
                        {
                            final var oldTile = this.cache.get(request);
                            AbstractTileDownloader.this.cache.put(request, newTile);
                            if (listener != null)
                            {
                                listener.onDownloaded(request, oldTile, newTile);
                            }
                        }
                        break;
                    }
                }
            }
            catch (final Exception e)
            {
                LOGGER.problem(e, "Unable to retrieve $", request);
            }
        }
    }

    private void freeze(final boolean freeze)
    {
        this.frozen = freeze;
    }

    private boolean shouldDownload(final Request request)
    {
        return request.forceUpdate() || get(request) == null || !AbstractTileDownloader.this.frozen;
    }
}
