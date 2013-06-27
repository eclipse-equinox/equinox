/*******************************************************************************
 * Copyright (c) 2005, 2013 Cognos Incorporated, IBM Corporation and others.
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
import org.osgi.service.cm.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * EventDispatcher is responsible for delivering Configuration Events to ConfigurationListeners.
 * The originating ConfigAdmin ServiceReference is needed when delivering events. This reference
 * is made available by the service factory before returning the service object.
 */

public class EventDispatcher {
	final ServiceTracker<ConfigurationListener, ConfigurationListener> tracker;
	final ServiceTracker<SynchronousConfigurationListener, SynchronousConfigurationListener> syncTracker;
	private final SerializedTaskQueue queue = new SerializedTaskQueue("ConfigurationListener Event Queue"); //$NON-NLS-1$
	/** @GuardedBy this */
	private ServiceReference<ConfigurationAdmin> configAdminReference;
	final LogTracker log;

	public EventDispatcher(BundleContext context, LogTracker log) {
		this.log = log;
		tracker = new ServiceTracker<ConfigurationListener, ConfigurationListener>(context, ConfigurationListener.class, null);
		syncTracker = new ServiceTracker<SynchronousConfigurationListener, SynchronousConfigurationListener>(context, SynchronousConfigurationListener.class, null);
	}

	public void start() {
		tracker.open();
		syncTracker.open();
	}

	public void stop() {
		tracker.close();
		syncTracker.close();
		synchronized (this) {
			configAdminReference = null;
		}
	}

	synchronized void setServiceReference(ServiceReference<ConfigurationAdmin> reference) {
		if (configAdminReference == null)
			configAdminReference = reference;
	}

	public void dispatchEvent(int type, String factoryPid, String pid) {
		final ConfigurationEvent event = createConfigurationEvent(type, factoryPid, pid);
		if (event == null)
			return;

		ServiceReference<SynchronousConfigurationListener>[] syncRefs = syncTracker.getServiceReferences();
		if (syncRefs != null) {
			for (ServiceReference<SynchronousConfigurationListener> ref : syncRefs) {
				SynchronousConfigurationListener syncListener = syncTracker.getService(ref);
				if (syncListener != null) {
					try {
						syncListener.configurationEvent(event);
					} catch (Throwable t) {
						log.log(LogService.LOG_ERROR, t.getMessage(), t);
					}
				}
			}
		}
		ServiceReference<ConfigurationListener>[] refs = tracker.getServiceReferences();
		if (refs == null)
			return;

		for (int i = 0; i < refs.length; ++i) {
			final ServiceReference<ConfigurationListener> ref = refs[i];
			queue.put(new Runnable() {
				public void run() {
					ConfigurationListener listener = tracker.getService(ref);
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
