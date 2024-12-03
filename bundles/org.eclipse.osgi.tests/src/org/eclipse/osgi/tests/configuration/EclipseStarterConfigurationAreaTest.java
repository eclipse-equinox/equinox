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

import static org.eclipse.osgi.tests.OSGiTestsActivator.PI_OSGI_TESTS;
import static org.eclipse.osgi.tests.OSGiTestsActivator.addRequiredOSGiTestsBundles;
import static org.eclipse.osgi.tests.OSGiTestsActivator.getContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EclipseStarterConfigurationAreaTest {
	private static final String WITH_COMPATIBILITY_FRAGMENT = "withCompatibilityFragment";

	private CustomSessionConfiguration sessionConfiguration = SessionTestExtension.createCustomConfiguration();

	@RegisterExtension
	SessionTestExtension extension = SessionTestExtension.forPlugin(PI_OSGI_TESTS)
			.withCustomization(sessionConfiguration).create();

	@BeforeEach
	@ExecuteInHost
	public void setUpSession(TestInfo testInfo) {
		addRequiredOSGiTestsBundles(sessionConfiguration);
		if (testInfo.getTags().contains(WITH_COMPATIBILITY_FRAGMENT)) {
			sessionConfiguration.addBundle(Platform.getBundle("org.eclipse.osgi.compatibility.state"));
		}
	}

	@Test
	@Tag(WITH_COMPATIBILITY_FRAGMENT)
	@Order(1)
	public void testInitializeExtension() {
		// initialization session
		List<BundleWire> fragWires = getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleWiring.class)
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		assertEquals(1, fragWires.size(), "Wrong number of system fragments.");
	}

	@Test
	@Order(2)
	public void testRemoveExtension() {
		// removed extension session
		List<BundleWire> fragWires = getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleWiring.class)
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		assertEquals(0, fragWires.size(), "Wrong number of system fragments.");
	}

}
