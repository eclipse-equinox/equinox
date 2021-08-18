/*******************************************************************************
 * Copyright (c) 2021 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.url;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarFile;
import org.eclipse.core.runtime.Platform;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;

public class BundleURLConnectionTest {

	private static Class<?> testClass;
	private static Bundle classBundle;

	@BeforeClass
	public static void setUpBeforeClass() {
		testClass = BundleURLConnectionTest.class;
		classBundle = FrameworkUtil.getBundle(testClass);
		assertNotNull("Class is not from a OSGi-bundle", classBundle);
	}

	@AfterClass
	public static void tearDownAfterClass() {
		testClass = null;
		classBundle = null;
	}

	@Test
	public void testBundleReference_classLoaderURLConnection() throws IOException {
		URL resource = testClass.getClassLoader().getResource(JarFile.MANIFEST_NAME);

		assertBundleReferenceURLConnection(resource, classBundle);
	}

	@Test
	public void testBundleReference_otherClassLoaderURLConnection() throws IOException {
		String resourceName = Platform.class.getName().replace(".", "/") + ".class";
		URL resource = testClass.getClassLoader().getResource(resourceName);

		assertBundleReferenceURLConnection(resource, FrameworkUtil.getBundle(Platform.class));
	}

	@Test
	public void testBundleReference_bundleEntryURLConnection() throws IOException {
		URL entry = classBundle.getEntry(JarFile.MANIFEST_NAME);

		assertBundleReferenceURLConnection(entry, classBundle);
	}

	@Test
	public void testBundleReference_bundleEntriesURLConnection() throws IOException {
		Enumeration<URL> entries = classBundle.findEntries("META-INF", null, true);

		while (entries.hasMoreElements()) {
			assertBundleReferenceURLConnection(entries.nextElement(), classBundle);
		}
	}

	@Test
	public void testBundleReference_bundleResourceURLConnection() throws IOException {
		URL entry = classBundle.getResource(JarFile.MANIFEST_NAME);

		assertBundleReferenceURLConnection(entry, classBundle);
	}

	@Test
	public void testBundleReference_bundleResourcesURLConnection() throws IOException {
		Enumeration<URL> entries = classBundle.getResources(JarFile.MANIFEST_NAME);

		while (entries.hasMoreElements()) {
			assertBundleReferenceURLConnection(entries.nextElement(), classBundle);
		}
	}

	private static void assertBundleReferenceURLConnection(URL resource, Bundle expectedBundle) throws IOException {
		URLConnection connection = resource.openConnection();

		assertThat(connection, is(instanceOf(BundleReference.class)));

		Bundle bundle = ((BundleReference) connection).getBundle();
		assertSame(expectedBundle, bundle);
	}
}
