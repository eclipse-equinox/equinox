/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others
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

import java.io.IOException;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigurationAdminTest extends AbstractCMTest {

	private Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
		return saveAndUpdate(cm.createFactoryConfiguration(factoryPid, location));
	}

	private Configuration getConfiguration(String pid, String location) throws IOException {
		return saveAndUpdate(cm.getConfiguration(pid, location));
	}

	@Test
	public void testCreateConfig() throws Exception {
		Configuration config = cm.getConfiguration("test");
		assertEquals("test", config.getPid());
	}

	@Test
	public void testCreateConfigNullPid() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> cm.getConfiguration(null));
	}

	@Test
	public void testCreateConfigWithLocation() throws Exception {
		Configuration config = cm.getConfiguration("test", null);
		assertEquals("test", config.getPid());
	}

	@Test
	public void testCreateConfigNullPidWithLocation() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> cm.getConfiguration(null, null));
	}

	@Test
	public void testCreateConfigWithAndWithoutLocation() throws Exception {
		Configuration config = getConfiguration("test", "x");
		Configuration config2 = cm.getConfiguration("test");
		assertEquals(config, config2);
	}

	@Test
	public void testCreateConfigWithAndWithoutNullLocation() throws Exception {
		Configuration config = getConfiguration("test", null);
		assertNull(config.getBundleLocation());
		Configuration config2 = cm.getConfiguration("test");
		assertEquals(config, config2);
		assertEquals(config2.getBundleLocation(), getBundleLocation());
	}

	@Test
	public void testCreateFactoryConfig() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test");
		assertEquals("test", config.getFactoryPid());
	}

	@Test
	public void testCreateFactoryConfigNullPid() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> cm.createFactoryConfiguration(null));
	}

	@Test
	public void testCreateFactoryConfigWithLocation() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test", null);
		assertEquals("test", config.getFactoryPid());
	}

	@Test
	public void testCreateFactoryConfigNullPidWithLocation() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> cm.createFactoryConfiguration(null, null));
	}

	@Test
	public void testCreateFactoryConfigWithAndWithoutLocation() throws Exception {
		Configuration config = createFactoryConfiguration("test", "x");
		Configuration config2 = cm.getConfiguration(config.getPid());
		assertEquals(config, config2);
	}

	@Test
	public void testCreateFactoryConfigWithAndWithoutNullLocation() throws Exception {
		Configuration config = createFactoryConfiguration("test", null);
		assertNull(config.getBundleLocation());
		Configuration config2 = cm.getConfiguration(config.getPid());
		assertEquals(config, config2);
		assertEquals(config2.getBundleLocation(), getBundleLocation());
	}

	@Test
	public void testListConfiguration() throws Exception {
		getConfiguration("test", null);
		Configuration[] configs = cm.listConfigurations("(" + Constants.SERVICE_PID + "=test)");
		assertTrue(configs != null && configs.length > 0);
	}

	@Test
	public void testListConfigurationWithBoundLocation() throws Exception {
		getConfiguration("test", null);
		String filterString = "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + getBundleLocation() + ")" + "("
				+ Constants.SERVICE_PID + "=test)" + ")";
		Configuration[] configs = cm.listConfigurations(filterString);
		assertNull(configs);
		// bind configuration to this bundle's location
		cm.getConfiguration("test");
		configs = cm.listConfigurations(filterString);
		assertTrue(configs != null && configs.length > 0);
	}

	@Test
	public void testListFactoryConfiguration() throws Exception {
		createFactoryConfiguration("test", null);
		Configuration[] configs = cm.listConfigurations("(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=test)");
		assertTrue(configs != null && configs.length > 0);
	}

	@Test
	public void testListFactoryConfigurationWithBoundLocation() throws Exception {
		Configuration config = createFactoryConfiguration("test", null);
		String filterString = "(&(" + ConfigurationAdmin.SERVICE_BUNDLELOCATION + "=" + getBundleLocation() + ")" + "("
				+ Constants.SERVICE_PID + "=" + config.getPid() + ")" + ")";
		Configuration[] configs = cm.listConfigurations(filterString);
		assertNull(configs);
		// bind configuration to this bundle's location
		cm.getConfiguration(config.getPid());
		configs = cm.listConfigurations(filterString);
		assertTrue(configs != null && configs.length > 0);
	}

	@Test
	public void testListConfigurationNull() throws Exception {
		createFactoryConfiguration("test", null);
		Configuration[] configs = cm.listConfigurations(null);
		assertNotNull(configs);
		assertTrue(configs.length > 0);
	}

	@Test
	public void testPersistentConfig() throws Exception {
		Configuration config = cm.getConfiguration("test");
		assertNull(config.getProperties());
		config.update(dictionaryOf("testkey", "testvalue"));
		assertEquals("test", config.getPid());
		assertEquals("testvalue", config.getProperties().get("testkey"));
		tearDown();
		setUp();
		config = cm.getConfiguration("test");
		assertEquals("testvalue", config.getProperties().get("testkey"));
		config.delete();
		tearDown();
		setUp();
		config = cm.getConfiguration("test");
		assertNull(config.getProperties());
	}

	@Test
	public void testPersistentFactoryConfig() throws Exception {
		Configuration config = cm.createFactoryConfiguration("test");
		assertNull(config.getProperties());
		config.update(dictionaryOf("testkey", "testvalue"));
		assertEquals("test", config.getFactoryPid());
		assertEquals("testvalue", config.getProperties().get("testkey"));
		String pid = config.getPid();
		tearDown();
		setUp();
		config = cm.getConfiguration(pid);
		assertEquals("testvalue", config.getProperties().get("testkey"));
		config.delete();
		tearDown();
		setUp();
		config = cm.getConfiguration(pid);
		assertNull(config.getProperties());
	}

	private static String getBundleLocation() {
		return getBundleContext().getBundle().getLocation();
	}
}
