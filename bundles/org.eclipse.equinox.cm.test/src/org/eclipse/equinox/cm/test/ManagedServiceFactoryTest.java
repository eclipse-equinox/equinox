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
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ManagedServiceFactory;

public class ManagedServiceFactoryTest extends AbstractCMTest {

	int updateCount = 0;
	boolean locked = false;
	Object lock = new Object();

	@Test
	public void testSamePidManagedServiceFactory() throws Exception {

		Configuration config = cm.createFactoryConfiguration("test");
		config.update(dictionaryOf("testkey", "testvalue"));

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

		Dictionary<String, Object> dict = dictionaryOf(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedServiceFactory> reg = null;
		synchronized (lock) {
			reg = registerService(ManagedServiceFactory.class, msf, dict);
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(1, updateCount);
		}

		ServiceRegistration<ManagedServiceFactory> reg2 = null;
		synchronized (lock) {
			reg2 = registerService(ManagedServiceFactory.class, msf, dict);
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

		ServiceRegistration<ManagedServiceFactory> reg = null;
		synchronized (lock) {
			reg = registerService(ManagedServiceFactory.class, msf, dictionaryOf(Constants.SERVICE_PID, "test"));
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(0, updateCount);
			locked = false;
		}

		Configuration config = cm.createFactoryConfiguration("test");
		assertNull(config.getProperties());
		Dictionary<String, Object> props = dictionaryOf("testkey", "testvalue");

		synchronized (lock) {
			config.update(props);
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(1, updateCount);
		}

		synchronized (lock) {
			reg.setProperties(emptyDictionary());
			config.update(dictionaryOf("testkey", "testvalue2"));
			locked = true;
			lock.wait(100);
			assertTrue(locked);
			assertEquals(1, updateCount);
			locked = false;
		}

		config.delete();
		config = cm.createFactoryConfiguration("test2");
		config.update(props);
		synchronized (lock) {
			reg.setProperties(dictionaryOf(Constants.SERVICE_PID, "test2"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(2, updateCount);
		}

		synchronized (lock) {
			config.delete();
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals(3, updateCount);
		}
		reg.unregister();
	}
}
