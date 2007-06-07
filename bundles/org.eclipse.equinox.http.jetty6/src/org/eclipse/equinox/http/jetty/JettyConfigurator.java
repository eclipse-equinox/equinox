/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
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
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationPermission;
import org.eclipse.equinox.http.jetty.internal.Activator;

/**
 * <p>
 * JettyConfigurator provides API level access for creating configured instances of a Jetty-based Http Service.
 * The created instances are not persistent across re-starts of the bundle.
 * </p>
 * Settings: <br />
 * <ul>
 * 		<li>name="http.enabled" type="Boolean" (default: true)</li>
 * 		<li>name="http.port" type="Integer" (default: 0 -- first available port)</li>
 * 		<li>name="http.host" type="String" (default: 0.0.0.0 -- all network adapters)</li>
 * 		<li>name="https.enabled" type="Boolean" (default: false)</li>
 * 		<li>name="https.port" type="Integer" (default: 0 -- first available port)</li>
 * 		<li>name="https.host" type="String" (default: 0.0.0.0 -- all network adapters)</li>
 * 		<li>name="ssl.keystore" type="String"</li>
 * 		<li>name="ssl.password" type="String"</li>
 * 		<li>name="ssl.keypassword" type="String"</li>
 * 		<li>name="ssl.needclientauth" type="Boolean"</li>
 * 		<li>name="ssl.wantclientauth" type="Boolean"</li>
 * 		<li>name="ssl.protocol" type="String"</li>
 * 		<li>name="ssl.algorithm" type="String"</li>
 * 		<li>name="ssl.keystoretype" type="String"</li>
 * 		<li>name="context.path" type="String"</li>
 * 		<li>name="context.sessioninactiveinterval" type="Integer"</li>
 * 		<li>name="other.info" type="String"</li>
 * </ul>
 *
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
	public static void startServer(String id, Dictionary settings) throws Exception {
		checkConfigurationPermission();
		String pid = PID_PREFIX + id;
		settings.put(Constants.SERVICE_PID, pid);
		Activator.startServer(pid, settings);
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
