/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.File;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class BundleInstallUpdateTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(BundleInstallUpdateTests.class);
	}

	// test installing with location
	public void testInstallWithLocation01() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test installing with location and null stream
	public void testInstallWithLocation02() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location, null);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test installing with location and non-null stream
	public void testInstallWithStream03() {
		Bundle test = null;
		try {
			String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
			String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location1, new URL(location2).openStream());
			assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateNoStream01() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			test.update();
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateNoStream02() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			test.update(null);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateWithStream01() {
		Bundle test = null;
		try {
			String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
			String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location1);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			test.update(new URL(location2).openStream());
			assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateWithStream02() {
		Bundle test = null;
		try {
			String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
			String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location1);
			Bundle b1 = installer.installBundle("chain.test"); //$NON-NLS-1$
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			test.update(new URL(location2).openStream());
			assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			// make sure b1 is still last bundle in bundles list
			Bundle[] bundles = OSGiTestsActivator.getContext().getBundles();
			assertTrue("Wrong bundle at the end: " + bundles[bundles.length - 1], bundles[bundles.length - 1] == b1); //$NON-NLS-1$
			Bundle[] tests = installer.getPackageAdmin().getBundles(test.getSymbolicName(), null);
			assertNotNull("null tests", tests); //$NON-NLS-1$
			assertEquals("Wrong number", 1, tests.length); //$NON-NLS-1$
			assertTrue("Wrong bundle: " + tests[0], tests[0] == test); //$NON-NLS-1$
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	public void testBug290193() {
		Bundle test = null;
		try {
			URL testBundle = OSGiTestsActivator.getContext().getBundle().getEntry("test_files/security/bundles/signed.jar");
			File testFile = OSGiTestsActivator.getContext().getDataFile("test with space/test.jar");
			assertTrue(testFile.getParentFile().mkdirs());
			AdaptorUtil.readFile(testBundle.openStream(), testFile);
			test = OSGiTestsActivator.getContext().installBundle("reference:" + testFile.toURI().toString());
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}
}
