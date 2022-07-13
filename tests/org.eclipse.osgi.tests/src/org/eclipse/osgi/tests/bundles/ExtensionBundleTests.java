/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class ExtensionBundleTests extends AbstractBundleTests {

	public static List<String> events = new ArrayList<>();

	@Test
	public void testFrameworkExtension01() throws Exception {
		Bundle fwkext = installer.installBundle("ext.framework.a", false); //$NON-NLS-1$
		Bundle importer = installer.installBundle("ext.framework.a.importer"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {fwkext, importer});

		importer.start();
		importer.stop();
		Object[] results = simpleResults.getResults(2);
		assertTrue("1.0", results.length == 2); //$NON-NLS-1$
		assertEquals("1.1", "success", results[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.2", "success", results[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testFrameworkExtension02() throws Exception {
		Bundle fwkext = installer.installBundle("ext.framework.a", false); //$NON-NLS-1$
		Bundle importer = installer.installBundle("ext.framework.a.requires"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {fwkext, importer});

		importer.start();
		importer.stop();
		Object[] results = simpleResults.getResults(2);
		assertTrue("1.0", results.length == 2); //$NON-NLS-1$
		assertEquals("1.1", "success", results[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.2", "success", results[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testExtClasspathExtension01() throws Exception {
		Bundle fwkext = installer.installBundle("ext.extclasspath.a", false); //$NON-NLS-1$
		Bundle importer = installer.installBundle("ext.extclasspath.a.importer"); //$NON-NLS-1$
		installer.resolveBundles(new Bundle[] {fwkext, importer});

		importer.start();
		importer.stop();
		Object[] results = simpleResults.getResults(2);
		assertTrue("1.0", results.length == 2); //$NON-NLS-1$
		assertEquals("1.1", "success", results[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("1.2", "success", results[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testExtensionBundleWithRequireCapabilityOsgiEeInstalls() throws BundleException {
		Bundle b = installer.installBundle("ext.framework.osgiee.b", false);
		assertTrue("Could not resolve bundle: " + b, installer.resolveBundles(new Bundle[] {b}));
		BundleWiring wiring = b.adapt(BundleWiring.class);
		assertNotNull("No wiring for bundle: " + b, wiring);
		List<BundleWire> allRequired = wiring.getRequiredWires(null);
		assertEquals("Wrong number of wires: " + allRequired, 2, allRequired.size());
		BundleWire hostWire = wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE).get(0);
		assertEquals("Wrong provider for host: " + hostWire.getProvider().getBundle(), 0, hostWire.getProvider().getBundle().getBundleId());
		BundleWire eeWire = wiring.getRequiredWires(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE).get(0);
		assertEquals("Wrong provider for osgi.ee: " + eeWire.getProvider().getBundle(), 0, eeWire.getProvider().getBundle().getBundleId());
	}

	@Test
	public void testActivatorOrder() throws Exception {
		Bundle b = installer.installBundle("ext.framework.a", false);
		Bundle bImp = installer.installBundle("ext.framework.a.importer");
		Bundle bReq = installer.installBundle("ext.framework.a.requires");
		installer.resolveBundles(new Bundle[] {b, bImp, bReq});

		try {
			bImp.start();
			bReq.start();
		} finally {
			installer.uninstallAllBundles();
		}
		List<String> expectedEvents = Arrays.asList(bImp.getSymbolicName() + " STARTED", bReq.getSymbolicName() + " STARTED", bReq.getSymbolicName() + " STOPPED", bImp.getSymbolicName() + " STOPPED");
		assertEquals("Expected number of events not found", expectedEvents.size(), events.size());
		for (int i = 0; i < events.size(); i++) {
			assertEquals("Expected event not found", expectedEvents.get(i), events.get(i));
		}
	}

	public void tearDown() throws Exception {
		super.tearDown();
		events.clear();
	}
}
