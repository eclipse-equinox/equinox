/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.equinox.internal.transforms.xslt;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Registers the XSLT transform type.
 */
public class Activator implements BundleActivator {

	private ServiceRegistration<Object> registration;
	private ServiceTracker<FrameworkLog, FrameworkLog> logTracker;

	public void start(BundleContext context) throws Exception {
		logTracker = new ServiceTracker<>(context, FrameworkLog.class, null);
		logTracker.open();

		Dictionary<String, String> properties = new Hashtable<>();
		properties.put("equinox.transformerType", "xslt"); //$NON-NLS-1$ //$NON-NLS-2$

		Object transformer = new XSLTStreamTransformer(logTracker);
		registration = context.registerService(Object.class, transformer, properties);

	}

	public void stop(BundleContext context) throws Exception {
		if (registration != null)
			registration.unregister();

		logTracker.close();
	}
}
