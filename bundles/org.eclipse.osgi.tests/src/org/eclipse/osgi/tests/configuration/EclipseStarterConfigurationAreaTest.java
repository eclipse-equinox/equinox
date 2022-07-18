/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.osgi.tests.configuration;

import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.tests.OSGiTest;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class EclipseStarterConfigurationAreaTest extends OSGiTest {

	public static Test suite() {
		TestSuite suite = new TestSuite(EclipseStarterConfigurationAreaTest.class.getName());

		ConfigurationSessionTestSuite initialization = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, EclipseStarterConfigurationAreaTest.class.getName());
		addRequiredOSGiTestsBundles(initialization);
		initialization.addBundle("org.eclipse.osgi.compatibility.state");
		// disable clean-up, we want to reuse the configuration
		initialization.setCleanup(false);
		initialization.addTest(new EclipseStarterConfigurationAreaTest("testInitializeExtension"));
		suite.addTest(initialization);

		// restart with cache but remove the compatibility fragment
		IPath configPath = initialization.getConfigurationPath();

		ConfigurationSessionTestSuite removeExtension = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, EclipseStarterConfigurationAreaTest.class.getName());
		removeExtension.setConfigurationPath(configPath);
		addRequiredOSGiTestsBundles(removeExtension);
		removeExtension.addTest(new EclipseStarterConfigurationAreaTest("testRemoveExtension"));
		suite.addTest(removeExtension);
		return suite;
	}

	public EclipseStarterConfigurationAreaTest(String name) {
		super(name);
	}

	public void testInitializeExtension() {
		// initialization session
		List<BundleWire> fragWires = getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleWiring.class).getProvidedWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of system fragments.", 1, fragWires.size());
	}

	public void testRemoveExtension() {
		// removed extension session
		List<BundleWire> fragWires = getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleWiring.class).getProvidedWires(HostNamespace.HOST_NAMESPACE);
		assertEquals("Wrong number of system fragments.", 0, fragWires.size());
	}

}
