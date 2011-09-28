/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import java.security.AccessController;
import java.security.PrivilegedAction;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {
	public static final String PI_APP = "org.eclipse.equinox.app"; //$NON-NLS-1$
	public static boolean DEBUG = false;
	private volatile static BundleContext _context;
	// PackageAdmin is a system service that never goes away as long 
	// as the framework is active.  No need to track it!!
	private volatile static PackageAdmin _packageAdmin;
	private volatile static EclipseAppContainer container;
	// tracks the FrameworkLog service
	private volatile static ServiceTracker _frameworkLogTracker;
	// tracks the extension registry and app launcher services
	private ServiceTracker registryTracker;
	private IExtensionRegistry registry;

	public void start(BundleContext bc) {
		_context = bc;
		// doing simple get service here because we expect the PackageAdmin service to always be available
		ServiceReference ref = bc.getServiceReference(PackageAdmin.class.getName());
		if (ref != null)
			_packageAdmin = (PackageAdmin) bc.getService(ref);
		_frameworkLogTracker = new ServiceTracker(bc, FrameworkLog.class.getName(), null);
		_frameworkLogTracker.open();
		getDebugOptions(bc);
		processCommandLineArgs(bc);
		// set the app manager context before starting the container
		AppPersistence.start(bc);
		// we must have an extension registry started before we can start the container
		registryTracker = new ServiceTracker(bc, IExtensionRegistry.class.getName(), this);
		registryTracker.open();
		// start the app commands for the console
		try {
			AppCommands.create(bc);
		} catch (NoClassDefFoundError e) {
			// catch incase CommandProvider is not available
		}
	}

	public void stop(BundleContext bc) {
		// stop the app commands for the console
		try {
			AppCommands.destroy(bc);
		} catch (NoClassDefFoundError e) {
			// catch incase CommandProvider is not available
		}
		// close the registry tracker; this will stop the container if it was started
		registryTracker.close();
		registryTracker = null;
		// unset the app manager context after the container has been stopped
		AppPersistence.stop();
		if (_frameworkLogTracker != null) {
			_frameworkLogTracker.close();
			_frameworkLogTracker = null;
		}
		_packageAdmin = null; // we do not unget PackageAdmin here; let the framework do it for us
		_context = null;
	}

	private void getDebugOptions(BundleContext context) {
		ServiceReference debugRef = context.getServiceReference(DebugOptions.class.getName());
		if (debugRef == null)
			return;
		DebugOptions debugOptions = (DebugOptions) context.getService(debugRef);
		DEBUG = debugOptions.getBooleanOption(PI_APP + "/debug", false); //$NON-NLS-1$
		context.ungetService(debugRef);
	}

	private static EnvironmentInfo getEnvironmentInfo() {
		BundleContext bc = Activator.getContext();
		if (bc == null)
			return null;
		ServiceReference infoRef = bc.getServiceReference(EnvironmentInfo.class.getName());
		if (infoRef == null)
			return null;
		EnvironmentInfo envInfo = (EnvironmentInfo) bc.getService(infoRef);
		if (envInfo == null)
			return null;
		bc.ungetService(infoRef);
		return envInfo;
	}

	private void processCommandLineArgs(BundleContext bc) {
		EnvironmentInfo envInfo = Activator.getEnvironmentInfo();
		if (envInfo != null)
			CommandLineArgs.processCommandLine(envInfo);
	}

	public Object addingService(ServiceReference reference) {
		BundleContext context = _context;
		if (context == null)
			return null; // really should never happen since we close the tracker before nulling out context
		Object service = null;
		EclipseAppContainer startContainer = null;
		synchronized (this) {
			if (container != null)
				return null; // container is already started; do nothing

			service = context.getService(reference);
			if (registry == null && service instanceof IExtensionRegistry) {
				registry = (IExtensionRegistry) service;
				// create and start the app container
				container = new EclipseAppContainer(context, registry);
				startContainer = container;
			}
		}
		// must not start the container while holding a lock because this will register additional services
		if (startContainer != null) {
			startContainer.start();
			return service;
		}
		// this means there is more than one registry; we don't need a second one
		if (service != null)
			context.ungetService(reference);
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// do nothing
	}

	public void removedService(ServiceReference reference, Object service) {
		EclipseAppContainer currentContainer = null;
		synchronized (this) {
			// either the registry or launcher is going away
			if (service == registry)
				registry = null;
			if (container == null)
				return; // do nothing; we have not started the container yet
			currentContainer = container;
			container = null;
		}
		// stop the app container outside the sync block
		if (currentContainer != null)
			currentContainer.stop();
	}

	// helper used to protect callers from permission checks when opening service trackers
	static void openTracker(final ServiceTracker tracker, final boolean allServices) {
		if (System.getSecurityManager() == null)
			tracker.open(allServices);
		else
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					tracker.open(allServices);
					return null;
				}
			});
	}

	// helper used to protect callers from permission checks when get services
	static Object getService(final ServiceTracker tracker) {
		if (System.getSecurityManager() == null)
			return tracker.getService();
		return AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return tracker.getService();
			}
		});
	}

	// helper used to protect callers from permission checks when getting locations
	static String getLocation(final Bundle bundle) {
		if (System.getSecurityManager() == null)
			return bundle.getLocation();
		return (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return bundle.getLocation();
			}
		});
	}

	// helper method to get a bundle from a contributor.
	static Bundle getBundle(IContributor contributor) {
		if (contributor instanceof RegistryContributor) {
			try {
				long id = Long.parseLong(((RegistryContributor) contributor).getActualId());
				BundleContext context = _context;
				if (context != null)
					return context.getBundle(id);
			} catch (NumberFormatException e) {
				// try using the name of the contributor below
			}
		}
		PackageAdmin packageAdmin = _packageAdmin;
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(contributor.getName(), null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++)
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				return bundles[i];
		return null;
	}

	static BundleContext getContext() {
		return _context;
	}

	public static EclipseAppContainer getContainer() {
		return container;
	}

	static void log(FrameworkLogEntry entry) {
		ServiceTracker frameworkLogTracker = _frameworkLogTracker;
		FrameworkLog log = frameworkLogTracker == null ? null : (FrameworkLog) frameworkLogTracker.getService();
		if (log != null)
			log.log(entry);
	}

	static void setProperty(String key, String value) {
		EnvironmentInfo envInfo = getEnvironmentInfo();
		if (envInfo != null)
			envInfo.setProperty(key, value);
		else
			System.getProperties().setProperty(key, value);
	}
}
