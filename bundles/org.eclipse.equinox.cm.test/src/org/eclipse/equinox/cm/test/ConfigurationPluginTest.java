/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others
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

public class ConfigurationPluginTest extends TestCase {

	private ConfigurationAdmin cm;
	private ServiceReference reference;
	boolean locked = false;
	Object lock = new Object();
	boolean success;

	public ConfigurationPluginTest(String name) {
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

	public void testPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Properties props = new Properties();
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

	public void testPidSpecificPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Properties props = new Properties();
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

	public void testPidSpecificMissPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Properties props = new Properties();
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

	public void testRankedPlugin() throws Exception {

		Configuration config = cm.getConfiguration("test");
		Properties props = new Properties();
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

}
