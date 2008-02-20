/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.transforms.xslt;

import java.util.Properties;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private ServiceRegistration registration;
	private ServiceTracker logTracker;

	public void start(BundleContext context) throws Exception {
		logTracker = new ServiceTracker(context, FrameworkLog.class.getName(), null);
		logTracker.open();

		Properties properties = new Properties();
		properties.put("isStreamTransformer", "true"); //$NON-NLS-1$ //$NON-NLS-2$

		Object transformer = new XSLTStreamTransformer(context.getDataFile("entities"), logTracker); //$NON-NLS-1$
		registration = context.registerService(Object.class.getName(), transformer, properties);

	}

	public void stop(BundleContext context) throws Exception {
		if (registration != null)
			registration.unregister();

		logTracker.close();
		context = null;
	}
}
