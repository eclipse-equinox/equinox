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
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.junit.Test;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;

public class ManagedServiceTest extends AbstractCMTest {

	int updateCount = 0;
	boolean locked = false;
	Object lock = new Object();

	@Test
	public void testSamePidManagedService() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = dictionaryOf("testkey", "testvalue");
		config.update(props);

		updateCount = 0;
		ManagedService ms = properties -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
				updateCount++;
			}

		};

		Dictionary<String, Object> dict = dictionaryOf(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			reg = registerService(ManagedService.class, ms, dict);
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(1, updateCount);
		}

		ServiceRegistration<ManagedService> reg2 = null;
		synchronized (lock) {
			reg2 = registerService(ManagedService.class, ms, dict);
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(2, updateCount);
		}
		reg.unregister();
		reg2.unregister();
		config.delete();
	}

	@Test
	public void testBug374637() throws Exception {

		ManagedService ms = properties -> {
			// nothing
		};

		BundleContext ctx = getBundleContext();
		ExtendedLogReaderService reader = ctx.getService(ctx.getServiceReference(ExtendedLogReaderService.class));
		synchronized (lock) {
			locked = false;
		}
		LogListener listener = entry -> {
			synchronized (lock) {
				locked = true;
				lock.notifyAll();
			}
		};
		reader.addLogListener(listener, (bundle, name, level) -> level == LogLevel.ERROR.ordinal());
		Dictionary<String, Object> dict = dictionaryOf(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg1 = registerService(ManagedService.class, ms, dict);
		ServiceRegistration<ManagedService> reg2 = registerService(ManagedService.class, ms, dict);

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
		ManagedService ms = properties -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
				updateCount++;
			}
		};

		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			reg = registerService(ManagedService.class, ms, dictionaryOf(Constants.SERVICE_PID, "test"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(1, updateCount);
		}

		Configuration config = cm.getConfiguration("test");
		assertNull(config.getProperties());
		synchronized (lock) {
			config.update(dictionaryOf("testkey", "testvalue"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(2, updateCount);
		}

		String location = config.getBundleLocation();

		synchronized (lock) {
			config.setBundleLocation("bogus");
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
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
			assertFalse("should have updated", locked);
			assertEquals(4, updateCount);
		}

		synchronized (lock) {
			reg.setProperties(emptyDictionary());
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(4, updateCount);
			locked = false;
		}

		synchronized (lock) {
			config.update(dictionaryOf("testkey", "testvalue2"));
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(4, updateCount);
			locked = false;
		}

		config.delete();
		config = cm.getConfiguration("test2");
		synchronized (lock) {
			reg.setProperties(dictionaryOf(Constants.SERVICE_PID, "test2"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(5, updateCount);
		}

		synchronized (lock) {
			config.delete();
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(6, updateCount);
		}
		reg.unregister();
	}
}
