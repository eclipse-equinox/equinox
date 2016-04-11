/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	private ServiceReference reference;
	boolean locked = false;
	Object lock = new Object();
	boolean success;

	@Before
	public void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.cm").start();
		reference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
		cm = (ConfigurationAdmin) Activator.getBundleContext().getService(reference);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(reference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
	}

	@Test
	public void testPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				properties.put("plugin", "plugin1");
			}
		};
		ServiceRegistration pluginReg = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin, null);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary properties) throws ConfigurationException {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary dict = new Properties();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class.getName(), ms, dict);
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
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				properties.put("plugin", "plugin1");
			}
		};
		Dictionary pluginDict = new Properties();
		pluginDict.put(ConfigurationPlugin.CM_TARGET, new String[] {"test"});
		ServiceRegistration pluginReg = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin, pluginDict);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary properties) throws ConfigurationException {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary dict = new Properties();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class.getName(), ms, dict);
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
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				properties.put("plugin", "plugin1");
			}
		};
		Dictionary pluginDict = new Properties();
		pluginDict.put(ConfigurationPlugin.CM_TARGET, new String[] {"testXXX"});
		ServiceRegistration pluginReg = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin, pluginDict);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary properties) throws ConfigurationException {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary dict = new Properties();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class.getName(), ms, dict);
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
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("testkey", "testvalue");
		config.update(props);

		ConfigurationPlugin plugin = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				properties.put("plugin", "plugin1");
			}
		};
		Dictionary pluginDict = new Properties();
		pluginDict.put(ConfigurationPlugin.CM_RANKING, new Integer(1));
		ServiceRegistration pluginReg1 = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin, pluginDict);

		ConfigurationPlugin plugin2 = new ConfigurationPlugin() {

			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				properties.put("plugin", "plugin2");
			}
		};

		ServiceRegistration pluginReg2 = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin2, null);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary properties) throws ConfigurationException {
				synchronized (lock) {
					locked = false;
					lock.notify();
					success = "plugin1".equals(properties.get("plugin"));
				}
			}
		};

		Dictionary dict = new Properties();
		dict.put(Constants.SERVICE_PID, "test");
		ServiceRegistration reg = null;
		synchronized (lock) {
			success = false;
			reg = Activator.getBundleContext().registerService(ManagedService.class.getName(), ms, dict);
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
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("testkey", "testvalue");
		config.update(props);
		final List pluginsCalled = new ArrayList();

		Hashtable pluginProps = new Hashtable();
		pluginProps.put(Constants.SERVICE_RANKING, new Integer(1));
		ConfigurationPlugin plugin1 = new ConfigurationPlugin() {
			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				pluginsCalled.add("plugin1");
			}
		};
		ServiceRegistration pluginReg1 = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin1, pluginProps);

		ConfigurationPlugin plugin2 = new ConfigurationPlugin() {
			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				pluginsCalled.add("plugin2");
			}
		};

		pluginProps.put(Constants.SERVICE_RANKING, new Integer(2));
		ServiceRegistration pluginReg2 = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin2, pluginProps);

		ConfigurationPlugin plugin3 = new ConfigurationPlugin() {
			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				pluginsCalled.add("plugin3");
			}
		};

		pluginProps.put(Constants.SERVICE_RANKING, new Integer(1));
		ServiceRegistration pluginReg3 = Activator.getBundleContext().registerService(ConfigurationPlugin.class.getName(), plugin3, pluginProps);

		ManagedService ms = new ManagedService() {
			public void updated(Dictionary properties) throws ConfigurationException {
				synchronized (lock) {
					locked = false;
					lock.notify();
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
			assertEquals("Wrong order called for plugins.", Arrays.asList(new String[] {"plugin2", "plugin1", "plugin3"}), pluginsCalled);
		}

		reg.unregister();
		pluginReg1.unregister();
		pluginReg2.unregister();
		pluginReg3.unregister();
		config.delete();
	}
}
