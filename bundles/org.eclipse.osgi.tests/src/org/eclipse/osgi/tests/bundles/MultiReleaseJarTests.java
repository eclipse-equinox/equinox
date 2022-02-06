/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertThrows;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.storage.StorageUtil;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Namespace;

public class MultiReleaseJarTests extends AbstractBundleTests {

	private final static String RNF = "RNF";
	private final static String CNFE = "CNFE";

	private File mrJarBundle;
	private String originalSpecVersion;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mrJarBundle = createMRJarBundle();
		originalSpecVersion = System.getProperty("java.specification.version");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		System.setProperty("java.specification.version", originalSpecVersion);
	}

	private static File createMRJarBundle() throws BundleException, IOException {
		BundleContext bc = OSGiTestsActivator.getContext();
		File mrJarBundle = bc.getDataFile("mrJarBundleTest.jar");
		if (mrJarBundle.exists()) {
			return mrJarBundle;
		}
		File classpathMrJar = bc.getDataFile("classpathMrJar.jar");

		Bundle base = installer.installBundle("mrBundleInputBase");

		Map<String, String> bundleHeaders = new LinkedHashMap<>();
		bundleHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		bundleHeaders.put(Constants.BUNDLE_SYMBOLICNAME, "mrBundle");
		bundleHeaders.put(Constants.BUNDLE_VERSION, "1.0.0");
		bundleHeaders.put(Constants.IMPORT_PACKAGE, "pkgbase");
		bundleHeaders.put(Constants.REQUIRE_CAPABILITY, "capbase");
		bundleHeaders.put(Constants.EXPORT_PACKAGE, "pkgbase, pkg8, pkg9, pkg10, pkg11");
		bundleHeaders.put(Constants.PROVIDE_CAPABILITY, "capbase, cap8, cap9, cap10, cap11");
		bundleHeaders.put(Constants.BUNDLE_CLASSPATH, "., " + classpathMrJar.getName() + ", classPathDir");

		Map<String, byte[]> bundleEntries = new LinkedHashMap<>();
		bundleEntries.put("multi/", null);
		bundleEntries.put("multi/release/", null);
		bundleEntries.put("multi/release/test/", null);
		bundleEntries.put("multi/release/test/TestClassBase.class", getBytes("multi/release/test/TestClassBase.class", base));
		bundleEntries.put("multi/release/test/TestClass8.class", getBytes("multi/release/test/TestClass8.class", base));
		bundleEntries.put("multi/release/test/TestClass9.class", getBytes("multi/release/test/TestClass9.class", base));
		bundleEntries.put("multi/release/test/TestClass10.class", getBytes("multi/release/test/TestClass10.class", base));
		bundleEntries.put("multi/release/test/TestClass11.class", getBytes("multi/release/test/TestClass11.class", base));
		bundleEntries.put("multi/release/test/TestService.class", getBytes("multi/release/test/TestService.class", base));
		bundleEntries.put("multi/release/test/TestService9.class", getBytes("multi/release/test/TestService9.class", base));
		bundleEntries.put("multi/release/test/TestServiceBase.class", getBytes("multi/release/test/TestServiceBase.class", base));

		bundleEntries.put("multi/release/test/testResourceBase.txt", getBytes("multi/release/test/testResourceBase.txt", base));
		bundleEntries.put("multi/release/test/testResource8.txt", getBytes("multi/release/test/testResource8.txt", base));
		bundleEntries.put("multi/release/test/testResource9.txt", getBytes("multi/release/test/testResource9.txt", base));
		bundleEntries.put("multi/release/test/testResource10.txt", getBytes("multi/release/test/testResource10.txt", base));
		bundleEntries.put("multi/release/test/testResource11.txt", getBytes("multi/release/test/testResource11.txt", base));

		bundleEntries.put("META-INF/services/", null);
		bundleEntries.put("META-INF/services/multi.release.test.TestService", "multi.release.test.TestServiceBase".getBytes(StandardCharsets.UTF_8));
		bundleEntries.put("META-INF/versions/", null);
		bundleEntries.put("META-INF/versions/8/", null);
		bundleEntries.put("META-INF/versions/8/multi/", null);
		bundleEntries.put("META-INF/versions/8/multi/release/", null);
		bundleEntries.put("META-INF/versions/8/multi/release/test/", null);
		bundleEntries.put("META-INF/versions/8/multi/release/test/TestClass8.class", getBytes("multi/release/test/TestClass8.class", base, new byte[] {'0', '8'}));
		bundleEntries.put("META-INF/versions/8/multi/release/test/TestClassAdd8.class", getBytes("multi/release/test/TestClassAdd8.class", base));
		bundleEntries.put("META-INF/versions/8/multi/release/test/testResource8.txt", getBytes("multi/release/test/testResource8.txt", base, new byte[] {'0', '8'}));
		bundleEntries.put("META-INF/versions/8/multi/release/test/testResourceAdd8.txt", getBytes("multi/release/test/testResourceAdd8.txt", base));
		bundleEntries.put("META-INF/versions/9/", null);
		bundleEntries.put("META-INF/versions/9/META-INF/", null);
		bundleEntries.put("META-INF/versions/9/META-INF/addedFor9.txt", "added for 9".getBytes(StandardCharsets.UTF_8));
		bundleEntries.put("META-INF/versions/9/META-INF/addedDirFor9/", null);
		bundleEntries.put("META-INF/versions/9/META-INF/addedDirFor9/addedFor9.txt", "added for 9".getBytes(StandardCharsets.UTF_8));
		bundleEntries.put("META-INF/versions/9/META-INF/services/", null);
		bundleEntries.put("META-INF/versions/9/META-INF/services/multi.release.test.TestService", "multi.release.test.TestService9".getBytes(StandardCharsets.UTF_8));
		bundleEntries.put("META-INF/versions/9/multi/", null);
		bundleEntries.put("META-INF/versions/9/multi/release/", null);
		bundleEntries.put("META-INF/versions/9/multi/release/test/", null);
		bundleEntries.put("META-INF/versions/9/multi/release/test/TestClass9.class", getBytes("multi/release/test/TestClass9.class", base, new byte[] {'0', '9'}));
		bundleEntries.put("META-INF/versions/9/multi/release/test/TestClassAdd9.class", getBytes("multi/release/test/TestClassAdd9.class", base));
		bundleEntries.put("META-INF/versions/9/multi/release/test/testResource9.txt", getBytes("multi/release/test/testResource9.txt", base, new byte[] {'0', '9'}));
		bundleEntries.put("META-INF/versions/9/multi/release/test/testResourceAdd9.txt", getBytes("multi/release/test/testResourceAdd9.txt", base));
		bundleEntries.put("META-INF/versions/10/", null);
		bundleEntries.put("META-INF/versions/10/multi/", null);
		bundleEntries.put("META-INF/versions/10/multi/release/", null);
		bundleEntries.put("META-INF/versions/10/multi/release/test/", null);
		bundleEntries.put("META-INF/versions/10/multi/release/test/TestClass10.class", getBytes("multi/release/test/TestClass10.class", base, new byte[] {'1', '0'}));
		bundleEntries.put("META-INF/versions/10/multi/release/test/TestClassAdd10.class", getBytes("multi/release/test/TestClassAdd10.class", base));
		bundleEntries.put("META-INF/versions/10/multi/release/test/testResource10.txt", getBytes("multi/release/test/testResource10.txt", base, new byte[] {'1', '0'}));
		bundleEntries.put("META-INF/versions/10/multi/release/test/testResourceAdd10.txt", getBytes("multi/release/test/testResourceAdd10.txt", base));
		bundleEntries.put("META-INF/versions/11/", null);
		bundleEntries.put("META-INF/versions/11/multi/", null);
		bundleEntries.put("META-INF/versions/11/multi/release/", null);
		bundleEntries.put("META-INF/versions/11/multi/release/test/", null);
		bundleEntries.put("META-INF/versions/11/multi/release/test/TestClass11.class", getBytes("multi/release/test/TestClass11.class", base, new byte[] {'1', '1'}));
		bundleEntries.put("META-INF/versions/11/multi/release/test/TestClassAdd11.class", getBytes("multi/release/test/TestClassAdd11.class", base));
		bundleEntries.put("META-INF/versions/11/multi/release/test/testResource11.txt", getBytes("multi/release/test/testResource11.txt", base, new byte[] {'1', '1'}));
		bundleEntries.put("META-INF/versions/11/multi/release/test/testResourceAdd11.txt", getBytes("multi/release/test/testResourceAdd11.txt", base));

		bundleEntries.put("META-INF/versions/8/OSGI-INF/", null);
		bundleEntries.put("META-INF/versions/8/OSGI-INF/MANIFEST.MF", getBytes("manifests/manifest8.mf", base));
		bundleEntries.put("META-INF/versions/9/OSGI-INF/", null);
		bundleEntries.put("META-INF/versions/9/OSGI-INF/MANIFEST.MF", getBytes("manifests/manifest9.mf", base));
		bundleEntries.put("META-INF/versions/10/OSGI-INF/", null);
		bundleEntries.put("META-INF/versions/10/OSGI-INF/MANIFEST.MF", getBytes("manifests/manifest10.mf", base));
		bundleEntries.put("META-INF/versions/11/OSGI-INF/", null);
		bundleEntries.put("META-INF/versions/11/OSGI-INF/MANIFEST.MF", getBytes("manifests/manifest11.mf", base));

		Map<String, byte[]> classPathJarEntries = new LinkedHashMap<>();
		classPathJarEntries.put("multi/", null);
		classPathJarEntries.put("multi/release/", null);
		classPathJarEntries.put("multi/release/test/", null);
		classPathJarEntries.put("multi/release/test/sub/", null);
		classPathJarEntries.put("multi/release/test/sub/TestClassBase.class", getBytes("multi/release/test/sub/TestClassBase.class", base));
		classPathJarEntries.put("multi/release/test/sub/TestClass8.class", getBytes("multi/release/test/sub/TestClass8.class", base));
		classPathJarEntries.put("multi/release/test/sub/TestClass9.class", getBytes("multi/release/test/sub/TestClass9.class", base));
		classPathJarEntries.put("multi/release/test/sub/TestClass10.class", getBytes("multi/release/test/sub/TestClass10.class", base));
		classPathJarEntries.put("multi/release/test/sub/TestClass11.class", getBytes("multi/release/test/sub/TestClass11.class", base));
		classPathJarEntries.put("multi/release/test/sub/testResourceBase.txt", getBytes("multi/release/test/sub/testResourceBase.txt", base));
		classPathJarEntries.put("multi/release/test/sub/testResource8.txt", getBytes("multi/release/test/sub/testResource8.txt", base));
		classPathJarEntries.put("multi/release/test/sub/testResource9.txt", getBytes("multi/release/test/sub/testResource9.txt", base));
		classPathJarEntries.put("multi/release/test/sub/testResource10.txt", getBytes("multi/release/test/sub/testResource10.txt", base));
		classPathJarEntries.put("multi/release/test/sub/testResource11.txt", getBytes("multi/release/test/sub/testResource11.txt", base));

		classPathJarEntries.put("META-INF/versions/", null);
		classPathJarEntries.put("META-INF/versions/8/", null);
		classPathJarEntries.put("META-INF/versions/8/multi/", null);
		classPathJarEntries.put("META-INF/versions/8/multi/release/", null);
		classPathJarEntries.put("META-INF/versions/8/multi/release/test/", null);
		classPathJarEntries.put("META-INF/versions/8/multi/release/test/sub/", null);
		classPathJarEntries.put("META-INF/versions/8/multi/release/test/sub/TestClass8.class", getBytes("multi/release/test/sub/TestClass8.class", base, new byte[] {'0', '8'}));
		classPathJarEntries.put("META-INF/versions/8/multi/release/test/sub/TestClassAdd8.class", getBytes("multi/release/test/sub/TestClassAdd8.class", base));
		classPathJarEntries.put("META-INF/versions/8/multi/release/test/sub/testResource8.txt", getBytes("multi/release/test/sub/testResource8.txt", base, new byte[] {'0', '8'}));
		classPathJarEntries.put("META-INF/versions/8/multi/release/test/sub/testResourceAdd8.txt", getBytes("multi/release/test/sub/testResourceAdd8.txt", base));

		classPathJarEntries.put("META-INF/versions/9/", null);
		classPathJarEntries.put("META-INF/versions/9/multi/", null);
		classPathJarEntries.put("META-INF/versions/9/multi/release/", null);
		classPathJarEntries.put("META-INF/versions/9/multi/release/test/", null);
		classPathJarEntries.put("META-INF/versions/9/multi/release/test/sub/", null);
		classPathJarEntries.put("META-INF/versions/9/multi/release/test/sub/TestClass9.class", getBytes("multi/release/test/sub/TestClass9.class", base, new byte[] {'0', '9'}));
		classPathJarEntries.put("META-INF/versions/9/multi/release/test/sub/TestClassAdd9.class", getBytes("multi/release/test/sub/TestClassAdd9.class", base));
		classPathJarEntries.put("META-INF/versions/9/multi/release/test/sub/testResource9.txt", getBytes("multi/release/test/sub/testResource9.txt", base, new byte[] {'0', '9'}));
		classPathJarEntries.put("META-INF/versions/9/multi/release/test/sub/testResourceAdd9.txt", getBytes("multi/release/test/sub/testResourceAdd9.txt", base));

		classPathJarEntries.put("META-INF/versions/10/", null);
		classPathJarEntries.put("META-INF/versions/10/multi/", null);
		classPathJarEntries.put("META-INF/versions/10/multi/release/", null);
		classPathJarEntries.put("META-INF/versions/10/multi/release/test/", null);
		classPathJarEntries.put("META-INF/versions/10/multi/release/test/sub/", null);
		classPathJarEntries.put("META-INF/versions/10/multi/release/test/sub/TestClass10.class", getBytes("multi/release/test/sub/TestClass10.class", base, new byte[] {'1', '0'}));
		classPathJarEntries.put("META-INF/versions/10/multi/release/test/sub/TestClassAdd10.class", getBytes("multi/release/test/sub/TestClassAdd10.class", base));
		classPathJarEntries.put("META-INF/versions/10/multi/release/test/sub/testResource10.txt", getBytes("multi/release/test/sub/testResource10.txt", base, new byte[] {'1', '0'}));
		classPathJarEntries.put("META-INF/versions/10/multi/release/test/sub/testResourceAdd10.txt", getBytes("multi/release/test/sub/testResourceAdd10.txt", base));

		classPathJarEntries.put("META-INF/versions/11/", null);
		classPathJarEntries.put("META-INF/versions/11/multi/", null);
		classPathJarEntries.put("META-INF/versions/11/multi/release/", null);
		classPathJarEntries.put("META-INF/versions/11/multi/release/test/", null);
		classPathJarEntries.put("META-INF/versions/11/multi/release/test/sub/", null);
		classPathJarEntries.put("META-INF/versions/11/multi/release/test/sub/TestClass11.class", getBytes("multi/release/test/sub/TestClass11.class", base, new byte[] {'1', '1'}));
		classPathJarEntries.put("META-INF/versions/11/multi/release/test/sub/TestClassAdd11.class", getBytes("multi/release/test/sub/TestClassAdd11.class", base));
		classPathJarEntries.put("META-INF/versions/11/multi/release/test/sub/testResource11.txt", getBytes("multi/release/test/sub/testResource11.txt", base, new byte[] {'1', '1'}));
		classPathJarEntries.put("META-INF/versions/11/multi/release/test/sub/testResourceAdd11.txt", getBytes("multi/release/test/sub/testResourceAdd11.txt", base));

		createMRJar(classpathMrJar, Collections.emptyMap(), classPathJarEntries);
		bundleEntries.put(classpathMrJar.getName(), StorageUtil.getBytes(new FileInputStream(classpathMrJar), -1, 4000));

		// This will not be required by the spec, but equinox does support exploded inner jars in a bundle
		bundleEntries.put("classPathDir/", null);
		bundleEntries.put("classPathDir/multi/", null);
		bundleEntries.put("classPathDir/multi/release/", null);
		bundleEntries.put("classPathDir/multi/release/test/", null);
		bundleEntries.put("classPathDir/multi/release/test/sub2/", null);
		bundleEntries.put("classPathDir/multi/release/test/sub2/TestClassBase.class", getBytes("multi/release/test/sub2/TestClassBase.class", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/TestClass8.class", getBytes("multi/release/test/sub2/TestClass8.class", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/TestClass9.class", getBytes("multi/release/test/sub2/TestClass9.class", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/TestClass10.class", getBytes("multi/release/test/sub2/TestClass10.class", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/TestClass11.class", getBytes("multi/release/test/sub2/TestClass11.class", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/testResourceBase.txt", getBytes("multi/release/test/sub2/testResourceBase.txt", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/testResource8.txt", getBytes("multi/release/test/sub2/testResource8.txt", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/testResource9.txt", getBytes("multi/release/test/sub2/testResource9.txt", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/testResource10.txt", getBytes("multi/release/test/sub2/testResource10.txt", base));
		bundleEntries.put("classPathDir/multi/release/test/sub2/testResource11.txt", getBytes("multi/release/test/sub2/testResource11.txt", base));

		String classPathDirManifest = //
				"Manifest-Version: 1\n" + //
						"Multi-Release: true\n\n";
		bundleEntries.put("classPathDir/META-INF/", null);
		bundleEntries.put("classPathDir/META-INF/MANIFEST.MF", classPathDirManifest.getBytes(StandardCharsets.UTF_8));
		bundleEntries.put("classPathDir/META-INF/versions/", null);
		bundleEntries.put("classPathDir/META-INF/versions/8/", null);
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/", null);
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/release/", null);
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/release/test/", null);
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/release/test/sub2/", null);
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/release/test/sub2/TestClass8.class", getBytes("multi/release/test/sub2/TestClass8.class", base, new byte[] {'0', '8'}));
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/release/test/sub2/TestClassAdd8.class", getBytes("multi/release/test/sub2/TestClassAdd8.class", base));
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/release/test/sub2/testResource8.txt", getBytes("multi/release/test/sub2/testResource8.txt", base, new byte[] {'0', '8'}));
		bundleEntries.put("classPathDir/META-INF/versions/8/multi/release/test/sub2/testResourceAdd8.txt", getBytes("multi/release/test/sub2/testResourceAdd8.txt", base));

		bundleEntries.put("classPathDir/META-INF/versions/9/", null);
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/", null);
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/release/", null);
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/release/test/", null);
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/release/test/sub2/", null);
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/release/test/sub2/TestClass9.class", getBytes("multi/release/test/sub2/TestClass9.class", base, new byte[] {'0', '9'}));
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/release/test/sub2/TestClassAdd9.class", getBytes("multi/release/test/sub2/TestClassAdd9.class", base));
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/release/test/sub2/testResource9.txt", getBytes("multi/release/test/sub2/testResource9.txt", base, new byte[] {'0', '9'}));
		bundleEntries.put("classPathDir/META-INF/versions/9/multi/release/test/sub2/testResourceAdd9.txt", getBytes("multi/release/test/sub2/testResourceAdd9.txt", base));

		bundleEntries.put("classPathDir/META-INF/versions/10/", null);
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/", null);
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/release/", null);
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/release/test/", null);
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/release/test/sub2/", null);
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/release/test/sub2/TestClass10.class", getBytes("multi/release/test/sub2/TestClass10.class", base, new byte[] {'1', '0'}));
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/release/test/sub2/TestClassAdd10.class", getBytes("multi/release/test/sub2/TestClassAdd10.class", base));
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/release/test/sub2/testResource10.txt", getBytes("multi/release/test/sub2/testResource10.txt", base, new byte[] {'1', '0'}));
		bundleEntries.put("classPathDir/META-INF/versions/10/multi/release/test/sub2/testResourceAdd10.txt", getBytes("multi/release/test/sub2/testResourceAdd10.txt", base));

		bundleEntries.put("classPathDir/META-INF/versions/11/", null);
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/", null);
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/release/", null);
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/release/test/", null);
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/release/test/sub2/", null);
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/release/test/sub2/TestClass11.class", getBytes("multi/release/test/sub2/TestClass11.class", base, new byte[] {'1', '1'}));
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/release/test/sub2/TestClassAdd11.class", getBytes("multi/release/test/sub2/TestClassAdd11.class", base));
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/release/test/sub2/testResource11.txt", getBytes("multi/release/test/sub2/testResource11.txt", base, new byte[] {'1', '1'}));
		bundleEntries.put("classPathDir/META-INF/versions/11/multi/release/test/sub2/testResourceAdd11.txt", getBytes("multi/release/test/sub2/testResourceAdd11.txt", base));

		createMRJar(mrJarBundle, bundleHeaders, bundleEntries);
		return mrJarBundle;
	}

	static void createMRJar(File file, Map<String, String> headers, Map<String, byte[]> entries) throws IOException {
		Manifest m = new Manifest();
		Attributes attributes = m.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		attributes.putValue("Multi-Release", "true");
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			attributes.putValue(entry.getKey(), entry.getValue());
		}
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), m);
		if (entries != null) {
			for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
				jos.putNextEntry(new JarEntry(entry.getKey()));
				if (entry.getValue() != null) {
					jos.write(entry.getValue());
				}
				jos.closeEntry();
			}
		}
		jos.flush();
		jos.close();
	}

	private static byte[] getBytes(String path, Bundle b) throws IOException {
		return getBytes(path, b, null);
	}

	private static byte[] getBytes(String path, Bundle b, byte[] replace) throws IOException {
		URL entry = b.getEntry(path);
		if (entry == null) {
			throw new FileNotFoundException("No entry found for: " + path);
		}
		byte[] result = StorageUtil.getBytes(entry.openStream(), -1, 4000);

		if (replace != null) {
			for (int i = 0; i < result.length - 1; i++) {
				if (result[i] == 'X' && result[i + 1] == 'X') {
					result[i] = replace[0];
					result[i + 1] = replace[1];
				}
			}
		}
		return result;
	}

	public void testMultiRelease8ClassLoad() throws Exception {
		doTestMultiReleaseClassLoad(8);
	}

	public void testMultiRelease9ClassLoad() throws Exception {
		doTestMultiReleaseClassLoad(9);
	}

	public void testMultiRelease10ClassLoad() throws Exception {
		doTestMultiReleaseClassLoad(10);
	}

	public void testMultiRelease11ClassLoad() throws Exception {
		doTestMultiReleaseClassLoad(11);
	}

	private void doTestMultiReleaseClassLoad(int rv) throws Exception {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle(mrJarBundle.toURI().toString());
			mrBundle.start();
			assertEquals("Wrong class.", "BASEXX", loadClass("multi.release.test.TestClassBase", mrBundle, false));
			assertEquals("Wrong class.", "BASEXX", loadClass("multi.release.test.TestClass8", mrBundle, false));
			assertEquals("Wrong class.", CNFE, loadClass("multi.release.test.TestClassAdd8", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 9) ? "BASE09" : "BASEXX", loadClass("multi.release.test.TestClass9", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 9) ? "ADD09" : CNFE, loadClass("multi.release.test.TestClassAdd9", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 10) ? "BASE10" : "BASEXX", loadClass("multi.release.test.TestClass10", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 10) ? "ADD10" : CNFE, loadClass("multi.release.test.TestClassAdd10", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 11) ? "BASE11" : "BASEXX", loadClass("multi.release.test.TestClass11", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 11) ? "ADD11" : CNFE, loadClass("multi.release.test.TestClassAdd11", mrBundle, true));

			assertEquals("Wrong class.", "BASEXX", loadClass("multi.release.test.sub.TestClassBase", mrBundle, false));
			assertEquals("Wrong class.", "BASEXX", loadClass("multi.release.test.sub.TestClass8", mrBundle, false));
			assertEquals("Wrong class.", CNFE, loadClass("multi.release.test.TestClassAdd8", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 9) ? "BASE09" : "BASEXX", loadClass("multi.release.test.sub.TestClass9", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 9) ? "ADD09" : CNFE, loadClass("multi.release.test.sub.TestClassAdd9", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 10) ? "BASE10" : "BASEXX", loadClass("multi.release.test.sub.TestClass10", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 10) ? "ADD10" : CNFE, loadClass("multi.release.test.sub.TestClassAdd10", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 11) ? "BASE11" : "BASEXX", loadClass("multi.release.test.sub.TestClass11", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 11) ? "ADD11" : CNFE, loadClass("multi.release.test.sub.TestClassAdd11", mrBundle, true));

			assertEquals("Wrong class.", "BASEXX", loadClass("multi.release.test.sub2.TestClassBase", mrBundle, false));
			assertEquals("Wrong class.", "BASEXX", loadClass("multi.release.test.sub2.TestClass8", mrBundle, false));
			assertEquals("Wrong class.", CNFE, loadClass("multi.release.test.TestClassAdd8", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 9) ? "BASE09" : "BASEXX", loadClass("multi.release.test.sub2.TestClass9", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 9) ? "ADD09" : CNFE, loadClass("multi.release.test.sub2.TestClassAdd9", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 10) ? "BASE10" : "BASEXX", loadClass("multi.release.test.sub2.TestClass10", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 10) ? "ADD10" : CNFE, loadClass("multi.release.test.sub2.TestClassAdd10", mrBundle, true));
			assertEquals("Wrong class.", (rv >= 11) ? "BASE11" : "BASEXX", loadClass("multi.release.test.sub2.TestClass11", mrBundle, false));
			assertEquals("Wrong class.", (rv >= 11) ? "ADD11" : CNFE, loadClass("multi.release.test.sub2.TestClassAdd11", mrBundle, true));
		} finally {
			stopQuietly(equinox);
		}
	}

	private String loadClass(String name, Bundle mrBundle, boolean cnfeExpected) throws Exception {
		try {
			return mrBundle.loadClass(name).getConstructor().newInstance().toString();
		} catch (ClassNotFoundException e) {
			if (cnfeExpected) {
				return CNFE;
			}
			throw e;
		}
	}

	public void testMultiRelease8GetResource() throws Exception {
		doTestMultiReleaseGetResource(8);
	}

	public void testMultiRelease9GetResource() throws Exception {
		doTestMultiReleaseGetResource(9);
	}

	public void testMultiRelease10GetResource() throws Exception {
		doTestMultiReleaseGetResource(10);
	}

	public void testMultiRelease11GetResource() throws Exception {
		doTestMultiReleaseGetResource(11);
	}

	private void doTestMultiReleaseGetResource(int rv) throws Exception {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle(mrJarBundle.toURI().toString());
			mrBundle.start();

			assertEquals("Wrong resource.", "RESOURCE XX", readResource("multi/release/test/testResourceBase.txt", mrBundle));
			assertEquals("Wrong resource.", "RESOURCE XX", readResource("multi/release/test/testResource8.txt", mrBundle));
			assertEquals("Wrong resource.", RNF, readResource("multi/release/test/testResourceAdd8.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "RESOURCE 09" : "RESOURCE XX", readResource("multi/release/test/testResource9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "ADD 09" : RNF, readResource("multi/release/test/testResourceAdd9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "RESOURCE 10" : "RESOURCE XX", readResource("multi/release/test/testResource10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "ADD 10" : RNF, readResource("multi/release/test/testResourceAdd10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "RESOURCE 11" : "RESOURCE XX", readResource("multi/release/test/testResource11.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "ADD 11" : RNF, readResource("multi/release/test/testResourceAdd11.txt", mrBundle));

			assertEquals("Wrong resource.", "RESOURCE XX", readResource("multi/release/test/sub/testResourceBase.txt", mrBundle));
			assertEquals("Wrong resource.", "RESOURCE XX", readResource("multi/release/test/sub/testResource8.txt", mrBundle));
			assertEquals("Wrong resource.", RNF, readResource("multi/release/test/testResourceAdd8.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "RESOURCE 09" : "RESOURCE XX", readResource("multi/release/test/sub/testResource9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "ADD 09" : RNF, readResource("multi/release/test/sub/testResourceAdd9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "RESOURCE 10" : "RESOURCE XX", readResource("multi/release/test/sub/testResource10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "ADD 10" : RNF, readResource("multi/release/test/sub/testResourceAdd10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "RESOURCE 11" : "RESOURCE XX", readResource("multi/release/test/sub/testResource11.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "ADD 11" : RNF, readResource("multi/release/test/sub/testResourceAdd11.txt", mrBundle));

			assertEquals("Wrong resource.", "RESOURCE XX", readResource("multi/release/test/sub2/testResourceBase.txt", mrBundle));
			assertEquals("Wrong resource.", "RESOURCE XX", readResource("multi/release/test/sub2/testResource8.txt", mrBundle));
			assertEquals("Wrong resource.", RNF, readResource("multi/release/test/testResourceAdd8.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "RESOURCE 09" : "RESOURCE XX", readResource("multi/release/test/sub2/testResource9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "ADD 09" : RNF, readResource("multi/release/test/sub2/testResourceAdd9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "RESOURCE 10" : "RESOURCE XX", readResource("multi/release/test/sub2/testResource10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "ADD 10" : RNF, readResource("multi/release/test/sub2/testResourceAdd10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "RESOURCE 11" : "RESOURCE XX", readResource("multi/release/test/sub2/testResource11.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "ADD 11" : RNF, readResource("multi/release/test/sub2/testResourceAdd11.txt", mrBundle));

		} finally {
			stopQuietly(equinox);
		}
	}

	private String readResource(String name, Bundle mrBundle) throws Exception {
		BundleWiring wiring = mrBundle.adapt(BundleWiring.class);
		URL url = wiring.getClassLoader().getResource(name);
		String result = readURL(url);

		int lastSlash = name.lastIndexOf('/');
		Collection<String> resourcePaths = wiring.listResources(name.substring(0, lastSlash + 1), name.substring(lastSlash + 1), 0);
		if (result == RNF) {
			if (!resourcePaths.isEmpty()) {
				fail("listResources found path for '" + name + "'");
			}
		} else {
			assertEquals("Found too many resource paths for '" + name + "'", 1, resourcePaths.size());
			assertEquals("Wrong path listed.", name, resourcePaths.iterator().next());
			assertURLCopy(result, url, mrBundle);
		}

		return result;
	}

	private void assertURLCopy(String expected, URL url, Bundle mrBundle) throws Exception {
		Class<?> testClassBase = mrBundle.loadClass("multi.release.test.TestClassBase");
		URL copy = (URL) testClassBase.getDeclaredMethod("createURL", String.class).invoke(null, url.toExternalForm());
		String copyResult = readURL(copy);
		assertEquals(expected, copyResult);
	}

	private String readURL(URL url) throws IOException {
		if (url == null) {
			return RNF;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
		try {
			return br.readLine();
		} finally {
			br.close();
		}
	}

	public void testMultiRelease8GetResources() throws Exception {
		doTestMultiReleaseGetResources(8);
	}

	public void testMultiRelease9GetResources() throws Exception {
		doTestMultiReleaseGetResources(9);
	}

	public void testMultiRelease10GetResources() throws Exception {
		doTestMultiReleaseGetResources(10);
	}

	public void testMultiRelease11GetResources() throws Exception {
		doTestMultiReleaseGetResources(11);
	}

	private void doTestMultiReleaseGetResources(int rv) throws Exception {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle(mrJarBundle.toURI().toString());
			mrBundle.start();

			assertEquals("Wrong resource.", "RESOURCE XX", readResources("multi/release/test/testResourceBase.txt", mrBundle));
			assertEquals("Wrong resource.", "RESOURCE XX", readResources("multi/release/test/testResource8.txt", mrBundle));
			assertEquals("Wrong resource.", RNF, readResources("multi/release/test/testResourceAdd8.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "RESOURCE 09" : "RESOURCE XX", readResources("multi/release/test/testResource9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "ADD 09" : RNF, readResources("multi/release/test/testResourceAdd9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "RESOURCE 10" : "RESOURCE XX", readResources("multi/release/test/testResource10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "ADD 10" : RNF, readResources("multi/release/test/testResourceAdd10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "RESOURCE 11" : "RESOURCE XX", readResources("multi/release/test/testResource11.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "ADD 11" : RNF, readResources("multi/release/test/testResourceAdd11.txt", mrBundle));

			assertEquals("Wrong resource.", "RESOURCE XX", readResources("multi/release/test/sub/testResourceBase.txt", mrBundle));
			assertEquals("Wrong resource.", "RESOURCE XX", readResources("multi/release/test/sub/testResource8.txt", mrBundle));
			assertEquals("Wrong resource.", RNF, readResources("multi/release/test/testResourceAdd8.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "RESOURCE 09" : "RESOURCE XX", readResources("multi/release/test/sub/testResource9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "ADD 09" : RNF, readResources("multi/release/test/sub/testResourceAdd9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "RESOURCE 10" : "RESOURCE XX", readResources("multi/release/test/sub/testResource10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "ADD 10" : RNF, readResources("multi/release/test/sub/testResourceAdd10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "RESOURCE 11" : "RESOURCE XX", readResources("multi/release/test/sub/testResource11.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "ADD 11" : RNF, readResources("multi/release/test/sub/testResourceAdd11.txt", mrBundle));

			assertEquals("Wrong resource.", "RESOURCE XX", readResources("multi/release/test/sub2/testResourceBase.txt", mrBundle));
			assertEquals("Wrong resource.", "RESOURCE XX", readResources("multi/release/test/sub2/testResource8.txt", mrBundle));
			assertEquals("Wrong resource.", RNF, readResources("multi/release/test/testResourceAdd8.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "RESOURCE 09" : "RESOURCE XX", readResources("multi/release/test/sub2/testResource9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 9) ? "ADD 09" : RNF, readResources("multi/release/test/sub2/testResourceAdd9.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "RESOURCE 10" : "RESOURCE XX", readResources("multi/release/test/sub2/testResource10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 10) ? "ADD 10" : RNF, readResources("multi/release/test/sub2/testResourceAdd10.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "RESOURCE 11" : "RESOURCE XX", readResources("multi/release/test/sub2/testResource11.txt", mrBundle));
			assertEquals("Wrong resource.", (rv >= 11) ? "ADD 11" : RNF, readResources("multi/release/test/sub2/testResourceAdd11.txt", mrBundle));

		} finally {
			stopQuietly(equinox);
		}
	}

	private String readResources(String name, Bundle mrBundle) throws IOException {
		BundleWiring wiring = mrBundle.adapt(BundleWiring.class);
		List<URL> urls = Collections.list(wiring.getClassLoader().getResources(name));
		if (urls.isEmpty()) {
			return RNF;
		}
		assertEquals("Wrong number of resources.", 1, urls.size());
		return readURL(urls.get(0));
	}

	public void testMultiRelease8ListResources() throws Exception {
		doTestMultiReleaseListResources(8);
	}

	public void testMultiRelease9ListResources() throws Exception {
		doTestMultiReleaseListResources(9);
	}

	public void testMultiRelease10ListResources() throws Exception {
		doTestMultiReleaseListResources(10);
	}

	public void testMultiRelease11ListResources() throws Exception {
		doTestMultiReleaseListResources(11);
	}

	private void doTestMultiReleaseListResources(int rv) throws Exception {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		Collection<String> expected = new ArrayList<>();
		Collection<String> expectedRecurse = new ArrayList<>();

		expected.add("multi/release/test/testResourceBase.txt");
		expected.add("multi/release/test/testResource8.txt");
		expected.add("multi/release/test/testResource9.txt");
		expected.add("multi/release/test/testResource10.txt");
		expected.add("multi/release/test/testResource11.txt");

		if (rv >= 9) {
			expected.add("multi/release/test/testResourceAdd9.txt");
		}
		if (rv >= 10) {
			expected.add("multi/release/test/testResourceAdd10.txt");
		}
		if (rv >= 11) {
			expected.add("multi/release/test/testResourceAdd11.txt");
		}

		expectedRecurse.addAll(expected);
		expectedRecurse.add("multi/release/test/sub/testResourceBase.txt");
		expectedRecurse.add("multi/release/test/sub/testResource8.txt");
		expectedRecurse.add("multi/release/test/sub/testResource9.txt");
		expectedRecurse.add("multi/release/test/sub/testResource10.txt");
		expectedRecurse.add("multi/release/test/sub/testResource11.txt");
		expectedRecurse.add("multi/release/test/sub2/testResourceBase.txt");
		expectedRecurse.add("multi/release/test/sub2/testResource8.txt");
		expectedRecurse.add("multi/release/test/sub2/testResource9.txt");
		expectedRecurse.add("multi/release/test/sub2/testResource10.txt");
		expectedRecurse.add("multi/release/test/sub2/testResource11.txt");

		if (rv >= 9) {
			expectedRecurse.add("multi/release/test/sub/testResourceAdd9.txt");
			expectedRecurse.add("multi/release/test/sub2/testResourceAdd9.txt");
		}
		if (rv >= 10) {
			expectedRecurse.add("multi/release/test/sub/testResourceAdd10.txt");
			expectedRecurse.add("multi/release/test/sub2/testResourceAdd10.txt");
		}
		if (rv >= 11) {
			expectedRecurse.add("multi/release/test/sub/testResourceAdd11.txt");
			expectedRecurse.add("multi/release/test/sub2/testResourceAdd11.txt");
		}

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle(mrJarBundle.toURI().toString());
			mrBundle.start();

			listResources("multi/release/test", expected, mrBundle, 0);
			listResources("multi/release/test", expectedRecurse, mrBundle, BundleWiring.LISTRESOURCES_RECURSE);
		} finally {
			stopQuietly(equinox);
		}
	}

	private void listResources(String path, Collection<String> expected, Bundle mrBundle, int options) {
		BundleWiring wiring = mrBundle.adapt(BundleWiring.class);
		Collection<String> found = wiring.listResources(path, "*.txt", options);
		assertEquals("Wrong resource listing.", new HashSet<>(expected), new HashSet<>(found));
	}

	public void testMultiReleaseBundleManifest8() throws Exception {
		doTestMultiReleaseBundleManifest(8);
	}

	public void testMultiReleaseBundleManifest9() throws Exception {
		doTestMultiReleaseBundleManifest(9);
	}

	public void testMultiReleaseBundleManifest10() throws Exception {
		doTestMultiReleaseBundleManifest(10);
	}

	public void testMultiReleaseBundleManifest11() throws Exception {
		doTestMultiReleaseBundleManifest(11);
	}

	private void doTestMultiReleaseBundleManifest(int rv) throws Exception {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		String expectedCap;
		String expectedPkg;
		if (rv < 9) {
			expectedCap = "capbase";
			expectedPkg = "pkgbase";
		} else {
			expectedCap = "cap" + rv;
			expectedPkg = "pkg" + rv;
		}

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Equinox equinox = new Equinox(Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle(mrJarBundle.toURI().toString());
			mrBundle.start();

			List<BundleWire> capWires = mrBundle.adapt(BundleWiring.class).getRequiredWires(expectedCap);
			assertEquals("Wrong number of capability wires.", 1, capWires.size());

			List<BundleRequirement> pkgReqs = mrBundle.adapt(BundleRevision.class).getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
			assertEquals("Wrong number of package requiremens.", 1, pkgReqs.size());
			String filter = pkgReqs.get(0).getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			assertTrue("Wrong package filter: " + filter, filter.contains(expectedPkg));

		} finally {
			stopQuietly(equinox);
		}
	}

	public void testMultiReleaseBundleManifestChangeRuntime() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configMap = Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configMap);
		String location;
		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle(mrJarBundle.toURI().toString());
			location = mrBundle.getLocation();
			mrBundle.start();
		} finally {
			stop(equinox);
		}
		System.out.println("Equinox state: " + equinox.getState());
		for (int rv = 8; rv <= 11; rv++) {
			doTestMultiReleaseBundleManifestChangeRuntime(rv, configMap, location);
		}

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle toUninstall = systemContext.getBundle(location);
			toUninstall.uninstall();
			Bundle mrBundle = systemContext.installBundle("reference:" + mrJarBundle.toURI().toString());
			location = mrBundle.getLocation();
			mrBundle.start();
		} finally {
			stop(equinox);
		}

		for (int rv = 8; rv <= 11; rv++) {
			doTestMultiReleaseBundleManifestChangeRuntime(rv, configMap, location);
		}
	}

	private void doTestMultiReleaseBundleManifestChangeRuntime(int rv, Map<String, String> configMap, String location) throws BundleException {
		if (rv < 9) {
			System.setProperty("java.specification.version", "1." + rv);
		} else {
			System.setProperty("java.specification.version", Integer.toString(rv));
		}

		String expectedCap;
		String expectedPkg;
		if (rv < 9) {
			expectedCap = "capbase";
			expectedPkg = "pkgbase";
		} else {
			expectedCap = "cap" + rv;
			expectedPkg = "pkg" + rv;
		}

		Equinox equinox = new Equinox(configMap);

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			System.out.println("Bundles: " + Arrays.toString(systemContext.getBundles()));
			Bundle mrBundle = systemContext.getBundle(location);
			assertNotNull("No mrBundle found: " + rv, mrBundle);
			assertEquals("Wrong state of mrBundle: " + rv, Bundle.ACTIVE, mrBundle.getState());

			List<BundleWire> capWires = mrBundle.adapt(BundleWiring.class).getRequiredWires(expectedCap);
			assertEquals("Wrong number of capability wires: " + rv, 1, capWires.size());

			List<BundleRequirement> pkgReqs = mrBundle.adapt(BundleRevision.class).getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
			assertEquals("Wrong number of package requiremens: " + rv, 1, pkgReqs.size());
			String filter = pkgReqs.get(0).getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			assertTrue("Wrong package filter: " + rv + " " + filter, filter.contains(expectedPkg));

		} finally {
			stopQuietly(equinox);
		}
	}

	public void testMultiReleaseBundleDeletedRestart() throws Exception {
		File copyMrJarBundle = OSGiTestsActivator.getContext().getDataFile("copy-" + mrJarBundle.getName());
		StorageUtil.copy(mrJarBundle, copyMrJarBundle);

		System.setProperty("java.specification.version", "9");

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configMap = Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configMap);

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle("reference:" + copyMrJarBundle.toURI().toString());
			mrBundle.start();
		} finally {
			stop(equinox);
		}

		copyMrJarBundle.delete();

		System.setProperty("java.specification.version", "10");

		equinox = new Equinox(configMap);
		try {
			equinox.start();
		} finally {
			stop(equinox);
		}
	}

	public void testMultiReleasePreventMetaInfServiceVersions() throws Exception {
		System.setProperty("java.specification.version", "9");

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configMap = Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configMap);

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle("reference:" + mrJarBundle.toURI().toString());
			mrBundle.start();

			Class<?> testServiceClass = mrBundle.loadClass("multi.release.test.TestService");
			ServiceLoader<?> loader = ServiceLoader.load(testServiceClass, mrBundle.adapt(BundleWiring.class).getClassLoader());
			Object testService = loader.iterator().next();
			assertEquals("Wrong service found.", "SERVICE_BASE", testService.toString());
		} finally {
			stop(equinox);
		}
	}

	public void testMultiReleasePreventMetaInfResourceURLs() throws Exception {
		System.setProperty("java.specification.version", "9");

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configMap = Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configMap);

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle("reference:" + mrJarBundle.toURI().toString());
			mrBundle.start();

			URL existingResource = mrBundle.getResource("multi/release/test/testResourceAdd9.txt");
			assertNotNull("Did not find Java 9 added resource.", existingResource);
			URL metaInfResource = new URL(existingResource, "/META-INF/addedFor9.txt");
			assertThrows("Expected error opening versioned META-INF resource.", IOException.class,
					() -> metaInfResource.openStream().close());
		} finally {
			stop(equinox);
		}
	}

	public void testMultiReleasePreventMetaInfVersionListing() throws Exception {
		System.setProperty("java.specification.version", "9");

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configMap = Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configMap);

		try {
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			Bundle mrBundle = systemContext.installBundle("reference:" + mrJarBundle.toURI().toString());
			mrBundle.start();

			Collection<String> list = mrBundle.adapt(BundleWiring.class).listResources("/META-INF/", "*.txt", 0);
			assertTrue("Found versioned META-INF resources: " + list, list.isEmpty());
		} finally {
			stop(equinox);
		}
	}
}
