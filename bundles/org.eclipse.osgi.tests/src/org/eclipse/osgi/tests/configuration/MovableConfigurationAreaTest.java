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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.FileSystemComparator;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MovableConfigurationAreaTest {

	@TempDir
	static Path destinationConfigurationDir;

	private static CustomSessionConfiguration sessionConfiguration = createSessionConfiguration();

	@RegisterExtension
	static SessionTestExtension extension = SessionTestExtension.forPlugin(PI_OSGI_TESTS)
			.withCustomization(sessionConfiguration).create();

	private static CustomSessionConfiguration createSessionConfiguration() {
		CustomSessionConfiguration configuration = SessionTestExtension.createCustomConfiguration().setReadOnly();
		addRequiredOSGiTestsBundles(configuration);
		return configuration;
	}

	private static void takeSnapshot(final Path path) throws IOException {
		// compute and save tree image
		FileSystemComparator comparator = new FileSystemComparator();
		Object snapshot = comparator.takeSnapshot(path.toFile(), true);
		comparator.saveSnapshot(snapshot, path.toFile());
	}

	@Test
	@Order(1)
	public void testInitialization() throws Exception {
		// initialization session
		Bundle installed = BundleTestingHelper.installBundle("1.0", getContext(),
				OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle01");
		// not read-only yet, should work fine
		assertTrue(BundleTestingHelper.resolveBundles(getContext(), new Bundle[] { installed }),
				"installed bundle could not be resolved: " + installed);
	}

	@Test
	@Order(2)
	@ExecuteInHost
	public void moveConfigurationDirectory() throws IOException {
		Files.deleteIfExists(destinationConfigurationDir);
		Files.move(sessionConfiguration.getConfigurationDirectory(), destinationConfigurationDir);
		takeSnapshot(destinationConfigurationDir);
		sessionConfiguration.setConfigurationDirectory(destinationConfigurationDir);
	}

	/**
	 * Tries to install a plug-in that has no manifest. Should fail because by
	 * default the manifest generation area is under the configuration area (which
	 * is read-only here)
	 */
	@Test
	@Order(3)
	@SuppressWarnings("deprecation")
	public void testAfterMoving() throws BundleException {
		// try to install plug-in and ensure it is not installed
		AtomicReference<Bundle> installed = new AtomicReference<>();
		assertThrows(BundleException.class, () -> {
			Bundle bundle = BundleTestingHelper.installBundle(getContext(),
					OSGiTestsActivator.TEST_FILES_ROOT + "configuration/bundle02");
			installed.set(bundle);
		});
		if (installed.get() != null) {
			// clean-up - only runs if we end-up accepting an invalid manifest
			installed.get().uninstall();
		}
	}

	@Test
	@Order(4)
	@ExecuteInHost
	public void testVerifySnapshot() throws IOException {
		FileSystemComparator comparator = new FileSystemComparator();
		Object oldSnaphot = comparator.loadSnapshot(destinationConfigurationDir.toFile());
		takeSnapshot(destinationConfigurationDir);
		Object newSnapshot = comparator.loadSnapshot(destinationConfigurationDir.toFile());
		comparator.compareSnapshots("", oldSnaphot, newSnapshot);
	}

}
