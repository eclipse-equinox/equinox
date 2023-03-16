/*******************************************************************************
 * Copyright (c) 2008, 2013 Heiko Seeberger and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *   Heiko Seeberger           initial implementation
 *   Martin Lippert            extracted caching service factory
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.ICachingServiceFactory;
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

	private static boolean verbose = Boolean.getBoolean("org.aspectj.osgi.verbose"); //$NON-NLS-1$

	private CachingServiceFactory cachingServiceFactory;

	private ServiceRegistration<?> cachingServiceFactoryRegistration;

	private void registerCachingServiceFactory(final BundleContext bundleContext) {
		cachingServiceFactory = new CachingServiceFactory(bundleContext);
		cachingServiceFactoryRegistration = bundleContext.registerService(ICachingServiceFactory.class.getName(),
				cachingServiceFactory, null);
		if (Log.isDebugEnabled()) {
			Log.debug("Created and registered SingletonCachingService."); //$NON-NLS-1$
		}
	}

	private void setDebugEnabled(final BundleContext bundleContext) {
		final ServiceReference<?> debugOptionsReference = bundleContext
				.getServiceReference(DebugOptions.class.getName());
		if (debugOptionsReference != null) {
			final DebugOptions debugOptions = (DebugOptions) bundleContext.getService(debugOptionsReference);
			if (debugOptions != null) {
				Log.debugEnabled = debugOptions.getBooleanOption("org.eclipse.equinox.weaving.caching/debug", false); //$NON-NLS-1$
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
			final Class<?> sharedClass = Class.forName("com.ibm.oti.shared.Shared"); //$NON-NLS-1$
			final Method isSharingEnabledMethod = sharedClass.getMethod("isSharingEnabled", (Class[]) null); //$NON-NLS-1$
			if (isSharingEnabledMethod != null) {
				final Boolean sharing = (Boolean) isSharingEnabledMethod.invoke(null, (Object[]) null);
				if (sharing != null && sharing.booleanValue()) {
					enabled = false;
				}
			}
		} catch (final ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException
				| IllegalAccessException | InvocationTargetException ex) {
		}

		return enabled;
	}

	/**
	 * Registers a new {@link CachingServiceFactory} instance as OSGi service under
	 * the interface {@link ICachingService}.
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(final BundleContext bundleContext) {
		setDebugEnabled(bundleContext);

		if (shouldRegister()) {
			if (verbose)
				System.err.println("[org.eclipse.equinox.weaving.caching] info starting standard caching service ..."); //$NON-NLS-1$
			registerCachingServiceFactory(bundleContext);
		} else {
			if (verbose)
				System.err.println(
						"[org.eclipse.equinox.weaving.caching] warning cannot start standard caching service on J9 VM"); //$NON-NLS-1$
		}
	}

	/**
	 * Shuts down the {@link CachingServiceFactory}.
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(final BundleContext context) {
		if (cachingServiceFactoryRegistration != null) {
			cachingServiceFactory.stop();
			cachingServiceFactoryRegistration.unregister();
		}
		if (Log.isDebugEnabled()) {
			Log.debug("Shut down and unregistered SingletonCachingService."); //$NON-NLS-1$
		}
	}
}
