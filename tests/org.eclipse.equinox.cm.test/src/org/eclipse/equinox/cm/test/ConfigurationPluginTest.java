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
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

public class ConfigurationPluginTest {

	private ConfigurationAdmin cm;
	private ServiceReference<ConfigurationAdmin> reference;
	boolean locked = false;
	Object lock = new Object();
	boolean success;

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
	public void testPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				properties.put("plugin", "plugin1");
			}
		};
		ServiceRegistration<ConfigurationPlugin> pluginReg = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin, null);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertTrue(success);
		}

		reg.unregister();
		pluginReg.unregister();
		config.delete();
	}

	@Test
	public void testPidSpecificPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				properties.put("plugin", "plugin1");
			}
		};
		Dictionary<String, Object> pluginDict = new Hashtable<>();
		pluginDict.put(ConfigurationPlugin.CM_TARGET, new String[] {"test"});
		ServiceRegistration<ConfigurationPlugin> pluginReg = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin, pluginDict);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
			assertTrue(success);
		}

		reg.unregister();
		pluginReg.unregister();
		config.delete();
	}

	@Test
	public void testPidSpecificMissPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				properties.put("plugin", "plugin1");
			}
		};
		Dictionary<String, Object> pluginDict = new Hashtable<>();
		pluginDict.put(ConfigurationPlugin.CM_TARGET, new String[] {"testXXX"});
		ServiceRegistration<ConfigurationPlugin> pluginReg = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin, pluginDict);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
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
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				properties.put("plugin", "plugin1");
			}
		};
		Dictionary<String, Object> pluginDict = new Hashtable<>();
		pluginDict.put(ConfigurationPlugin.CM_RANKING, Integer.valueOf(1));
		ServiceRegistration<ConfigurationPlugin> pluginReg1 = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin, pluginDict);

		ConfigurationPlugin plugin2 = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				properties.put("plugin", "plugin2");
			}
		};

		ServiceRegistration<ConfigurationPlugin> pluginReg2 = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin2, null);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration<ManagedService> reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class, ms, dict);
			locked = true;
			lock.wait(5000);
			if (locked)
				fail("should have updated");
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
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("testkey", "testvalue");
		config.update(props);
		final List<String> pluginsCalled = new ArrayList<>();

		Hashtable<String, Object> pluginProps = new Hashtable<>();
		pluginProps.put(Constants.SERVICE_RANKING, Integer.valueOf(1));
		ConfigurationPlugin plugin1 = new ConfigurationPlugin() {
			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				pluginsCalled.add("plugin1");
			}
		};
		ServiceRegistration<ConfigurationPlugin> pluginReg1 = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin1, pluginProps);

		ConfigurationPlugin plugin2 = new ConfigurationPlugin() {
			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				pluginsCalled.add("plugin2");
			}
		};

		pluginProps.put(Constants.SERVICE_RANKING, Integer.valueOf(2));
		ServiceRegistration<ConfigurationPlugin> pluginReg2 = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin2, pluginProps);

		ConfigurationPlugin plugin3 = new ConfigurationPlugin() {
			public void modifyConfiguration(ServiceReference<?> serviceReference, Dictionary<String, Object> properties) {
				pluginsCalled.add("plugin3");
			}
		};

		pluginProps.put(Constants.SERVICE_RANKING, Integer.valueOf(1));
		ServiceRegistration<ConfigurationPlugin> pluginReg3 = Activator.getBundleContext().registerService(ConfigurationPlugin.class, plugin3, pluginProps);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary<String, ?> properties) {
				synchronized (lock) {
					locked = false;
					lock.notify();
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
			assertEquals("Wrong order called for plugins.", Arrays.asList(new String[] {"plugin2", "plugin1", "plugin3"}), pluginsCalled);
		}

		reg.unregister();
		pluginReg1.unregister();
		pluginReg2.unregister();
		pluginReg3.unregister();
		config.delete();
	}
}
