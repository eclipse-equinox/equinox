/*******************************************************************************
 * Copyright (c) 2008, 2009 Heiko Seeberger and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *     Martin Lippert - asynchronous cache writing
 *     Martin Lippert - caching of generated classes
 *     Stefan Winkler - fixed concurrency issues
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * {@link ICachingService} instantiated by {@link CachingServiceFactory} for
 * each bundle.
 * </p>
 * <p>
 *
 * @author Heiko Seeberger
 * @author Martin Lippert
 */
public class BundleCachingService implements ICachingService {

	private static final int READ_BUFFER_SIZE = 8 * 1024;

	private final Bundle bundle;

	private File cacheDirectory;

	/**
	 * Cache and reuse the {@link #cacheDirectory} string representation instead of
	 * calling {@link File#getAbsolutePath()} multiple times.
	 */
	private String cacheDirectoryString;

	private final String cacheKey;

	private final BlockingQueue<CacheItem> cacheWriterQueue;

	/**
	 * A map for items that are currently contained in the {@link #cacheWriterQueue}
	 */
	private final Map<CacheItemKey, byte[]> itemsInQueue;

	/**
	 * the lock manager to protect against concurrent file system access
	 */
	private final ClassnameLockManager lockManager;

	/**
	 * @param bundleContext    Must not be null!
	 * @param bundle           Must not be null!
	 * @param key              Must not be null!
	 * @param cacheWriterQueue The queue for items to be written to the cache, must
	 *                         not be null
	 * @param itemsInQueue     lookup map for the items in the CacheWriter queue
	 * @param lockManager      the lock manager to protect against concurrent file
	 *                         system access
	 * @throws IllegalArgumentException if given bundleContext or bundle is null.
	 */
	public BundleCachingService(final BundleContext bundleContext, final Bundle bundle, final String key,
			final BlockingQueue<CacheItem> cacheWriterQueue, final Map<CacheItemKey, byte[]> itemsInQueue,
			final ClassnameLockManager lockManager) {
		if (bundleContext == null) {
			throw new IllegalArgumentException("Argument \"bundleContext\" must not be null!"); //$NON-NLS-1$
		}
		if (bundle == null) {
			throw new IllegalArgumentException("Argument \"bundle\" must not be null!"); //$NON-NLS-1$
		}
		if (key == null) {
			throw new IllegalArgumentException("Argument \"key\" must not be null!"); //$NON-NLS-1$
		}

		this.bundle = bundle;
		this.cacheKey = hashNamespace(key);
		this.cacheWriterQueue = cacheWriterQueue;
		this.itemsInQueue = itemsInQueue;
		this.lockManager = lockManager;

		final File dataFile = bundleContext.getDataFile(cacheKey);
		if (dataFile != null) {
			final String bundleCacheDir = bundle.getBundleId() + "-" + bundle.getLastModified(); //$NON-NLS-1$
			cacheDirectory = new File(dataFile, bundleCacheDir);
			cacheDirectoryString = cacheDirectory.getAbsolutePath();
		} else {
			Log.error("Cannot initialize cache!", null); //$NON-NLS-1$
		}
	}

	/**
	 * @see org.eclipse.equinox.service.weaving.ICachingService#canCacheGeneratedClasses()
	 */
	@Override
	public boolean canCacheGeneratedClasses() {
		return true;
	}

	/**
	 * @see org.eclipse.equinox.service.weaving.ICachingService#findStoredClass(java.lang.String,
	 *      java.net.URL, java.lang.String)
	 */
	@Override
	public CacheEntry findStoredClass(final String namespace, final URL sourceFileURL, final String name) {

		if (name == null) {
			throw new IllegalArgumentException("Argument \"name\" must not be null!"); //$NON-NLS-1$
		}

		byte[] storedClass = null;
		boolean isCached = false;

		if (cacheDirectory != null) {
			final String directoryString = cacheDirectory.getAbsolutePath();
			// first check whether the class is currently in the CacheWriter queue, and if
			// so, take it from there
			storedClass = itemsInQueue.get(new CacheItemKey(directoryString, name));
			if (storedClass == null) {
				// else, read it from disk (if it exists)
				final File cachedBytecodeFile = new File(cacheDirectory, name);
				if (cachedBytecodeFile.exists()) {
					storedClass = lockManager.executeRead(name, () -> read(name, cachedBytecodeFile));
				}
			}
			isCached = storedClass != null;
		}

		if (Log.isDebugEnabled()) {
			Log.debug(MessageFormat.format("for [{0}]: {1} {2}", bundle //$NON-NLS-1$
					.getSymbolicName(),
					((storedClass != null) ? "Found" //$NON-NLS-1$
							: "Found NOT"), //$NON-NLS-1$
					name));
		}
		return new CacheEntry(isCached, storedClass);
	}

	/**
	 * Hash the shared class namespace using MD5
	 *
	 * @param keyToHash
	 * @return the MD5 version of the input string
	 */
	private String hashNamespace(final String namespace) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		final byte[] bytes = md.digest(namespace.getBytes());
		final StringBuffer result = new StringBuffer();
		for (final byte b : bytes) {
			int num;
			if (b < 0) {
				num = b + 256;
			} else {
				num = b;
			}
			String s = Integer.toHexString(num);
			while (s.length() < 2) {
				s = "0" + s; //$NON-NLS-1$
			}
			result.append(s);
		}
		return new String(result);
	}

	private byte[] read(final String name, final File file) {
		int length = (int) file.length();

		try (InputStream in = new FileInputStream(file)) {
			byte[] classbytes = new byte[length];
			int bytesread = 0;
			int readcount;

			if (length > 0) {
				classbytes = new byte[length];
				for (; bytesread < length; bytesread += readcount) {
					readcount = in.read(classbytes, bytesread, length - bytesread);
					if (readcount <= 0) /* if we didn't read anything */
						break; /* leave the loop */
				}
			} else /* BundleEntry does not know its own length! */ {
				length = READ_BUFFER_SIZE;
				classbytes = new byte[length];
				readloop: while (true) {
					for (; bytesread < length; bytesread += readcount) {
						readcount = in.read(classbytes, bytesread, length - bytesread);
						if (readcount <= 0) /* if we didn't read anything */
							break readloop; /* leave the loop */
					}
					final byte[] oldbytes = classbytes;
					length += READ_BUFFER_SIZE;
					classbytes = new byte[length];
					System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
				}
			}
			if (classbytes.length > bytesread) {
				final byte[] oldbytes = classbytes;
				classbytes = new byte[bytesread];
				System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
			}
			return classbytes;
		} catch (final IOException e) {
			Log.debug(MessageFormat.format("for [{0}]: Cannot read [1] from cache!", bundle //$NON-NLS-1$
					.getSymbolicName(), name));
			return null;
		}
	}

	/**
	 * Writes the remaining cache to disk.
	 */
	@Override
	public void stop() {
	}

	/**
	 * @see org.eclipse.equinox.service.weaving.ICachingService#storeClass(java.lang.String,
	 *      java.net.URL, java.lang.Class, byte[])
	 */
	@Override
	public boolean storeClass(final String namespace, final URL sourceFileURL, final Class<?> clazz,
			final byte[] classbytes) {
		if (clazz == null) {
			throw new IllegalArgumentException("Argument \"clazz\" must not be null!"); //$NON-NLS-1$
		}
		if (classbytes == null) {
			throw new IllegalArgumentException("Argument \"classbytes\" must not be null!"); //$NON-NLS-1$
		}
		if (cacheDirectory == null) {
			return false;
		}

		final String className = clazz.getName();
		final CacheItem item = new CacheItem(classbytes, cacheDirectoryString, className);

		final boolean queued = this.cacheWriterQueue.offer(item);
		if (queued) {
			itemsInQueue.put(new CacheItemKey(cacheDirectoryString, className), classbytes);
		}
		return queued;
	}

	/**
	 * @see org.eclipse.equinox.service.weaving.ICachingService#storeClassAndGeneratedClasses(java.lang.String,
	 *      java.net.URL, java.lang.Class, byte[], java.util.Map)
	 */
	@Override
	public boolean storeClassAndGeneratedClasses(final String namespace, final URL sourceFileUrl, final Class<?> clazz,
			final byte[] classbytes, final Map<String, byte[]> generatedClasses) {
		final String className = clazz.getName();
		final CacheItem item = new CacheItem(classbytes, cacheDirectoryString, className, generatedClasses);

		final boolean queued = this.cacheWriterQueue.offer(item);
		if (queued) {
			itemsInQueue.put(new CacheItemKey(cacheDirectoryString, className), classbytes);
			for (final Entry<String, byte[]> generatedClass : generatedClasses.entrySet()) {
				itemsInQueue.put(new CacheItemKey(cacheDirectoryString, generatedClass.getKey()),
						generatedClass.getValue());
			}
		}
		return queued;
	}
}
