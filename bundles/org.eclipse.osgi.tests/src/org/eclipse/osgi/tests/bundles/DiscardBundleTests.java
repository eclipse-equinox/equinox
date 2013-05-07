/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;

/*
 * The framework must discard a persisted bundle when the 
 * osgi.checkConfiguration configuration property is specified and equal to
 * true.
 * 
 * On a related note, if the osgi.dev configuration property is specified but 
 * the osgi.checkConfiguration configuration property is not specified, the
 * framework must specify the osgi.checkConfiguration configuration property
 * with a value equal to true.
 */
public class DiscardBundleTests extends AbstractBundleTests {
	private static final String BUNDLE_DIR = "discardable";
	private static final String BUNDLE_JAR = BUNDLE_DIR + ".jar";
	private static final String BUNDLE_MANIFEST = "META-INF/MANIFEST.MF";
	private static final String OSGI_DEV = "osgi.dev";
	private static final String OSGI_CHECKCONFIGURATION = "osgi.checkConfiguration";
	private static final String REFERENCE_PROTOCOL = "reference:";

	private File root;

	protected void setUp() throws Exception {
		super.setUp();
		root = OSGiTestsActivator.getContext().getDataFile(getName());
		createBundleDirectory();
		createBundleJar();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public static Test suite() {
		return new TestSuite(DiscardBundleTests.class);
	}

	public void testDiscardOsgiCheckConfigurationTrueOsgiDevSpecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.TRUE.toString());
		configuration.put(OSGI_DEV, "");
		doTest(configuration, true);
	}

	public void testDiscardOsgiCheckConfigurationTrueOsgiDevUnspecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.TRUE.toString());
		doTest(configuration, true);
	}

	public void testDiscardOsgiCheckConfigurationUnspecifiedOsgiDevSpecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_DEV, "");
		doTest(configuration, true);
	}

	public void testNoDiscardOsgiCheckConfigurationFalseOsgiDevSpecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.FALSE.toString());
		configuration.put(OSGI_DEV, "");
		doTest(configuration, false);
	}

	public void testNoDiscardOsgiCheckConfigurationFalseOsgiDevUnspecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.FALSE.toString());
		doTest(configuration, false);
	}

	public void testNoDiscardOsgiCheckConfigurationUnspecifiedOsgiDevUnspecified() throws Exception {
		doTest(createConfiguration(), false);
	}

	private void assertDiscarded(String location, Equinox equinox) {
		assertNull("The bundle was not discarded", equinox.getBundleContext().getBundle(location));
	}

	private void assertNotDiscarded(String location, Equinox equinox) {
		assertNotNull("The bundle was discarded", equinox.getBundleContext().getBundle(location));
	}

	private void createBundleDirectory() throws IOException {
		File file = new File(root, BUNDLE_DIR + '/' + BUNDLE_MANIFEST);
		assertTrue("Could not make directories", file.getParentFile().mkdirs());
		Manifest manifest = createBundleManifest();
		FileOutputStream fos = new FileOutputStream(file);
		try {
			manifest.write(fos);
		} finally {
			fos.close();
		}
	}

	private void createBundleJar() throws IOException {
		Manifest manifest = createBundleManifest();
		JarOutputStream target = new JarOutputStream(new FileOutputStream(new File(root, BUNDLE_JAR)), manifest);
		target.close();
	}

	private Manifest createBundleManifest() {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, BUNDLE_DIR);
		return manifest;
	}

	private Map<String, Object> createConfiguration() {
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> result = new HashMap<String, Object>();
		result.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		return result;
	}

	private void doTest(Map<String, ?> configuration, boolean discard) throws Exception {
		doTest(configuration, discard, getDirectoryLocation());
		doTest(configuration, discard, getJarLocation());
	}

	private void doTest(Map<String, ?> configuration, boolean discard, String location) throws Exception {
		Equinox equinox = new Equinox(configuration);
		initAndStart(equinox);
		try {
			equinox.getBundleContext().installBundle(location);
			try {
				equinox = restart(equinox, configuration);
				assertNotDiscarded(location, equinox);
				touchFile(location);
				equinox = restart(equinox, configuration);
				if (discard)
					assertDiscarded(location, equinox);
				else
					assertNotDiscarded(location, equinox);
			} finally {
				try {
					equinox.getBundleContext().getBundle(location).uninstall();
				} catch (Exception e) {
					// Ignore
				}
			}
		} finally {
			stopQuietly(equinox);
		}
	}

	private String getDirectoryLocation() {
		return REFERENCE_PROTOCOL + root.toURI() + BUNDLE_DIR;
	}

	private String getJarLocation() {
		return REFERENCE_PROTOCOL + root.toURI() + BUNDLE_JAR;
	}

	private void initAndStart(Equinox equinox) throws BundleException {
		equinox.init();
		equinox.start();
	}

	private Equinox restart(Equinox equinox, Map<String, ?> configuration) throws BundleException, InterruptedException {
		stop(equinox);
		equinox = new Equinox(configuration);
		initAndStart(equinox);
		return equinox;
	}

	private void stop(Equinox equinox) throws BundleException, InterruptedException {
		equinox.stop();
		FrameworkEvent event = equinox.waitForStop(5000);
		assertEquals("The framework was not stopped", FrameworkEvent.STOPPED, event.getType());
	}

	private void stopQuietly(Equinox equinox) {
		if (equinox == null)
			return;
		try {
			equinox.stop();
			equinox.waitForStop(5000);
		} catch (Exception e) {
			// Ignore
		}
	}

	private void touchFile(String location) {
		File file = new File(location.substring((REFERENCE_PROTOCOL + "file:").length()));
		if (file.isDirectory())
			file = new File(file, BUNDLE_MANIFEST);
		assertTrue("Could not set last modified", file.setLastModified(file.lastModified() + 1000));
	}
}