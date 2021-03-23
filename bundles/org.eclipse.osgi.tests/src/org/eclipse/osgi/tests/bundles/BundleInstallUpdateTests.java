/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.wiring.BundleWiring;

public class BundleInstallUpdateTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(BundleInstallUpdateTests.class);
	}

	// test installing with location
	public void testInstallWithLocation01() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test installing with location and null stream
	public void testInstallWithLocation02() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location, null);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test installing with location and non-null stream
	public void testInstallWithStream03() {
		Bundle test = null;
		try {
			String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
			String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location1, new URL(location2).openStream());
			assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateNoStream01() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			test.update();
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateNoStream02() {
		Bundle test = null;
		try {
			String location = installer.getBundleLocation("test"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			test.update(null);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateWithStream01() {
		Bundle test = null;
		try {
			String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
			String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location1);
			assertEquals("Wrong BSN", "test1", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
			test.update(new URL(location2).openStream());
			assertEquals("Wrong BSN", "test2", test.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	// test update with null stream
	public void testUpdateWithStream02() {
		Bundle test = null;
		try {
			String location1 = installer.getBundleLocation("test"); //$NON-NLS-1$
			String location2 = installer.getBundleLocation("test2"); //$NON-NLS-1$
			test = OSGiTestsActivator.getContext().installBundle(location1);
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
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	public void testBug290193() {
		Bundle test = null;
		try {
			URL testBundle = OSGiTestsActivator.getContext().getBundle().getEntry("test_files/security/bundles/signed.jar");
			File testFile = OSGiTestsActivator.getContext().getDataFile("test with space/test.jar");
			assertTrue(testFile.getParentFile().mkdirs());
			readFile(testBundle.openStream(), testFile);
			test = OSGiTestsActivator.getContext().installBundle("reference:" + testFile.toURI().toString());
		} catch (Exception e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	public static void readFile(InputStream in, File file) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);

			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0) {
				fos.write(buffer, 0, count);
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}

			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ee) {
					// nothing to do here
				}
			}
		}
	}

	public void testCollisionHook() throws BundleException, MalformedURLException, IOException {
		Bundle test1 = installer.installBundle("test");
		installer.installBundle("test2");
		try {
			test1.update(new URL(installer.getBundleLocation("test2")).openStream());
			fail("Expected to fail to update to another bsn/version that causes collision");
		} catch (BundleException e) {
			// expected;
		}
		Bundle junk = null;
		try {
			junk = OSGiTestsActivator.getContext().installBundle("junk", new URL(installer.getBundleLocation("test2")).openStream());
			fail("Expected to fail to install duplication bsn/version that causes collision");
		} catch (BundleException e) {
			// expected;
		} finally {
			if (junk != null)
				junk.uninstall();
			junk = null;
		}

		CollisionHook hook = new CollisionHook() {
			public void filterCollisions(int operationType, Bundle target, Collection collisionCandidates) {
				collisionCandidates.clear();
			}
		};
		ServiceRegistration reg = OSGiTestsActivator.getContext().registerService(CollisionHook.class, hook, null);
		try {
			try {
				test1.update(new URL(installer.getBundleLocation("test2")).openStream());
			} catch (BundleException e) {
				fail("Expected to succeed in updating to a duplicate bsn/version", e);
			}
			try {
				junk = OSGiTestsActivator.getContext().installBundle("junk", new URL(installer.getBundleLocation("test2")).openStream());
			} catch (BundleException e) {
				fail("Expected to succeed to install duplication bsn/version that causes collision", e);
			} finally {
				if (junk != null)
					junk.uninstall();
				junk = null;
			}
		} finally {
			reg.unregister();
		}
	}

	public void testInstallWithInterruption() {
		Bundle test = null;
		Thread.currentThread().interrupt();
		try {
			test = installer.installBundle("test"); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected failure", e); //$NON-NLS-1$
		} finally {
			Thread.interrupted();
			try {
				if (test != null)
					test.uninstall();
			} catch (BundleException e) {
				// nothing
			}
		}
	}

	public void testPercentLocation() {
		doTestSpecialChars('%', false);
		doTestSpecialChars('%', true);
	}

	public void testSpaceLocation() {
		doTestSpecialChars(' ', false);
		doTestSpecialChars(' ', true);
	}

	public void testPlusLocation() {
		doTestSpecialChars('+', true);
		doTestSpecialChars('+', false);
	}

	public void testOctothorpLocation() {
		doTestSpecialChars('#', true);
		// # must be encoded for anything to pass
		doTestSpecialChars('#', false, false, false);
	}

	public void testQuestionMarkLocation() {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			// Skip this test on windows
			return;
		}
		doTestSpecialChars('?', true);
		// ? must only be encoded for non-reference installs
		doTestSpecialChars('?', false, true, false);

	}

	private void doTestSpecialChars(char c, boolean encode) {
		doTestSpecialChars(c, encode, true, true);
	}

	private void doTestSpecialChars(char c, boolean encode, boolean refPass, boolean filePass) {
		File bundlesDirectory = OSGiTestsActivator.getContext().getDataFile("file_with_" + c + "_char");
		bundlesDirectory.mkdirs();

		File testBundleJarFile = null;
		String testBundleJarFileURL = null;
		File testBundleDirFile = null;
		String testBundleDirFileURL = null;
		try {
			testBundleJarFile = SystemBundleTests.createBundle(bundlesDirectory, getName() + 1, false, false);
			testBundleJarFileURL = encode ? testBundleJarFile.toURI().toString() : testBundleJarFile.toURL().toString();
			testBundleDirFile = SystemBundleTests.createBundle(bundlesDirectory, getName() + 2, false, true);
			testBundleDirFileURL = encode ? testBundleDirFile.toURI().toString() : testBundleDirFile.toURL().toString();
		} catch (IOException e) {
			fail(e.getMessage());
		}

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

	void testInstallSpecialCharBundle(String location, boolean openStream, boolean expectSuccess) {
		try {
			Bundle b;
			if (openStream) {
				b = getContext().installBundle(location, new URL(location).openStream());
			} else {
				b = getContext().installBundle(location);
			}
			b.start();
			b.uninstall();
			if (!expectSuccess) {
				fail("Should have failed for location: " + location);
			}
		} catch (Exception e) {
			if (expectSuccess) {
				fail("Should not have failed for location: " + location + " " + e.getMessage());
			}
		}
	}

	public void testPercentCharBundleEntry() throws IOException, BundleException {
		doTestSpaceCharsBundleEntry('%');
	}

	public void testSpaceCharBundleEntry() throws IOException, BundleException {
		doTestSpaceCharsBundleEntry(' ');
	}

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

	public void testEscapeZipRoot() throws IOException, BundleException, InvalidSyntaxException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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
