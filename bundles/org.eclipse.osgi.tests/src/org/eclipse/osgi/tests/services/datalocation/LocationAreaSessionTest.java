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

import java.io.File;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.core.tests.session.ConfigurationSessionTestSuite;
import org.eclipse.core.tests.session.SetupManager.SetupException;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class LocationAreaSessionTest extends TestCase {
	private static final String JAVA_NIO = "java.nio"; //$NON-NLS-1$
	private static final String JAVA_IO = "java.io"; //$NON-NLS-1$
	private static final String TEST_LOCATION_DIR = "osgi.test.location.dir"; //$NON-NLS-1$

	private static Location lockedTestLocation;
	static String testLocationLockDir = OSGiTestsActivator.getContext().getDataFile("testLocation").getAbsolutePath();

	public static Test suite() {
		TestSuite suite = new TestSuite(LocationAreaSessionTest.class.getName());

		// first lock with java.nio
		suite.addTest(new TestCase("testLockJavaNIO") {
			@Override
			public void runBare() throws Throwable {
				doLock(testLocationLockDir, JAVA_NIO, false, true);
			}
		});

		// attempt to lock same location with a session
		ConfigurationSessionTestSuite sessionLock = new ConfigurationSessionTestSuite(PI_OSGI_TESTS,
				LocationAreaSessionTest.class.getName());
		addRequiredOSGiTestsBundles(sessionLock);
		try {
			sessionLock.getSetup().setSystemProperty(TEST_LOCATION_DIR, testLocationLockDir);
		} catch (SetupException e) {
			// what can we do; just fail the testcase later when the prop is not set.
			e.printStackTrace();
		}
		sessionLock.addTest(new LocationAreaSessionTest("testSessionFailLockJavaNIO"));
		suite.addTest(sessionLock);

		// now release lock
		suite.addTest(new TestCase("testReleaseJavaNIO") {
			@Override
			public void runBare() throws Throwable {
				doRelease();
			}
		});

		// attempt to lock the location with a session
		sessionLock = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, LocationAreaSessionTest.class.getName());
		addRequiredOSGiTestsBundles(sessionLock);
		try {
			sessionLock.getSetup().setSystemProperty(TEST_LOCATION_DIR, testLocationLockDir);
		} catch (SetupException e) {
			// what can we do; just fail the testcase later when the prop is not set.
			e.printStackTrace();
		}
		sessionLock.addTest(new LocationAreaSessionTest("testSessionSuccessLockJavaNIO"));
		suite.addTest(sessionLock);

		// now test with java.io
		suite.addTest(new TestCase("testLockJavaIO") {
			@Override
			public void runBare() throws Throwable {
				// Note that java.io locking only seems to work reliably on windows
				if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os")))
					return;
				doLock(testLocationLockDir, JAVA_IO, false, true);
			}
		});

		// attempt to lock same location with a session
		sessionLock = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, LocationAreaSessionTest.class.getName());
		addRequiredOSGiTestsBundles(sessionLock);
		try {
			sessionLock.getSetup().setSystemProperty(TEST_LOCATION_DIR, testLocationLockDir);
		} catch (SetupException e) {
			// what can we do; just fail the testcase later when the prop is not set.
			e.printStackTrace();
		}
		sessionLock.addTest(new LocationAreaSessionTest("testSessionFailLockJavaIO"));
		suite.addTest(sessionLock);

		// now release lock
		suite.addTest(new TestCase("testReleaseJavaIO") {
			@Override
			public void runBare() throws Throwable {
				if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os")))
					return;
				doRelease();
			}
		});

		// attempt to lock the location with a session
		sessionLock = new ConfigurationSessionTestSuite(PI_OSGI_TESTS, LocationAreaSessionTest.class.getName());
		addRequiredOSGiTestsBundles(sessionLock);
		try {
			sessionLock.getSetup().setSystemProperty(TEST_LOCATION_DIR, testLocationLockDir);
		} catch (SetupException e) {
			// what can we do; just fail the testcase later when the prop is not set.
			e.printStackTrace();
		}
		sessionLock.addTest(new LocationAreaSessionTest("testSessionSuccessLockJavaIO"));
		suite.addTest(sessionLock);
		return suite;

	}

	public LocationAreaSessionTest(String name) {
		super(name);
	}

	static void doLock(String testLocationDir, String type, boolean release, boolean succeed)
			throws InvalidSyntaxException, IOException {
		String oldLockingValue = System.setProperty(LocationHelper.PROP_OSGI_LOCKING, type);
		try {
			doLock(testLocationDir, release, succeed);
		} finally {
			if (oldLockingValue == null)
				System.getProperties().remove(LocationHelper.PROP_OSGI_LOCKING);
			else
				System.setProperty(LocationHelper.PROP_OSGI_LOCKING, oldLockingValue);
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
			if (succeed ? testLocation.isLocked() : !testLocation.isLocked())
				fail("location should " + (succeed ? "not " : "") + "be locked");
			if (succeed ? !testLocation.lock() : testLocation.lock())
				fail((succeed ? "Could not" : "Could") + " lock location");
			if (!testLocation.isLocked())
				fail("location should be locked");
		} finally {
			if (release && testLocation != null)
				testLocation.release();
			if (!release)
				lockedTestLocation = testLocation;
			OSGiTestsActivator.getContext().ungetService(refs[0]);
		}
	}

	static void doRelease() throws IOException {
		try {
			if (lockedTestLocation == null)
				fail("lockedTestLocation == null !!");
			if (!lockedTestLocation.isLocked())
				fail("lockedTestLocation is not locked!!");
			lockedTestLocation.release();
			if (lockedTestLocation.isLocked())
				fail("lockedTestLocation is still locked!!");
		} finally {
			lockedTestLocation = null;
		}
	}

	public void testSessionFailLockJavaNIO() throws Exception {
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_NIO, true, false);
	}

	public void testSessionSuccessLockJavaNIO() throws Exception {
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_NIO, true, true);
	}

	public void testSessionFailLockJavaIO() throws Exception {
		if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os")))
			return;
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_IO, true, false);
	}

	public void testSessionSuccessLockJavaIO() throws Exception {
		if (!Constants.OS_WIN32.equals(System.getProperty("osgi.os")))
			return;
		doLock(System.getProperty(TEST_LOCATION_DIR), JAVA_IO, true, true);
	}
}
