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
import java.net.MalformedURLException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.FileSystemComparator;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class MovableConfigurationAreaTest extends TestCase {

	static void doMove(final IPath sourcePath, final IPath destinationPath) {
		assertTrue("Failed moving " + sourcePath + " to " + destinationPath,
				sourcePath.toFile().renameTo(destinationPath.toFile()));
	}

	static void doTakeSnapshot(final IPath destinationPath) {
		// compute and save tree image
		File configurationDir = destinationPath.toFile();
		FileSystemComparator comparator = new FileSystemComparator();
		Object snapshot = comparator.takeSnapshot(configurationDir, true);
		try {
			comparator.saveSnapshot(snapshot, configurationDir);
		} catch (IOException e) {
			fail("1.0");
		}
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(MovableConfigurationAreaTest.class.getName());

		ConfigurationSessionTestSuite initialization = new ConfigurationSessionTestSuite(PI_OSGI_TESTS,
				MovableConfigurationAreaTest.class.getName());
		addRequiredOSGiTestsBundles(initialization);
		initialization.setReadOnly(true);
		// disable clean-up, we want to reuse the configuration
		initialization.setCleanup(false);
		initialization.addTest(new MovableConfigurationAreaTest("testInitialization"));
		suite.addTest(initialization);

		// add a helper test that just moves the configuration area
		final IPath sourcePath = initialization.getConfigurationPath();
		final IPath destinationPath = FileSystemHelper.getRandomLocation(FileSystemHelper.getTempDir());
		suite.addTest(new TestCase("testMove") {
			public void runBare() throws Throwable {
				doMove(sourcePath, destinationPath);
			}
		});
		suite.addTest(new TestCase("testTakeSnapshot") {
			public void runBare() throws Throwable {
				doTakeSnapshot(destinationPath);
			}
		});

		ConfigurationSessionTestSuite afterMoving = new ConfigurationSessionTestSuite(PI_OSGI_TESTS,
				MovableConfigurationAreaTest.class.getName());
		afterMoving.setConfigurationPath(destinationPath);
		afterMoving.addMinimalBundleSet();
		afterMoving.setReadOnly(true);
		// make sure we don't allow priming for the first run
		afterMoving.setPrime(false);
		afterMoving.addTest(new MovableConfigurationAreaTest("testAfterMoving"));
		afterMoving.addTest(new MovableConfigurationAreaTest("testVerifySnapshot"));
		suite.addTest(afterMoving);
		return suite;
	}

	public MovableConfigurationAreaTest(String name) {
		super(name);
	}

	/**
	 * Tries to install a plug-in that has no manifest. Should fail because by
	 * default the manifest generation area is under the configuration area (which
	 * is read-only here)
	 */
	@SuppressWarnings("deprecation") // installBundle
	public void testAfterMoving() throws MalformedURLException, IOException, BundleException {
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
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest
				installed.uninstall();
		}
	}

	public void testInitialization() throws Exception {
		// initialization session
		Bundle installed = BundleTestingHelper.installBundle("1.0", getContext(),
				OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle01");
		// not read-only yet, should work fine
		assertTrue("installed bundle could not be resolved: " + installed,
				BundleTestingHelper.resolveBundles(getContext(), new Bundle[] { installed }));
	}

	public void testVerifySnapshot() throws IOException {
		FileSystemComparator comparator = new FileSystemComparator();
		File configurationDir = ConfigurationSessionTestSuite.getConfigurationDir();
		Object oldSnaphot = comparator.loadSnapshot(configurationDir);
		Object newSnapshot = comparator.takeSnapshot(configurationDir, true);
		comparator.compareSnapshots("1.0", oldSnaphot, newSnapshot);
	}

}
