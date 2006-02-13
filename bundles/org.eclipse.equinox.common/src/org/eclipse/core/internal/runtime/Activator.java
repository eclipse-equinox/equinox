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
package org.eclipse.core.internal.runtime;

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The Common runtime plugin class.
 * 
 * This class can only be used if OSGi plugin is available.
 */
public class Activator implements BundleActivator {

	/**
	 * Table to keep track of all the URL converter services.
	 */
	private static Map urlTrackers = new HashMap();

	/**
	 * The bundle context associated this plug-in
	 */
	private static BundleContext bundleContext;

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		CommonOSGiUtils.getDefault().closeServices();
		closeURLTrackerServices();
		bundleContext = null;
	}

	static BundleContext getContext() {
		return bundleContext;
	}

	/*
	 * Let go of all the services that we aquired and kept track of.
	 */
	private static void closeURLTrackerServices() {
		synchronized (urlTrackers) {
			if (!urlTrackers.isEmpty()) {
				for (Iterator iter = urlTrackers.keySet().iterator(); iter.hasNext();) {
					String key = (String) iter.next();
					ServiceTracker tracker = (ServiceTracker) urlTrackers.get(key);
					tracker.close();
				}
				urlTrackers = new HashMap();
			}
		}
	}

	/*
	 * Return the URL Converter for the given URL. Return null if we can't
	 * find one.
	 */
	public static URLConverter getURLConverter(URL url) {
		String protocol = url.getProtocol();
		synchronized (urlTrackers) {
			ServiceTracker tracker = (ServiceTracker) urlTrackers.get(protocol);
			if (tracker == null) {
				// get the right service based on the protocol
				String FILTER_PREFIX = "(&(objectClass=" + URLConverter.class.getName() + ")(protocol="; //$NON-NLS-1$ //$NON-NLS-2$
				String FILTER_POSTFIX = "))"; //$NON-NLS-1$
				Filter filter = null;
				try {
					filter = getContext().createFilter(FILTER_PREFIX + protocol + FILTER_POSTFIX);
				} catch (InvalidSyntaxException e) {
					return null;
				}
				tracker = new ServiceTracker(getContext(), filter, null);
				tracker.open();
				// cache it in the registry
				urlTrackers.put(protocol, tracker);
			}
			return (URLConverter) tracker.getService();
		}
	}
}
