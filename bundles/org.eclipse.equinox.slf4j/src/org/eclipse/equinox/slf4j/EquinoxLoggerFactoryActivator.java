/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.slf4j;

import org.eclipse.equinox.log.ExtendedLogService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Manages the interaction with OSGi objects
 */
public class EquinoxLoggerFactoryActivator implements BundleActivator, BundleListener {

	@Override
	public void start(BundleContext context) throws Exception {
		context.addBundleListener(this);
		ServiceTracker<ExtendedLogService, ExtendedLogService> tracker = new ServiceTracker<>(context,
				ExtendedLogService.class, new ServiceTrackerCustomizer<ExtendedLogService, ExtendedLogService>() {

					@Override
					public ExtendedLogService addingService(ServiceReference<ExtendedLogService> reference) {
						ExtendedLogService service = context.getService(reference);
						if (service != null) {
							if (EquinoxLoggerFactory.logService.compareAndSet(null, service)) {
								EquinoxLoggerFactory.LOGGER_MAP.clear();
							}
						}
						return service;
					}

					@Override
					public void modifiedService(ServiceReference<ExtendedLogService> reference,
							ExtendedLogService service) {
						// don't care...

					}

					@Override
					public void removedService(ServiceReference<ExtendedLogService> reference,
							ExtendedLogService service) {
						if (EquinoxLoggerFactory.logService.compareAndSet(service, null)) {
							EquinoxLoggerFactory.LOGGER_MAP.clear();
						}
					}
				});
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context.removeBundleListener(this);
		EquinoxLoggerFactory.logService.set(null);
		EquinoxLoggerFactory.LOGGER_MAP.clear();
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.UNINSTALLED || event.getType() == BundleEvent.UNRESOLVED) {
			EquinoxLoggerFactory.LOGGER_MAP.remove(event.getBundle());
		}
	}

}
