/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others
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

import java.util.*;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.*;

public class ConfigurationPluginTest extends AbstractCMTest {

	boolean locked = false;
	Object lock = new Object();
	boolean success;

	@Test
	public void testPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		config.update(dictionaryOf("testkey", "testvalue"));

		ConfigurationPlugin plugin = (serviceReference, properties) -> properties.put("plugin", "plugin1");
		ServiceRegistration<ConfigurationPlugin> pluginReg = registerService(ConfigurationPlugin.class, plugin, null);

		ManagedService ms = properties -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
				success = "plugin1".equals(properties.get("plugin"));
			}
		};

		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = registerService(ManagedService.class, ms, dictionaryOf(Constants.SERVICE_PID, "test"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertTrue(success);
		}

		reg.unregister();
		pluginReg.unregister();
		config.delete();
	}

	@Test
	public void testPidSpecificPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		config.update(dictionaryOf("testkey", "testvalue"));

		ConfigurationPlugin plugin = (serviceReference, properties) -> properties.put("plugin", "plugin1");
		ServiceRegistration<ConfigurationPlugin> pluginReg = registerService(ConfigurationPlugin.class, plugin,
				dictionaryOf(ConfigurationPlugin.CM_TARGET, new String[] { "test" }));

		ManagedService ms = properties -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
				success = "plugin1".equals(properties.get("plugin"));
			}
		};

		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = registerService(ManagedService.class, ms, dictionaryOf(Constants.SERVICE_PID, "test"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertTrue(success);
		}

		reg.unregister();
		pluginReg.unregister();
		config.delete();
	}

	@Test
	public void testPidSpecificMissPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		config.update(dictionaryOf("testkey", "testvalue"));

		ConfigurationPlugin plugin = (serviceReference, properties) -> properties.put("plugin", "plugin1");
		ServiceRegistration<ConfigurationPlugin> pluginReg = registerService(ConfigurationPlugin.class, plugin,
				dictionaryOf(ConfigurationPlugin.CM_TARGET, new String[] { "testXXX" }));

		ManagedService ms = properties -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
				success = "plugin1".equals(properties.get("plugin"));
			}
		};

		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = registerService(ManagedService.class, ms, dictionaryOf(Constants.SERVICE_PID, "test"));
			locked = true;
			lock.wait(5000);
			assertFalse(success);
		}

		reg.unregister();
		pluginReg.unregister();
		config.delete();
	}

	@Test
	public void testRankedPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		config.update(dictionaryOf("testkey", "testvalue"));

		ConfigurationPlugin plugin = (serviceReference, properties) -> properties.put("plugin", "plugin1");
		ServiceRegistration<ConfigurationPlugin> pluginReg1 = registerService(ConfigurationPlugin.class, plugin,
				dictionaryOf(ConfigurationPlugin.CM_RANKING, 1));

		ConfigurationPlugin plugin2 = (serviceReference, properties) -> properties.put("plugin", "plugin2");

		ServiceRegistration<ConfigurationPlugin> pluginReg2 = registerService(ConfigurationPlugin.class, plugin2, null);

		ManagedService ms = properties -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
				success = "plugin1".equals(properties.get("plugin"));
			}
		};

		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = registerService(ManagedService.class, ms, dictionaryOf(Constants.SERVICE_PID, "test"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertTrue(success);
		}

		reg.unregister();
		pluginReg1.unregister();
		pluginReg2.unregister();
		config.delete();
	}

	@Test
	public void testSameRankedPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		config.update(dictionaryOf("testkey", "testvalue"));
		final List<String> pluginsCalled = new ArrayList<>();

		ConfigurationPlugin plugin1 = (serviceReference, properties) -> pluginsCalled.add("plugin1");
		ServiceRegistration<ConfigurationPlugin> pluginReg1 = registerService(ConfigurationPlugin.class, plugin1,
				dictionaryOf(Constants.SERVICE_RANKING, 1));

		ConfigurationPlugin plugin2 = (serviceReference, properties) -> pluginsCalled.add("plugin2");

		ServiceRegistration<ConfigurationPlugin> pluginReg2 = registerService(ConfigurationPlugin.class, plugin2,
				dictionaryOf(Constants.SERVICE_RANKING, 2));

		ConfigurationPlugin plugin3 = (serviceReference, properties) -> pluginsCalled.add("plugin3");

		ServiceRegistration<ConfigurationPlugin> pluginReg3 = registerService(ConfigurationPlugin.class, plugin3,
				dictionaryOf(Constants.SERVICE_RANKING, 1));

		ManagedService ms = properties -> {
			synchronized (lock) {
				locked = false;
				lock.notify();
			}
		};

		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			reg = registerService(ManagedService.class, ms, dictionaryOf(Constants.SERVICE_PID, "test"));
			locked = true;
			lock.wait(5000);
			assertFalse("should have updated", locked);
			assertEquals("Wrong order called for plugins.", Arrays.asList("plugin2", "plugin1", "plugin3"),
					pluginsCalled);
		}

		reg.unregister();
		pluginReg1.unregister();
		pluginReg2.unregister();
		pluginReg3.unregister();
		config.delete();
	}
}
