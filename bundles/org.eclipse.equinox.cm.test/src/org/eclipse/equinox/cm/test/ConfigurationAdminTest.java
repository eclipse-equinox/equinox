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

import java.util.Properties;
import junit.framework.TestCase;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigurationAdminTest extends TestCase {

	private ConfigurationAdmin cm;
	private ServiceReference reference;

	public ConfigurationAdminTest(String name) {
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

	public void testCreateConfig() throws Exception {
		Configuration config = cm.getConfiguration("test");
		assertEquals("test", config.getPid());
	}

	public void testCreateConfigNullPid() throws Exception {
		try {
			cm.getConfiguration(null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testCreateConfigWithLocation() throws Exception {
		Configuration config = cm.getConfiguration("test", null);
		assertEquals("test", config.getPid());
	}

	public void testCreateConfigNullPidWithLocation() throws Exception {
		try {
			cm.getConfiguration(null, null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testCreateConfigWithAndWithoutLocation() throws Exception {
		Configuration config = cm.getConfiguration("test", "x");
		config.update();
		try {
			Configuration config2 = cm.getConfiguration("test");
			assertEquals(config, config2);
		} finally {
			config.delete();
		}
	}

	public void testCreateConfigWithAndWithoutNullLocation() throws Exception {
		Configuration config = cm.getConfiguration("test", null);
		config.update();
		assertNull(config.getBundleLocation());
		try {
			Configuration config2 = cm.getConfiguration("test");
			assertEquals(config, config2);
			assertEquals(config2.getBundleLocation(), Activator.getBundleContext().getBundle().getLocation());
		} finally {
			config.delete();
		}
	}

	public void testCreateFactoryConfig() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test");
		assertEquals("test", config.getFactoryPid());
	}

	public void testCreateFactoryConfigNullPid() throws Exception {
		try {
			cm.createFactoryConfiguration(null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testCreateFactoryConfigWithLocation() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test", null);
		assertEquals("test", config.getFactoryPid());
	}

	public void testCreateFactoryConfigNullPidWithLocation() throws Exception {
		try {
			cm.createFactoryConfiguration(null, null);
		} catch (IllegalArgumentException e) {
			return;
		}
		fail();
	}

	public void testCreateFactoryConfigWithAndWithoutLocation() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test", "x");
		config.update();
		try {
			Configuration config2 = cm.getConfiguration(config.getPid());
			assertEquals(config, config2);
		} finally {
			config.delete();
		}
	}

	public void testCreateFactoryConfigWithAndWithoutNullLocation() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test", null);
		config.update();
		assertNull(config.getBundleLocation());
		try {
			Configuration config2 = cm.getConfiguration(config.getPid());
			assertEquals(config, config2);
			assertEquals(config2.getBundleLocation(), Activator.getBundleContext().getBundle().getLocation());
		} finally {
			config.delete();
		}
	}

	public void testListConfiguration() throws Exception {
		Configuration config = cm.getConfiguration("test", null);
		config.update();
		try {
			Configuration[] configs = cm.listConfigurations("(" + Constants.SERVICE_PID + "=test)");
			assertTrue(configs != null && configs.length > 0);
		} finally {
			config.delete();
		}
	}

	public void testListConfigurationWithBoundLocation() throws Exception {
		Configuration config = cm.getConfiguration("test", null);
		config.update();
		try {
			String filterString = "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + Activator.getBundleContext().getBundle().getLocation() + ")" + "(" + Constants.SERVICE_PID + "=test)" + ")";
			Configuration[] configs = cm.listConfigurations(filterString);
			assertNull(configs);
			// bind configuration to this bundle's location
			cm.getConfiguration("test");
			configs = cm.listConfigurations(filterString);
			assertTrue(configs != null && configs.length > 0);
		} finally {
			config.delete();
		}
	}

	public void testListFactoryConfiguration() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test", null);
		config.update();
		try {
			Configuration[] configs = cm.listConfigurations("(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=test)");
			assertTrue(configs != null && configs.length > 0);
		} finally {
			config.delete();
		}
	}

	public void testListFactoryConfigurationWithBoundLocation() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test", null);
		config.update();
		try {
			String filterString = "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + Activator.getBundleContext().getBundle().getLocation() + ")" + "(" + Constants.SERVICE_PID + "=" + config.getPid() + ")" + ")";
			Configuration[] configs = cm.listConfigurations(filterString);
			assertNull(configs);
			// bind configuration to this bundle's location
			cm.getConfiguration(config.getPid());
			configs = cm.listConfigurations(filterString);
			assertTrue(configs != null && configs.length > 0);
		} finally {
			config.delete();
		}
	}

	public void testListConfigurationNull() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test", null);
		config.update();
		try {
			Configuration[] configs = cm.listConfigurations(null);
			assertTrue(configs != null && configs.length > 0);
		} finally {
			config.delete();
		}
	}

	public void testPersistentConfig() throws Exception {
		Configuration config = cm.getConfiguration("test");
		assertNull(config.getProperties());
		Properties props = new Properties();
		props.put("testkey", "testvalue");
		config.update(props);
		assertTrue(config.getPid().equals("test"));
		assertTrue(config.getProperties().get("testkey").equals("testvalue"));
		tearDown();
		setUp();
		config = cm.getConfiguration("test");
		assertTrue(config.getProperties().get("testkey").equals("testvalue"));
		config.delete();
		tearDown();
		setUp();
		config = cm.getConfiguration("test");
		assertNull(config.getProperties());
	}

	public void testPersistentFactoryConfig() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test");
		assertNull(config.getProperties());
		Properties props = new Properties();
		props.put("testkey", "testvalue");
		config.update(props);
		assertTrue(config.getFactoryPid().equals("test"));
		assertTrue(config.getProperties().get("testkey").equals("testvalue"));
		String pid = config.getPid();
		tearDown();
		setUp();
		config = cm.getConfiguration(pid);
		assertTrue(config.getProperties().get("testkey").equals("testvalue"));
		config.delete();
		tearDown();
		setUp();
		config = cm.getConfiguration(pid);
		assertNull(config.getProperties());
	}
}
