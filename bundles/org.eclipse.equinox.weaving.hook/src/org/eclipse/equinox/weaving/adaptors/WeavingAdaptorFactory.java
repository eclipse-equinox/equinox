/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   David Knibb               initial implementation
 *   Matthew Webster           Eclipse 3.2 changes
 *   Heiko Seeberger           Enhancements for service dynamics
 *   Martin Lippert            extracted weaving and caching service factories
 *******************************************************************************/

package org.eclipse.equinox.weaving.adaptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.ICachingServiceFactory;
import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.service.weaving.IWeavingServiceFactory;
import org.eclipse.osgi.framework.util.Wirings;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

public class WeavingAdaptorFactory {

    private static final Collection<String> IGNORE_WEAVING_SERVICE_BUNDLES = Arrays
            .asList(new String[] { "org.eclipse.equinox.weaving.aspectj", //$NON-NLS-1$
                    "org.eclipse.equinox.weaving.caching", //$NON-NLS-1$
                    "org.eclipse.equinox.weaving.caching.j9", //$NON-NLS-1$
                    "org.eclipse.equinox.simpleconfigurator", //$NON-NLS-1$
                    "org.eclipse.equinox.common" }); //$NON-NLS-1$

    private ServiceTracker<ICachingServiceFactory, ICachingServiceFactory> cachingServiceFactoryTracker;

    private StartLevel startLevelService;

    private ISupplementerRegistry supplementerRegistry;

    private ServiceTracker<IWeavingServiceFactory, IWeavingServiceFactory> weavingServiceFactoryTracker;

    private ServiceListener weavingServiceListener;

    private final Map<Bundle, IWeavingService> weavingServices = new ConcurrentHashMap<Bundle, IWeavingService>();

    public WeavingAdaptorFactory() {
    }

    public void dispose(final BundleContext context) {

        context.removeServiceListener(weavingServiceListener);
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Removed service listener for weaving service."); //$NON-NLS-1$

        weavingServiceFactoryTracker.close();
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Closed service tracker for weaving service."); //$NON-NLS-1$

        cachingServiceFactoryTracker.close();
        if (Debug.DEBUG_CACHE)
            Debug.println("> Closed service tracker for caching service."); //$NON-NLS-1$
    }

    protected ICachingService getCachingService(final ModuleClassLoader loader,
            final Bundle bundle, final IWeavingService weavingService) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> WeavingAdaptorFactory.getCachingService() bundle=" //$NON-NLS-1$
                    + bundle + ", weavingService=" + weavingService); //$NON-NLS-1$
        ICachingService service = null;
        String key = ""; //$NON-NLS-1$

        if (weavingService != null) {
            key = weavingService.getKey();
        }
        final ICachingServiceFactory cachingServiceFactory = cachingServiceFactoryTracker
                .getService();
        if (cachingServiceFactory != null) {
            service = cachingServiceFactory.createCachingService(loader, bundle,
                    key);
        }
        if (Debug.DEBUG_CACHE)
            Debug.println("< WeavingAdaptorFactory.getCachingService() service=" //$NON-NLS-1$
                    + service + ", key='" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        return service;
    }

    public Bundle getHost(final Bundle fragment) {
        if (Debug.DEBUG_GENERAL) Debug.println(
                "> WeavingAdaptorFactory.getHost() fragment=" + fragment); //$NON-NLS-1$

        Bundle host = Wirings.getHosts(fragment).get(0);

        if (Debug.DEBUG_GENERAL)
            Debug.println("< WeavingAdaptorFactory.getHost() " + host); //$NON-NLS-1$
        return host;
    }

    protected IWeavingService getWeavingService(
            final ModuleClassLoader loader) {
        if (Debug.DEBUG_WEAVE) Debug.println(
                "> WeavingAdaptorFactory.getWeavingService() baseClassLoader=" //$NON-NLS-1$
                        + loader);

        final Generation generation = loader.getClasspathManager()
                .getGeneration();
        final Bundle bundle = loader.getBundle();

        IWeavingService weavingService = null;
        if (!IGNORE_WEAVING_SERVICE_BUNDLES
                .contains(bundle.getSymbolicName())) {
            final IWeavingServiceFactory weavingServiceFactory = weavingServiceFactoryTracker
                    .getService();
            if (weavingServiceFactory != null) {
                weavingService = weavingServiceFactory.createWeavingService(
                        loader, bundle, generation.getRevision(),
                        supplementerRegistry);

                if (weavingService != null) {
                    weavingServices.put(bundle, weavingService);
                }
            }
        }
        if (Debug.DEBUG_WEAVE)
            Debug.println("< WeavingAdaptorFactory.getWeavingService() service=" //$NON-NLS-1$
                    + weavingService);
        return weavingService;
    }

    public void initialize(final BundleContext context,
            final ISupplementerRegistry supplementerRegistry) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> WeavingAdaptorFactory.initialize() context=" //$NON-NLS-1$
                    + context);
        this.supplementerRegistry = supplementerRegistry;

        initializeStartLevelService(context);

        // Service tracker for weaving service
        weavingServiceFactoryTracker = new ServiceTracker<IWeavingServiceFactory, IWeavingServiceFactory>(
                context, IWeavingServiceFactory.class, null);
        weavingServiceFactoryTracker.open();
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Opened service tracker for weaving service."); //$NON-NLS-1$

        // Service listener for weaving service
        weavingServiceListener = new ServiceListener() {

            @Override
            public void serviceChanged(final ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    final List<Bundle> bundlesToRefresh = new ArrayList<Bundle>();

                    synchronized (weavingServices) {
                        final Iterator<Bundle> bundleEntries = weavingServices
                                .keySet().iterator();
                        while (bundleEntries.hasNext()) {
                            final Bundle bundle = bundleEntries.next();
                            bundleEntries.remove();
                            bundlesToRefresh.add(bundle);
                            if (Debug.DEBUG_WEAVE)
                                Debug.println("> Updated bundle " //$NON-NLS-1$
                                        + bundle.getSymbolicName());
                        }
                    }

                    if (bundlesToRefresh.size() > 0) {
                        supplementerRegistry.refreshBundles(bundlesToRefresh
                                .toArray(new Bundle[bundlesToRefresh.size()]));
                    }
                }
                if (event.getType() == ServiceEvent.UNREGISTERING
                        && startLevelService != null
                        && startLevelService.getStartLevel() > 0) {
                    final List<Bundle> bundlesToRefresh = new ArrayList<Bundle>();

                    synchronized (weavingServices) {
                        final Iterator<Bundle> bundleEntries = weavingServices
                                .keySet().iterator();
                        while (bundleEntries.hasNext()) {
                            final Bundle bundle = bundleEntries.next();
                            bundleEntries.remove();
                            bundlesToRefresh.add(bundle);
                            if (Debug.DEBUG_WEAVE)
                                Debug.println("> Updated bundle " //$NON-NLS-1$
                                        + bundle.getSymbolicName());
                        }
                    }
                    if (bundlesToRefresh.size() > 0) {
                        supplementerRegistry.refreshBundles(bundlesToRefresh
                                .toArray(new Bundle[bundlesToRefresh.size()]));
                    }
                }
            }
        };

        //        if (System.getProperty(WEAVING_SERVICE_DYNAMICS_PROPERTY, "false")
        //                .equals("true")) {
        try {
            context.addServiceListener(weavingServiceListener, "(" //$NON-NLS-1$
                    + Constants.OBJECTCLASS + "=" //$NON-NLS-1$
                    + IWeavingServiceFactory.class.getName() + ")"); //$NON-NLS-1$
        } catch (final InvalidSyntaxException e) { // This is correct!
        }

        // Service tracker for caching service
        cachingServiceFactoryTracker = new ServiceTracker<ICachingServiceFactory, ICachingServiceFactory>(
                context, ICachingServiceFactory.class, null);
        cachingServiceFactoryTracker.open();
        if (Debug.DEBUG_CACHE)
            Debug.println("> Opened service tracker for caching service."); //$NON-NLS-1$
    }

    private void initializeStartLevelService(final BundleContext context) {
        if (Debug.DEBUG_GENERAL) Debug.println(
                "> AdaptorFactory.initializeStartLevelService() context=" //$NON-NLS-1$
                        + context);

        final ServiceReference<StartLevel> ref = context
                .getServiceReference(StartLevel.class);
        if (ref != null) {
            startLevelService = context.getService(ref);
        }

        if (Debug.DEBUG_GENERAL)
            Debug.println("< AdaptorFactory.initializeStartLevelService() " //$NON-NLS-1$
                    + startLevelService);
    }
}
