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
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj;

import java.util.Properties;

import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.weaving.aspectj.loadtime.AspectDefinitionRegistry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The main plugin class to be used in the desktop.
 */
public class WeavingServicePlugin implements BundleActivator {

    public static boolean DEBUG;

    public static boolean verbose = Boolean
            .getBoolean("org.aspectj.osgi.verbose");

    //The shared instance.
    private static WeavingServicePlugin plugin;

    private AspectDefinitionRegistry aspectDefinitionRegistry;

    private BundleContext context;

    /**
     * The constructor.
     */
    public WeavingServicePlugin() {
        plugin = this;
        //		IWeavingContext iwc = new WeavingContext(null,null);//force bundle to be resolved
    }

    /**
     * Returns the shared instance.
     */
    public static WeavingServicePlugin getDefault() {
        return plugin;
    }

    public AspectDefinitionRegistry getAspectDefinitionRegistry() {
        return aspectDefinitionRegistry;
    }

    /**
     * @return The bundle context of the weaving service bundle or null, of
     *         bundle is not started
     */
    public BundleContext getContext() {
        return this.context;
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(final BundleContext context) throws Exception {
        this.context = context;

        this.aspectDefinitionRegistry = new AspectDefinitionRegistry();
        context.addBundleListener(this.aspectDefinitionRegistry);
        this.aspectDefinitionRegistry.initialize(context.getBundles());

        loadOptions(context);
        if (verbose)
            System.err
                    .println("[org.eclipse.equinox.weaving.aspectj] info Starting AspectJ weaving service ...");
        final String serviceName = IWeavingService.class.getName();
        //		ServiceFactory factory = new WeavingServiceFactory();
        final IWeavingService weavingService = new WeavingService();
        final Properties props = new Properties();
        //		context.registerService(serviceName, factory, props);
        context.registerService(serviceName, weavingService, props);
        //		System.out.println("WeavingServicePlugin.start() - registered WeavingService");
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(final BundleContext context) throws Exception {
        this.context = null;
        plugin = null;
    }

    private void loadOptions(final BundleContext context) {
        // all this is only to get the application args		
        DebugOptions service = null;
        final ServiceReference reference = context
                .getServiceReference(DebugOptions.class.getName());
        if (reference != null)
            service = (DebugOptions) context.getService(reference);
        if (service == null) return;
        try {
            DEBUG = service.getBooleanOption(
                    "org.aspectj.osgi.service.weaving/debug", false);
        } finally {
            // we have what we want - release the service
            context.ungetService(reference);
        }
    }

}
