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

package org.eclipse.equinox.internal.transforms.xslt;

import java.util.Properties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Registers the XSLT transform type.
 */
public class Activator implements BundleActivator {

	private ServiceRegistration registration;
	private ServiceTracker logTracker;

	public void start(BundleContext context) throws Exception {
		logTracker = new ServiceTracker(context, FrameworkLog.class.getName(), null);
		logTracker.open();

		Properties properties = new Properties();
		properties.put("equinox.transformerType", "xslt"); //$NON-NLS-1$ //$NON-NLS-2$

		Object transformer = new XSLTStreamTransformer(logTracker);
		registration = context.registerService(Object.class.getName(), transformer, properties);

	}

	public void stop(BundleContext context) throws Exception {
		if (registration != null)
			registration.unregister();

		logTracker.close();
		context = null;
	}
}
