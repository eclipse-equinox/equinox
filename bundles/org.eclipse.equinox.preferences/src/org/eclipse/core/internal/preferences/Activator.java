/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.util.Hashtable;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The Preferences bundle activator.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	public static final String PI_PREFERENCES = "org.eclipse.equinox.preferences"; //$NON-NLS-1$

	/**
	 * Track the registry service - only register preference service if the registry is 
	 * available
	 */
	private ServiceTracker registryServiceTracker;

	/**
	 * The bundle associated this plug-in
	 */
	private static BundleContext bundleContext;

	/**
	 * This plugin provides a Preferences service.
	 */
	private ServiceRegistration preferencesService = null;

	/**
	 * This plugin provides the OSGi Preferences service.
	 */
	private ServiceRegistration osgiPreferencesService;

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		processCommandLine();
		PreferencesOSGiUtils.getDefault().openServices();
		preferencesService = bundleContext.registerService(IPreferencesService.class.getName(), PreferencesService.getDefault(), new Hashtable());
		osgiPreferencesService = bundleContext.registerService(org.osgi.service.prefs.PreferencesService.class.getName(), new OSGiPreferencesServiceManager(bundleContext), null);
		registerServices();
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		PreferencesOSGiUtils.getDefault().closeServices();
		if (registryServiceTracker != null) {
			registryServiceTracker.close();
			registryServiceTracker = null;
		}
		if (preferencesService != null) {
			preferencesService.unregister();
			preferencesService = null;
		}
		if (osgiPreferencesService != null) {
			osgiPreferencesService.unregister();
			osgiPreferencesService = null;
		}
		bundleContext = null;
	}

	static BundleContext getContext() {
		return bundleContext;
	}

	private void registerServices() {
		if (registryServiceTracker == null) {
			// use the string for the class name here in case the registry isn't around
			registryServiceTracker = new ServiceTracker(bundleContext, "org.eclipse.core.runtime.IExtensionRegistry", this); //$NON-NLS-1$
			registryServiceTracker.open();
		}
		Object service = registryServiceTracker.getService();
		if (service != null) {
			try {
				Object helper = new PreferenceServiceRegistryHelper(PreferencesService.getDefault(), service);
				PreferencesService.getDefault().setRegistryHelper(helper);
			} catch (Exception e) {
				RuntimeLog.log(new Status(IStatus.ERROR, PI_PREFERENCES, 0, PrefsMessages.noRegistry, e));
			} catch (NoClassDefFoundError error) {
				// TODO: Verify behaviour. I think this catch should not be needed since we should never see the
				// IExtensionRegistry service without resolving against registry.
				RuntimeLog.log(new Status(IStatus.ERROR, PI_PREFERENCES, 0, PrefsMessages.noRegistry, error));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	public synchronized Object addingService(ServiceReference reference) {
		registerServices();
		//return the registry service so we track it
		return bundleContext.getService(reference);
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void modifiedService(ServiceReference reference, Object service) {
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public synchronized void removedService(ServiceReference reference, Object service) {
		PreferencesService.getDefault().setRegistryHelper(null);
	}

	/**
	 * Look for the plug-in customization file.
	 * @param args - command line arguments
	 */
	private void processCommandLine() {
		ServiceTracker environmentTracker = new ServiceTracker(bundleContext, EnvironmentInfo.class.getName(), null);
		environmentTracker.open();
		EnvironmentInfo environmentInfo = (EnvironmentInfo) environmentTracker.getService();
		environmentTracker.close();
		if (environmentInfo == null)
			return;
		String[] args = environmentInfo.getNonFrameworkArgs();
		if (args == null || args.length == 0)
			return;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(IPreferencesConstants.PLUGIN_CUSTOMIZATION)) {
				if (args.length > i + 1) // make sure the file name is actually there
					DefaultPreferences.pluginCustomizationFile = args[i + 1];
				break; // only interested in this one
			}
		}
	}
}
