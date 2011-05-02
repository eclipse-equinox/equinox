/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.ICachingServiceFactory;
import org.eclipse.equinox.service.weaving.ISupplementerRegistry;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.service.weaving.IWeavingServiceFactory;
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
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

public class WeavingAdaptorFactory {

    private static final Collection IGNORE_WEAVING_SERVICE_BUNDLES = Arrays
            .asList(new String[] { "org.eclipse.equinox.weaving.aspectj",
                    "org.eclipse.equinox.weaving.caching",
                    "org.eclipse.equinox.weaving.caching.j9",
                    "org.eclipse.update.configurator",
                    "org.eclipse.equinox.simpleconfigurator",
                    "org.eclipse.equinox.common" });

    private static final String WEAVING_SERVICE_DYNAMICS_PROPERTY = "equinox.weaving.service.dynamics";

    private ServiceTracker cachingServiceFactoryTracker;

    private PackageAdmin packageAdminService;

    private StartLevel startLevelService;

    private ISupplementerRegistry supplementerRegistry;

    private ServiceTracker weavingServiceFactoryTracker;

    private ServiceListener weavingServiceListener;

    /**
     * Bundle -> Local weaving service
     */
    private final Map weavingServices = Collections
            .synchronizedMap(new HashMap());

    public WeavingAdaptorFactory() {
    }

    public void dispose(final BundleContext context) {

        context.removeServiceListener(weavingServiceListener);
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Removed service listener for weaving service.");

        weavingServiceFactoryTracker.close();
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Closed service tracker for weaving service.");

        cachingServiceFactoryTracker.close();
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
            final ISupplementerRegistry supplementerRegistry) {
        if (Debug.DEBUG_GENERAL)
            Debug.println("> AspectJAdaptorFactory.initialize() context="
                    + context);
        this.supplementerRegistry = supplementerRegistry;

        initializePackageAdminService(context);
        initializeStartLevelService(context);

        // Service tracker for weaving service
        weavingServiceFactoryTracker = new ServiceTracker(context,
                IWeavingServiceFactory.class.getName(), null);
        weavingServiceFactoryTracker.open();
        if (Debug.DEBUG_WEAVE)
            Debug.println("> Opened service tracker for weaving service.");

        // Service listener for weaving service
        weavingServiceListener = new ServiceListener() {

            public void serviceChanged(final ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    final List<Bundle> bundlesToRefresh = new ArrayList<Bundle>();

                    final Iterator bundleEntries = weavingServices.entrySet()
                            .iterator();
                    synchronized (weavingServices) {
                        while (bundleEntries.hasNext()) {
                            final Entry entry = (Entry) bundleEntries.next();
                            final Bundle bundle = (Bundle) entry.getKey();
                            if (entry.getValue() == null) {
                                bundleEntries.remove();
                                System.err
                                        .println("bundle update because of weaving service start: "
                                                + bundle.getSymbolicName());
                                bundlesToRefresh.add(bundle);
                                if (Debug.DEBUG_WEAVE)
                                    Debug.println("> Updated bundle "
                                            + bundle.getSymbolicName());
                            }
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

                    final Iterator bundleEntries = weavingServices.entrySet()
                            .iterator();
                    synchronized (weavingServices) {
                        while (bundleEntries.hasNext()) {
                            final Entry entry = (Entry) bundleEntries.next();
                            final Bundle bundle = (Bundle) entry.getKey();
                            if (entry.getValue() != null) {
                                bundleEntries.remove();
                                System.err
                                        .println("bundle update because of weaving service stop: "
                                                + bundle.getSymbolicName());
                                bundlesToRefresh.add(bundle);
                                if (Debug.DEBUG_WEAVE)
                                    Debug.println("> Updated bundle "
                                            + bundle.getSymbolicName());
                            }
                        }
                    }
                    if (bundlesToRefresh.size() > 0) {
                        supplementerRegistry.refreshBundles(bundlesToRefresh
                                .toArray(new Bundle[bundlesToRefresh.size()]));
                    }
                }
            }
        };

        if (System.getProperty(WEAVING_SERVICE_DYNAMICS_PROPERTY, "false")
                .equals("true")) {
            try {
                context.addServiceListener(weavingServiceListener, "("
                        + Constants.OBJECTCLASS + "="
                        + IWeavingService.class.getName() + ")");
            } catch (final InvalidSyntaxException e) { // This is correct!
            }
        }

        // Service tracker for caching service
        cachingServiceFactoryTracker = new ServiceTracker(context,
                ICachingServiceFactory.class.getName(), null);
        cachingServiceFactoryTracker.open();
        if (Debug.DEBUG_CACHE)
            Debug.println("> Opened service tracker for caching service.");
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
        final ICachingServiceFactory cachingServiceFactory = (ICachingServiceFactory) cachingServiceFactoryTracker
                .getService();
        if (cachingServiceFactory != null) {
            service = cachingServiceFactory.createCachingService(
                    (ClassLoader) loader, bundle, key);
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
            final IWeavingServiceFactory weavingServiceFactory = (IWeavingServiceFactory) weavingServiceFactoryTracker
                    .getService();
            if (weavingServiceFactory != null) {
                weavingService = weavingServiceFactory.createWeavingService(
                        (ClassLoader) loader, bundle, state, bundleDescription,
                        supplementerRegistry);
            }
            synchronized (weavingServices) {
                weavingServices.put(bundle, weavingService);
            }
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

    private void initializeStartLevelService(final BundleContext context) {
        if (Debug.DEBUG_GENERAL)
            Debug
                    .println("> AspectJAdaptorFactory.initializeStartLevelService() context="
                            + context);

        final ServiceReference ref = context
                .getServiceReference(StartLevel.class.getName());
        if (ref != null) {
            startLevelService = (StartLevel) context.getService(ref);
        }

        if (Debug.DEBUG_GENERAL)
            Debug
                    .println("< AspectJAdaptorFactory.initializeStartLevelService() "
                            + startLevelService);
    }
}
