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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION;
import static org.osgi.framework.Constants.BUNDLE_NATIVECODE;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

public class BundleNativeCodeTests extends AbstractBundleTests {

	@Test
	public void testMultipleBundleNativeHostOnlyOneMatch() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();

		Map<String, String> hostHeaders = new HashMap<>();
		hostHeaders.put(BUNDLE_MANIFESTVERSION, "2");
		hostHeaders.put(BUNDLE_SYMBOLICNAME, getName() + ".host");
		hostHeaders.put(BUNDLE_NATIVECODE,
				"nativeCode1.txt; selection-filter=\"(library.match=1)\","
						+ "nativeCode2.txt; selection-filter=\"(library.match=2)\","
						+ "nativeCode3.txt; selection-filter=\"(library.match=3)\","
						// make native code optional
						+ "*");

		File hostBundleFile = SystemBundleTests.createBundle(config, hostHeaders.get(BUNDLE_SYMBOLICNAME), hostHeaders,
				Collections.singletonMap("nativeCode1.txt", "nativeCode1.txt"),
				Collections.singletonMap("nativeCode2.txt", "nativeCode2.txt"),
				Collections.singletonMap("nativeCode3.txt", "nativeCode3.txt"));

		doTestNativeCode(config, "1", "nativeCode1.txt", hostBundleFile);
		doTestNativeCode(config, "2", "nativeCode2.txt", hostBundleFile);
		doTestNativeCode(config, "3", "nativeCode3.txt", hostBundleFile);
	}

	@Test
	public void testSingleBundleNativeHostOneMatch() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();

		Map<String, String> hostHeaders = new HashMap<>();
		hostHeaders.put(BUNDLE_MANIFESTVERSION, "2");
		hostHeaders.put(BUNDLE_SYMBOLICNAME, getName() + ".host");
		hostHeaders.put(BUNDLE_NATIVECODE, "nativeCode1.txt; selection-filter=\"(library.match=1)\","
				// make native code optional
				+ "*");

		File hostBundleFile = SystemBundleTests.createBundle(config, hostHeaders.get(BUNDLE_SYMBOLICNAME), hostHeaders,
				Collections.singletonMap("nativeCode1.txt", "nativeCode1.txt"));

		doTestNativeCode(config, "1", "nativeCode1.txt", hostBundleFile);
	}

	@Test
	public void testMultipleBundleNativeHostWithFragmentsOneMatch() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();

		Map<String, String> hostHeaders = new HashMap<>();
		hostHeaders.put(BUNDLE_MANIFESTVERSION, "2");
		hostHeaders.put(BUNDLE_SYMBOLICNAME, getName() + ".host");
		hostHeaders.put(BUNDLE_NATIVECODE,
				"nativeCode1.txt; selection-filter=\"(library.match=1)\","
						+ "nativeCode2.txt; selection-filter=\"(library.match=2)\","
						+ "nativeCode3.txt; selection-filter=\"(library.match=3)\","
						// make native code optional
						+ "*");

		File hostBundleFile = SystemBundleTests.createBundle(config, hostHeaders.get(BUNDLE_SYMBOLICNAME), hostHeaders,
				Collections.singletonMap("nativeCode1.txt", "nativeCode1.txt"),
				Collections.singletonMap("nativeCode2.txt", "nativeCode2.txt"),
				Collections.singletonMap("nativeCode3.txt", "nativeCode3.txt"));

		Map<String, String> frag1Headers = new HashMap<>();
		frag1Headers.put(BUNDLE_MANIFESTVERSION, "2");
		frag1Headers.put(BUNDLE_SYMBOLICNAME, getName() + ".frag1");
		frag1Headers.put(FRAGMENT_HOST, getName() + ".host");
		frag1Headers.put(BUNDLE_NATIVECODE,
				"nativeCode4.txt; selection-filter=\"(library.match=4)\","
						+ "nativeCode5.txt; selection-filter=\"(library.match=5)\","
						+ "nativeCode6.txt; selection-filter=\"(library.match=6)\","
						// make native code optional
						+ "*");

		File frag1BundleFile = SystemBundleTests.createBundle(config, frag1Headers.get(BUNDLE_SYMBOLICNAME),
				frag1Headers, Collections.singletonMap("nativeCode4.txt", "nativeCode4.txt"),
				Collections.singletonMap("nativeCode5.txt", "nativeCode5.txt"),
				Collections.singletonMap("nativeCode6.txt", "nativeCode6.txt"));

		doTestNativeCode(config, "1", "nativeCode1.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "2", "nativeCode2.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "3", "nativeCode3.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "4", "nativeCode4.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "5", "nativeCode5.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "6", "nativeCode6.txt", hostBundleFile, frag1BundleFile);
	}

	@Test
	public void testSingleBundleNativeHostWithFragmentsOneMatch() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();

		Map<String, String> hostHeaders = new HashMap<>();
		hostHeaders.put(BUNDLE_MANIFESTVERSION, "2");
		hostHeaders.put(BUNDLE_SYMBOLICNAME, getName() + ".host");
		hostHeaders.put(BUNDLE_NATIVECODE, "nativeCode1.txt; selection-filter=\"(library.match=1)\","
				// make native code optional
				+ "*");

		File hostBundleFile = SystemBundleTests.createBundle(config, hostHeaders.get(BUNDLE_SYMBOLICNAME), hostHeaders,
				Collections.singletonMap("nativeCode1.txt", "nativeCode1.txt"));

		Map<String, String> frag1Headers = new HashMap<>();
		frag1Headers.put(BUNDLE_MANIFESTVERSION, "2");
		frag1Headers.put(BUNDLE_SYMBOLICNAME, getName() + ".frag1");
		frag1Headers.put(FRAGMENT_HOST, getName() + ".host");
		frag1Headers.put(BUNDLE_NATIVECODE, "nativeCode4.txt; selection-filter=\"(library.match=4)\","
				// make native code optional
				+ "*");

		File frag1BundleFile = SystemBundleTests.createBundle(config, frag1Headers.get(BUNDLE_SYMBOLICNAME),
				frag1Headers, Collections.singletonMap("nativeCode4.txt", "nativeCode4.txt"));

		doTestNativeCode(config, "1", "nativeCode1.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "4", "nativeCode4.txt", hostBundleFile, frag1BundleFile);
	}

	@Test
	public void testMultipleBundleNativeHostWithFragmentsMultipleMatch() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();

		Map<String, String> hostHeaders = new HashMap<>();
		hostHeaders.put(BUNDLE_MANIFESTVERSION, "2");
		hostHeaders.put(BUNDLE_SYMBOLICNAME, getName() + ".host");
		hostHeaders.put(BUNDLE_NATIVECODE,
				"nativeCode1.txt; selection-filter=\"(library.match=1)\","
						+ "nativeCode2.txt; selection-filter=\"(library.match=2)\","
						+ "nativeCode3.txt; selection-filter=\"(library.match=3)\","
						// make native code optional
						+ "*");

		File hostBundleFile = SystemBundleTests.createBundle(config, hostHeaders.get(BUNDLE_SYMBOLICNAME), hostHeaders,
				Collections.singletonMap("nativeCode1.txt", "nativeCode1.txt"),
				Collections.singletonMap("nativeCode2.txt", "nativeCode2.txt"),
				Collections.singletonMap("nativeCode3.txt", "nativeCode3.txt"));

		Map<String, String> frag1Headers = new HashMap<>();
		frag1Headers.put(BUNDLE_MANIFESTVERSION, "2");
		frag1Headers.put(BUNDLE_SYMBOLICNAME, getName() + ".frag1");
		frag1Headers.put(FRAGMENT_HOST, getName() + ".host");
		frag1Headers.put(BUNDLE_NATIVECODE,
				"nativeCode4.txt; selection-filter=\"(library.match=1)\","
						+ "nativeCode5.txt; selection-filter=\"(library.match=2)\","
						+ "nativeCode6.txt; selection-filter=\"(library.match=3)\","
						// make native code optional
						+ "*");

		File frag1BundleFile = SystemBundleTests.createBundle(config, frag1Headers.get(BUNDLE_SYMBOLICNAME),
				frag1Headers, Collections.singletonMap("nativeCode4.txt", "nativeCode4.txt"),
				Collections.singletonMap("nativeCode5.txt", "nativeCode5.txt"),
				Collections.singletonMap("nativeCode6.txt", "nativeCode6.txt"));

		doTestNativeCode(config, "1", "nativeCode1.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "2", "nativeCode2.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "3", "nativeCode3.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "1", "nativeCode4.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "2", "nativeCode5.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "3", "nativeCode6.txt", hostBundleFile, frag1BundleFile);
	}

	@Test
	public void testSingleBundleNativeHostWithFragmentsMultipleMatch() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();

		Map<String, String> hostHeaders = new HashMap<>();
		hostHeaders.put(BUNDLE_MANIFESTVERSION, "2");
		hostHeaders.put(BUNDLE_SYMBOLICNAME, getName() + ".host");
		hostHeaders.put(BUNDLE_NATIVECODE, "nativeCode1.txt; selection-filter=\"(library.match=1)\","
				// make native code optional
				+ "*");

		File hostBundleFile = SystemBundleTests.createBundle(config, hostHeaders.get(BUNDLE_SYMBOLICNAME), hostHeaders,
				Collections.singletonMap("nativeCode1.txt", "nativeCode1.txt"));

		Map<String, String> frag1Headers = new HashMap<>();
		frag1Headers.put(BUNDLE_MANIFESTVERSION, "2");
		frag1Headers.put(BUNDLE_SYMBOLICNAME, getName() + ".frag1");
		frag1Headers.put(FRAGMENT_HOST, getName() + ".host");
		frag1Headers.put(BUNDLE_NATIVECODE, "nativeCode4.txt; selection-filter=\"(library.match=1)\","
				// make native code optional
				+ "*");

		File frag1BundleFile = SystemBundleTests.createBundle(config, frag1Headers.get(BUNDLE_SYMBOLICNAME),
				frag1Headers, Collections.singletonMap("nativeCode4.txt", "nativeCode4.txt"));

		doTestNativeCode(config, "1", "nativeCode1.txt", hostBundleFile, frag1BundleFile);
		doTestNativeCode(config, "1", "nativeCode4.txt", hostBundleFile, frag1BundleFile);
	}

	private void doTestNativeCode(File config, String libraryMatchValue, String nativeCodeName, File hostBundleFile,
			File... fragmentBundleFiles)
			throws Exception {
		Map<String, String> fwkConfig = new HashMap<>();
		fwkConfig.put(FRAMEWORK_STORAGE, config.getAbsolutePath());
		fwkConfig.put(FRAMEWORK_STORAGE_CLEAN, FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		fwkConfig.put("library.match", libraryMatchValue);
		Equinox equinox = new Equinox(fwkConfig);
		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			// install fragments first to allow them to attach to host
			for (File fragmentBundleFile : fragmentBundleFiles) {
				systemContext.installBundle(fragmentBundleFile.toURI().toString());
			}
			Bundle host = systemContext.installBundle(hostBundleFile.toURI().toString());
			// start host now to resolve and attach any possible fragments
			host.start();
			ClassLoader cl = host.adapt(BundleWiring.class).getClassLoader();
			Method findLibrary = findDeclaredMethod(cl.getClass(), "findLibrary", String.class);
			findLibrary.setAccessible(true);
			String libraryPath = (String) findLibrary.invoke(cl, nativeCodeName);
			assertNotNull("No library found: " + nativeCodeName, libraryPath);
			assertTrue("Wrong library found: " + libraryPath, libraryPath.endsWith(nativeCodeName));
		} finally {
			stopQuietly(equinox);
		}
	}

	private Method findDeclaredMethod(Class<?> clazz, String method, Class... args) throws NoSuchMethodException {
		do {
			try {
				return clazz.getDeclaredMethod(method, args);
			} catch (NoSuchMethodException e) {
				clazz = clazz.getSuperclass();
			}
		} while (clazz != null);
		throw new NoSuchMethodException(method);
	}
}
