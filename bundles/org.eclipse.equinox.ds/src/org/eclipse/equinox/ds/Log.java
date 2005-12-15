/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility class with static methods for logging to LogService, if available 
 * 
 * @version $Revision: 1.2 $
 */
public class Log {
	static private ServiceTracker logTracker;

	private Log() {
	};

	static void init(BundleContext bc) {
		logTracker = new ServiceTracker(bc, LogService.class.getName(), null);
		logTracker.open();
	}

	static void dispose() {
		if (logTracker != null) {
			logTracker.close();
		}
		logTracker = null;
	}

	public static void log(int level, String message) {
		log(level, message, null);
	}

	public static void log(int level, String message, Throwable e) {
		LogService logService = (LogService) logTracker.getService();
		if (logService != null) {
			logService.log(level, message, e);
		}
	}

}