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
package org.eclipse.core.internal.registry.osgi;

import org.eclipse.core.internal.registry.IRegistryConstants;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The extension registry bundle manager class.
 */
public class Activator implements BundleActivator {

	private static BundleContext bundleContext;

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		processCommandLine();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	public static BundleContext getContext() {
		return bundleContext;
	}

	/**
	 * Look for the no registry cache flag and check to see if we should NOT be lazily loading plug-in 
	 * definitions from the registry cache file.
	 * NOTE: this command line processing is only performed in the presence of OSGi
	 * 
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
			if (args[i].equalsIgnoreCase(IRegistryConstants.NO_REGISTRY_CACHE))
				System.getProperties().setProperty(IRegistryConstants.PROP_NO_REGISTRY_CACHE, "true"); //$NON-NLS-1$
			else if (args[i].equalsIgnoreCase(IRegistryConstants.NO_LAZY_REGISTRY_CACHE_LOADING))
				System.getProperties().setProperty(IRegistryConstants.PROP_NO_LAZY_CACHE_LOADING, "true"); //$NON-NLS-1$
		}
	}

}
