/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.osgi.tests.internal.plugins;

import java.io.IOException;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Provisory home for tests that install plugins.
 */
public class InstallTests extends CoreTest {

	public InstallTests() {
		super();
	}

	public InstallTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testInstallNoVersionManifest01() throws BundleException, IOException {
		// Note that this test case has changed since the removing of plugin.xml conversion
		// Before this tested that a plugin.xml with no version specified would fail.
		// It is valid for a bundle to omit Bundle-Version header (and default to version 0.0.0
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle03"); //$NON-NLS-1$
			// success - should allow manifests with no version.
		} catch (BundleException be) {
			// should not have failed with BundleException
			fail("1.0"); //$NON-NLS-1$
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	/**
	 * Test invalid manifest; missing Bundle-SymbolicName
	 */
	public void testInstallInvalidManifest02() throws IOException, BundleException {
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle("testInstallInvalidManifest02", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle11"); //$NON-NLS-1$ //$NON-NLS-2$
			// should have failed with BundleException
			fail("Expected a failure with no Bundle-SymbolicName header"); //$NON-NLS-1$
		} catch (BundleException be) {
			// success - the manifest was invalid
			assertEquals("Expected manifest error", BundleException.MANIFEST_ERROR, be.getType()); //$NON-NLS-1$
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	/**
	 * Test invalid manifest; duplicate directive
	 */
	public void testInstallInvalidManifest03() throws IOException, BundleException {
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle("testInstallInvalidManifest03", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle12"); //$NON-NLS-1$ //$NON-NLS-2$
			// should have failed with BundleException
			fail("Expected a failure with duplicate directives"); //$NON-NLS-1$
		} catch (BundleException be) {
			// success - the manifest was invalid
			assertEquals("Expected manifest error", BundleException.MANIFEST_ERROR, be.getType()); //$NON-NLS-1$
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	/**
	 * Test invalid manifest; use attributes bundle-version and bundle-symbolic-name in Export-Package
	 */
	public void testInstallInvalidManifest04() throws IOException, BundleException {
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle("testInstallInvalidManifest04", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle13"); //$NON-NLS-1$ //$NON-NLS-2$
			// should have failed with BundleException
			fail("Expected a failure with duplicate attributes"); //$NON-NLS-1$
		} catch (BundleException be) {
			// success - the manifest was invalid
			assertEquals("Expected manifest error", BundleException.MANIFEST_ERROR, be.getType()); //$NON-NLS-1$
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	/**
	 * Test invalid manifest; imports same package twice
	 */
	public void testInstallInvalidManifest05() throws IOException, BundleException {
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle("testInstallInvalidManifest05", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle14"); //$NON-NLS-1$ //$NON-NLS-2$
			// should have failed with BundleException
			fail("Expected a failure with duplicate imports"); //$NON-NLS-1$
		} catch (BundleException be) {
			// success - the manifest was invalid
			assertEquals("Expected manifest error", BundleException.MANIFEST_ERROR, be.getType()); //$NON-NLS-1$
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	/**
	 * Test unresolvable
	 */
	public void testStartError01() throws IOException, BundleException {
		Bundle installed = null;
		try {
			try {
				installed = BundleTestingHelper.installBundle("testStartError01", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle15"); //$NON-NLS-1$ //$NON-NLS-2$
				// should be able to install
			} catch (BundleException be) {
				// failed to install unresolvable bundle
				fail("Unexpected installation error", be); //$NON-NLS-1$
			}
			try {
				installed.start();
				// expected exception starting
				fail("Expected a failure to start unresolved bundle"); //$NON-NLS-1$
			} catch (BundleException be) {
				// success - the bundle can not resolve
				assertEquals("Expected manifest error", BundleException.RESOLVE_ERROR, be.getType()); //$NON-NLS-1$
			}

		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	/**
	 * Test start fragment
	 */
	public void testStartError02() throws IOException, BundleException {
		Bundle host = null;
		Bundle fragment = null;
		try {
			try {
				host = BundleTestingHelper.installBundle("testStartError02_host", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle16"); //$NON-NLS-1$ //$NON-NLS-2$
				fragment = BundleTestingHelper.installBundle("testStartError02_frag", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle17"); //$NON-NLS-1$ //$NON-NLS-2$
				// should be able to install host
			} catch (BundleException be) {
				// failed to install unresolvable bundle
				fail("Unexpected installation error", be); //$NON-NLS-1$
			}
			try {
				host.start();
			} catch (BundleException be) {
				fail("Unexpected start host error", be); //$NON-NLS-1$
			}
			try {
				fragment.start();
				// expected exception starting
				fail("Expected a failure to start fragment bundle"); //$NON-NLS-1$
			} catch (BundleException be) {
				// success - the bundle can not resolve
				assertEquals("Expected manifest error", BundleException.INVALID_OPERATION, be.getType()); //$NON-NLS-1$
			}

		} finally {
			if (host != null)
				host.uninstall();
			if (fragment != null)
				fragment.uninstall();

		}
	}

	/**
	 * Test unsupported operation with boot classpath extension
	 */
	public void testUnsupportedOperation01() throws IOException, BundleException {
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle("testUnsupportedOperation01", OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle18"); //$NON-NLS-1$ //$NON-NLS-2$
			// should have failed with BundleException
			fail("Expected an unsupported operation exception"); //$NON-NLS-1$
		} catch (BundleException be) {
			// success - the manifest was invalid
			assertEquals("Expected unsupported error", BundleException.UNSUPPORTED_OPERATION, be.getType()); //$NON-NLS-1$
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	public void testInstallLocationWithSpaces() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle 01"); //$NON-NLS-1$
		try {
			assertEquals("1.0", "bundle01", installed.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.1", Bundle.INSTALLED, installed.getState()); //$NON-NLS-1$
		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	public void testInstallLocationWithUnderscores() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle02_1.0.0"); //$NON-NLS-1$
		try {
			assertEquals("1.0", "bundle02", installed.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.1", Bundle.INSTALLED, installed.getState()); //$NON-NLS-1$
			assertEquals("1.2", new Version("2.0"), new Version(installed.getHeaders().get(Constants.BUNDLE_VERSION))); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures we see a bundle with only a extension point as a singleton */
	public void testInstallBundleWithExtensionPointOnly() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle04"); //$NON-NLS-1$
		try {
			assertEquals("1.0", "bundle04", installed.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.1", Bundle.INSTALLED, installed.getState()); //$NON-NLS-1$
			assertEquals("1.2", "1.3.7", installed.getHeaders().get(Constants.BUNDLE_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$
			String symbolicNameString = installed.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
			assertNotNull("1.3", symbolicNameString); //$NON-NLS-1$
			ManifestElement[] symbolicNameHeader = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameString);
			assertEquals("1.4", 1, symbolicNameHeader.length); //$NON-NLS-1$
			assertEquals("1.5", "true", symbolicNameHeader[0].getDirective(Constants.SINGLETON_DIRECTIVE)); //$NON-NLS-1$ //$NON-NLS-2$

		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures we see a bundle with only a extension as a singleton */
	public void testInstallBundleWithExtensionOnly() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle05"); //$NON-NLS-1$
		try {
			assertEquals("1.0", "bundle05", installed.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.1", Bundle.INSTALLED, installed.getState()); //$NON-NLS-1$
			assertEquals("1.2", "1.3.8", installed.getHeaders().get(Constants.BUNDLE_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$
			String symbolicNameString = installed.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
			assertNotNull("1.3", symbolicNameString); //$NON-NLS-1$
			ManifestElement[] symbolicNameHeader = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameString);
			assertEquals("1.4", 1, symbolicNameHeader.length); //$NON-NLS-1$
			assertEquals("1.5", "true", symbolicNameHeader[0].getDirective(Constants.SINGLETON_DIRECTIVE)); //$NON-NLS-1$ //$NON-NLS-2$

		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures we see a bundle with only extension and extension point as a singleton */
	public void testInstallBundleWithExtensionAndExtensionPoint() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle06"); //$NON-NLS-1$
		try {
			assertEquals("1.0", "bundle06", installed.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.1", Bundle.INSTALLED, installed.getState()); //$NON-NLS-1$
			assertEquals("1.2", "1.3.9", installed.getHeaders().get(Constants.BUNDLE_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$
			String symbolicNameString = installed.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
			assertNotNull("1.3", symbolicNameString); //$NON-NLS-1$
			ManifestElement[] symbolicNameHeader = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameString);
			assertEquals("1.4", 1, symbolicNameHeader.length); //$NON-NLS-1$
			assertEquals("1.5", "true", symbolicNameHeader[0].getDirective(Constants.SINGLETON_DIRECTIVE)); //$NON-NLS-1$ //$NON-NLS-2$

		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures two versions of a non-singleton bundle are accepted */
	public void testInstall2NonSingletonBundles() throws BundleException, IOException {
		Bundle installed1 = org.eclipse.core.tests.harness.BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle07"); //$NON-NLS-1$
		ServiceReference packageAdminSR = OSGiTestsActivator.getContext().getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) OSGiTestsActivator.getContext().getService(packageAdminSR);
		packageAdmin.resolveBundles(null);
		Bundle installed2 = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle07b"); //$NON-NLS-1$
		packageAdmin.resolveBundles(null);
		OSGiTestsActivator.getContext().ungetService(packageAdminSR);
		try {
			assertEquals("1.0", "bundle07", installed2.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.1", Bundle.RESOLVED, installed2.getState()); //$NON-NLS-1$
			assertEquals("1.2", "1.0.0.b", installed2.getHeaders().get(Constants.BUNDLE_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$

			assertEquals("1.3", "bundle07", installed1.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.4", Bundle.RESOLVED, installed1.getState()); //$NON-NLS-1$
			assertEquals("1.5", "1.0.0", installed1.getHeaders().get(Constants.BUNDLE_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			installed1.uninstall();
			installed2.uninstall();
		}
	}

	/** Ensures two versions of a singleton bundle are accepted */
	public void testInstall2SingletonBundles() throws BundleException, IOException {
		Bundle installed1 = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle08"); //$NON-NLS-1$
		ServiceReference packageAdminSR = OSGiTestsActivator.getContext().getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) OSGiTestsActivator.getContext().getService(packageAdminSR);
		packageAdmin.resolveBundles(null);
		Bundle installed2 = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle08b"); //$NON-NLS-1$
		packageAdmin.resolveBundles(null);
		OSGiTestsActivator.getContext().ungetService(packageAdminSR);
		try {
			assertEquals("1.0", "bundle08", installed1.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.1", Bundle.RESOLVED, installed1.getState()); //$NON-NLS-1$
			assertEquals("1.2", "1.0.0", installed1.getHeaders().get(Constants.BUNDLE_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$

			assertEquals("1.3", "bundle08", installed2.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("1.4", Bundle.INSTALLED, installed2.getState()); //$NON-NLS-1$
			assertEquals("1.5", "1.0.0.b", installed2.getHeaders().get(Constants.BUNDLE_VERSION)); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			installed1.uninstall();
			installed2.uninstall();
		}
	}

}
