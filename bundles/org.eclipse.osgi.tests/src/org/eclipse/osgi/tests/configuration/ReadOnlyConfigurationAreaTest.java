/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.configuration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import junit.framework.Test;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.FileSystemComparator;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.tests.OSGiTest;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class ReadOnlyConfigurationAreaTest extends OSGiTest {

	public static Test suite() {
		ConfigurationSessionTestSuite suite = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, ReadOnlyConfigurationAreaTest.class);
		suite.setReadOnly(true);
		String[] ids = ConfigurationSessionTestSuite.MINIMAL_BUNDLE_SET;
		for (int i = 0; i < ids.length; i++)
			suite.addBundle(ids[i]);
		suite.addBundle(PI_OSGI_TESTS);
		return suite;
	}

	public ReadOnlyConfigurationAreaTest(String name) {
		super(name);
	}

	public void test0thSession() throws MalformedURLException, IOException {
		// initialization session		
		try {
			Bundle installed = BundleTestingHelper.installBundle("1.0", getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle01");
			// not read-only yet, should work fine
			BundleTestingHelper.resolveBundles(getContext(), new Bundle[] {installed});
		} catch (BundleException be) {
			fail("1.0", be);
		}
	}

	/**
	 * Takes a snapshot of the file system.
	 */
	public void test1stSession() {
		// compute and save tree image
		File configurationDir = ConfigurationSessionTestSuite.getConfigurationDir();
		FileSystemComparator comparator = new FileSystemComparator();
		Object snapshot = comparator.takeSnapshot(configurationDir, true);
		try {
			comparator.saveSnapshot(snapshot, configurationDir);
		} catch (IOException e) {
			fail("1.0");
		}
	}

	public void test1stSessionFollowUp() throws IOException {
		FileSystemComparator comparator = new FileSystemComparator();
		File configurationDir = ConfigurationSessionTestSuite.getConfigurationDir();
		Object oldSnaphot = comparator.loadSnapshot(configurationDir);
		Object newSnapshot = comparator.takeSnapshot(configurationDir, true);
		comparator.compareSnapshots("1.0", oldSnaphot, newSnapshot);
	}

	/**
	 * Tries to install a plug-in that has no manifest. Should fail because by default the manifest generation area
	 * is under the configuration area (which is read-only here)
	 */
	public void test2ndSession() throws BundleException, IOException {
		// try to install plug-in
		// ensure it is not installed		
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle(getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle02");
			// should have failed with BundleException, does not have a bundle manifest
			fail("1.0");
		} catch (BundleException be) {
			// success
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest				
				installed.uninstall();
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
	 * Tries to install a plug-in that has manifest. Should fail because by default the manifest generation area
	 * is under the configuration area (which is read-only here)
	 */
	public void test3rdSession() throws BundleException, IOException {
		// install plug-in
		// ensure it is not installed		
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle(getContext(), OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle03");
			// should have failed - cannot install a bundle in read-only mode
			fail("1.0");
		} catch (BundleException be) {
			// success 
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest				
				installed.uninstall();
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
