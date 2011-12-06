/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.equinox.http.servlet.tests.bundle.Activator;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleAdvisor;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleInstaller;
import org.eclipse.equinox.http.servlet.tests.util.ServletRequestAdvisor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class ServletTest extends TestCase {
	private static final String EQUINOX_DS_BUNDLE = "org.eclipse.equinox.ds"; //$NON-NLS-1$
	private static final String EQUINOX_JETTY_BUNDLE = "org.eclipse.equinox.http.jetty"; //$NON-NLS-1$
	private static final String JETTY_PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty."; //$NON-NLS-1$
	private static final String OSGI_HTTP_PORT_PROPERTY = "org.osgi.service.http.port"; //$NON-NLS-1$
	private static final String STATUS_OK = "OK"; //$NON-NLS-1$
	private static final String TEST_BUNDLES_BINARY_DIRECTORY = "/bundles_bin/"; //$NON-NLS-1$
	private static final String TEST_BUNDLE_1 = "tb1"; //$NON-NLS-1$
	
	private static final String[] BUNDLES = new String[] {
		ServletTest.EQUINOX_DS_BUNDLE
	};

	private BundleInstaller installer;
	private BundleAdvisor advisor;
	private ServletRequestAdvisor requestAdvisor;

	private BundleContext getBundleContext() {
		return Activator.getBundleContext();
	}
	
	private String getJettyProperty(String key, String defaultValue) {
		String qualifiedKey = ServletTest.JETTY_PROPERTY_PREFIX + key;
		String value = getProperty(qualifiedKey);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	private String getPort() {
		String defaultPort = getProperty(ServletTest.OSGI_HTTP_PORT_PROPERTY);
		if (defaultPort == null) {
			defaultPort = "80"; //$NON-NLS-1$
		}
		return getJettyProperty("port", defaultPort); //$NON-NLS-1$
	}
	
	private String getProperty(String key) {
		BundleContext bundleContext = getBundleContext();
		String value = bundleContext.getProperty(key);
		return value;
	}
	
	private Bundle installBundle(String bundle) throws BundleException {
		return installer.installBundle(bundle);
	}

	public void setUp() throws Exception {
		BundleContext bundleContext = getBundleContext();
		installer = new BundleInstaller(ServletTest.TEST_BUNDLES_BINARY_DIRECTORY, bundleContext);
		advisor = new BundleAdvisor(bundleContext);
		String port = getPort();
		requestAdvisor = new ServletRequestAdvisor(port);
		startBundles();
		stopJetty();
		startJetty();
	}

	private void startBundles() throws BundleException {
		for (int i = 0; i < ServletTest.BUNDLES.length; i++) {
			String bundle = ServletTest.BUNDLES[i];
			advisor.startBundle(bundle);
		}
	}

	private void startJetty() throws BundleException {
		advisor.startBundle(ServletTest.EQUINOX_JETTY_BUNDLE);
	}

	private void stopBundles() throws BundleException {
		for (int i = ServletTest.BUNDLES.length - 1; i >= 0; i--) {
			String bundle = ServletTest.BUNDLES[i];
			advisor.stopBundle(bundle);
		}
	}

	private void stopJetty() throws BundleException {
		advisor.stopBundle(ServletTest.EQUINOX_JETTY_BUNDLE);
	}

	public void tearDown() throws Exception {
		stopJetty();
		stopBundles();
		requestAdvisor = null;		
		advisor = null;
		try {
			installer.shutdown();
		} finally {
			installer = null;
		}
	}

	public void test_TestServlet1() throws Exception {
		String expected = ServletTest.STATUS_OK;
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet1"); //$NON-NLS-1$
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}
	
	public void test_TestServlet2() throws Exception {
		String expected = "3";  //$NON-NLS-1$
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet2"); //$NON-NLS-1$
			Assert.assertEquals(expected, actual);
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);		
	}

	public void test_TestServlet3() throws Exception {
		String expected = ServletTest.STATUS_OK;
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet3"); //$NON-NLS-1$
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}
	
	private void uninstallBundle(Bundle bundle) throws BundleException {
		installer.uninstallBundle(bundle);
	}
}
