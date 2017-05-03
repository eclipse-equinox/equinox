/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.jetty;

import java.security.Permission;
import java.util.Dictionary;
import org.eclipse.equinox.http.jetty.internal.Activator;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationPermission;

/**
 * <p>
 * JettyConfigurator provides API level access for creating configured instances of a Jetty-based Http Service.
 * The created instances are not persistent across re-starts of the bundle.
 * </p>
 * 
 * @see org.eclipse.equinox.http.jetty.JettyConstants
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class JettyConfigurator {
	private static final String PID_PREFIX = "org.eclipse.equinox.http.jetty.JettyConfigurator."; //$NON-NLS-1$
	private static Permission configurationPermission = new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE); //$NON-NLS-1$

	/**
	 * Creates an instance of Jetty parameterized with a dictionary of settings
	 * @param id The identifier for the server instance
	 * @param settings The dictionary of settings used to configure the server instance
	 * @throws Exception If the server failed to start for any reason
	 */
	public static void startServer(String id, Dictionary<String, ? extends Object> settings) throws Exception {
		checkConfigurationPermission();
		String pid = PID_PREFIX + id;
		@SuppressWarnings("unchecked")
		Dictionary<String, Object> raw = (Dictionary<String, Object>) settings;
		raw.put(Constants.SERVICE_PID, pid);
		Activator.startServer(pid, raw);
	}

	/**
	 * Stops a previously started instance of Jetty. If the identified instance is not started this will call will do nothing.
	 * @param id The identifier for the server instance
	 * @throws Exception If the server failed to stop for any reason.
	 */
	public static void stopServer(String id) throws Exception {
		checkConfigurationPermission();
		Activator.stopServer(PID_PREFIX + id);
	}

	private static void checkConfigurationPermission() throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(configurationPermission);
	}
}
