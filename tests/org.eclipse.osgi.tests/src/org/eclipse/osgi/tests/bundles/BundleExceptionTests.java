/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
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
import static org.junit.Assert.assertThrows;

import java.io.InputStream;
import java.net.URL;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BundleExceptionTests extends AbstractBundleTests {

	// test throwing exception from activator constructor
	@Test
	public void testInvalidBundleActivator01() throws BundleException {
		Bundle error1 = installer.installBundle("activator.error1"); //$NON-NLS-1$

		BundleException e = assertThrows(BundleException.class, error1::start);
		assertEquals("Expected activator error", BundleException.ACTIVATOR_ERROR, e.getType());
	}

	// test throwing exception from activator start
	@Test
	public void testInvalidBundleActivator02() throws BundleException {
		Bundle error1 = installer.installBundle("activator.error2"); //$NON-NLS-1$

		BundleException e = assertThrows(BundleException.class, error1::start);
		assertEquals("Expected activator error", BundleException.ACTIVATOR_ERROR, e.getType());
	}

	// test throwing exception from activator stop
	@Test
	public void testInvalidBundleActivator03() throws BundleException {
		Bundle error1 = installer.installBundle("activator.error3"); //$NON-NLS-1$
		error1.start();

		BundleException e = assertThrows(BundleException.class, error1::stop);
		assertEquals("Expected activator error", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
	}

	// test throwing exception when installing duplicate bundles
	@Test
	public void testDuplicateError01() throws BundleException {
		installer.installBundle("activator.error1"); //$NON-NLS-1$

		BundleException e = assertThrows(BundleException.class, () -> installer.installBundle("activator.error4"));
		assertEquals("Expected duplicate error", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType()); //$NON-NLS-1$
	}

	// test throwing exception when updating to a duplicate bundle
	@Test
	public void testDuplicateError02() throws Exception {
		installer.installBundle("activator.error1"); //$NON-NLS-1$
		Bundle error2 = installer.installBundle("activator.error2"); //$NON-NLS-1$
		try (InputStream input = new URL(installer.getBundleLocation("activator.error4")).openStream()) {
			BundleException e = assertThrows(BundleException.class, () -> error2.update(input));
			assertEquals("Expected duplicate error", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType()); //$NON-NLS-1$
		}
	}

	// test uninstalling the system bundle
	@Test
	public void testUninstallSystemBundle() {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(0);
		assertNotNull("System Bundle is null!!", systemBundle); //$NON-NLS-1$
		BundleException e = assertThrows(BundleException.class, systemBundle::uninstall);
		assertEquals("Expected invalid error", BundleException.INVALID_OPERATION, e.getType()); //$NON-NLS-1$
	}
}
