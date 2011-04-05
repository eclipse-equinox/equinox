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

import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.eclipse.osgi.framework.internal.core.FrameworkCommandProvider;
import org.eclipse.osgi.framework.internal.core.FrameworkConsoleSession;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.eclipse.osgi.tests.OSGiTest;
import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;

/**
 * This test tests if the built-in console is correctly disabled - the Framework command provider should not be registered,
 * as well as the FrameworkConsoleSession.
 *
 */
public class TestEquinoxStartWithoutConsole extends OSGiTest {
	public static Test suite() {
		return new TestSuite(TestEquinoxStartWithoutConsole.class);
	}

	public void testEquinoxStart() {
		Framework framework = null;
		try {
			Map configuration = new HashMap();
			configuration.put("osgi.console.enable.builtin", "false");
			configuration.put("osgi.configuration.area", "inner");
			EquinoxFactory factory = new EquinoxFactory();
			framework = factory.newFramework(configuration);
			framework.start();
			BundleContext context = framework.getBundleContext();

			ServiceReference[] commandProviders = context.getAllServiceReferences(CommandProvider.class.getName(), null);
			if (commandProviders != null) {
				for (int i = 0; i < commandProviders.length; i++) {
					if (commandProviders[i] instanceof FrameworkCommandProvider) {
						fail("FrameworkCommandProvider is registered, but should not be");
					}
				}
			}

			ServiceReference[] consoleSessions = context.getAllServiceReferences(ConsoleSession.class.getName(), null);
			if (consoleSessions != null) {
				for (int i = 0; i < consoleSessions.length; i++) {
					if (consoleSessions[i] instanceof FrameworkConsoleSession) {
						fail("FrameworkConsoleSession is registered, but should not be");
					}
				}
			}
		} catch (Exception e) {
			fail("Unexpected failure", e);
		} finally {
			if (framework != null)
				try {
					framework.stop();
				} catch (BundleException e) {
					// Ignore; just trying to clean up the test framework
				}
		}
	}
}
