/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 578606 - Leverage JUnit-4 methods and simplify tests
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import org.eclipse.osgi.framework.util.ThreadInfoReport;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;

public class AbstractBundleTests {
	public static int BUNDLE_LISTENER = 0x01;
	public static int SYNC_BUNDLE_LISTENER = 0x02;
	public static int SIMPLE_RESULTS = 0x04;
	public static final String BUNDLES_ROOT = "bundle_tests";
	public static TestResults simpleResults;
	public static EventListenerTestResults listenerResults;
	public static SyncEventListenerTestResults syncListenerResults;
	public static EventListenerTestResults frameworkListenerResults;
	public static BundleInstaller installer;

	static class BundleBuilder {
		static class BundleManifestBuilder {
			private final Manifest manifest = new Manifest();

			public Manifest build() {
				manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
				return manifest;
			}

			public BundleManifestBuilder symbolicName(String value) {
				manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, value);
				return this;
			}
		}

		private final BundleManifestBuilder manifestBuilder = new BundleManifestBuilder();

		public InputStream build() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JarOutputStream jos = new JarOutputStream(baos, manifestBuilder.build());
			jos.close();
			return new ByteArrayInputStream(baos.toByteArray());
		}

		public BundleBuilder symbolicName(String value) {
			manifestBuilder.symbolicName(value);
			return this;
		}
	}

	@Before
	public void setUp() throws Exception {
		installer = new BundleInstaller(BUNDLES_ROOT, getContext());
		installer.refreshPackages(null);
		listenerResults = new EventListenerTestResults();
		getContext().addBundleListener(listenerResults);
		syncListenerResults = new SyncEventListenerTestResults();
		getContext().addBundleListener(syncListenerResults);
		simpleResults = new TestResults();
		frameworkListenerResults = new EventListenerTestResults();
		getContext().addFrameworkListener(frameworkListenerResults);
	}

	@After
	public void tearDown() throws Exception {
		installer.shutdown();
		installer = null;
		getContext().removeBundleListener(listenerResults);
		getContext().removeBundleListener(syncListenerResults);
		getContext().removeFrameworkListener(frameworkListenerResults);
		listenerResults = null;
		syncListenerResults = null;
		simpleResults = null;
		frameworkListenerResults = null;
	}

	@Rule
	public final TestName testName = new TestName();

	protected String getName() {
		return testName.getMethodName();
	}

	public BundleContext getContext() {
		return OSGiTestsActivator.getContext();
	}

	static public void compareResults(Object[] expectedEvents, Object[] actualEvents) {
		String expectedActual = " -- EXPECTED:" + toStringEventArray(expectedEvents) + " ACTUAL:" + toStringEventArray(actualEvents);
		assertEquals("compareResults length" + expectedActual, expectedEvents.length, actualEvents.length);
		for (int i = 0; i < expectedEvents.length; i++) {
			String assertMsg = "compareResults: " + i + expectedActual;
			assertEqualEvent(assertMsg, expectedEvents[i], actualEvents[i]);
		}
	}

	private static String toStringEventArray(Object[] events) {
		return Arrays.stream(events).map(AbstractBundleTests::toString).collect(Collectors.joining(", ", "[", "]"));
	}

	protected static void assertEqualEvent(String message, Object expected, Object actual) {
		if (expected == null && actual == null)
			return;
		if ((expected == null || actual == null))
			assertEquals(message, toString(expected), toString(actual));
		if (isEqual(expected, actual))
			return;
		assertEquals(message, toString(expected), toString(actual));
	}

	private static boolean isEqual(Object expected, Object actual) {
		if (expected instanceof BundleEvent && actual instanceof BundleEvent) {
			return isEqual((BundleEvent) expected, (BundleEvent) actual);
		} else if (expected instanceof FrameworkEvent && actual instanceof FrameworkEvent) {
			return isEqual((FrameworkEvent) expected, (FrameworkEvent) actual);
		}
		return expected.equals(actual);
	}

	private static boolean isEqual(BundleEvent expected, BundleEvent actual) {
		return expected.getSource() == actual.getSource() && expected.getType() == actual.getType();
	}

	private static boolean isEqual(FrameworkEvent expected, FrameworkEvent actual) {
		return expected.getSource() == actual.getSource() && expected.getType() == actual.getType();
	}

	private static String toString(Object object) {
		if (object instanceof BundleEvent)
			return toString((BundleEvent) object);
		else if (object instanceof FrameworkEvent)
			return toString((FrameworkEvent) object);
		return object.toString();
	}

	private static String toString(FrameworkEvent event) {
		StringBuilder result = new StringBuilder("FrameworkEvent [");
		switch (event.getType()) {
			case FrameworkEvent.ERROR :
				result.append("ERROR");
				break;
			case FrameworkEvent.INFO :
				result.append("INFO");
				break;
			case FrameworkEvent.PACKAGES_REFRESHED :
				result.append("PACKAGES_REFRESHED");
				break;
			case FrameworkEvent.STARTED :
				result.append("STARTED");
				break;
			case FrameworkEvent.STARTLEVEL_CHANGED :
				result.append("STARTLEVEL_CHANGED");
				break;
			case FrameworkEvent.WARNING :
				result.append("WARNING");
				break;
			default :
				break;
		}
		result.append("] ").append(event.getSource());
		return result.toString();
	}

	private static String toString(BundleEvent event) {
		StringBuilder result = new StringBuilder("BundleEvent [");
		switch (event.getType()) {
			case BundleEvent.INSTALLED :
				result.append("INSTALLED");
				break;
			case BundleEvent.LAZY_ACTIVATION :
				result.append("LAZY_ACTIVATION");
				break;
			case BundleEvent.RESOLVED :
				result.append("RESOLVED");
				break;
			case BundleEvent.STARTED :
				result.append("STARTED");
				break;
			case BundleEvent.STARTING :
				result.append("STARTING");
				break;
			case BundleEvent.STOPPED :
				result.append("STOPPED");
				break;
			case BundleEvent.STOPPING :
				result.append("STOPPING");
				break;
			case BundleEvent.UNINSTALLED :
				result.append("UNINSTALLED");
				break;
			case BundleEvent.UNRESOLVED :
				result.append("UNRESOLVED");
				break;
			case BundleEvent.UPDATED :
				result.append("UPDATED");
				break;
			default :
				break;
		}
		result.append("] ").append(event.getSource());
		return result.toString();
	}

	protected Map<String, Object> createConfiguration() {
		File file = getContext().getDataFile(getName());
		Map<String, Object> result = new HashMap<>();
		result.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		return result;
	}

	protected void initAndStart(Equinox equinox) throws BundleException {
		equinox.init();
		equinox.start();
	}

	static public void stop(Framework equinox, int expected) {
		FrameworkEvent actual = stop(equinox);
		if (expected > 0) {
			assertNotNull("No FrameworkEvent returned.", actual);
			assertEquals("Wrong FrameworkEvent.", getFrameworkEventType(expected),
					getFrameworkEventType(actual.getType()));
		}
	}

	static private String getFrameworkEventType(int type) {
		switch (type) {
		case FrameworkEvent.ERROR:
			return "ERROR";
		case FrameworkEvent.INFO:
			return "INFO";
		case FrameworkEvent.STARTED:
			return "STARTED";
		case FrameworkEvent.STOPPED:
			return "STOPPED";
		case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
			return "STOPPED_BOOTCLASSPATH_MODIFIED";
		case FrameworkEvent.STOPPED_SYSTEM_REFRESHED:
			return "STOPPED_SYSTEM_REFRESHED";
		case FrameworkEvent.STOPPED_UPDATE:
			return "STOPPED_UPDATE";
		default:
			return "UNKNOWN:" + type;
		}
	}

	static public FrameworkEvent stop(Framework equinox) {
		return stop(equinox, false, 10000);
	}

	static public FrameworkEvent stopQuietly(Framework equinox) {
		return stop(equinox, true, 10000);
	}

	protected static FrameworkEvent update(final Framework equinox) throws BundleException, InterruptedException {
		final FrameworkEvent[] success = new FrameworkEvent[] { null };
		final String uuid = getUUID(equinox);
		Thread waitForUpdate = new Thread(() -> success[0] = waitForStop(equinox, uuid, false, 10000), "test waitForStop thread"); //$NON-NLS-1$
		waitForUpdate.start();

		// delay hack to allow waitForUpdate thread to block on waitForStop before we
		// update.
		Thread.sleep(200);

		equinox.update();

		waitForUpdate.join();
		return success[0];
	}

	static public FrameworkEvent stop(Framework equinox, boolean quietly, long timeout) {
		if (equinox == null)
			return null;
		final String uuid = getUUID(equinox);
		try {
			equinox.stop();
		} catch (BundleException e) {
			if (!quietly) {
				fail("Unexpected error stopping framework:" + e.getMessage()); //$NON-NLS-1$
			}
		}
		return waitForStop(equinox, uuid, quietly, timeout);
	}

	protected static boolean delete(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				String[] list = file.list();
				if (list != null) {
					int len = list.length;
					for (int i = 0; i < len; i++) {
						delete(new File(file, list[i]));
					}
				}
			}

			return file.delete();
		}
		return (true);
	}

	static public FrameworkEvent waitForStop(Framework equinox, String uuid, boolean quietly, long timeout) {
		try {
			FrameworkEvent stopEvent = equinox.waitForStop(timeout);
			if (stopEvent.getType() == FrameworkEvent.WAIT_TIMEDOUT) {
				StringBuilder sb = new StringBuilder("Framework state is: ");
				sb.append(getState(equinox)).append(" - ").append(uuid).append('\n');
				sb.append(ThreadInfoReport.getThreadDump(null)).append('\n');
				if (!quietly) {
					fail(sb.toString());
				} else {
					System.out.println(sb.toString());
				}
			}
			return stopEvent;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (!quietly) {
				fail("Unexpected interrupted exception"); //$NON-NLS-1$
			}
		}
		return null;
	}

	public static String getUUID(Framework equinox) {
		BundleContext bc = equinox.getBundleContext();
		return bc == null ? null : bc.getProperty(Constants.FRAMEWORK_UUID);
	}

	static private String getState(Framework framework) {
		int state = framework.getState();
		switch (state) {
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.STOPPING:
			return "STOPPING";
		default:
			return "UNKNOWN:" + state;
		}
	}
}
