/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
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

	@Override
	public void setUp() throws Exception {
		super.setUp();
		originalSpecVersion = System.getProperty("java.specification.version");
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		System.setProperty("java.specification.version", originalSpecVersion);
	}

	@Test
	public void testExportPackageCannotContainJavaPackages() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		headers.put(Constants.EXPORT_PACKAGE, JAVA_LANG);
		config.mkdirs();
		File bundle = SystemBundleTests.createBundle(config, getName(), headers);
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));
		try {
			BundleException e = assertThrows(BundleException.class, () -> {
				equinox.start();
				BundleContext systemContext = equinox.getBundleContext();
				Bundle testBundle = systemContext.installBundle(bundle.toURI().toString());
				testBundle.start();
			});
			assertEquals("It should throw a bundle exception of type manifest error", BundleException.MANIFEST_ERROR, e.getType());
			assertTrue("It should throw a Bundle Exception stating Invalid manifest header Export-Package", e.getMessage().contains("Cannot specify java.* packages in Export headers"));
		} finally {
			stopQuietly(equinox);
		}

	}

	@Test
	public void testImportPackageCanContainJavaPackages() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, String> headers = new HashMap<>();
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
		} finally {
			stopQuietly(equinox);
		}
	}

	@Test
	public void testSystemPackages() throws Exception {
		Map<Integer, Integer> packagesPerVersion = new HashMap<>();
		packagesPerVersion.put(8, 63);
		if (!originalSpecVersion.startsWith("1.")) {
			packagesPerVersion.put(9, calculateJavaPackageCount());
		} else {
			packagesPerVersion.put(9, 66);
		}

		for (Entry<Integer, Integer> entry : packagesPerVersion.entrySet()) {
			doSystemPackages(entry.getKey(), entry.getValue());
		}

	}

	private int calculateJavaPackageCount() throws Exception {
		int javaPackages = 0;
		Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer"); //$NON-NLS-1$
		Method boot = moduleLayerClass.getMethod("boot"); //$NON-NLS-1$
		Method modules = moduleLayerClass.getMethod("modules"); //$NON-NLS-1$
		Class<?> moduleClass = Class.forName("java.lang.Module"); //$NON-NLS-1$
		Method getDescriptor = moduleClass.getMethod("getDescriptor"); //$NON-NLS-1$
		Class<?> moduleDescriptorClass = Class.forName("java.lang.module.ModuleDescriptor"); //$NON-NLS-1$
		Method exports = moduleDescriptorClass.getMethod("exports"); //$NON-NLS-1$
		Method isAutomatic = moduleDescriptorClass.getMethod("isAutomatic"); //$NON-NLS-1$
		Method packagesMethod = moduleDescriptorClass.getMethod("packages"); //$NON-NLS-1$
		Class<?> exportsClass = Class.forName("java.lang.module.ModuleDescriptor$Exports"); //$NON-NLS-1$
		Method isQualified = exportsClass.getMethod("isQualified"); //$NON-NLS-1$
		Method source = exportsClass.getMethod("source"); //$NON-NLS-1$

		Object bootLayer = boot.invoke(null);
		Set<?> bootModules = (Set<?>) modules.invoke(bootLayer);
		for (Object m : bootModules) {
			Object descriptor = getDescriptor.invoke(m);
			if ((Boolean) isAutomatic.invoke(descriptor)) {
				/*
				 * Automatic modules are supposed to export all their packages.
				 * However, java.lang.module.ModuleDescriptor::exports returns an empty set for them.
				 * Add all their packages (as returned by java.lang.module.ModuleDescriptor::packages)
				 * to the list of VM supplied packages.
				 */
				for (String packageName : ((Set<String>) packagesMethod.invoke(descriptor))) {
					if (packageName.startsWith("java.")) {
						javaPackages++;
					}
				}
			} else {
				for (Object export : (Set<?>) exports.invoke(descriptor)) {
					String pkg = (String) source.invoke(export);
					if (!((Boolean) isQualified.invoke(export)) && pkg.startsWith("java.")) {
						javaPackages++;
					}
				}
			}
		}
		return javaPackages;
	}

	public void doSystemPackages(int rv, int expectedPackages) throws Exception {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		config.mkdirs();
		File bundle = SystemBundleTests.createBundle(config, getName(), headers);

		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));
		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Dictionary<String, String> testHeaders = equinox.getHeaders();
			assertTrue(Constants.EXPORT_PACKAGE + " does not contain the java.lang package", testHeaders.get(Constants.EXPORT_PACKAGE).contains(JAVA_LANG));
			assertTrue(Constants.EXPORT_PACKAGE + " does not contain the java.util package", testHeaders.get(Constants.EXPORT_PACKAGE).contains(JAVA_UTIL));
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
			assertTrue("System packages should include java.lang packages", systemPackages.contains(JAVA_LANG));
			assertTrue("System packages should include java.util packages", systemPackages.contains(JAVA_UTIL));
		} finally {
			stopQuietly(equinox);
		}
	}

}
