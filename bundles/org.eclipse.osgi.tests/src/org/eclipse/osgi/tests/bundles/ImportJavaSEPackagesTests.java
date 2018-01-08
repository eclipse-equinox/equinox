/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.io.File;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class ImportJavaSEPackagesTests extends AbstractBundleTests {

	private static final String JAVA_LANG = "java.lang";
	private static final String JAVA_UTIL = "java.util";
	private static String originalSpecVersion;

	public static Test suite() {
		return new TestSuite(ImportJavaSEPackagesTests.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		originalSpecVersion = System.getProperty("java.specification.version");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		System.setProperty("java.specification.version", originalSpecVersion);
	}

	public void testExportPackageCannotContainJavaPackages() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		headers.put(Constants.EXPORT_PACKAGE, JAVA_LANG);
		config.mkdirs();
		File bundle = SystemBundleTests.createBundle(config, getName(), headers);
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));
		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle testBundle = systemContext.installBundle(bundle.toURI().toString());
			testBundle.start();
			fail("Failed to test Export-Package header");
		} catch (BundleException e) {
			assertEquals("It should throw a bundle exception of type manifest error", BundleException.MANIFEST_ERROR, e.getType());
			assertTrue("It should throw a Bundle Exception stating Invalid manifest header Export-Package", e.getMessage().contains("Cannot specify java.* packages in Export headers"));
		} finally {

			try {
				equinox.stop();
				equinox.waitForStop(10000);
			} catch (Exception e) {
				//do nothing
			}
		}

	}

	public void testImportPackageCanContainJavaPackages() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		headers.put(Constants.IMPORT_PACKAGE, JAVA_LANG);
		config.mkdirs();
		File bundle = SystemBundleTests.createBundle(config, getName(), headers);
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));
		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle testBundle = systemContext.installBundle(bundle.toURI().toString());
			testBundle.start();
			Dictionary<String, String> testHeaders = testBundle.getHeaders();
			assertTrue(Constants.IMPORT_PACKAGE + " does not contain the java.* package", testHeaders.get(Constants.IMPORT_PACKAGE).contains(JAVA_LANG));
			List<BundleWire> pkgWires = testBundle.adapt(BundleWiring.class).getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
			assertEquals("Wrong number of package requiremens: ", 1, pkgWires.size());
			assertEquals("Wrong package found: " + pkgWires.get(0), JAVA_LANG, pkgWires.get(0).getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		} catch (BundleException e) {
			fail("Failed to test Import-Package header");
		} finally {

			try {
				equinox.stop();
				equinox.waitForStop(10000);
			} catch (Exception e) {
				//do nothing
			}
		}
	}

	public void testSystemPackages() throws Exception {
		Map<Integer, Integer> packagesPerVersion = new HashMap<Integer, Integer>();
		packagesPerVersion.put(7, 56);
		packagesPerVersion.put(8, 63);
		packagesPerVersion.put(9, 66);

		for (Entry<Integer, Integer> entry : packagesPerVersion.entrySet()) {
			doSystemPackages(entry.getKey(), entry.getValue());
		}

	}

	public void doSystemPackages(int rv, int expectedPackages) throws Exception {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		config.mkdirs();
		File bundle = SystemBundleTests.createBundle(config, getName(), headers);

		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));
		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Dictionary<String, String> testHeaders = equinox.getHeaders();
			assertTrue(Constants.EXPORT_PACKAGE + " does not contain the java.* package", testHeaders.get(Constants.EXPORT_PACKAGE).contains(JAVA_LANG));
			assertTrue(Constants.EXPORT_PACKAGE + " does not contain the java.* package", testHeaders.get(Constants.EXPORT_PACKAGE).contains(JAVA_UTIL));
			List<BundleCapability> capabilities = equinox.adapt(BundleWiring.class).getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);

			int count = 0;
			for (BundleCapability cap : capabilities) {
				if (cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).toString().startsWith("java.")) {
					count++;
				}
			}
			assertEquals("Wrong number of java.* packages for version " + rv, expectedPackages, count);

			Bundle testBundle = systemContext.installBundle(bundle.toURI().toString());
			testBundle.start();
			String systemPackages = testBundle.getBundleContext().getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
			assertTrue("System packages should include java.* packages", systemPackages.contains(JAVA_LANG));
			assertTrue("System packages should include java.* packages", systemPackages.contains(JAVA_UTIL));
		} catch (BundleException e) {
			fail("Failed to test System packages");
		} finally {

			try {
				equinox.stop();
				equinox.waitForStop(10000);
			} catch (Exception e) {
				//do nothing
			}
		}
	}

}
