/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

import java.io.IOException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BundleExceptionTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(BundleExceptionTests.class);
	}

	// test throwing exception from activator constructor
	public void testInvalidBundleActivator01() throws BundleException {
		Bundle error1 = installer.installBundle("activator.error1"); //$NON-NLS-1$
		try {
			error1.start();
			fail("Expected a start failure on invalid activator"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Expected activator error", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
		}
	}

	// test throwing exception from activator start
	public void testInvalidBundleActivator02() throws BundleException {
		Bundle error1 = installer.installBundle("activator.error2"); //$NON-NLS-1$
		try {
			error1.start();
			fail("Expected a start failure on invalid activator"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Expected activator error", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
		}
	}

	// test throwing exception from activator stop
	public void testInvalidBundleActivator03() throws BundleException {
		Bundle error1 = installer.installBundle("activator.error3"); //$NON-NLS-1$
		error1.start();
		try {
			error1.stop();
			fail("Expected a stop failure on invalid activator"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Expected activator error", BundleException.ACTIVATOR_ERROR, e.getType()); //$NON-NLS-1$
		}
	}

	// test throwing exception when installing duplicate bundles
	public void testDuplicateError01() throws BundleException {
		installer.installBundle("activator.error1"); //$NON-NLS-1$
		try {
			installer.installBundle("activator.error4"); //$NON-NLS-1$;
			fail("Expected an install failure on duplicate bundle"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Expected duplicate error", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType()); //$NON-NLS-1$
		}
	}

	// test throwing exception when updating to a duplicate bundle
	public void testDuplicateError02() throws BundleException {
		installer.installBundle("activator.error1"); //$NON-NLS-1$
		Bundle error2 = installer.installBundle("activator.error2"); //$NON-NLS-1$
		try {
			URL updateURL = new URL(installer.getBundleLocation("activator.error4")); //$NON-NLS-1$
			error2.update(updateURL.openStream());
			fail("Expected an update failure on duplicate bundle"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Expected duplicate error", BundleException.DUPLICATE_BUNDLE_ERROR, e.getType()); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Unexpected io exception updating", e); //$NON-NLS-1$
		}
	}

	// test uninstalling the system bundle
	public void testUninstallSystemBundle() {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(0);
		assertNotNull("System Bundle is null!!", systemBundle); //$NON-NLS-1$
		try {
			systemBundle.uninstall();
			fail("Expected error on uninstall of system bundle"); //$NON-NLS-1$
		} catch (BundleException e) {
			assertEquals("Expected invalid error", BundleException.INVALID_OPERATION, e.getType()); //$NON-NLS-1$
		}
	}
}
