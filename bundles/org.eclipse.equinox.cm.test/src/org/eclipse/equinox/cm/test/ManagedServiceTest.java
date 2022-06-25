/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others
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
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.LogFilter;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.log.*;

public class ManagedServiceTest {

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
	public void testSamePidManagedService() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);

		updateCount = 0;
		ManagedService ms = new ManagedService() {

			public void updated(Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}

			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			reg = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(1, updateCount);
		}

		ServiceRegistration<ManagedService> reg2 = null;
		synchronized (lock) {
			reg2 = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
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
	public void testBug374637() throws Exception {

		ManagedService ms = new ManagedService() {

			public void updated(Dictionary<String, ?> properties) {
				// nothing
			}
		};

		ExtendedLogReaderService reader = Activator.getBundleContext().getService(Activator.getBundleContext().getServiceReference(ExtendedLogReaderService.class));
		synchronized (lock) {
			locked = false;
		}
		LogListener listener = new LogListener() {
			public void logged(LogEntry entry) {
				synchronized (lock) {
					locked = true;
					lock.notifyAll();
				}
			}
		};
		reader.addLogListener(listener, new LogFilter() {
			public boolean isLoggable(Bundle bundle, String loggerName, int logLevel) {
				return logLevel == LogService.LOG_ERROR;
			}
		});
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg1 = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
		ServiceRegistration<ManagedService> reg2 = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);

		reg1.unregister();
		reg2.unregister();
		reader.removeLogListener(listener);

		synchronized (lock) {
			lock.wait(1000);
			assertFalse("Got a error log", locked);
		}
	}

	@Test
	public void testGeneralManagedService() throws Exception {
		updateCount = 0;
		ManagedService ms = new ManagedService() {

			public void updated(Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					updateCount++;
				}

			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");

		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			reg = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(1, updateCount);
		}

		Configuration config = cm.getConfiguration("test");
		assertNull(config.getProperties());
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");

		synchronized (lock) {
			config.update(props);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(2, updateCount);
		}

		String location = config.getBundleLocation();

		synchronized (lock) {
			config.setBundleLocation("bogus");
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(3, updateCount);
		}

		synchronized (lock) {
			config.update();
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(3, updateCount);
			locked = false;
		}

		synchronized (lock) {
			config.setBundleLocation(location);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(4, updateCount);
		}

		dict.remove(Constants.SERVICE_PID);
		synchronized (lock) {
			reg.setProperties(dict);
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(4, updateCount);
			locked = false;
		}

		synchronized (lock) {
			props.put("testkey", "testvalue2");
			config.update(props);
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(4, updateCount);
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
			assertEquals(5, updateCount);
		}

		synchronized (lock) {
			config.delete();
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertEquals(6, updateCount);
		}
		reg.unregister();
	}
}
