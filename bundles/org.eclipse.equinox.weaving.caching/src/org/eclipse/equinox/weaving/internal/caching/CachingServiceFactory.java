/*******************************************************************************
 * Copyright (c) 2008, 2009 Heiko Seeberger and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *     Martin Lippert - further improvements and optimizations
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.ICachingServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;

/**
 * {@link ICachingService} used as "singleton" OSGi service by
 * "org.aspectj.osgi".
 * 
 * @author Heiko Seeberger
 */
public class CachingServiceFactory implements ICachingServiceFactory {

    private final Map<String, ICachingService> bundleCachingServices = new HashMap<String, ICachingService>();

    private final BundleContext bundleContext;

    private final BlockingQueue<CacheItem> cacheQueue;

    private final CacheWriter cacheWriter;

    /**
     * @param bundleContext Must not be null!
     * @throws IllegalArgumentException if given bundleContext is null.
     */
    public CachingServiceFactory(final BundleContext bundleContext) {
        if (bundleContext == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundleContext\" must not be null!"); //$NON-NLS-1$
        }
        this.bundleContext = bundleContext;
        this.cacheQueue = new ArrayBlockingQueue<CacheItem>(5000);
        this.cacheWriter = new CacheWriter(this.cacheQueue);
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
    public synchronized ICachingService createCachingService(
            final ClassLoader classLoader, final Bundle bundle, final String key) {

        if (bundle == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundle\" must not be null!"); //$NON-NLS-1$
        }

        final String cacheId = getCacheId(bundle);

        ICachingService bundleCachingService = bundleCachingServices
                .get(cacheId);

        if (bundleCachingService == null) {

            if (key != null && key.length() > 0) {
                bundleCachingService = new BundleCachingService(bundleContext,
                        bundle, key, this.cacheQueue);
            } else {
                bundleCachingService = new UnchangedCachingService();
            }
            bundleCachingServices.put(cacheId, bundleCachingService);

            if (Log.isDebugEnabled()) {
                Log.debug(MessageFormat.format(
                        "Created BundleCachingService for [{0}].", cacheId)); //$NON-NLS-1$
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
        String bundleVersion = (String) bundle.getHeaders().get(
                Constants.BUNDLE_VERSION);
        if (bundleVersion == null || bundleVersion.length() == 0) {
            bundleVersion = "0.0.0"; //$NON-NLS-1$
        }
        return bundle.getSymbolicName() + "_" + bundleVersion; //$NON-NLS-1$
    }

    /**
     * Stops all individual bundle services.
     */
    public synchronized void stop() {
        for (final ICachingService bundleCachingService : bundleCachingServices
                .values()) {
            bundleCachingService.stop();
        }
        bundleCachingServices.clear();
        this.cacheWriter.stop();
    }

    /**
     * Stops individual bundle caching service if the bundle is uninstalled
     * 
     * @param event The event contains the information for which bundle to stop
     *            the caching service
     */
    protected void stopBundleCachingService(final BundleEvent event) {
        final String cacheId = getCacheId(event.getBundle());
        final ICachingService bundleCachingService = bundleCachingServices
                .get(cacheId);
        if (bundleCachingService != null) {
            bundleCachingService.stop();
            bundleCachingServices.remove(cacheId);
        }
    }

}
