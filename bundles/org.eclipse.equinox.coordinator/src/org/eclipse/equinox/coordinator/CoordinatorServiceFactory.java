/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	private final LogTracker logTracker;
	private final Timer timer = new Timer(true);

	public CoordinatorServiceFactory(BundleContext bundleContext) {
		logTracker = new LogTracker(bundleContext, System.out);
	}

	public Coordinator getService(Bundle bundle, ServiceRegistration<Coordinator> registration) {
		return new CoordinatorImpl(bundle, logTracker, timer);
	}

	public void ungetService(Bundle bundle, ServiceRegistration<Coordinator> registration, Coordinator service) {
		((CoordinatorImpl) service).shutdown();
	}

	void shutdown() {
		timer.cancel();
		logTracker.close();
	}
}
