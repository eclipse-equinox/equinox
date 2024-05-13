/*******************************************************************************
 * Copyright (c) 2009, 2024 Martin Lippert and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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
	 * A timeout value in milliseconds to wait for the writer thread to finish after
	 * signalling the interrupt.
	 */
	private static final long JOIN_TIMEOUT = 5000L;

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
					store(item);
				}
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
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
		// wait for the writer thread to finish, so we don't System.exit() in the middle
		// of writing...
		try {
			this.writerThread.join(JOIN_TIMEOUT);
		} catch (final InterruptedException e) {
			Log.error("Interrupted while joining the writerThread", e); //$NON-NLS-1$
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * store the cache item to disk
	 *
	 * This operation creates the appropriate directory for the cache item if it
	 * does not exist
	 *
	 * @param item the cache item to store to disc
	 */
	protected void store(final CacheItem item) {
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

	private void storeSingleClass(final String className, final byte[] classBytes, final String cacheDirectory) {
		final File directory = new File(cacheDirectory);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		final File outputFile = new File(directory, className);
		boolean success = true;
		try (FileOutputStream fosCache = new FileOutputStream(outputFile);
				DataOutputStream outCache = new DataOutputStream(new BufferedOutputStream(fosCache))) {
			outCache.write(classBytes);
			outCache.flush();
			fosCache.getFD().sync();
		} catch (final IOException e) {
			Log.error("Failed to store class " + className + " in cache", e); //$NON-NLS-1$ //$NON-NLS-2$
			success = false;
		}

		// if there was an error during writing the file, or if an interrupt happened,
		// we have a risk of having written an incomplete file. To be sure, we check
		// the length of the file on disk. If it does not match, we delete the file
		// again.
		if ((!success || Thread.currentThread().isInterrupted()) && outputFile.exists()
				&& outputFile.length() != classBytes.length) {
			Log.debug("File " + outputFile.getAbsolutePath() + " was not completely written to disk. Removing it."); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				Files.delete(outputFile.toPath());
			} catch (final IOException e) {
				Log.error("File " + outputFile.getAbsolutePath() + " is corrupted but could not be deleted.", e); //$NON-NLS-1$ //$NON-NLS-2$
				// last resort: try to delete the file when the VM terminates
				outputFile.deleteOnExit();
			}
		}

		// after writing the file, remove the item from the itemsInQueue lookup map as
		// well - we do this even the writing was unsuccessful to keep the queue and the
		// itemsInQueue in sync. It will do no further harm, just the file is not cached
		// and will be woven and
		// cached again next time.
		itemsInQueue.remove(new CacheItemKey(cacheDirectory, className));
	}
}
