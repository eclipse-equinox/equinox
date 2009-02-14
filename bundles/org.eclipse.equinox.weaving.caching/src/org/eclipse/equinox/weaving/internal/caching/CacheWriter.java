/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Martin Lippert - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * The CacheWriter is responsible to store cache items to disk. The cache items
 * are read from the given queue.
 * 
 * @author Martin Lippert
 */
public class CacheWriter {

    private final Thread writerThread;

    /**
     * Create a new cache writer for the given queue of cache items
     * 
     * @param cacheQueue The blocking queue that delivers the cache items to
     *            store to this cache writer
     */
    public CacheWriter(final BlockingQueue<CacheItem> cacheQueue) {
        this.writerThread = new Thread(new Runnable() {

            public void run() {
                try {
                    while (true) {
                        final CacheItem item = cacheQueue.take();
                        try {
                            store(item);
                        } catch (final IOException ioe) {
                            // storing in cache failed, do nothing
                        }
                    }
                } catch (final InterruptedException e) {
                }
            }
        });
        this.writerThread.setPriority(Thread.MIN_PRIORITY);
    }

    /**
     * start the cache writers work (creates a new thread to work on the queue)
     */
    public void start() {
        this.writerThread.start();
    }

    /**
     * stops the cache writer
     */
    public void stop() {
        this.writerThread.interrupt();
    }

    /**
     * store the cache item to disk
     * 
     * This operation creates the appropriate directory for the cache item if it
     * does not exist
     * 
     * @param item the cache item to store to disc
     * @throws IOException if an error occurs while writing to the cache
     */
    protected void store(final CacheItem item) throws IOException {
        DataOutputStream outState = null;
        FileOutputStream fosState = null;
        try {
            final File directory = new File(item.getDirectory());
            if (!directory.exists()) {
                directory.mkdirs();
            }

            fosState = new FileOutputStream(new File(directory, item.getName()));
            outState = new DataOutputStream(new BufferedOutputStream(fosState));

            outState.write(item.getCachedBytes());
        } finally {
            if (outState != null) {
                try {
                    outState.flush();
                    fosState.getFD().sync();
                } catch (final IOException e) {
                    // do nothing, we tried
                }
                try {
                    outState.close();
                } catch (final IOException e) {
                    // do nothing
                }
            }
        }
    }

}
