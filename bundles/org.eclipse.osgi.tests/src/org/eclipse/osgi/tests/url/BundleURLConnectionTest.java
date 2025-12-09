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
import static org.junit.Assert.assertEquals;
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

	@Test
	public void testBundleEntryURL_withQueryParameter() throws Exception {
		URL entry = classBundle.getEntry("/META-INF/MANIFEST.MF");
		assertNotNull("Entry should exist", entry);

		URL urlWithQuery = new URL(entry.toExternalForm() + "?param=value");

		assertEquals("Query parameter should be preserved", "param=value", urlWithQuery.getQuery());
		assertEquals("/META-INF/MANIFEST.MF", urlWithQuery.getPath());

		String externalForm = urlWithQuery.toExternalForm();
		assertEquals("External form should include query", entry.toExternalForm() + "?param=value", externalForm);
	}

	@Test
	public void testBundleEntryURL_withMultipleQueryParameters() throws Exception {
		URL entry = classBundle.getEntry("/META-INF/MANIFEST.MF");
		assertNotNull("Entry should exist", entry);

		URL urlWithQuery = new URL(entry.toExternalForm() + "?param1=value1&param2=value2");

		assertEquals("Query parameters should be preserved", "param1=value1&param2=value2", urlWithQuery.getQuery());
		assertEquals("/META-INF/MANIFEST.MF", urlWithQuery.getPath());
	}

	@Test
	public void testBundleResourceURL_withQueryParameter() throws Exception {
		URL resource = classBundle.getResource("/META-INF/MANIFEST.MF");
		assertNotNull("Resource should exist", resource);

		URL urlWithQuery = new URL(resource.toExternalForm() + "?param=value");

		assertEquals("Query parameter should be preserved", "param=value", urlWithQuery.getQuery());
		assertEquals("/META-INF/MANIFEST.MF", urlWithQuery.getPath());
	}

	@Test
	public void testBundleEntryURL_withQueryAndFragment() throws Exception {
		URL entry = classBundle.getEntry("/META-INF/MANIFEST.MF");
		assertNotNull("Entry should exist", entry);

		URL urlWithQueryAndFragment = new URL(entry.toExternalForm() + "?param=value#section");

		assertEquals("Query parameter should be preserved", "param=value", urlWithQueryAndFragment.getQuery());
		assertEquals("Fragment should be preserved", "section", urlWithQueryAndFragment.getRef());
		assertEquals("/META-INF/MANIFEST.MF", urlWithQueryAndFragment.getPath());
	}

	@Test
	public void testBundleURL_constructorWithQuery() throws Exception {
		URL entry = classBundle.getEntry("/");
		assertNotNull("Entry should exist", entry);

		URL urlWithQuery = new URL(entry, "META-INF/MANIFEST.MF?param=value");

		assertEquals("Query parameter should be preserved", "param=value", urlWithQuery.getQuery());
		assertEquals("/META-INF/MANIFEST.MF", urlWithQuery.getPath());
	}

	@Test
	public void testBundleURL_parseRelativePathWithQuery() throws Exception {
		URL base = classBundle.getEntry("/META-INF/");
		assertNotNull("Entry should exist", base);

		URL urlWithQuery = new URL(base, "resource.txt?param=value");

		assertEquals("Query parameter should be preserved", "param=value", urlWithQuery.getQuery());
		assertEquals("/META-INF/resource.txt", urlWithQuery.getPath());
	}

	@Test
	public void testBundleURL_openConnectionWithQuery() throws Exception {
		// Test that URLs with query parameters can be opened and read correctly
		URL entry = classBundle.getEntry("/META-INF/MANIFEST.MF");
		assertNotNull("Entry should exist", entry);

		// Create URL with query parameter
		URL urlWithQuery = new URL(entry.toExternalForm() + "?param=value");
		assertEquals("Query parameter should be preserved", "param=value", urlWithQuery.getQuery());

		// Open connection and read content
		URLConnection connection = urlWithQuery.openConnection();
		assertNotNull("Connection should not be null", connection);

		// Verify we can read the content (MANIFEST.MF should contain "Manifest-Version")
		byte[] buffer = new byte[1024];
		int bytesRead = connection.getInputStream().read(buffer);
		assertThat("Should read some bytes", bytesRead > 0);

		String content = new String(buffer, 0, bytesRead);
		assertThat("Content should contain 'Manifest-Version'", content.contains("Manifest-Version"));
	}

	@Test
	public void testBundleURL_openConnectionWithQueryAndFragment() throws Exception {
		// Test that URLs with both query and fragment can be opened and read correctly
		URL entry = classBundle.getEntry("/META-INF/MANIFEST.MF");
		assertNotNull("Entry should exist", entry);

		// Create URL with query parameter and fragment
		URL urlWithQueryAndFragment = new URL(entry.toExternalForm() + "?param=value#section");
		assertEquals("Query parameter should be preserved", "param=value", urlWithQueryAndFragment.getQuery());
		assertEquals("Fragment should be preserved", "section", urlWithQueryAndFragment.getRef());

		// Open connection and read content
		URLConnection connection = urlWithQueryAndFragment.openConnection();
		assertNotNull("Connection should not be null", connection);

		// Verify we can read the content
		byte[] buffer = new byte[1024];
		int bytesRead = connection.getInputStream().read(buffer);
		assertThat("Should read some bytes", bytesRead > 0);

		String content = new String(buffer, 0, bytesRead);
		assertThat("Content should contain 'Manifest-Version'", content.contains("Manifest-Version"));
	}
}
