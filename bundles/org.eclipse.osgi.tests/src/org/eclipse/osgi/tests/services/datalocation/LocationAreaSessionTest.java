/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.osgi.tests.services.datalocation;

import static org.eclipse.osgi.tests.OSGiTestsActivator.PI_OSGI_TESTS;
import static org.eclipse.osgi.tests.OSGiTestsActivator.addRequiredOSGiTestsBundles;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import org.eclipse.core.tests.harness.session.CustomSessionConfiguration;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocationAreaSessionTest {
	private static final String JAVA_NIO = "java.nio"; //$NON-NLS-1$
	private static final String JAVA_IO = "java.io"; //$NON-NLS-1$
	private static final String TEST_LOCATION_DIR = "osgi.test.location.dir"; //$NON-NLS-1$

	private static Location lockedTestLocation;
	static String testLocationLockDir = OSGiTestsActivator.getContext().getDataFile("testLocation").getAbsolutePath();

	private static CustomSessionConfiguration sessionConfiguration = createSessionConfiguration();

	@RegisterExtension
	static SessionTestExtension extension = SessionTestExtension.forPlugin(PI_OSGI_TESTS)
			.withCustomization(sessionConfiguration).create();

	private static CustomSessionConfiguration createSessionConfiguration() {
		CustomSessionConfiguration configuration = SessionTestExtension.createCustomConfiguration().setReadOnly();
		configuration.setSystemProperty(TEST_LOCATION_DIR, testLocationLockDir);
		addRequiredOSGiTestsBundles(configuration);
		return configuration;
	}

	@Test
	@Order(1)
	@ExecuteInHost
	public void initializeLockWithNIO() throws InvalidSyntaxException, IOException {
		doLock(testLocationLockDir, JAVA_NIO, false, true);
	}

	@Test
	@Order(2)
	public void attempLockSameLocationWithSessionNIO() throws InvalidSyntaxException, IOException {
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_NIO, true, false);
	}

	@Test
	@Order(3)
	@ExecuteInHost
	public void releaseLockNIO() throws IOException {
		doRelease();
	}

	@Test
	@Order(4)
	public void attempLockSameLocationWithSessionAgainNIO() throws InvalidSyntaxException, IOException {
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_NIO, true, true);
	}

	@Test
	@Order(5)
	@ExecuteInHost
	public void initializeLockWithIO() throws IOException, InvalidSyntaxException {
		// Note that java.io locking only seems to work reliably on windows
		if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os"))) {
			return;
		}
		doLock(testLocationLockDir, JAVA_IO, false, true);
	}

	@Test
	@Order(6)
	public void attempLockSameLocationWithSessionIO() throws InvalidSyntaxException, IOException {
		if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os"))) {
			return;
		}
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_IO, true, false);
	}

	@Test
	@Order(7)
	@ExecuteInHost
	public void releaseLockIO() throws IOException {
		if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os"))) {
			return;
		}
		doRelease();
	}

	@Test
	@Order(7)
	public void attempLockSameLocationWithSessionAgainIO() throws InvalidSyntaxException, IOException {
		if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os"))) {
			return;
		}
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_IO, true, true);
	}

	static void doLock(String testLocationDir, String type, boolean release, boolean succeed)
			throws InvalidSyntaxException, IOException {
		String oldLockingValue = System.setProperty(LocationHelper.PROP_OSGI_LOCKING, type);
		try {
			doLock(testLocationDir, release, succeed);
		} finally {
			if (oldLockingValue == null) {
				System.getProperties().remove(LocationHelper.PROP_OSGI_LOCKING);
			} else {
				System.setProperty(LocationHelper.PROP_OSGI_LOCKING, oldLockingValue);
			}
		}
	}

	@SuppressWarnings("deprecation") // setURL
	static void doLock(String testLocationDir, boolean release, boolean succeed)
			throws InvalidSyntaxException, IOException {
		assertNotNull("The testLocationDir is not set", testLocationDir);
		ServiceReference[] refs = OSGiTestsActivator.getContext().getServiceReferences(Location.class.getName(),
				"(type=osgi.configuration.area)");
		// this is test code so we are not very careful; just assume there is at lease
		// one service. Do not copy and paste this code!!!
		Location configLocation = (Location) OSGiTestsActivator.getContext().getService(refs[0]);
		Location testLocation = null;
		try {
			testLocation = configLocation.createLocation(null, new File(testLocationDir).toURL(), false);
			testLocation.setURL(testLocation.getDefault(), false);
			// try locking location
			if (succeed ? testLocation.isLocked() : !testLocation.isLocked()) {
				fail("location should " + (succeed ? "not " : "") + "be locked");
			}
			if (succeed ? !testLocation.lock() : testLocation.lock()) {
				fail((succeed ? "Could not" : "Could") + " lock location");
			}
			if (!testLocation.isLocked()) {
				fail("location should be locked");
			}
		} finally {
			if (release && testLocation != null) {
				testLocation.release();
			}
			if (!release) {
				lockedTestLocation = testLocation;
			}
			OSGiTestsActivator.getContext().ungetService(refs[0]);
		}
	}

	static void doRelease() throws IOException {
		try {
			if (lockedTestLocation == null) {
				fail("lockedTestLocation == null !!");
			}
			if (!lockedTestLocation.isLocked()) {
				fail("lockedTestLocation is not locked!!");
			}
			lockedTestLocation.release();
			if (lockedTestLocation.isLocked()) {
				fail("lockedTestLocation is still locked!!");
			}
		} finally {
			lockedTestLocation = null;
		}
	}


}
