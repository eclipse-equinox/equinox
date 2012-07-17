/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.datalocation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.internal.location.LocationManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.tests.OSGiTestsActivator;

public class BasicLocationTests extends CoreTest {

	String originalUser = null;
	String originalInstance = null;
	String originalConfiguration = null;
	String originalInstall = null;
	String prefix = "";
	boolean windows = Platform.getOS().equals(Platform.OS_WIN32);

	public BasicLocationTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(BasicLocationTests.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		prefix = windows ? "c:" : "";
		originalUser = System.getProperty(LocationManager.PROP_USER_AREA);
		originalInstance = System.getProperty(LocationManager.PROP_INSTANCE_AREA);
		originalConfiguration = System.getProperty(LocationManager.PROP_CONFIG_AREA);
		originalInstall = System.getProperty(LocationManager.PROP_INSTALL_AREA);
	}

	private void setProperty(String key, String value) {
		if (value == null)
			System.getProperties().remove(key);
		else
			System.setProperty(key, value);
	}

	protected void tearDown() throws Exception {
		setProperty(LocationManager.PROP_USER_AREA, originalUser);
		setProperty(LocationManager.PROP_INSTANCE_AREA, originalInstance);
		setProperty(LocationManager.PROP_CONFIG_AREA, originalConfiguration);
		setProperty(LocationManager.PROP_INSTALL_AREA, originalInstall);
		LocationManager.initializeLocations();
		super.tearDown();
	}

	private void checkSlashes() {
		checkLocation(LocationManager.getUserLocation(), true, true, null);
		checkLocation(LocationManager.getInstanceLocation(), true, true, null);
		checkLocation(LocationManager.getConfigurationLocation(), true, true, null);
		checkLocation(LocationManager.getInstallLocation(), true, true, null);
	}

	private void checkLocation(Location location, boolean leading, boolean trailing, String scheme) {
		if (location == null)
			return;
		URL url = location.getURL();
		if (scheme != null)
			assertEquals(scheme, url.getProtocol());
		if (!url.getProtocol().equals("file"))
			return;
		assertTrue(url.toExternalForm() + " should " + (trailing ? "" : "not") + " have a trailing slash", url.getFile().endsWith("/") == trailing);
		if (windows)
			assertTrue(url.toExternalForm() + " should " + (leading ? "" : "not") + " have a leading slash", url.getFile().startsWith("/") == leading);
	}

	public void testCreateLocation01() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testCreateLocation01");
		Location testLocation = configLocation.createLocation(null, null, false);
		try {
			testLocation.set(testLocationFile.toURL(), false);
		} catch (Throwable t) {
			fail("Failed to set location", t);
		}
		try {
			assertTrue("Could not lock location", testLocation.lock());
		} catch (IOException e) {
			fail("Failed to lock location", e);
		}
		testLocation.release();
	}

	public void testCreateLocation02() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testCreateLocation02");
		Location testLocation = configLocation.createLocation(null, null, true);
		try {
			testLocation.set(testLocationFile.toURL(), false);
		} catch (Throwable t) {
			fail("Failed to set location", t);
		}
		try {
			assertTrue("Could not lock location", testLocation.lock());
			testLocation.release();
			fail("Should not be able to lock read-only location");
		} catch (IOException e) {
			// expected
		}
	}

	public void testCreateLocation03() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testCreateLocation03");
		Location testLocation = configLocation.createLocation(null, null, false);
		try {
			testLocation.set(testLocationFile.toURL(), true);
		} catch (Throwable t) {
			fail("Failed to set location", t);
		}
		try {
			assertTrue("Could not lock location", testLocation.isLocked());
		} catch (IOException e) {
			fail("Failed to lock location", e);
		}
		testLocation.release();
	}

	public void testCreateLocation04() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testCreateLocation04");
		Location testLocation = configLocation.createLocation(null, null, true);
		try {
			testLocation.set(testLocationFile.toURL(), true);
			testLocation.release();
			fail("Should not be able to lock read-only location");
		} catch (Throwable t) {
			// expected
		}
	}

	public void testCreateLocation05() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testCreateLocation01");
		Location testLocation = configLocation.createLocation(null, null, false);
		try {
			testLocation.set(testLocationFile.toURL(), false);
		} catch (Throwable t) {
			fail("Failed to set location", t);
		}
		try {
			assertTrue("Could not lock location", testLocation.lock());
			assertFalse("Could lock a secend time", testLocation.lock());
			assertFalse("Could lock a third time", testLocation.lock());
		} catch (IOException e) {
			fail("Failed to lock location", e);
		} finally {
			testLocation.release();
		}
		try {
			assertTrue("Could not lock location", testLocation.lock());
		} catch (IOException e) {
			fail("Failed to lock location", e);
		} finally {
			testLocation.release();
		}
	}

	private static final String INSTANCE_DATA_AREA_PREFIX = ".metadata/.plugins/"; //$NON-NLS-1$

	public void testLocationDataArea01() {
		Location instance = LocationManager.getInstanceLocation();
		doAllTestLocationDataArea(instance, INSTANCE_DATA_AREA_PREFIX);

		Location configuration = LocationManager.getConfigurationLocation();
		doAllTestLocationDataArea(configuration, "");
	}

	private void doAllTestLocationDataArea(Location location, String dataAreaPrefix) {
		doTestLocateDataArea(location, dataAreaPrefix, getName());
		doTestLocateDataArea(location, dataAreaPrefix, "");
		doTestLocateDataArea(location, dataAreaPrefix, "test/multiple/paths");
		doTestLocateDataArea(location, dataAreaPrefix, "test/multiple/../paths");
		doTestLocateDataArea(location, dataAreaPrefix, "test\\multiple\\paths");

		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/" + getName());
		Location createdLocation = location.createLocation(null, null, false);
		try {
			createdLocation.set(testLocationFile.toURL(), false);
		} catch (Exception e) {
			fail("Failed to set location", e);
		}
		doTestLocateDataArea(createdLocation, dataAreaPrefix, getName());
		doTestLocateDataArea(createdLocation, dataAreaPrefix, "");
		doTestLocateDataArea(createdLocation, dataAreaPrefix, "test/multiple/paths");
		doTestLocateDataArea(createdLocation, dataAreaPrefix, "test/multiple/../paths");
		doTestLocateDataArea(location, dataAreaPrefix, "test\\multiple\\paths");

		createdLocation = location.createLocation(null, null, false);
		try {
			createdLocation.getDataArea("shouldFail");
			fail("expected failure when location is not set");
		} catch (IOException e) {
			// expected;
		}
	}

	private void doTestLocateDataArea(Location location, String dataAreaPrefix, String namespace) {
		assertTrue("Location is not set", location.isSet());
		URL dataArea = null;
		try {
			dataArea = location.getDataArea(namespace);
		} catch (IOException e) {
			fail("Failed to get data area.", e);
		}
		assertNotNull("Data area is null.", dataArea);

		namespace = namespace.replace('\\', '/');
		assertTrue("Data area is not the expected value: " + dataArea.toExternalForm(), dataArea.toExternalForm().endsWith(dataAreaPrefix + namespace));
	}

	public void testSetLocationWithEmptyLockFile() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testSetLocationWithEmptyLockFile"); //$NON-NLS-1$
		Location testLocation = configLocation.createLocation(null, null, false);
		try {
			testLocation.set(testLocationFile.toURL(), true, ""); //$NON-NLS-1$
			// Make sure it has created the default lock file
			File lockFile = new File(testLocationFile, ".metadata/.lock"); //$NON-NLS-1$
			assertTrue("Lock file does not exist!", lockFile.exists()); //$NON-NLS-1$
		} catch (Throwable t) {
			fail("Failed to set location", t); //$NON-NLS-1$
		}
		try {
			assertTrue("Could not lock location", testLocation.isLocked()); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Failed to lock location", e); //$NON-NLS-1$
		}
		testLocation.release();
	}

	public void testSetLocationWithRelLockFile() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testSetLocationWithRelLockFile"); //$NON-NLS-1$
		Location testLocation = configLocation.createLocation(null, null, false);
		try {
			testLocation.set(testLocationFile.toURL(), true, ".mocklock"); //$NON-NLS-1$
			File lockFile = new File(testLocationFile, ".mocklock"); //$NON-NLS-1$
			assertTrue("Lock file does not exist!", lockFile.exists()); //$NON-NLS-1$
		} catch (Throwable t) {
			fail("Failed to set location", t); //$NON-NLS-1$
		}
		try {
			assertTrue("Could not lock location", testLocation.isLocked()); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Failed to lock location", e); //$NON-NLS-1$
		}
		testLocation.release();
	}

	public void testSetLocationWithAbsLockFile() {
		Location configLocation = LocationManager.getConfigurationLocation();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testSetLocationWithAbsLockFile"); //$NON-NLS-1$
		File testLocationLockFile = OSGiTestsActivator.getContext().getDataFile("testLocations/mock.lock"); //$NON-NLS-1$
		assertTrue(testLocationLockFile.isAbsolute());
		Location testLocation = configLocation.createLocation(null, null, false);
		try {
			testLocation.set(testLocationFile.toURL(), true, testLocationLockFile.getAbsolutePath());
			assertTrue("The lock file should be present!", testLocationLockFile.exists()); //$NON-NLS-1$
		} catch (Throwable t) {
			fail("Failed to set location", t); //$NON-NLS-1$
		}
		try {
			assertTrue("Could not lock location", testLocation.isLocked()); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Failed to lock location", e); //$NON-NLS-1$
		}
		testLocation.release();
		assertTrue("The lock file could not be removed!", testLocationLockFile.delete()); //$NON-NLS-1$
	}

	public void testSlashes() {
		setProperty(LocationManager.PROP_USER_AREA, prefix + "/a");
		setProperty(LocationManager.PROP_INSTANCE_AREA, prefix + "/c/d");
		setProperty(LocationManager.PROP_CONFIG_AREA, prefix + "/e/f");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		LocationManager.initializeLocations();
		checkSlashes();
	}

	public void testSchemes() {
		setProperty(LocationManager.PROP_USER_AREA, "http://example.com/a");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "ftp://example.com/c/d");
		setProperty(LocationManager.PROP_CONFIG_AREA, "platform:/base/e/f");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		LocationManager.initializeLocations();
		checkSlashes();
		checkLocation(LocationManager.getUserLocation(), true, true, "http");
		checkLocation(LocationManager.getInstanceLocation(), true, true, "ftp");
		checkLocation(LocationManager.getConfigurationLocation(), true, true, "platform");
		checkLocation(LocationManager.getInstallLocation(), true, true, "file");

	}

	public void testNone() {
		setProperty(LocationManager.PROP_USER_AREA, "@none");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "@none");
		setProperty(LocationManager.PROP_CONFIG_AREA, "@none");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		LocationManager.initializeLocations();
		assertNull("User location should be null", LocationManager.getUserLocation());
		assertNull("Instance location should be null", LocationManager.getUserLocation());
		assertNull("Configuration location should be null", LocationManager.getUserLocation());
	}

	public void testUserDir() {
		setProperty(LocationManager.PROP_USER_AREA, "@user.dir");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "@user.dir");
		setProperty(LocationManager.PROP_CONFIG_AREA, "@user.dir");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		LocationManager.initializeLocations();
		checkLocation(LocationManager.getUserLocation(), true, true, "file");
		checkLocation(LocationManager.getInstanceLocation(), true, true, "file");
		checkLocation(LocationManager.getConfigurationLocation(), true, true, "file");
		checkLocation(LocationManager.getInstallLocation(), true, true, "file");
	}

	public void testUserHome() {
		setProperty(LocationManager.PROP_USER_AREA, "@user.home");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "@user.home");
		setProperty(LocationManager.PROP_CONFIG_AREA, "@user.home");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		LocationManager.initializeLocations();
		checkLocation(LocationManager.getUserLocation(), true, true, "file");
		checkLocation(LocationManager.getInstanceLocation(), true, true, "file");
		checkLocation(LocationManager.getConfigurationLocation(), true, true, "file");
		checkLocation(LocationManager.getInstallLocation(), true, true, "file");
	}

	public void testUNC() {
		if (!windows)
			return;
		setProperty(LocationManager.PROP_USER_AREA, "//server/share/a");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "//server/share/b");
		setProperty(LocationManager.PROP_CONFIG_AREA, "//server/share/c");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file://server/share/g");
		LocationManager.initializeLocations();
		checkLocation(LocationManager.getUserLocation(), true, true, "file");
		checkLocation(LocationManager.getInstanceLocation(), true, true, "file");
		checkLocation(LocationManager.getConfigurationLocation(), true, true, "file");
		checkLocation(LocationManager.getInstallLocation(), true, true, "file");
	}
}
