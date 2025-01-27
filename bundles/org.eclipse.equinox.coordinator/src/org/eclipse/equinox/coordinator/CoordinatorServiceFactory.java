/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import java.util.Timer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;

public class CoordinatorServiceFactory implements ServiceFactory<Coordinator> {
	private final BundleContext bundleContext;
	private final LogTracker logTracker;
	private final Timer timer = new Timer(true);

	public CoordinatorServiceFactory(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		logTracker = new LogTracker(bundleContext, System.out);
	}

	@Override
	public Coordinator getService(Bundle bundle, ServiceRegistration<Coordinator> registration) {
		return new CoordinatorImpl(bundle, logTracker, timer, getMaxTimeout());
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<Coordinator> registration, Coordinator service) {
		((CoordinatorImpl) service).shutdown();
	}

	void shutdown() {
		timer.cancel();
		logTracker.close();
	}

	private long getMaxTimeout() {
		String prop = bundleContext.getProperty("org.eclipse.equinox.coordinator.timeout"); //$NON-NLS-1$
		// Intentionally letting the possible NumberFormatException propagate.
		return prop == null ? 0 : Long.parseLong(prop);
	}
}
