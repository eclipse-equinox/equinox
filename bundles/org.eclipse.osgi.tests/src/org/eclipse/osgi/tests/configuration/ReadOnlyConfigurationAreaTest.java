/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.FileSystemComparator;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class ReadOnlyConfigurationAreaTest extends TestCase {

	public static Test suite() {
		ConfigurationSessionTestSuite suite = new ConfigurationSessionTestSuite(PI_OSGI_TESTS,
				ReadOnlyConfigurationAreaTest.class);
		suite.setReadOnly(true);
		addRequiredOSGiTestsBundles(suite);
		return suite;
	}

	public ReadOnlyConfigurationAreaTest(String name) {
		super(name);
	}

	public void test0thSession() throws Exception {
		// initialization session
		Bundle installed = BundleTestingHelper.installBundle("1.0", getContext(),
				OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle01");
		// not read-only yet, should work fine
		assertTrue("installed bundle could not be resolved: " + installed,
				BundleTestingHelper.resolveBundles(getContext(), new Bundle[] { installed }));
	}

	/**
	 * Takes a snapshot of the file system.
	 *
	 * @throws IOException
	 */
	public void test1stSession() throws IOException {
		// compute and save tree image
		File configurationDir = ConfigurationSessionTestSuite.getConfigurationDir();
		FileSystemComparator comparator = new FileSystemComparator();
		Object snapshot = comparator.takeSnapshot(configurationDir, true);
		comparator.saveSnapshot(snapshot, configurationDir);
	}

	public void test1stSessionFollowUp() throws IOException {
		FileSystemComparator comparator = new FileSystemComparator();
		File configurationDir = ConfigurationSessionTestSuite.getConfigurationDir();
		Object oldSnaphot = comparator.loadSnapshot(configurationDir);
		Object newSnapshot = comparator.takeSnapshot(configurationDir, true);
		comparator.compareSnapshots("1.0", oldSnaphot, newSnapshot);
	}

	/**
	 * Tries to install a plug-in that has no manifest. Should fail because by
	 * default the manifest generation area is under the configuration area (which
	 * is read-only here)
	 */
	@SuppressWarnings("deprecation") // installBundle
	public void test2ndSession() throws BundleException, IOException {
		// try to install plug-in
		// ensure it is not installed
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle(getContext(),
					OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle02");
			// should have failed with BundleException, does not have a bundle manifest
			fail("1.0");
		} catch (BundleException be) {
			// success
		} finally {
			if (installed != null) {
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
			}
		}
	}

	public void test2ndSessionFollowUp() throws IOException {
		FileSystemComparator comparator = new FileSystemComparator();
		File configurationDir = ConfigurationSessionTestSuite.getConfigurationDir();
		Object oldSnaphot = comparator.loadSnapshot(configurationDir);
		Object newSnapshot = comparator.takeSnapshot(configurationDir, true);
		comparator.compareSnapshots("1.0", oldSnaphot, newSnapshot);
	}

	/**
	 * Tries to install a plug-in that has manifest. Should fail because by default
	 * the manifest generation area is under the configuration area (which is
	 * read-only here)
	 */
	@SuppressWarnings("deprecation") // installBundle
	public void test3rdSession() throws BundleException, IOException {
		// install plug-in
		// ensure it is not installed
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle(getContext(),
					OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle03");
			// should have failed - cannot install a bundle in read-only mode
			fail("1.0");
		} catch (BundleException be) {
			// success
		} finally {
			if (installed != null) {
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
			}
		}
	}

	public void test3rdSessionFollowUp() throws IOException {
		FileSystemComparator comparator = new FileSystemComparator();
		File configurationDir = ConfigurationSessionTestSuite.getConfigurationDir();
		Object oldSnaphot = comparator.loadSnapshot(configurationDir);
		Object newSnapshot = comparator.takeSnapshot(configurationDir, true);
		comparator.compareSnapshots("1.0", oldSnaphot, newSnapshot);
	}

}
