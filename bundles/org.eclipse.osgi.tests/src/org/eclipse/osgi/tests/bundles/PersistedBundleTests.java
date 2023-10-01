/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;
import org.eclipse.osgi.launch.Equinox;
import org.junit.Test;

/*
 * The framework must persist data according to the value of the
 * eclipse.stateSaveDelayInterval property. The value is of type long and
 * represents the number of milliseconds between persists. A positive value
 * represents the number of milliseconds between persists. A value of zero
 * indicates data should be immediately persisted with each update. A negative
 * value disables persistence on update altogether (but data will still be
 * persisted on shutdown).
 */
public class PersistedBundleTests extends AbstractBundleTests {

	private static final String ECLIPSE_STATESAVEDELAYINTERVAL = "eclipse.stateSaveDelayInterval";

	private static final String IMMEDIATE_PERSISTENCE = "0";
	private static final String NO_PERSISTENCE = "-1";
	private static final String PERIODIC_PERSISTENCE = "4000";

	/*
	 * Test that a value of zero for eclipse.stateSaveDelayInterval results in
	 * immediate persistence.
	 */
	@Test
	public void testImmediatePersistence() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(ECLIPSE_STATESAVEDELAYINTERVAL, IMMEDIATE_PERSISTENCE);
		Equinox equinox1 = new Equinox(configuration);
		initAndStart(equinox1);
		try {
			assertNull("Bundle exists", equinox1.getBundleContext().getBundle(getName()));
			equinox1.getBundleContext().installBundle(getName(), new BundleBuilder().symbolicName(getName()).build());
			Equinox equinox2 = new Equinox(configuration);
			initAndStart(equinox2);
			try {
				assertNotNull("Bundle does not exist", equinox2.getBundleContext().getBundle(getName()));
			} finally {
				stopQuietly(equinox2);
			}
		} finally {
			stopQuietly(equinox1);
		}
	}

	/*
	 * Test that a negative value for eclipse.stateSaveDelayInterval results in no
	 * persistence.
	 */
	@Test
	public void testNoPersistence() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(ECLIPSE_STATESAVEDELAYINTERVAL, NO_PERSISTENCE);
		Equinox equinox1 = new Equinox(configuration);
		initAndStart(equinox1);
		try {
			assertNull("Bundle exists", equinox1.getBundleContext().getBundle(getName()));
			equinox1.getBundleContext().installBundle(getName(), new BundleBuilder().symbolicName(getName()).build());
			Thread.sleep(Long.valueOf(PERIODIC_PERSISTENCE));
			Equinox equinox2 = new Equinox(configuration);
			initAndStart(equinox2);
			try {
				assertNull("Bundle exists", equinox2.getBundleContext().getBundle(getName()));
			} finally {
				stopQuietly(equinox2);
			}
		} finally {
			stopQuietly(equinox1);
		}
		// make sure it persisted after successful stop
		equinox1 = new Equinox(configuration);
		initAndStart(equinox1);
		try {
			assertNotNull("Bundle does not exists", equinox1.getBundleContext().getBundle(getName()));
		} finally {
			stopQuietly(equinox1);
		}
	}

	/*
	 * Test that a positive value for eclipse.stateSaveDelayInterval results in
	 * periodic persistence.
	 */
	@Test
	public void testPeriodicPersistence() throws Exception {
		// Specify periodic persistence in the configuration.
		Map<String, Object> configuration = createConfiguration();
		configuration.put(ECLIPSE_STATESAVEDELAYINTERVAL, PERIODIC_PERSISTENCE);
		// Create an equinox instance that will be responsible for persisting
		// the bundle once the first period elapses.
		Equinox equinox1 = new Equinox(configuration);
		initAndStart(equinox1);
		try {
			// The bundle has not yet been installed.
			assertNull("Bundle exists", equinox1.getBundleContext().getBundle(getName()));
			// Install the bundle.
			equinox1.getBundleContext().installBundle(getName(), new BundleBuilder().symbolicName(getName()).build());
			// Create a second equinox instance to ensure the first instance
			// has not yet persisted the bundle.
			Equinox equinox2 = new Equinox(configuration);
			initAndStart(equinox2);
			try {
				// The bundle should not have been persisted and therefore be
				// unknown to the second equinox instance. This check must
				// happen before the first period elapses.
				assertNull("Bundle exists", equinox2.getBundleContext().getBundle(getName()));
				stopQuietly(equinox2);
				// Ensure the first instance is given a reasonable amount of
				// time to persist the bundle.
				Thread.sleep(Long.valueOf(PERIODIC_PERSISTENCE) + 2000);
				equinox2 = new Equinox(configuration);
				initAndStart(equinox2);
				// The persisted bundle should now be visible to the second
				// equinox instance.
				assertNotNull("Bundle does not exist", equinox2.getBundleContext().getBundle(getName()));
			} finally {
				stopQuietly(equinox2);
			}
		} finally {
			stopQuietly(equinox1);
		}
	}

}
