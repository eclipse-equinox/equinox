/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package org.eclipse.osgi.tests.security;

import static org.eclipse.osgi.tests.security.BaseSecurityTest.copy;
import static org.eclipse.osgi.tests.security.BaseSecurityTest.getEntryFile;
import static org.eclipse.osgi.tests.security.BaseSecurityTest.getTestJarPath;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.eclipse.osgi.internal.signedcontent.BundleToJarInputStream;
import org.eclipse.osgi.storage.bundlefile.DirBundleFile;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class BundleToJarInputStreamTest {

	static private final List<String> testJarNames = Arrays.asList("multiply_signed", "SHA1withDSA", "SHA1withRSA",
			"SHA256withRSA", "SHA384withRSA", "SHA512withRSA", "signed_tsa", "signed_with_corrupt",
			"signed_with_metadata_added", "signed_with_metadata_corrupt", "signed_with_metadata_removed",
			"signed_with_metadata", "signed_with_missing_digest", "signed_with_sf_corrupted", "signed", "signedJava16",
			"test.bug252098", "unsigned");

	@Test
	public void testInputStreamEquality() throws IOException {
		for (String testJarName : testJarNames) {
			compareContent(testJarName);
		}
	}

	private void compareContent(String testJarName) throws IOException {
		File jar = getEntryFile(getTestJarPath(testJarName));
		File extracted = extract(jar);
		compare(jar, extracted);
	}

	private void compare(File jar, File extracted) throws IOException {
		// Using ZipFile and ZipInputStream to avoid validation
		try (ZipFile jarFile = new ZipFile(jar)) {
			Set<String> validated = new LinkedHashSet<>();
			BundleToJarInputStream inputToJar = new BundleToJarInputStream(new DirBundleFile(extracted, false));
			try (ZipInputStream jarInput = new ZipInputStream(inputToJar)) {
				for (ZipEntry extractedEntry = jarInput
						.getNextEntry(); extractedEntry != null; extractedEntry = jarInput.getNextEntry()) {
					if (!extractedEntry.isDirectory()) {
						byte[] extractedBytes = getBytes(jarInput);
						byte[] originalBytes = getBytes(
								jarFile.getInputStream(jarFile.getEntry(extractedEntry.getName())));
						assertArrayEquals("Wrong entry content: " + extractedEntry.getName(), originalBytes,
								extractedBytes);
						validated.add(extractedEntry.getName());
					}
				}
			}
			// make sure manifest and signature files are first
			Iterator<String> validpaths = validated.iterator();
			String first = validpaths.next();
			if (first.toUpperCase().endsWith("META-INF/")) {
				first = validpaths.next();
			}
			assertEquals("Expected manifest.", JarFile.MANIFEST_NAME, first.toUpperCase());
			// If there are signature files, make sure they are before all other entries
			AtomicReference<String> foundNonSignatureFile = new AtomicReference<>();
			validpaths.forEachRemaining((s) -> {
				if (isSignatureFile(s)) {
					assertNull("Found non signature file before.", foundNonSignatureFile.get());
				} else {
					foundNonSignatureFile.compareAndSet(null, s);
				}
			});

			for (Enumeration<? extends ZipEntry> originalEntries = jarFile.entries(); originalEntries
					.hasMoreElements();) {
				ZipEntry originalEntry = originalEntries.nextElement();
				validated.remove(originalEntry.getName());
			}
			assertTrue("More paths extracted content: " + validated, validated.isEmpty());
		}
	}

	private boolean isSignatureFile(String s) {
		s = s.toUpperCase();
		if (s.startsWith("META-INF/") && s.indexOf('/', "META-INF/".length()) == -1) { //$NON-NLS-1$ //$NON-NLS-2$
			return s.endsWith(".SF") //$NON-NLS-1$
					|| s.endsWith(".DSA") //$NON-NLS-1$
					|| s.endsWith(".RSA") //$NON-NLS-1$
					|| s.endsWith(".EC"); //$NON-NLS-1$
		}
		return false;
	}

	byte[] getBytes(InputStream in) throws IOException {
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		byte[] drain = new byte[4096];
		for (int read = in.read(drain, 0, drain.length); read != -1; read = in.read(drain, 0, drain.length)) {
			content.write(drain, 0, read);
		}
		return content.toByteArray();
	}

	private File extract(File jar) throws IOException {
		BundleContext bc = OSGiTestsActivator.getContext();
		File dir = bc.getDataFile("extracted/" + jar.getName());
		if (dir.isDirectory()) {
			return dir;
		}
		dir.mkdirs();
		try (ZipFile jarFile = new ZipFile(jar)) {
			for (Enumeration<? extends ZipEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				ZipEntry entry = entries.nextElement();
				if (!entry.isDirectory()) {
					try (InputStream in = jarFile.getInputStream(entry)) {
						File destination = new File(dir, entry.getName());
						destination.getParentFile().mkdirs();
						copy(in, destination);
					}
				}
			}
		}
		return dir;
	}
}
