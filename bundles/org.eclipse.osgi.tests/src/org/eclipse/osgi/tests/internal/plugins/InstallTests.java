/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.internal.plugins;

import java.io.IOException;
import junit.framework.TestCase;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Provisory home for tests that install plugins.
 */
public class InstallTests extends TestCase {

	public InstallTests() {
		super();
	}

	public InstallTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testInstallInvalidManifest() throws BundleException, IOException {
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle03");
			// should have failed with BundleException
			fail("1.0");
		} catch (BundleException be) {
			// success - the manifest was invalid
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest				
				installed.uninstall();
		}
	}

	public void testInstallLocationWithSpaces() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle 01");
		try {
			assertEquals("1.0", "bundle01", installed.getSymbolicName());
			assertEquals("1.1", Bundle.INSTALLED, installed.getState());
		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	public void testInstallLocationWithUnderscores() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle02_1.0.0");
		try {
			assertEquals("1.0", "bundle02", installed.getSymbolicName());
			assertEquals("1.1", Bundle.INSTALLED, installed.getState());
			assertEquals("1.2", "2.0", installed.getHeaders().get(Constants.BUNDLE_VERSION));
		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures we see a bundle with only a extension point as a singleton */
	public void testInstallBundleWithExtensionPointOnly() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle04");
		try {
			assertEquals("1.0", "bundle04", installed.getSymbolicName());
			assertEquals("1.1", Bundle.INSTALLED, installed.getState());
			assertEquals("1.2", "1.3.7", installed.getHeaders().get(Constants.BUNDLE_VERSION));
			String symbolicNameString = (String) installed.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
			assertNotNull("1.3", symbolicNameString);
			ManifestElement[] symbolicNameHeader = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameString);
			assertEquals("1.4", 1, symbolicNameHeader.length);
			// TODO When runtime moves to singleton directive this call needs to be getDirective
			assertEquals("1.5", "true", symbolicNameHeader[0].getAttribute(Constants.SINGLETON_DIRECTIVE));

		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures we see a bundle with only a extension as a singleton */
	public void testInstallBundleWithExtensionOnly() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle05");
		try {
			assertEquals("1.0", "bundle05", installed.getSymbolicName());
			assertEquals("1.1", Bundle.INSTALLED, installed.getState());
			assertEquals("1.2", "1.3.8", installed.getHeaders().get(Constants.BUNDLE_VERSION));
			String symbolicNameString = (String) installed.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
			assertNotNull("1.3", symbolicNameString);
			ManifestElement[] symbolicNameHeader = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameString);
			assertEquals("1.4", 1, symbolicNameHeader.length);
			// TODO When runtime moves to singleton directive this call needs to be getDirective
			assertEquals("1.5", "true", symbolicNameHeader[0].getAttribute(Constants.SINGLETON_DIRECTIVE));

		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures we see a bundle with only extension and extension point as a singleton */
	public void testInstallBundleWithExtensionAndExtensionPoint() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle06");
		try {
			assertEquals("1.0", "bundle06", installed.getSymbolicName());
			assertEquals("1.1", Bundle.INSTALLED, installed.getState());
			assertEquals("1.2", "1.3.9", installed.getHeaders().get(Constants.BUNDLE_VERSION));
			String symbolicNameString = (String) installed.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
			assertNotNull("1.3", symbolicNameString);
			ManifestElement[] symbolicNameHeader = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicNameString);
			assertEquals("1.4", 1, symbolicNameHeader.length);
			// TODO When runtime moves to singleton directive this call needs to be getDirective
			assertEquals("1.5", "true", symbolicNameHeader[0].getAttribute(Constants.SINGLETON_DIRECTIVE));

		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	/** Ensures two versions of a non-singleton bundle are accepted */
	public void testInstall2NonSingletonBundles() throws BundleException, IOException {
		Bundle installed1 = org.eclipse.core.tests.harness.BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle07");
		ServiceReference packageAdminSR = OSGiTestsActivator.getContext().getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) OSGiTestsActivator.getContext().getService(packageAdminSR);
		packageAdmin.resolveBundles(null);
		Bundle installed2 = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle07b");
		packageAdmin.resolveBundles(null);
		OSGiTestsActivator.getContext().ungetService(packageAdminSR);
		try {
			assertEquals("1.0", "bundle07", installed2.getSymbolicName());
			assertEquals("1.1", Bundle.RESOLVED, installed2.getState());
			assertEquals("1.2", "1.0.0.b", installed2.getHeaders().get(Constants.BUNDLE_VERSION));

			assertEquals("1.3", "bundle07", installed1.getSymbolicName());
			assertEquals("1.4", Bundle.RESOLVED, installed1.getState());
			assertEquals("1.5", "1.0.0", installed1.getHeaders().get(Constants.BUNDLE_VERSION));
		} finally {
			installed1.uninstall();
			installed2.uninstall();
		}
	}

	/** Ensures two versions of a singleton bundle are accepted */
	public void testInstall2SingletonBundles() throws BundleException, IOException {
		Bundle installed1 = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle08");
		ServiceReference packageAdminSR = OSGiTestsActivator.getContext().getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = (PackageAdmin) OSGiTestsActivator.getContext().getService(packageAdminSR);
		packageAdmin.resolveBundles(null);
		Bundle installed2 = BundleTestingHelper.installBundle(OSGiTestsActivator.getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "internal/plugins/installTests/bundle08b");
		packageAdmin.resolveBundles(null);
		OSGiTestsActivator.getContext().ungetService(packageAdminSR);
		try {
			assertEquals("1.0", "bundle08", installed1.getSymbolicName());
			assertEquals("1.1", Bundle.RESOLVED, installed1.getState());
			assertEquals("1.2", "1.0.0", installed1.getHeaders().get(Constants.BUNDLE_VERSION));

			assertEquals("1.3", "bundle08", installed2.getSymbolicName());
			assertEquals("1.4", Bundle.INSTALLED, installed2.getState());
			assertEquals("1.5", "1.0.0.b", installed2.getHeaders().get(Constants.BUNDLE_VERSION));
		} finally {
			installed1.uninstall();
			installed2.uninstall();
		}
	}

}
