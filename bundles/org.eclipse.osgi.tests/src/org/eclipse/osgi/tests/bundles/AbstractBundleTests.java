/*******************************************************************************
 * Copyright (c) 2006, 20010 IBM Corporation and others.
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

import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;

public class AbstractBundleTests extends CoreTest {
	public static int BUNDLE_LISTENER = 0x01;
	public static int SYNC_BUNDLE_LISTENER = 0x02;
	public static int SIMPLE_RESULTS = 0x04;
	public static final String BUNDLES_ROOT = "bundle_tests";
	public static TestResults simpleResults;
	public static EventListenerTestResults listenerResults;
	public static SyncEventListenerTestResults syncListenerResults;
	public static EventListenerTestResults frameworkListenerResults;
	public static BundleInstaller installer;

	protected void setUp() throws Exception {
		installer = new BundleInstaller(BUNDLES_ROOT, OSGiTestsActivator.getContext());
		installer.refreshPackages(null);
		listenerResults = new EventListenerTestResults();
		OSGiTestsActivator.getContext().addBundleListener(listenerResults);
		syncListenerResults = new SyncEventListenerTestResults();
		OSGiTestsActivator.getContext().addBundleListener(syncListenerResults);
		simpleResults = new TestResults();
		frameworkListenerResults = new EventListenerTestResults();
		OSGiTestsActivator.getContext().addFrameworkListener(frameworkListenerResults);
	}

	protected void tearDown() throws Exception {
		installer.shutdown();
		installer = null;
		OSGiTestsActivator.getContext().removeBundleListener(listenerResults);
		OSGiTestsActivator.getContext().removeBundleListener(syncListenerResults);
		OSGiTestsActivator.getContext().removeFrameworkListener(frameworkListenerResults);
		listenerResults = null;
		syncListenerResults = null;
		simpleResults = null;
		frameworkListenerResults = null;
	}

	public BundleContext getContext() {
		return OSGiTestsActivator.getContext();
	}

	static public void compareResults(Object[] expectedEvents, Object[] actualEvents) {
		String expectedActual = " -- EXPECTED:" + toStringEventArray(expectedEvents) + " ACTUAL:" + toStringEventArray(actualEvents);
		assertEquals("compareResults length" + expectedActual, expectedEvents.length, actualEvents.length);
		for (int i = 0; i < expectedEvents.length; i++) {
			String assertMsg = "compareResults: " + i + expectedActual;
			assertEquals(assertMsg, expectedEvents[i], actualEvents[i]);
		}
	}

	static public String toStringEventArray(Object[] events) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		sb.append('[');
		for (Object event : events) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(toString(event));
		}
		sb.append(']');
		return sb.toString();
	}

	static public void assertEquals(String message, Object expected, Object actual) {
		if (expected == null && actual == null)
			return;
		if ((expected == null || actual == null) && expected != actual)
			failNotEquals(message, toString(expected), toString(actual));
		if (isEqual(expected, actual))
			return;
		failNotEquals(message, toString(expected), toString(actual));
	}

	static public void assertEquals(String message, int[] expected, int[] actual) {
		if (expected == null && actual == null)
			return;
		if ((expected == null || actual == null) && expected != actual)
			failNotEquals(message, toString(expected), toString(actual));
		if (expected.length != actual.length)
			failNotEquals(message, toString(expected), toString(actual));
		for (int i = 0; i < expected.length; i++)
			if (expected[i] != actual[i])
				failNotEquals(message, toString(expected), toString(actual));
	}

	private static boolean isEqual(Object expected, Object actual) {
		if (!expected.getClass().isAssignableFrom(actual.getClass()))
			return false;
		if (expected instanceof BundleEvent)
			return isEqual((BundleEvent) expected, (BundleEvent) actual);
		else if (expected instanceof FrameworkEvent)
			return isEqual((FrameworkEvent) expected, (FrameworkEvent) actual);
		return expected.equals(actual);
	}

	private static boolean isEqual(BundleEvent expected, BundleEvent actual) {
		return expected.getSource() == actual.getSource() && expected.getType() == actual.getType();
	}

	private static boolean isEqual(FrameworkEvent expected, FrameworkEvent actual) {
		return expected.getSource() == actual.getSource() && expected.getType() == actual.getType();
	}

	private static String toString(int[] array) {
		if (array == null)
			return "null"; //$NON-NLS-1$
		String result = "["; //$NON-NLS-1$
		for (int i = 0; i < array.length; i++) {
			if (i != 0)
				result += ',';
			result += array[i];
		}
		result += "]"; //$NON-NLS-1$
		return result;
	}

	private static Object toString(Object object) {
		if (object instanceof BundleEvent)
			return toString((BundleEvent) object);
		else if (object instanceof FrameworkEvent)
			return toString((FrameworkEvent) object);
		return object.toString();
	}

	private static Object toString(FrameworkEvent event) {
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

	private static Object toString(BundleEvent event) {
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

}