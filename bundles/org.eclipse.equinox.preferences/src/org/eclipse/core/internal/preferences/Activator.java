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

/**
 * The Jobs plugin class.
 */
public class Activator implements BundleActivator {

	/**
	 * The bundle associated this plug-in
	 */
	private static BundleContext bundleContext;

	/**
	 * This plugin provides a Preferences service.
	 */
	private ServiceRegistration preferencesService = null;

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		processCommandLine();
		registerServices();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		unregisterServices();
		PreferencesOSGiUtils.getDefault().closeServices();
		bundleContext = null;
	}

	static BundleContext getContext() {
		return bundleContext;
	}

	private void registerServices() {
		preferencesService = bundleContext.registerService(IPreferencesService.class.getName(), PreferencesService.getDefault(), new Hashtable());
	}

	private void unregisterServices() {
		preferencesService.unregister();
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
