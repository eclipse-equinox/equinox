/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes
 *   Heiko Seeberger           Enhancements for service dynamics     
 *******************************************************************************/

package org.eclipse.equinox.weaving.adaptors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class AspectJAdaptorFactory {

    private static final Collection IGNORE_WEAVING_SERVICE_BUNDLES = Arrays
            .asList(new String[] { "org.eclipse.equinox.weaving.aspectj",
                    "org.eclipse.equinox.caching",
                    "org.eclipse.equinox.caching.j9" });

    private BundleContext bundleContext;

    private ServiceTracker cachingServiceTracker;

    private PackageAdmin packageAdminService;

    private SupplementerRegistry supplementerRegistry;

    private ServiceListener weavingServiceListener;

    /**
     * Bundle -> Local weaving service
     */
    private final Map weavingServices = Collections
            .synchronizedMap(new HashMap());

    private ServiceTracker weavingServiceTracker;

    public AspectJAdaptorFactory() {
    }

    public void dispose(final BundleContext context) {

        context.removeServiceListener(weavingServiceListener);
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Removed service listener for weaving service.");

        weavingServiceTracker.close();
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Closed service tracker for weaving service.");

        cachingServiceTracker.close();
        if (Debug.DEBUG_CACHE)
            Debug.println("> Closed service tracker for caching service.");
    }

    public Bundle getHost(final Bundle fragment) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> AspectJAdaptorFactory.getHost() fragment="
                    + fragment);

        Bundle host = null;
        if (packageAdminService != null)
            host = packageAdminService.getHosts(fragment)[0];

        if (Debug.DEBUG_GENERAL)
            Debug.println("< AspectJAdaptorFactory.getHost() " + host);
        return host;
    }

    public void initialize(final BundleContext context,
            final SupplementerRegistry supplementerRegistry) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> AspectJAdaptorFactory.initialize() context="
                    + context);
        this.bundleContext = context;
        this.supplementerRegistry = supplementerRegistry;

        // Service tracker for weaving service
        weavingServiceTracker = new ServiceTracker(context,
                IWeavingService.class.getName(), null);
        weavingServiceTracker.open();
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Opened service tracker for weaving service.");

        // Service listener for weaving service
        weavingServiceListener = new ServiceListener() {

            public void serviceChanged(final ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    final Iterator bundles = weavingServices.keySet()
                            .iterator();
                    synchronized (weavingServices) {
                        while (bundles.hasNext()) {
                            final Bundle bundle = (Bundle) bundles.next();
                            if (weavingServices.get(bundle) == null) {
                                supplementerRegistry
                                        .updateInstalledBundle(bundle);
                                if (Debug.DEBUG_WEAVE)
                                    Debug.println("> Updated bundle "
                                            + bundle.getSymbolicName());
                            }
                        }
                    }
                }
                if (event.getType() == ServiceEvent.UNREGISTERING) {
                    final Iterator bundles = weavingServices.keySet()
                            .iterator();
                    synchronized (weavingServices) {
                        while (bundles.hasNext()) {
                            final Bundle bundle = (Bundle) bundles.next();
                            if (weavingServices.get(bundle) != null) {
                                supplementerRegistry
                                        .updateInstalledBundle(bundle);
                                if (Debug.DEBUG_WEAVE)
                                    Debug.println("> Updated bundle "
                                            + bundle.getSymbolicName());
                            }
                        }
                    }
                }
            }
        };
        try {
            context.addServiceListener(weavingServiceListener, "("
                    + Constants.OBJECTCLASS + "="
                    + IWeavingService.class.getName() + ")");
        } catch (final InvalidSyntaxException e) { // This is correct!
        }

        // Service tracker for caching service
        cachingServiceTracker = new ServiceTracker(context,
                ICachingService.class.getName(), null);
        cachingServiceTracker.open();
        if (Debug.DEBUG_CACHE)
            Debug.println("> Opened service tracker for caching service.");

        initializePackageAdminService(context);
    }

    protected ICachingService getCachingService(final BaseClassLoader loader,
            final Bundle bundle, final IWeavingService weavingService) {
        if (Debug.DEBUG_CACHE)
            Debug.println("> AspectJAdaptorFactory.getCachingService() bundle="
                    + bundle + ", weavingService=" + weavingService);
        ICachingService service = null;
        String key = "";

        if (weavingService != null) {
            key = weavingService.getKey();
        }
        final ICachingService singletonCachingService = (ICachingService) cachingServiceTracker
                .getService();
        if (singletonCachingService != null) {
            service = singletonCachingService.getInstance((ClassLoader) loader,
                    bundle, key);
        }
        if (Debug.DEBUG_CACHE)
            Debug
                    .println("< AspectJAdaptorFactory.getCachingService() service="
                            + service + ", key='" + key + "'");
        return service;
    }

    protected IWeavingService getWeavingService(final BaseClassLoader loader) {
        if (Debug.DEBUG_WEAVE)
            Debug
                    .println("> AspectJAdaptorFactory.getWeavingService() baseClassLoader="
                            + loader);

        final BaseData baseData = loader.getClasspathManager().getBaseData();
        final State state = baseData.getAdaptor().getState();
        final Bundle bundle = baseData.getBundle();
        final BundleDescription bundleDescription = state.getBundle(bundle
                .getBundleId());

        IWeavingService weavingService = null;
        if (!IGNORE_WEAVING_SERVICE_BUNDLES.contains(bundle.getSymbolicName())) {
            final IWeavingService singletonWeavingService = (IWeavingService) weavingServiceTracker
                    .getService();
            if (singletonWeavingService != null) {
                weavingService = singletonWeavingService.getInstance(
                        (ClassLoader) loader, bundle, state, bundleDescription,
                        supplementerRegistry);
            }
            weavingServices.put(bundle, weavingService);
        }
        if (Debug.DEBUG_WEAVE)
            Debug
                    .println("< AspectJAdaptorFactory.getWeavingService() service="
                            + weavingService);
        return weavingService;
    }

    private void initializePackageAdminService(final BundleContext context) {
        if (Debug.DEBUG_GENERAL)
            Debug
                    .println("> AspectJAdaptorFactory.initializePackageAdminService() context="
                            + context);

        final ServiceReference ref = context
                .getServiceReference(PackageAdmin.class.getName());
        if (ref != null) {
            packageAdminService = (PackageAdmin) context.getService(ref);
        }

        if (Debug.DEBUG_GENERAL)
            Debug
                    .println("< AspectJAdaptorFactory.initializePackageAdminService() "
                            + packageAdminService);
    }
}
