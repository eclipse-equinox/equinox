/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     Martin Lippert - initial implementation
 *     Martin Lippert - caching of generated classes
 *     Stefan Winkler - fixed concurrency issues
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

/**
 * The CacheWriter is responsible to store cache items to disk. The cache items
 * are read from the given queue.
 *
 * @author Martin Lippert
 */
public class CacheWriter {
	/**
	 * A map for items that are currently contained in the {@link #cacheWriterQueue}
	 */
	private final Map<CacheItemKey, byte[]> itemsInQueue;

	/**
	 * the lock manager to protect against concurrent file system access
	 */
	private final ClassnameLockManager lockManager;

	private final Thread writerThread;

	/**
	 * Create a new cache writer for the given queue of cache items
	 *
	 * @param cacheQueue   The blocking queue that delivers the cache items to store
	 *                     to this cache writer
	 * @param itemsInQueue The lookup map for items currently in the queue
	 * @param lockManager  the lock manager to protect against concurrent file
	 *                     system access
	 */
	public CacheWriter(final BlockingQueue<CacheItem> cacheQueue, final Map<CacheItemKey, byte[]> itemsInQueue,
			final ClassnameLockManager lockManager) {
		this.itemsInQueue = itemsInQueue;
		this.lockManager = lockManager;
		this.writerThread = new Thread(() -> {
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
		// write out generated classes first
		final Map<String, byte[]> generatedClasses = item.getGeneratedClasses();
		if (generatedClasses != null) {
			for (final Entry<String, byte[]> entry : generatedClasses.entrySet()) {
				final String className = entry.getKey();
				final byte[] classBytes = entry.getValue();
				// write the class to the cache in a locked operation to prevent concurrent
				// reads of incompletely written files
				lockManager.executeWrite(className, () -> storeSingleClass(className, classBytes, item.getDirectory()));
			}
		}

		// write out the woven class
		lockManager.executeWrite(item.getName(),
				() -> storeSingleClass(item.getName(), item.getCachedBytes(), item.getDirectory()));
	}

	private void storeSingleClass(final String className, final byte[] classBytes, final String cacheDirectory)
			throws FileNotFoundException, IOException {
		final File directory = new File(cacheDirectory);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		try (FileOutputStream fosCache = new FileOutputStream(new File(directory, className));
				DataOutputStream outCache = new DataOutputStream(new BufferedOutputStream(fosCache))) {
			outCache.write(classBytes);
			outCache.flush();
			fosCache.getFD().sync();
		}

		// after writing the file, remove the item from the itemsInQueue lookup map as
		// well
		itemsInQueue.remove(new CacheItemKey(cacheDirectory, className));
	}
}