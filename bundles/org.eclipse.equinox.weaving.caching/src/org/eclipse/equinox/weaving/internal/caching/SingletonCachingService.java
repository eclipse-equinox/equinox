/*******************************************************************************
 * Copyright (c) 2008 Heiko Seeberger and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * {@link ICachingService} used as "singleton" OSGi service by
 * "org.aspectj.osgi".
 * 
 * @author Heiko Seeberger
 */
public class SingletonCachingService extends BaseCachingService {

    private final Map<String, BundleCachingService> bundleCachingServices = new HashMap<String, BundleCachingService>();

    private final BundleContext bundleContext;

    /**
     * @param bundleContext
     *            Must not be null!
     * @throws IllegalArgumentException
     *             if given bundleContext is null.
     */
    public SingletonCachingService(final BundleContext bundleContext) {
        if (bundleContext == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundleContext\" must not be null!");
        }
        this.bundleContext = bundleContext;

        this.bundleContext.addBundleListener(new SynchronousBundleListener() {

            public void bundleChanged(final BundleEvent event) {
                if (event.getType() == BundleEvent.UNINSTALLED) {
                    stopBundleCachingService(event);
                }
            }
        });
    }

    /**
     * Looks for a stored {@link BaseCachingService} for the given bundle. If
     * this exits it is returned, else created and stored.
     * 
     * @see org.eclipse.equinox.service.weaving.ICachingService#getInstance(java.lang.ClassLoader,
     *      org.osgi.framework.Bundle, java.lang.String)
     */
    @Override
    public synchronized ICachingService getInstance(
            final ClassLoader classLoader, final Bundle bundle, String key) {

        if (bundle == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundle\" must not be null!");
        }

        BundleCachingService bundleCachingService = bundleCachingServices
                .get(bundle.getSymbolicName());

        key = key == null || key.length() == 0 ? "defaultCache" : key;

        if (bundleCachingService == null) {

            bundleCachingService = new BundleCachingService(bundleContext,
                    bundle, key);
            bundleCachingServices.put(bundle.getSymbolicName(),
                    bundleCachingService);

            if (Log.isDebugEnabled()) {
                Log.debug(MessageFormat.format(
                        "Created BundleCachingService for [{0}].", bundle
                                .getSymbolicName()));
            }
        }

        return bundleCachingService;
    }

    /**
     * Stops all individual bundle services.
     */
    public synchronized void stop() {
        for (final BundleCachingService bundleCachingService : bundleCachingServices
                .values()) {
            bundleCachingService.stop();
        }
        bundleCachingServices.clear();
    }

    /**
     * Stops individual bundle caching service if the bundle is uninstalled
     */
    protected void stopBundleCachingService(final BundleEvent event) {
        final String symbolicName = event.getBundle().getSymbolicName();
        final BundleCachingService bundleCachingService = bundleCachingServices
                .get(symbolicName);
        if (bundleCachingService != null) {
            bundleCachingService.stop();
            bundleCachingServices.remove(symbolicName);
        }
    }

}
