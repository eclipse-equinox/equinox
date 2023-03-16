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
 *     Martin Lippert - further improvements and optimizations
 *     Stefan Winkler - fixed concurrency issues
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.ICachingServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;

/**
 * {@link ICachingService} used as "singleton" OSGi service by
 * "org.aspectj.osgi".
 *
 * @author Heiko Seeberger
 */
public class CachingServiceFactory implements ICachingServiceFactory {

	private final Map<String, ICachingService> bundleCachingServices = new HashMap<>();

	private final BundleContext bundleContext;

	private final BlockingQueue<CacheItem> cacheQueue;

	private final CacheWriter cacheWriter;

	/**
	 * A map for items that are currently contained in the {@link #cacheWriterQueue}
	 */
	private final ConcurrentMap<CacheItemKey, byte[]> itemsInCacheQueue;

	/**
	 * the lock manager to protect against concurrent file system access
	 */
	private final ClassnameLockManager lockManager = new ClassnameLockManager();

	/**
	 * @param bundleContext Must not be null!
	 * @throws IllegalArgumentException if given bundleContext is null.
	 */
	public CachingServiceFactory(final BundleContext bundleContext) {
		if (bundleContext == null) {
			throw new IllegalArgumentException("Argument \"bundleContext\" must not be null!"); //$NON-NLS-1$
		}
		this.bundleContext = bundleContext;
		this.cacheQueue = new ArrayBlockingQueue<>(IBundleConstants.QUEUE_CAPACITY);
		this.itemsInCacheQueue = new ConcurrentHashMap<>();
		this.cacheWriter = new CacheWriter(this.cacheQueue, this.itemsInCacheQueue, this.lockManager);
		this.cacheWriter.start();

		this.bundleContext.addBundleListener(new SynchronousBundleListener() {

			public void bundleChanged(final BundleEvent event) {
				if (event.getType() == BundleEvent.UNINSTALLED) {
					stopBundleCachingService(event);
				} else if (event.getType() == BundleEvent.UPDATED) {
					stopBundleCachingService(event);
				}
			}
		});
	}

	/**
	 * @see org.eclipse.equinox.service.weaving.ICachingServiceFactory#createCachingService(java.lang.ClassLoader,
	 *      org.osgi.framework.Bundle, java.lang.String)
	 */
	public synchronized ICachingService createCachingService(final ClassLoader classLoader, final Bundle bundle,
			final String key) {

		if (bundle == null) {
			throw new IllegalArgumentException("Argument \"bundle\" must not be null!"); //$NON-NLS-1$
		}

		final String cacheId = getCacheId(bundle);

		ICachingService bundleCachingService = bundleCachingServices.get(cacheId);

		if (bundleCachingService == null) {

			if (key != null && key.length() > 0) {
				bundleCachingService = new BundleCachingService(bundleContext, bundle, key, this.cacheQueue,
						this.itemsInCacheQueue, this.lockManager);
			} else {
				bundleCachingService = new UnchangedCachingService();
			}
			bundleCachingServices.put(cacheId, bundleCachingService);

			if (Log.isDebugEnabled()) {
				Log.debug(MessageFormat.format("Created BundleCachingService for [{0}].", cacheId)); //$NON-NLS-1$
			}
		}

		return bundleCachingService;
	}

	/**
	 * Generates the unique id for the cache of the given bundle (usually the
	 * symbolic name and the bundle version)
	 * 
	 * @param bundle The bundle for which a unique cache id should be calculated
	 * @return The unique id of the cache for the given bundle
	 */
	public String getCacheId(final Bundle bundle) {
		final Version v = bundle.getVersion();
		return bundle.getSymbolicName() + "_" + v; //$NON-NLS-1$
	}

	/**
	 * Stops all individual bundle services.
	 */
	public synchronized void stop() {
		for (final ICachingService bundleCachingService : bundleCachingServices.values()) {
			bundleCachingService.stop();
		}
		bundleCachingServices.clear();
		this.cacheWriter.stop();
	}

	/**
	 * Stops individual bundle caching service if the bundle is uninstalled
	 * 
	 * @param event The event contains the information for which bundle to stop the
	 *              caching service
	 */
	protected void stopBundleCachingService(final BundleEvent event) {
		final String cacheId = getCacheId(event.getBundle());
		final ICachingService bundleCachingService = bundleCachingServices.get(cacheId);
		if (bundleCachingService != null) {
			bundleCachingService.stop();
			bundleCachingServices.remove(cacheId);
		}
	}

}
