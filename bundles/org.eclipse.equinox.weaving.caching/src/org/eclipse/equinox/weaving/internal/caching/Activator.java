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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link BundleActivator} for "org.aspectj.osgi.service.caching".
 * 
 * @author Heiko Seeberger
 */
public class Activator implements BundleActivator {

    public static boolean verbose = Boolean
            .getBoolean("org.aspectj.osgi.verbose");

    private SingletonCachingService singletonCachingService;

    private ServiceRegistration singletonCachingServiceRegistration;

    /**
     * Registers a new {@link SingletonCachingService} instance as OSGi service
     * under the interface {@link ICachingService}.
     * 
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext bundleContext) {
        setDebugEnabled(bundleContext);

        if (shouldRegister()) {
            if (verbose)
                System.err
                        .println("[org.aspectj.osgi.service.caching] info starting standard caching service ...");
            registerSingletonCachingService(bundleContext);
        } else {
            if (verbose)
                System.err
                        .println("[org.aspectj.osgi.service.caching] warning cannot start standard caching service on J9 VM");
        }
    }

    /**
     * Shuts down the {@link SingletonCachingService}.
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) {
        singletonCachingService.stop();
        singletonCachingServiceRegistration.unregister();
        if (Log.isDebugEnabled()) {
            Log.debug("Shut down and unregistered SingletonCachingService.");
        }
    }

    private void registerSingletonCachingService(
            final BundleContext bundleContext) {
        singletonCachingService = new SingletonCachingService(bundleContext);
        singletonCachingServiceRegistration = bundleContext.registerService(
                ICachingService.class.getName(), singletonCachingService, null);
        if (Log.isDebugEnabled()) {
            Log.debug("Created and registered SingletonCachingService.");
        }
    }

    private void setDebugEnabled(final BundleContext bundleContext) {
        final ServiceReference debugOptionsReference = bundleContext
                .getServiceReference(DebugOptions.class.getName());
        if (debugOptionsReference != null) {
            final DebugOptions debugOptions = (DebugOptions) bundleContext
                    .getService(debugOptionsReference);
            if (debugOptions != null) {
                Log.debugEnabled = debugOptions.getBooleanOption(
                        "org.aspectj.osgi.service.caching/debug", true); //$NON-NLS-1$
            }
        }
        if (debugOptionsReference != null) {
            bundleContext.ungetService(debugOptionsReference);
        }
    }

    private boolean shouldRegister() {
        boolean enabled = true;
        try {
            Class.forName("com.ibm.oti.vm.VM"); // if this fails we are not on J9 //$NON-NLS-1$
            final Class<?> sharedClass = Class
                    .forName("com.ibm.oti.shared.Shared"); //$NON-NLS-1$
            final Method isSharingEnabledMethod = sharedClass.getMethod(
                    "isSharingEnabled", (Class[]) null); //$NON-NLS-1$
            if (isSharingEnabledMethod != null) {
                final Boolean sharing = (Boolean) isSharingEnabledMethod
                        .invoke(null, (Object[]) null);
                if (sharing != null && sharing.booleanValue()) {
                    enabled = false;
                }
            }
        } catch (final ClassNotFoundException ex) {
        } catch (final SecurityException e) {
        } catch (final NoSuchMethodException e) {
        } catch (final IllegalArgumentException e) {
        } catch (final IllegalAccessException e) {
        } catch (final InvocationTargetException e) {
        }

        return enabled;
    }
}
