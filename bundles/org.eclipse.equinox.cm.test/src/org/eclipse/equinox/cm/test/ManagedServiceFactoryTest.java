/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
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
package org.eclipse.equinox.cm.test;

import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

public class ManagedServiceFactoryTest {

	private ConfigurationAdmin cm;
	private ServiceReference<ConfigurationAdmin> reference;
	int updateCount = 0;
	boolean locked = false;
	Object lock = new Object();

	@Before
	public void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.cm").start();
		reference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class);
		cm = Activator.getBundleContext().getService(reference);

	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(reference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
	}

	@Test
	public void testSamePidManagedServiceFactory() throws Exception {

		Configuration config = cm.createFactoryConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);

		updateCount = 0;
		ManagedServiceFactory msf = new ManagedServiceFactory() {

			public void deleted(String pid) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}
			}

			public String getName() {
				return null;
			}

			public void updated(String pid, Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}
			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedServiceFactory> reg = null;
		synchronized (lock) {
			reg = Activator.getBundleContext().registerService(ManagedServiceFactory.class, msf, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(1, updateCount);
		}

		ServiceRegistration<ManagedServiceFactory> reg2 = null;
		synchronized (lock) {
			reg2 = Activator.getBundleContext().registerService(ManagedServiceFactory.class, msf, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(2, updateCount);
		}
		reg.unregister();
		reg2.unregister();
		config.delete();
	}

	@Test
	public void testGeneralManagedServiceFactory() throws Exception {
		updateCount = 0;
		ManagedServiceFactory msf = new ManagedServiceFactory() {

			public void deleted(String pid) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}
			}

			public String getName() {
				return null;
			}

			public void updated(String pid, Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}
			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");

		ServiceRegistration<ManagedServiceFactory> reg = null;
		synchronized (lock) {
			reg = Activator.getBundleContext().registerService(ManagedServiceFactory.class, msf, dict);
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(0, updateCount);
			locked = false;
		}

		Configuration config = cm.createFactoryConfiguration("test");
		assertNull(config.getProperties());
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");

		synchronized (lock) {
			config.update(props);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(1, updateCount);
		}

		dict.remove(Constants.SERVICE_PID);
		synchronized (lock) {
			reg.setProperties(dict);
			props.put("testkey", "testvalue2");
			config.update(props);
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(1, updateCount);
			locked = false;
		}

		config.delete();
		config = cm.createFactoryConfiguration("test2");
		config.update(props);
		dict.put(Constants.SERVICE_PID, "test2");
		synchronized (lock) {
			reg.setProperties(dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(2, updateCount);
		}

		synchronized (lock) {
			config.delete();
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(3, updateCount);
		}
		reg.unregister();
	}
}
