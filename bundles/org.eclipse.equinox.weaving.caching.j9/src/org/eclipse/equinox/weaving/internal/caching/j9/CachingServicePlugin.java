/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes
 *   Martin Lippert            minor changes    
 *   Martin Lippert            extracted caching service factory
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching.j9;

import com.ibm.oti.shared.Shared;

import org.eclipse.equinox.service.weaving.ICachingServiceFactory;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The main plugin class to be used in the desktop.
 */
public class CachingServicePlugin implements BundleActivator {

    public static boolean DEBUG;

    public static boolean verbose = Boolean
            .getBoolean("org.aspectj.osgi.verbose");

    /**
     * The constructor.
     */
    public CachingServicePlugin() {
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(final BundleContext context) throws Exception {
        if (CachingServicePlugin.DEBUG)
            System.out.println("> CachingServicePlugin.start() context="
                    + context);

        loadOptions(context);

        //are we on J9?
        if (shouldRegister()) {
            if (verbose)
                System.err
                        .println("[org.eclipse.equinox.weaving.caching.j9] info starting J9 caching service ...");
            final String name = ICachingServiceFactory.class.getName();
            final CachingServiceFactory cachingServiceFactory = new CachingServiceFactory();
            context.registerService(name, cachingServiceFactory, null);
        } else {
            if (verbose)
                System.err
                        .println("[org.eclipse.equinox.weaving.caching.j9] warning cannot start J9 caching service");
        }

        if (CachingServicePlugin.DEBUG)
            System.out.println("< CachingServicePlugin.start()");
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(final BundleContext context) throws Exception {
    }

    private void loadOptions(final BundleContext context) {
        // all this is only to get the application args		
        DebugOptions service = null;
        final ServiceReference<?> reference = context
                .getServiceReference(DebugOptions.class.getName());
        if (reference != null)
            service = (DebugOptions) context.getService(reference);
        if (service == null) return;
        try {
            DEBUG = service.getBooleanOption(
                    "org.aspectj.osgi.service.caching.j9/debug", false);
        } finally {
            // we have what we want - release the service
            context.ungetService(reference);
        }
    }

    private boolean shouldRegister() {
        if (CachingServicePlugin.DEBUG)
            System.out.println("> CachingServicePlugin.shouldRegister()");

        boolean enabled;
        try {
            Class.forName("com.ibm.oti.vm.VM"); //if this fails we are not on J9
            final boolean sharing = Shared.isSharingEnabled(); //if not using shared classes we want a different adaptor
            if (CachingServicePlugin.DEBUG)
                System.out
                        .println("- CachingServicePlugin.shouldRegister() sharing="
                                + sharing);

            if (sharing) {
                enabled = true;
            } else {
                enabled = false;
            }
        } catch (final ClassNotFoundException ex) {
            if (CachingServicePlugin.DEBUG)
                System.out
                        .println("E CachingServicePlugin.shouldRegister() ex="
                                + ex);
            //not on J9
            enabled = false;
        }

        if (CachingServicePlugin.DEBUG)
            System.out.println("< CachingServicePlugin.shouldRegister() "
                    + enabled);
        return enabled;
    }

}
