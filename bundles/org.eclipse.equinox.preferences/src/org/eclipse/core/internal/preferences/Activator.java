/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The Jobs plugin class.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	private static final String OSGI_PREFERENCES_SERVICE = "org.osgi.service.prefs.PreferencesService"; //$NON-NLS-1$	

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
	
	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		processCommandLine();
		registryServiceTracker = new ServiceTracker(context,"org.eclipse.equinox.registry.IExtensionRegistry",this);
		registryServiceTracker.open();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		registryServiceTracker.close(); //unregisters services
		registryServiceTracker = null;
		PreferencesOSGiUtils.getDefault().closeServices();
		bundleContext = null;
	}

	static BundleContext getContext() {
		return bundleContext;
	}

	private void registerServices() {
		preferencesService = bundleContext.registerService(IPreferencesService.class.getName(), PreferencesService.getDefault(), new Hashtable());
		osgiPreferencesService = bundleContext.registerService(OSGI_PREFERENCES_SERVICE, new OSGiPreferencesServiceManager(bundleContext), null);
	}

	public synchronized Object addingService(ServiceReference reference) {
		if (preferencesService == null) {
			registerServices();
			//return the registry service so we track it
			return bundleContext.getService(reference); 
		}
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public synchronized void removedService(ServiceReference reference, Object service) {
		if (preferencesService != null) {
			preferencesService.unregister();
			preferencesService = null;
		}
		if (osgiPreferencesService != null) {
			osgiPreferencesService.unregister();
			osgiPreferencesService = null;		
		}
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
