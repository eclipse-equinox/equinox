/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.CollisionHook;

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
			readFile(testBundle.openStream(), testFile);
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

	public static void readFile(InputStream in, File file) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);

			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0) {
				fos.write(buffer, 0, count);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}

			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}
		}
	}

	public void testCollisionHook() throws BundleException, MalformedURLException, IOException {
		Bundle test1 = installer.installBundle("test");
		installer.installBundle("test2");
		try {
			test1.update(new URL(installer.getBundleLocation("test2")).openStream());
			fail("Expected to fail to update to another bsn/version that causes collision");
		} catch (BundleException e) {
			// expected;
		}
		Bundle junk = null;
		try {
			junk = OSGiTestsActivator.getContext().installBundle("junk", new URL(installer.getBundleLocation("test2")).openStream());
			fail("Expected to fail to install duplication bsn/version that causes collision");
		} catch (BundleException e) {
			// expected;
		} finally {
			if (junk != null)
				junk.uninstall();
			junk = null;
		}

		CollisionHook hook = new CollisionHook() {
			public void filterCollisions(int operationType, Bundle target, Collection collisionCandidates) {
				collisionCandidates.clear();
			}
		};
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(CollisionHook.class, hook, null);
		try {
			try {
				test1.update(new URL(installer.getBundleLocation("test2")).openStream());
			} catch (BundleException e) {
				fail("Expected to succeed in updating to a duplicate bsn/version", e);
			}
			try {
				junk = OSGiTestsActivator.getContext().installBundle("junk", new URL(installer.getBundleLocation("test2")).openStream());
			} catch (BundleException e) {
				fail("Expected to succeed to install duplication bsn/version that causes collision", e);
			} finally {
				if (junk != null)
					junk.uninstall();
				junk = null;
			}
		} finally {
			reg.unregister();
		}
	}

	public void testInstallWithInterruption() {
		Bundle test = null;
		Thread.currentThread().interrupt();
		try {
			test = installer.installBundle("test"); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			Thread.interrupted();
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}
}
