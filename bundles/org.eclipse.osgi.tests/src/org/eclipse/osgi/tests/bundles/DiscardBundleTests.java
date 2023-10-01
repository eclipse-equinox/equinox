/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import static org.eclipse.osgi.tests.bundles.SystemBundleTests.createBundle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

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

	@Override
	public void setUp() throws Exception {
		super.setUp();
		root = OSGiTestsActivator.getContext().getDataFile(getName());
		root.mkdirs();
		createBundleDirectory();
		createBundleJar();
	}

	@Test
	public void testDiscardOsgiCheckConfigurationTrueOsgiDevSpecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.TRUE.toString());
		configuration.put(OSGI_DEV, "");
		doTest(configuration, true);
	}

	@Test
	public void testDiscardOsgiCheckConfigurationTrueOsgiDevUnspecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.TRUE.toString());
		doTest(configuration, true);
	}

	@Test
	public void testDiscardOsgiCheckConfigurationUnspecifiedOsgiDevSpecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_DEV, "");
		doTest(configuration, true);
	}

	@Test
	public void testNoDiscardOsgiCheckConfigurationFalseOsgiDevSpecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.FALSE.toString());
		configuration.put(OSGI_DEV, "");
		doTest(configuration, false);
	}

	@Test
	public void testNoDiscardOsgiCheckConfigurationFalseOsgiDevUnspecified() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.FALSE.toString());
		doTest(configuration, false);
	}

	@Test
	public void testNoDiscardOsgiCheckConfigurationUnspecifiedOsgiDevUnspecified() throws Exception {
		doTest(createConfiguration(), false);
	}

	@Test
	public void testDiscardDeletedBundleFile() throws Exception {
		doTestDiscardDeletedBundleFile(getDirectoryLocation());
		doTestDiscardDeletedBundleFile(getJarLocation());
	}

	@Test
	public void testLimitResolutionAfterDiscard() throws Exception {
		Map<String, Object> configuration = createConfiguration();
		configuration.put(OSGI_CHECKCONFIGURATION, Boolean.TRUE.toString());
		Equinox equinox = new Equinox(configuration);
		initAndStart(equinox);
		try {
			// Install non extension bundles.
			// If a discarded bundle causes on of these bundles to refresh then
			// it should not be allowed to resolve.
			File discard = createBundle(root, "discard", Collections.singletonMap("Export-Package", "discard"));
			String discardLocation = REFERENCE_PROTOCOL + discard.toURI();

			// Optionally import the package from a discarded bundle
			// This would would allow the bundle to resolve, but the framework is not yet
			// initialized
			// so it should be prevented from resolving when discarding what it imports.
			File importDiscard = createBundle(root, "importDiscard",
					Collections.singletonMap(Constants.IMPORT_PACKAGE, "discard; resolution:=optional"));
			String importDiscardLocation = REFERENCE_PROTOCOL + importDiscard.toURI();

			// Install extension bundles, these bundles should resolve even
			// if what they require gets discarded.
			Hashtable<String, String> extensionHeaders = new Hashtable<>();

			extensionHeaders.put(Constants.FRAGMENT_HOST, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
			extensionHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
			extensionHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "discardExtension");
			extensionHeaders.put(Constants.EXPORT_PACKAGE, "discard.extension");
			File discardExtension = createBundle(root, "discardExtension", extensionHeaders);
			String discardExtensionLocation = REFERENCE_PROTOCOL + discardExtension.toURI();

			extensionHeaders.remove(Constants.EXPORT_PACKAGE);
			extensionHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "importDiscardExtension");
			extensionHeaders.put(Constants.IMPORT_PACKAGE, "discard.extesion; resolution:=optional");
			File importDiscardExtension = createBundle(root, "importDiscardExtension", extensionHeaders);
			String importDiscardExtensionLocation = REFERENCE_PROTOCOL + importDiscardExtension.toURI();

			equinox.getBundleContext().installBundle(discardLocation).start();
			equinox.getBundleContext().installBundle(importDiscardLocation).start();
			equinox.getBundleContext().installBundle(discardExtensionLocation);
			equinox.getBundleContext().installBundle(importDiscardExtensionLocation);
			assertEquals("Wrong number of bundles.", 5, equinox.getBundleContext().getBundles().length);

			stop(equinox);
			touchFile(discard);
			touchFile(discardExtension);

			equinox = new Equinox(configuration);
			equinox.init();

			assertDiscarded(discardLocation, equinox);
			assertEquals("importDiscard should be in the installed state", Bundle.INSTALLED,
					equinox.getBundleContext().getBundle(importDiscardLocation).getState());
			assertEquals("importDiscardExtension should be in the resolved state", Bundle.RESOLVED,
					equinox.getBundleContext().getBundle(importDiscardExtensionLocation).getState());
			assertEquals("Wrong number of bundles.", 3, equinox.getBundleContext().getBundles().length);
		} finally {
			stopQuietly(equinox);
		}
	}

	private void doTestDiscardDeletedBundleFile(File bundleFile) throws Exception {
		Map<String, Object> configuration = createConfiguration();
		Equinox equinox = new Equinox(configuration);
		initAndStart(equinox);
		try {
			String location = REFERENCE_PROTOCOL + bundleFile.toURI();
			equinox.getBundleContext().installBundle(location);
			equinox = restart(equinox, configuration);
			assertNotDiscarded(location, equinox);
			// Attempting to delete the file with equinox still running
			// will sometimes result in failure presumably due to a locked
			// file.
			stop(equinox);
			delete(bundleFile);
			equinox = restart(equinox, configuration);
			assertDiscarded(location, equinox);
		} finally {
			stopQuietly(equinox);
		}
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
		try (FileOutputStream fos = new FileOutputStream(file)) {
			manifest.write(fos);
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

	private void doTest(Map<String, ?> configuration, boolean discard) throws Exception {
		doTest(configuration, discard, getDirectoryLocation());
		doTest(configuration, discard, getJarLocation());
	}

	private void doTest(Map<String, ?> configuration, boolean discard, File bundleFile) throws Exception {
		Equinox equinox = new Equinox(configuration);
		initAndStart(equinox);
		try {
			String location = REFERENCE_PROTOCOL + bundleFile.toURI();
			equinox.getBundleContext().installBundle(location);
			try {
				equinox = restart(equinox, configuration);
				assertNotDiscarded(location, equinox);
				// Attempting to touch the file with equinox still running
				// will sometimes result in failure presumably due to a locked
				// file.
				stop(equinox);
				touchFile(bundleFile);
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

	private File getDirectoryLocation() {
		return new File(root, BUNDLE_DIR);
	}

	private File getJarLocation() {
		return new File(root, BUNDLE_JAR);
	}

	private Equinox restart(Equinox equinox, Map<String, ?> configuration) throws BundleException {
		stop(equinox);
		equinox = new Equinox(configuration);
		initAndStart(equinox);
		return equinox;
	}

	private void touchFile(File file) {
		if (file.isDirectory())
			file = new File(file, BUNDLE_MANIFEST);
		assertTrue("Could not set last modified: " + file, file.setLastModified(file.lastModified() + 1000));
	}
}
