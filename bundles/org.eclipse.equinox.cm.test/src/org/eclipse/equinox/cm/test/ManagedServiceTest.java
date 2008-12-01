/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.cm.test;

import java.util.Dictionary;
import java.util.Properties;
import junit.framework.TestCase;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

public class ManagedServiceTest extends TestCase {

	private ConfigurationAdmin cm;
	private ServiceReference reference;
	int updateCount = 0;
	boolean locked = false;
	Object lock = new Object();

	public ManagedServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.cm").start();
		reference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
		cm = (ConfigurationAdmin) Activator.getBundleContext().getService(reference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(reference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
	}

	public void testSamePidManagedService() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Properties props = new Properties();
		props.put("testkey", "testvalue");
		config.update(props);

		updateCount = 0;
		ManagedService ms = new ManagedService() {

			public void updated(Dictionary properties) throws ConfigurationException {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}

			}
		};

		Dictionary dict = new Properties();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration reg = null;
		synchronized (lock) {
			reg = Activator.getBundleContext().registerService(ManagedService.class.getName(), ms, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(1, updateCount);
		}

		ServiceRegistration reg2 = null;
		synchronized (lock) {
			reg2 = Activator.getBundleContext().registerService(ManagedService.class.getName(), ms, dict);
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(1, updateCount);
			locked = false;
		}
		reg.unregister();
		reg2.unregister();
		config.delete();
	}

	public void testGeneralManagedService() throws Exception {
		updateCount = 0;
		ManagedService ms = new ManagedService() {

			public void updated(Dictionary properties) throws ConfigurationException {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}

			}
		};

		Dictionary dict = new Properties();
		dict.put(Constants.SERVICE_PID, "test");

		ServiceRegistration reg = null;
		synchronized (lock) {
			reg = Activator.getBundleContext().registerService(ManagedService.class.getName(), ms, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(1, updateCount);
		}

		Configuration config = cm.getConfiguration("test");
		assertNull(config.getProperties());
		Properties props = new Properties();
		props.put("testkey", "testvalue");

		synchronized (lock) {
			config.update(props);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(2, updateCount);
		}

		dict.remove(Constants.SERVICE_PID);
		synchronized (lock) {
			reg.setProperties(dict);
			props.put("testkey", "testvalue2");
			config.update(props);
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(2, updateCount);
			locked = false;
		}

		config.delete();
		config = cm.getConfiguration("test2");
		dict.put(Constants.SERVICE_PID, "test2");
		synchronized (lock) {
			reg.setProperties(dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(3, updateCount);
		}

		synchronized (lock) {
			config.delete();
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(4, updateCount);
		}
		reg.unregister();
	}
}
