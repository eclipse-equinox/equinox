/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.wiring.BundleWiring;

public class BundleInstallUpdateTests extends AbstractBundleTests {

	// test installing with location
	@Test
	public void testInstallWithLocation01() throws BundleException {
		String location = installer.getBundleLocation("test"); //$NON-NLS-1$
		Bundle test = installer.installBundleAtLocation(location);
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// test installing with location and null stream
	@Test
	public void testInstallWithLocation02() throws BundleException {
		String location = installer.getBundleLocation("test"); //$NON-NLS-1$
		Bundle test = installer.installBundleAtLocation(location);
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// test installing with location and non-null stream
	@Test
	public void testInstallWithStream03() throws Exception {
		String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
		String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
		Bundle test = installer.installBundleAtLocation(location1, new URL(location2).openStream());
		assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// test update with null stream
	@Test
	public void testUpdateNoStream01() throws BundleException {
		String location = installer.getBundleLocation("test"); //$NON-NLS-1$
		Bundle test = installer.installBundleAtLocation(location);
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		test.update();
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// test update with null stream
	@Test
	public void testUpdateNoStream02() throws BundleException {
		String location = installer.getBundleLocation("test"); //$NON-NLS-1$
		Bundle test = installer.installBundleAtLocation(location);
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		test.update(null);
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// test update with null stream
	@Test
	public void testUpdateWithStream01() throws Exception {
		String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
		String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
		Bundle test = installer.installBundleAtLocation(location1);
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		test.update(new URL(location2).openStream());
		assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// test update with null stream
	@Test
	public void testUpdateWithStream02() throws Exception {
		String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
		String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
		Bundle test = installer.installBundleAtLocation(location1);
		Bundle b1 = installer.installBundle("chain.test"); //$NON-NLS-1$
		assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		test.update(new URL(location2).openStream());
		assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		// make sure b1 is still last bundle in bundles list
		Bundle[] bundles = OSGiTestsActivator.getContext().getBundles();
		assertTrue("Wrong bundle at the end: " + bundles[bundles.length - 1], bundles[bundles.length - 1] == b1); //$NON-NLS-1$
		Bundle[] tests = installer.getPackageAdmin().getBundles(test.getSymbolicName(), null);
		assertNotNull("null tests", tests); //$NON-NLS-1$
		assertEquals("Wrong number", 1, tests.length); //$NON-NLS-1$
		assertTrue("Wrong bundle: " + tests[0], tests[0] == test); //$NON-NLS-1$
	}

	@Test
	public void testBug290193() throws Exception {
		URL testBundle = OSGiTestsActivator.getBundle().getEntry("test_files/security/bundles/signed.jar");
		File testFile = OSGiTestsActivator.getContext().getDataFile("test with space/test.jar");
		assertTrue(testFile.getParentFile().mkdirs());
		Files.copy(testBundle.openStream(), testFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		installer.installBundleAtLocation("reference:" + testFile.toURI().toString());
	}

	@Test
	public void testCollisionHook() throws BundleException, IOException {
		Bundle test1 = installer.installBundle("test");
		installer.installBundle("test2");

		URL testLocation = new URL(installer.getBundleLocation("test2"));
		try (InputStream input = testLocation.openStream()) {
			assertThrows("Expected to fail to update to another bsn/version that causes collision",
					BundleException.class, () -> test1.update(input));
		}

		try (InputStream input = testLocation.openStream()) {
			assertThrows("Expected to fail to install duplication bsn/version that causes collision",
					BundleException.class, () -> installer.installBundleAtLocation("junk", input));
		}
		installer.uninstallBundle("junk");

		CollisionHook hook = (operationType, target, collisionCandidates) -> collisionCandidates.clear();
		ServiceRegistration<?> reg = OSGiTestsActivator.getContext().registerService(CollisionHook.class, hook, null);
		try {
			try (InputStream input = testLocation.openStream()) {
				test1.update(input);
			}
			try (InputStream input = testLocation.openStream()) {
				installer.installBundleAtLocation("junk", input);
			}
		} finally {
			reg.unregister();
		}
	}

	@Test
	public void testInstallWithInterruption() throws BundleException {
		Thread.currentThread().interrupt();
		installer.installBundle("test"); //$NON-NLS-1$
		// TODO: check that the bundle is uninstalled
	}

	@Test
	public void testPercentLocation() throws Exception {
		doTestSpecialChars('%', false);
		doTestSpecialChars('%', true);
	}

	@Test
	public void testSpaceLocation() throws Exception {
		doTestSpecialChars(' ', false);
		doTestSpecialChars(' ', true);
	}

	@Test
	public void testPlusLocation() throws Exception {
		doTestSpecialChars('+', true);
		doTestSpecialChars('+', false);
	}

	@Test
	public void testOctothorpLocation() throws Exception {
		doTestSpecialChars('#', true);
		// # must be encoded for anything to pass
		doTestSpecialChars('#', false, false, false);
	}

	@Test
	public void testQuestionMarkLocation() throws Exception {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			// Skip this test on windows
			return;
		}
		doTestSpecialChars('?', true);
		// ? must only be encoded for non-reference installs
		doTestSpecialChars('?', false, true, false);

	}

	private void doTestSpecialChars(char c, boolean encode) throws Exception {
		doTestSpecialChars(c, encode, true, true);
	}

	private void doTestSpecialChars(char c, boolean encode, boolean refPass, boolean filePass) throws Exception {
		File bundlesDirectory = OSGiTestsActivator.getContext().getDataFile("file_with_" + c + "_char");
		bundlesDirectory.mkdirs();

		File testBundleJarFile = SystemBundleTests.createBundle(bundlesDirectory, getName() + 1, false, false);
		String testBundleJarFileURL = (encode ? testBundleJarFile.toURI() : testBundleJarFile.toURL()).toString();
		File testBundleDirFile = SystemBundleTests.createBundle(bundlesDirectory, getName() + 2, false, true);
		String testBundleDirFileURL = (encode ? testBundleDirFile.toURI() : testBundleDirFile.toURL()).toString();

		String refToJarURL = "reference:" + testBundleJarFileURL;
		// Test with reference stream to jar bundle
		testInstallSpecialCharBundle(refToJarURL, true, refPass);

		// Test with reference URL to jar bundle
		testInstallSpecialCharBundle(refToJarURL, false, refPass);

		// Test with reference URL to dir bundle
		testInstallSpecialCharBundle("reference:" + testBundleDirFileURL, false, refPass);

		// Test with jar bundle
		testInstallSpecialCharBundle(testBundleJarFileURL, false, filePass);

		// Test with dir bundle
		testInstallSpecialCharBundle(testBundleDirFileURL, false, filePass);
	}

	void testInstallSpecialCharBundle(String location, boolean openStream, boolean expectSuccess) throws Exception {
		Callable<Void> testInstall = () -> {
			try (InputStream input = openStream ? new URL(location).openStream() : null) {
				Bundle b = installer.installBundleAtLocation(location, input);
				b.start();
				b.uninstall();
			}
			return null;
		};
		if (expectSuccess) {
			testInstall.call();
		} else {
			assertThrows(Exception.class, testInstall::call);
		}
	}

	@Test
	public void testPercentCharBundleEntry() throws IOException, BundleException {
		doTestSpaceCharsBundleEntry('%');
	}

	@Test
	public void testSpaceCharBundleEntry() throws IOException, BundleException {
		doTestSpaceCharsBundleEntry(' ');
	}

	@Test
	public void testPlusCharBundleEntry() throws IOException, BundleException {
		doTestSpaceCharsBundleEntry('+');
	}

	public void doTestSpaceCharsBundleEntry(char c) throws IOException, BundleException {
		String entryName = "file_with_" + c + "_char";
		File bundlesDirectory = OSGiTestsActivator.getContext().getDataFile(getName());
		bundlesDirectory.mkdirs();
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		Map<String, String> entry = Collections.singletonMap(entryName, "value");

		File testBundleJarFile = SystemBundleTests.createBundle(bundlesDirectory, getName(), headers, entry);
		Bundle testBundle = getContext().installBundle(getName(), new FileInputStream(testBundleJarFile));
		URL entryURL = testBundle.getEntry(entryName);
		assertNotNull("Entry not found.", entryURL);
		InputStream is = entryURL.openStream();
		is.close();

		String encodeEntry = URLEncoder.encode(entryName, "UTF-8");
		String urlString = entryURL.toExternalForm();
		urlString = urlString.substring(0, urlString.indexOf(entryName)) + encodeEntry;
		URL encodedURL = new URL(urlString);
		is = encodedURL.openStream();
		is.close();
	}

	public static Method findDeclaredMethod(Class<?> clazz, String method, Class... args) throws NoSuchMethodException {
		do {
			try {
				return clazz.getDeclaredMethod(method, args);
			} catch (NoSuchMethodException e) {
				clazz = clazz.getSuperclass();
			}
		} while (clazz != null);
		throw new NoSuchMethodException(method);
	}

	@Test
	public void testEscapeZipRoot() throws Exception {
		String entry1 = "../../escapedZipRoot1.txt";
		String entry2 = "dir1/../../../escapedZipRoot2.txt";
		String cp1 = "../../cp.jar";
		String nativeCode = "../../lib/nativeCode";
		File bundlesDirectory = OSGiTestsActivator.getContext().getDataFile(getName());
		bundlesDirectory.mkdirs();
		Map<String, String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		headers.put(Constants.BUNDLE_CLASSPATH, "., " + cp1);
		headers.put(Constants.BUNDLE_NATIVECODE, nativeCode);
		Map<String, String> entries = new HashMap<>();
		entries.put(entry1, "value");
		entries.put(entry2, "value");
		entries.put(cp1, "value");
		entries.put(nativeCode, "value");

		File testBundleJarFile = SystemBundleTests.createBundle(bundlesDirectory, getName(), headers, entries);
		Bundle testBundle = getContext().installBundle(getName(), new FileInputStream(testBundleJarFile));
		testBundle.start();
		try {
			testBundle.loadClass("does.not.exist.Test");
		} catch (ClassNotFoundException e) {
			// expected
		}
		Object cl = testBundle.adapt(BundleWiring.class).getClassLoader();
		Method findLibrary = findDeclaredMethod(cl.getClass(), "findLibrary", String.class);
		findLibrary.setAccessible(true);
		assertNull("Found library.", findLibrary.invoke(cl, "nativeCode"));

		URLConverter bundleURLConverter = getContext().getService(getContext().getServiceReferences(URLConverter.class, "(protocol=bundleentry)").iterator().next());

		URL dir1 = bundleURLConverter.toFileURL(testBundle.getEntry("dir1/"));
		File dir1File = new File(dir1.toExternalForm().substring(5));

		File dir1EscapedFile2 = new File(dir1File, entry2.substring("dir1".length()));
		assertFalse("File escaped zip root: " + dir1EscapedFile2.getCanonicalPath(), dir1EscapedFile2.exists());

		URL root = bundleURLConverter.toFileURL(testBundle.getEntry("/"));
		File rootFile = new File(root.toExternalForm().substring(5));

		File rootEscapedFile1 = new File(rootFile, entry1);
		assertFalse("File escaped zip root: " + rootEscapedFile1.getCanonicalPath(), rootEscapedFile1.exists());

		File rootEscapedFile2 = new File(rootFile, entry2);
		assertFalse("File escaped zip root: " + rootEscapedFile2.getCanonicalPath(), rootEscapedFile2.exists());

		File rootEscapedFile3 = new File(rootFile, cp1);
		assertFalse("File escaped zip root: " + rootEscapedFile3.getCanonicalPath(), rootEscapedFile3.exists());

	}
}
