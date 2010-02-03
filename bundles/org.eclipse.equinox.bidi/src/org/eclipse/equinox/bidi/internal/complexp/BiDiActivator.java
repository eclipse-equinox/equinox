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
package org.eclipse.equinox.bidi.internal.complexp;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class BiDiActivator implements BundleActivator {

	private ServiceTracker logTracker = null;
	private BundleContext bundleContext;

	private static BiDiActivator instance;

	public BiDiActivator() {
		instance = this; // there is only one bundle activator
	}

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		instance = this;
	}

	public void stop(BundleContext context) throws Exception {
		if (logTracker != null) {
			logTracker.close();
			logTracker = null;
		}
		bundleContext = null;
	}

	private FrameworkLog getFrameworkLog() {
		if (logTracker == null) {
			logTracker = new ServiceTracker(bundleContext, FrameworkLog.class.getName(), null);
			logTracker.open();
		}
		return (FrameworkLog) logTracker.getService();
	}

	static public void logError(String message, Exception e) {
		FrameworkLog frameworkLog = instance.getFrameworkLog();
		if (frameworkLog != null) {
			frameworkLog.log(new FrameworkLogEntry("org.eclipse.equinox.bidi", FrameworkLogEntry.ERROR, 1, message, 0, e, null)); //$NON-NLS-1$
			return;
		}
		System.err.println(message);
		if (e != null)
			e.printStackTrace();
	}

}
