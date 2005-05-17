/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.datalocation;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.tests.adaptor.testsupport.TestLocationManager;

public class BasicLocationTests extends TestCase {

	String originalUser = null;
	String originalInstance = null;
	String originalConfiguration = null;
	String originalInstall = null;
	String prefix = "";
	boolean windows = Platform.getOS().equals(Platform.OS_WIN32);
	
	public BasicLocationTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(BasicLocationTests.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		prefix = windows ? "c:" : "";
		originalUser = System.getProperty(LocationManager.PROP_USER_AREA);
		originalInstance = System.getProperty(LocationManager.PROP_INSTANCE_AREA);
		originalConfiguration = System.getProperty(LocationManager.PROP_CONFIG_AREA);
		originalInstall = System.getProperty(LocationManager.PROP_INSTALL_AREA);
	}

	private void setProperty(String key, String value) {
		if (value == null)
			System.getProperties().remove(key);
		else
			System.setProperty(key, value);
	}
	
	protected void tearDown() throws Exception {
		setProperty(LocationManager.PROP_USER_AREA, originalUser);
		setProperty(LocationManager.PROP_INSTANCE_AREA, originalInstance);
		setProperty(LocationManager.PROP_CONFIG_AREA, originalConfiguration);
		setProperty(LocationManager.PROP_INSTALL_AREA, originalInstall);
		TestLocationManager.initializeLocations();
		super.tearDown();
	}

	private void checkSlashes() {
		checkLocation(TestLocationManager.getUserLocation(), true, true, null);
		checkLocation(TestLocationManager.getInstanceLocation(), true, true, null);
		checkLocation(TestLocationManager.getConfigurationLocation(), true, true, null);
		checkLocation(TestLocationManager.getInstallLocation(), true, true, null);
	}

	private void checkLocation(Location location, boolean leading, boolean trailing, String scheme) {
		if (location == null)
			return;
		URL url = location.getURL();
		if (scheme != null)
			assertEquals(scheme, url.getProtocol());
		if (!url.getProtocol().equals("file"))
			return;
		assertTrue(url.toExternalForm() + " should " + (trailing ? "" : "not") + " have a trailing slash", url.getFile().endsWith("/") == trailing);
		if (windows)
			assertTrue(url.toExternalForm() + " should " + (leading ? "" : "not") + " have a leading slash", url.getFile().startsWith("/") == leading);
	}

	public void testSlashes() {
		setProperty(LocationManager.PROP_USER_AREA, prefix + "/a");
		setProperty(LocationManager.PROP_INSTANCE_AREA, prefix + "/c/d");
		setProperty(LocationManager.PROP_CONFIG_AREA, prefix + "/e/f");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		TestLocationManager.initializeLocations();
		checkSlashes();
	}

	public void testSchemes() {
		setProperty(LocationManager.PROP_USER_AREA, "http://example.com/a");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "ftp://example.com/c/d");
		setProperty(LocationManager.PROP_CONFIG_AREA, "platform:/base/e/f");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		TestLocationManager.initializeLocations();
		checkSlashes();
		checkLocation(TestLocationManager.getUserLocation(), true, true, "http");
		checkLocation(TestLocationManager.getInstanceLocation(), true, true, "ftp");
		checkLocation(TestLocationManager.getConfigurationLocation(), true, true, "platform");
		checkLocation(TestLocationManager.getInstallLocation(), true, true, "file");
		
	}

	public void testNone() {
		setProperty(LocationManager.PROP_USER_AREA, "@none");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "@none");
		setProperty(LocationManager.PROP_CONFIG_AREA, "@none");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		TestLocationManager.initializeLocations();
		assertNull("User location should be null", TestLocationManager.getUserLocation());
		assertNull("Instance location should be null", TestLocationManager.getUserLocation());
		assertNull("Configuration location should be null", TestLocationManager.getUserLocation());
	}

	public void testUserDir() {
		setProperty(LocationManager.PROP_USER_AREA, "@user.dir");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "@user.dir");
		setProperty(LocationManager.PROP_CONFIG_AREA, "@user.dir");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		TestLocationManager.initializeLocations();
		checkLocation(TestLocationManager.getUserLocation(), true, true, "file");
		checkLocation(TestLocationManager.getInstanceLocation(), true, true, "file");
		checkLocation(TestLocationManager.getConfigurationLocation(), true, true, "file");
		checkLocation(TestLocationManager.getInstallLocation(), true, true, "file");
	}

	public void testUserHome() {
		setProperty(LocationManager.PROP_USER_AREA, "@user.home");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "@user.home");
		setProperty(LocationManager.PROP_CONFIG_AREA, "@user.home");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file:" + prefix + "/g");
		TestLocationManager.initializeLocations();
		checkLocation(TestLocationManager.getUserLocation(), true, true, "file");
		checkLocation(TestLocationManager.getInstanceLocation(), true, true, "file");
		checkLocation(TestLocationManager.getConfigurationLocation(), true, true, "file");
		checkLocation(TestLocationManager.getInstallLocation(), true, true, "file");
	}

	public void testUNC() {
		if (!windows)
			return;
		setProperty(LocationManager.PROP_USER_AREA, "//server/share/a");
		setProperty(LocationManager.PROP_INSTANCE_AREA, "//server/share/b");
		setProperty(LocationManager.PROP_CONFIG_AREA, "//server/share/c");
		setProperty(LocationManager.PROP_INSTALL_AREA, "file://server/share/g");
		TestLocationManager.initializeLocations();
		checkLocation(TestLocationManager.getUserLocation(), true, true, "file");
		checkLocation(TestLocationManager.getInstanceLocation(), true, true, "file");
		checkLocation(TestLocationManager.getConfigurationLocation(), true, true, "file");
		checkLocation(TestLocationManager.getInstallLocation(), true, true, "file");
	}

}
