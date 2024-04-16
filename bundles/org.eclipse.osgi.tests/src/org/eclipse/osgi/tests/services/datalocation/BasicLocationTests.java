/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import junit.framework.AssertionFailedError;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

public class BasicLocationTests {

	@Rule
	public TestName testName = new TestName();

	String prefix = "";
	ServiceTracker<Location, Location> configLocationTracker = null;
	ServiceTracker<Location, Location> instanceLocationTracker = null;

	@Before
	public void setUp() throws Exception {
		prefix = OS.isWindows() ? "c:" : "";

		configLocationTracker = new ServiceTracker<>(OSGiTestsActivator.getContext(),
				OSGiTestsActivator.getContext().createFilter(Location.CONFIGURATION_FILTER), null);
		instanceLocationTracker = new ServiceTracker<>(OSGiTestsActivator.getContext(),
				OSGiTestsActivator.getContext().createFilter(Location.INSTANCE_FILTER), null);

		configLocationTracker.open();
		instanceLocationTracker.open();
	}

	@After
	public void tearDown() throws Exception {
		configLocationTracker.close();
		instanceLocationTracker.close();
	}

	private void checkSlashes(Map<String, Location> locations) {
		checkLocation(locations.get(Location.USER_FILTER), true, true, null);
		checkLocation(locations.get(Location.INSTANCE_FILTER), true, true, null);
		checkLocation(locations.get(Location.CONFIGURATION_FILTER), true, true, null);
		checkLocation(locations.get(Location.INSTALL_FILTER), true, true, null);
	}

	private void checkLocation(Location location, boolean leading, boolean trailing, String scheme) {
		if (location == null)
			return;
		URL url = location.getURL();
		if (scheme != null)
			assertEquals(scheme, url.getProtocol());
		if (!url.getProtocol().equals("file"))
			return;
		assertTrue(url.toExternalForm() + " should " + (trailing ? "" : "not") + " have a trailing slash",
				url.getFile().endsWith("/") == trailing);
		if (OS.isWindows())
			assertTrue(url.toExternalForm() + " should " + (leading ? "" : "not") + " have a leading slash",
					url.getFile().startsWith("/") == leading);
	}

	private void fail(String message, Throwable exception) {
		AssertionFailedError error = new AssertionFailedError(message);
		error.initCause(exception);
		throw error;
	}

	@Test
	public void testCreateLocation01() {
		Location configLocation = configLocationTracker.getService();
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

	@Test
	public void testCreateLocation02() {
		Location configLocation = configLocationTracker.getService();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testCreateLocation02");
		Location testLocation = configLocation.createLocation(null, null, true);
		try {
			testLocation.set(testLocationFile.toURL(), false);
		} catch (Throwable t) {
			fail("Failed to set location", t);
		}
		assertThrows("Should not be able to lock read-only location", IOException.class, () -> {
			assertTrue("Could not lock location", testLocation.lock());
			testLocation.release();
		});
	}

	@Test
	public void testCreateLocation03() {
		Location configLocation = configLocationTracker.getService();
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

	@Test
	public void testCreateLocation04() {
		Location configLocation = configLocationTracker.getService();
		File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testCreateLocation04");
		Location testLocation = configLocation.createLocation(null, null, true);
		try {
			testLocation.set(testLocationFile.toURL(), true);
			testLocation.release();
			Assert.fail("Should not be able to lock read-only location");
		} catch (Throwable t) {
			// expected
		}
	}

	@Test
	public void testCreateLocation05() {
		Location configLocation = configLocationTracker.getService();
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

	@Test
	public void testLocationDataArea01() {
		Location instance = instanceLocationTracker.getService();
		doAllTestLocationDataArea(instance, INSTANCE_DATA_AREA_PREFIX);

		Location configuration = configLocationTracker.getService();
		doAllTestLocationDataArea(configuration, "");
	}

	private void doAllTestLocationDataArea(Location location, String dataAreaPrefix) {
		doTestLocateDataArea(location, dataAreaPrefix, testName.getMethodName());
		doTestLocateDataArea(location, dataAreaPrefix, "");
		doTestLocateDataArea(location, dataAreaPrefix, "test/multiple/paths");
		doTestLocateDataArea(location, dataAreaPrefix, "test/multiple/../paths");
		doTestLocateDataArea(location, dataAreaPrefix, "test\\multiple\\paths");
		doTestLocateDataArea(location, dataAreaPrefix, "/test/begin/slash");

		File testLocationFile = OSGiTestsActivator.getContext()
				.getDataFile("testLocations/" + testName.getMethodName());
		Location createdLocation = location.createLocation(null, null, false);
		try {
			createdLocation.set(testLocationFile.toURL(), false);
		} catch (Exception e) {
			fail("Failed to set location", e);
		}
		doTestLocateDataArea(createdLocation, dataAreaPrefix, testName.getMethodName());
		doTestLocateDataArea(createdLocation, dataAreaPrefix, "");
		doTestLocateDataArea(createdLocation, dataAreaPrefix, "test/multiple/paths");
		doTestLocateDataArea(createdLocation, dataAreaPrefix, "test/multiple/../paths");
		doTestLocateDataArea(location, dataAreaPrefix, "test\\multiple\\paths");
		doTestLocateDataArea(location, dataAreaPrefix, "/test/begin/slash");

		Location recreatedLocation = location.createLocation(null, null, false);
		assertThrows("expected failure when location is not set", IOException.class,
				() -> recreatedLocation.getDataArea("shouldFail"));
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
		if (namespace.startsWith("/")) {
			namespace = namespace.substring(1);
		}
		assertTrue("Data area is not the expected value: " + dataArea.toExternalForm(),
				dataArea.toExternalForm().endsWith(dataAreaPrefix + namespace));
	}

	@Test
	public void testSetLocationWithEmptyLockFile() {
		Location configLocation = configLocationTracker.getService();
		File testLocationFile = OSGiTestsActivator.getContext()
				.getDataFile("testLocations/testSetLocationWithEmptyLockFile"); //$NON-NLS-1$
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

	@Test
	public void testSetLocationWithRelLockFile() {
		Location configLocation = configLocationTracker.getService();
		File testLocationFile = OSGiTestsActivator.getContext()
				.getDataFile("testLocations/testSetLocationWithRelLockFile"); //$NON-NLS-1$
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

	@Test
	public void testSetLocationWithAbsLockFile() {
		Location configLocation = configLocationTracker.getService();
		File testLocationFile = OSGiTestsActivator.getContext()
				.getDataFile("testLocations/testSetLocationWithAbsLockFile"); //$NON-NLS-1$
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

	@Test
	public void testSlashes() throws BundleException, InvalidSyntaxException {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, prefix + "/a");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, prefix + "/c/d");
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA, prefix + "/e/f");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			checkSlashes(getLocations(equinox));
		} finally {
			equinox.stop();
		}

	}

	private static Map<String, Location> getLocations(Equinox equinox) throws InvalidSyntaxException {
		Map<String, Location> locations = new HashMap<>();
		BundleContext context = equinox.getBundleContext();
		addLocation(context, Location.CONFIGURATION_FILTER, locations);
		addLocation(context, Location.INSTALL_FILTER, locations);
		addLocation(context, Location.INSTANCE_FILTER, locations);
		addLocation(context, Location.USER_FILTER, locations);
		return locations;
	}

	private static void addLocation(BundleContext context, String filter, Map<String, Location> locations)
			throws InvalidSyntaxException {
		Collection<ServiceReference<Location>> locationRefs = context.getServiceReferences(Location.class, filter);
		if (!locationRefs.isEmpty()) {
			locations.put(filter, context.getService(locationRefs.iterator().next()));
		}
	}

	@Test
	public void testSchemes() throws Exception {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, "http://example.com/a");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, "ftp://example.com/c/d");
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA, "platform:/base/e/f");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			Map<String, Location> locations = getLocations(equinox);
			checkSlashes(locations);
			checkLocation(locations.get(Location.USER_FILTER), true, true, "http");
			checkLocation(locations.get(Location.INSTANCE_FILTER), true, true, "ftp");
			checkLocation(locations.get(Location.CONFIGURATION_FILTER), true, true, "platform");
			checkLocation(locations.get(Location.INSTALL_FILTER), true, true, "file");
		} finally {
			equinox.stop();
		}
	}

	@Test
	public void testNone() throws Exception {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, "@none");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, "@none");
		// TODO framework does not handle no config area well
		// fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA, "@none");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			Map<String, Location> locations = getLocations(equinox);
			assertNull("User location should be null", locations.get(Location.USER_FILTER));
			assertNull("Instance location should be null", locations.get(Location.INSTANCE_FILTER));
			// TODO assertNull("Configuration location should be null",
			// locations.get(Location.CONFIGURATION_FILTER));
		} finally {
			equinox.stop();
		}
	}

	@Test
	public void testNoDefault() throws Exception {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, "@noDefault");
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, "@noDefault");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			Map<String, Location> locations = getLocations(equinox);
			Location userLocation = locations.get(Location.USER_FILTER);
			Location instanceLocation = locations.get(Location.INSTANCE_FILTER);
			assertNull("User locatoin is not null.", userLocation.getURL());
			assertNull("Instance location is not null.", instanceLocation.getURL());
		} finally {
			equinox.stop();
		}
	}

	@Test
	public void testSetUrl() throws Exception {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, "@noDefault");
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, "@noDefault");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {

			BundleContext context = equinox.getBundleContext();
			ServiceTracker<Location, Location> tracker = new ServiceTracker<>(context,
					context.createFilter(Location.INSTANCE_FILTER), null);
			tracker.open();
			SortedMap<ServiceReference<Location>, Location> serviceMap = tracker.getTracked();
			assertTrue("no service matching " + Location.INSTANCE_FILTER + " found!", serviceMap.size() == 1);
			Entry<ServiceReference<Location>, Location> entry = serviceMap.entrySet().iterator().next();
			Location location = entry.getValue();
			assertNull("Instance location is not null.", location.getURL());
			ServiceReference<Location> serviceReference = entry.getKey();
			assertNull("Url property is set!", serviceReference.getProperty(Location.SERVICE_PROPERTY_URL));
			File testLocationFile = OSGiTestsActivator.getContext().getDataFile("testLocations/testseturl");
			location.set(testLocationFile.toURL(), false);
			assertNotNull("Instance location is still null.", location.getURL());
			assertEquals("Url not set to the service properties", location.getURL().toExternalForm(),
					serviceReference.getProperty("url"));

		} finally {
			equinox.stop();
		}
	}

	@Test
	public void testUserDir() throws Exception {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, "@user.dir");
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, "@user.dir");
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA, "@user.dir");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			Map<String, Location> locations = getLocations(equinox);
			checkLocation(locations.get(Location.USER_FILTER), true, true, "file");
			checkLocation(locations.get(Location.INSTANCE_FILTER), true, true, "file");
			checkLocation(locations.get(Location.CONFIGURATION_FILTER), true, true, "file");
			checkLocation(locations.get(Location.INSTALL_FILTER), true, true, "file");
		} finally {
			equinox.stop();
		}
	}

	@Test
	public void testUserHome() throws Exception {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, "@user.home");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, "@user.home");
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA, "@user.home");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			Map<String, Location> locations = getLocations(equinox);
			checkLocation(locations.get(Location.USER_FILTER), true, true, "file");
			checkLocation(locations.get(Location.INSTANCE_FILTER), true, true, "file");
			checkLocation(locations.get(Location.CONFIGURATION_FILTER), true, true, "file");
			checkLocation(locations.get(Location.INSTALL_FILTER), true, true, "file");
		} finally {
			equinox.stop();
		}

	}

	@Test
	public void testUNC() throws Exception {
		assumeTrue("only relevant on Windows", OS.isWindows());

		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_USER_AREA, "//server/share/a");
		fwkConfig.put(EquinoxLocations.PROP_INSTANCE_AREA, "//server/share/b");
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA, "//server/share/c");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file://server/share/g");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			Map<String, Location> locations = getLocations(equinox);
			checkLocation(locations.get(Location.USER_FILTER), true, true, "file");
			checkLocation(locations.get(Location.INSTANCE_FILTER), true, true, "file");
			checkLocation(locations.get(Location.CONFIGURATION_FILTER), true, true, "file");
			checkLocation(locations.get(Location.INSTALL_FILTER), true, true, "file");
		} finally {
			equinox.stop();
		}
	}

	@Test
	public void testDebugLogOnGetURL() throws Exception {
		Properties debugOptions = new Properties();
		debugOptions.put("org.eclipse.osgi/debug/location", "true");
		File debugOptionsFile = OSGiTestsActivator.getContext().getDataFile(testName.getMethodName() + ".options");
		debugOptions.store(new FileOutputStream(debugOptionsFile), testName.getMethodName());
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(EquinoxLocations.PROP_CONFIG_AREA + EquinoxLocations.READ_ONLY_AREA_SUFFIX, "true");
		fwkConfig.put(EquinoxLocations.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		fwkConfig.put(EquinoxConfiguration.PROP_DEBUG, debugOptionsFile.getAbsolutePath());
		fwkConfig.put("eclipse.consoleLog", "true");

		Equinox equinox = new Equinox(fwkConfig);
		equinox.init();
		try {
			final List<LogEntry> logEntries = new ArrayList<>();
			LogReaderService logReaderService = getLogReaderService(equinox);
			SynchronousLogListener logListener = logEntries::add;
			logReaderService.addLogListener(logListener);
			Map<String, Location> locations = getLocations(equinox);
			Location userLocation = locations.get(Location.USER_FILTER);
			Location instanceLocation = locations.get(Location.INSTANCE_FILTER);
			assertNotNull("User locatoin is not null.", userLocation.getURL());
			assertNotNull("Instance location is not null.", instanceLocation.getURL());
			assertEquals("Wrong number of log entries", 2, logEntries.size());
		} finally {
			equinox.stop();
		}
	}

	private LogReaderService getLogReaderService(Equinox equinox) {
		return equinox.getBundleContext()
				.getService(equinox.getBundleContext().getServiceReference(LogReaderService.class));
	}
}
