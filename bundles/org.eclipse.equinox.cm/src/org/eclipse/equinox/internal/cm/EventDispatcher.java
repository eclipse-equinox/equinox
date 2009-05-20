/*******************************************************************************
 * Copyright (c) 2005, 2008 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * EventDispatcher is responsible for delivering Configuration Events to ConfigurationListeners.
 * The originating ConfigAdmin ServiceReference is needed when delivering events. This reference
 * is made available by the service factory before returning the service object.
 */

public class EventDispatcher {
	final ServiceTracker tracker;
	private final SerializedTaskQueue queue = new SerializedTaskQueue("ConfigurationListener Event Queue"); //$NON-NLS-1$
	/** @GuardedBy this */
	private ServiceReference configAdminReference;
	final LogService log;

	public EventDispatcher(BundleContext context, LogService log) {
		this.log = log;
		tracker = new ServiceTracker(context, ConfigurationListener.class.getName(), null);
	}

	public void start() {
		tracker.open();
	}

	public void stop() {
		tracker.close();
		synchronized (this) {
			configAdminReference = null;
		}
	}

	synchronized void setServiceReference(ServiceReference reference) {
		if (configAdminReference == null)
			configAdminReference = reference;
	}

	public void dispatchEvent(int type, String factoryPid, String pid) {
		final ConfigurationEvent event = createConfigurationEvent(type, factoryPid, pid);
		if (event == null)
			return;

		ServiceReference[] refs = tracker.getServiceReferences();
		if (refs == null)
			return;

		for (int i = 0; i < refs.length; ++i) {
			final ServiceReference ref = refs[i];
			queue.put(new Runnable() {
				public void run() {
					ConfigurationListener listener = (ConfigurationListener) tracker.getService(ref);
					if (listener == null) {
						return;
					}
					try {
						listener.configurationEvent(event);
					} catch (Throwable t) {
						log.log(LogService.LOG_ERROR, t.getMessage(), t);
					}
				}
			});
		}
	}

	private synchronized ConfigurationEvent createConfigurationEvent(int type, String factoryPid, String pid) {
		if (configAdminReference == null)
			return null;

		return new ConfigurationEvent(configAdminReference, type, factoryPid, pid);
	}
}
