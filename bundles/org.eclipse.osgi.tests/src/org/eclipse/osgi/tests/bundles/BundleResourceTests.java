/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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

import java.net.URL;
import java.util.Enumeration;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class BundleResourceTests extends CoreTest {
	private BundleInstaller installer;

	protected void setUp() throws Exception {
		try {
			installer = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "resourcetests/bundles", OSGiTestsActivator.getContext()); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Failed to create bundle installer", e); //$NON-NLS-1$
		}
	}

	protected void tearDown() throws Exception {
		installer.shutdown();
	}

	public static Test suite() {
		return new TestSuite(BundleResourceTests.class);
	}

	public void testBug320546_01() throws Exception {
		Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
		URL result = bundle.getEntry("../../../../security");
		assertNull("Found resource!", result);
		result = bundle.getEntry("../file.txt");
		assertNull("Found resource!", result);
		result = bundle.getEntry("../../");
		assertNull("Found resource!", result);
		result = bundle.getEntry("folder/../../file.txt");
		assertNull("Found resource!", result);
		result = bundle.getEntry("folder/../plugin.xml");
		assertNotNull("Did not find resource!", result);
		result = bundle.getEntry("/plugin.xml");
		assertNotNull("Did not find resource!", result);
	}

	public void testBug320546_02() throws Exception {
		Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
		Enumeration paths = bundle.getEntryPaths("../");
		assertNull("found resource!", paths);
		paths = bundle.getEntryPaths("folder");
		assertNotNull("Did not find resource!", paths);
		paths = bundle.getEntryPaths("folder/../../");
		assertNull("found resource!", paths);
		paths = bundle.getEntryPaths("folder/..");
		assertNotNull("Did not find resource!", paths);
	}

	public void testBreakOutDirBundle() throws Exception {
		Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
		URL result = bundle.getEntry("../testout/file.txt");
		assertNull("Found resource!", result);
	}

	public void testBug395274() throws Exception {
		ServiceReference<EnvironmentInfo> infoRef = OSGiTestsActivator.getContext().getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo info = OSGiTestsActivator.getContext().getService(infoRef);
		String original = info.setProperty("osgi.strictBundleEntryPath", "true");
		try {
			Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
			URL path = bundle.getEntry("META-INF./MANIFEST.MF");
			assertNull("found resource!", path);
			path = bundle.getEntry("META-INF/MANIFEST.MF");
			assertNotNull("Did not find resource!", path);
			path = bundle.getEntry("folder/file1.TXT");
			assertNull("found resource!", path);
			path = bundle.getEntry("folder/file1.txt");
			assertNotNull("Did not find resource!", path);
			checkEntries(bundle, "/./file1.txt", 1);
			checkEntries(bundle, "//file1.txt", 1);
			checkEntries(bundle, "/", 1);
			checkEntries(bundle, "/.", 1);
		} finally {
			info.setProperty("osgi.strictBundleEntryPath", original);
			OSGiTestsActivator.getContext().ungetService(infoRef);
		}
	}

	public void testBug328795() throws BundleException {
		Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
		checkEntries(bundle, "notFound\\", 0); // this results in invalid syntax exception which is logged because of trailing escape
		checkEntries(bundle, "notFound\\\\", 0); // test escaped escape "notFound\"
		checkEntries(bundle, "notFound(", 0); // test unescaped trailing (
		checkEntries(bundle, "notFound\\(", 0); // test escaped trailing (
		checkEntries(bundle, "notFound)", 0); // test unescaped trailing )
		checkEntries(bundle, "notFound\\)", 0); // test escaped trailing )
		checkEntries(bundle, "notFound*", 0); // test trailing unescaped *
		checkEntries(bundle, "notFound\\*", 0); // test trailing escaped *
		checkEntries(bundle, "paren(.txt", 1); // test unescaped ( -> should find one
		checkEntries(bundle, "paren\\(.txt", 1); // test escaped ( -> should find one
		checkEntries(bundle, "paren\\\\(.txt", 0); // test escaped escape before unescaped ( -> should find none; looks for paren\(.txt file
		checkEntries(bundle, "paren).txt", 1); // test unescaped ) -> should find one
		checkEntries(bundle, "paren\\).txt", 1); // test escaped ) -> should find one
		checkEntries(bundle, "paren\\\\).txt", 0); // test escaped escape before unescaped ) -> should find none; looks for paren\).txt file
		checkEntries(bundle, "paren(", 1); // test unescaped trailing ( -> should find one
		checkEntries(bundle, "paren\\(", 1); // test escaped trailing ( -> should find one
		checkEntries(bundle, "paren\\\\(", 0); // test escaped escape before ( -> should find none; looks for paren\(
		checkEntries(bundle, "paren)", 1); // test unescaped trailing ( -> should find one
		checkEntries(bundle, "paren\\)", 1); // test escaped trailing ( -> should find one
		checkEntries(bundle, "paren\\\\)", 0); // test escaped escape before ) -> should find none; looks for paren\)
		checkEntries(bundle, "paren*", 4); // test trailing wild cards
		checkEntries(bundle, "paren*.txt", 2); // test middle wild cards
		checkEntries(bundle, "paren\\*", 0); // test escaped wild card -> should find none; looks for paren*
		checkEntries(bundle, "paren\\\\*", 0); // test escaped escape before wild card -> should find none; looks for paren\*
		checkEntries(bundle, "p*r*n*", 4); // test multiple wild cards
		checkEntries(bundle, "p*r*n*.txt", 2); // test multiple wild cards
		checkEntries(bundle, "*)*", 2);
		checkEntries(bundle, "*(*", 2);
		checkEntries(bundle, "*\\)*", 2);
		checkEntries(bundle, "*\\(*", 2);
	}

	public void testBug338081() throws BundleException {
		Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
		// Empty string same as '/' for bundle root
		Enumeration entries = bundle.findEntries("", "file1.txt", false); //$NON-NLS-1$
		assertNotNull("An entry should have been found", entries);
		assertTrue("An entry should have been found", entries.hasMoreElements());
		assertTrue("Wrong entry found", ((URL) entries.nextElement()).toString().indexOf("file1.txt") > -1);
		assertFalse("Only one entry should have been found", entries.hasMoreElements());
	}

	private void checkEntries(Bundle bundle, String filePattern, int expectedNumber) {
		Enumeration entries = bundle.findEntries("folder", filePattern, false);
		if (expectedNumber == 0) {
			assertNull("Expected nothing here.", entries);
			return;
		}
		int i = 0;
		while (entries.hasMoreElements()) {
			entries.nextElement();
			i++;
		}
		assertEquals("Unexpected number of entries", expectedNumber, i);
	}
}
