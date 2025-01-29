/*******************************************************************************
 * Copyright (c) 2005, 2024 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Christoph Läubrich - add support for Coordinator
 *******************************************************************************/
package org.eclipse.equinox.internal.cm;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * EventDispatcher is responsible for delivering Configuration Events to
 * ConfigurationListeners. The originating ConfigAdmin ServiceReference is
 * needed when delivering events. This reference is made available by the
 * service factory before returning the service object.
 */

public class EventDispatcher {
	final ServiceTracker<ConfigurationListener, ConfigurationListener> tracker;
	final ServiceTracker<SynchronousConfigurationListener, SynchronousConfigurationListener> syncTracker;
	private final SerializedTaskQueue queue = new SerializedTaskQueue("ConfigurationListener Event Queue"); //$NON-NLS-1$
	/** @GuardedBy this */
	private ServiceReference<ConfigurationAdmin> configAdminReference;
	final LogTracker log;
	private final ConfigurationAdminFactory configurationAdminFactory;

	public EventDispatcher(BundleContext context, LogTracker log, ConfigurationAdminFactory configurationAdminFactory) {
		this.log = log;
		this.configurationAdminFactory = configurationAdminFactory;
		tracker = new ServiceTracker<>(context, ConfigurationListener.class, null);
		syncTracker = new ServiceTracker<>(context, SynchronousConfigurationListener.class, null);
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
						log.error(t.getMessage(), t);
					}
				}
			}
		}
		ServiceReference<ConfigurationListener>[] refs = tracker.getServiceReferences();
		if (refs == null)
			return;

		for (final ServiceReference<ConfigurationListener> ref : refs) {
			configurationAdminFactory.executeCoordinated(new Object(), () -> enqueue(event, ref));
		}
	}

	protected void enqueue(final ConfigurationEvent event, final ServiceReference<ConfigurationListener> ref) {
		queue.put(new Runnable() {
			@Override
			public void run() {
				ConfigurationListener listener = tracker.getService(ref);
				if (listener == null) {
					return;
				}
				try {
					listener.configurationEvent(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		});
	}

	private synchronized ConfigurationEvent createConfigurationEvent(int type, String factoryPid, String pid) {
		if (configAdminReference == null)
			return null;

		return new ConfigurationEvent(configAdminReference, type, factoryPid, pid);
	}

}
