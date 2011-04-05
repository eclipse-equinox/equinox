/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.console;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/**
 * This test tests if when the telnet access is restricted to a particular host address (in this case localhost),
 * a telnet connection cannot be open on another of the host's IP addresses.
 *
 */
public class TestRestrictedTelnetHost extends CoreTest {
	public static Test suite() {
		return new TestSuite(TestRestrictedTelnetHost.class);
	}

	public void testRestrictedTelnetHost() {
		Framework framework = null;
		try {
			Map configuration = new HashMap();
			configuration.put("osgi.console", "localhost:55555");
			configuration.put("osgi.configuration.area", "inner");
			EquinoxFactory factory = new EquinoxFactory();
			framework = factory.newFramework(configuration);
			framework.start();
			InetAddress address = InetAddress.getLocalHost();
			try {
				Socket clientSocket = new Socket(address, 55555);
				fail("Telnet should listen only on localhost, not on " + address.getHostAddress());
			} catch (ConnectException e) {
				// it's ok; do nothing
			}
		} catch (Exception e) {
			fail("Unexpected failure", e);
		} finally {
			try {
				framework.stop();
			} catch (BundleException e) {
				// Ignore; just try to clean up test framework
			}
		}
	}
}
