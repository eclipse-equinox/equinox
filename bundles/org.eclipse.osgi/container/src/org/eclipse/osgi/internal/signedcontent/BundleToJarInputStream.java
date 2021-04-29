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

package org.eclipse.osgi.internal.signedcontent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;

/*
 * Converts a BundleFile into an input stream that is appropriate for
 * creating a JarInputStream.
 */
public final class BundleToJarInputStream extends InputStream {
	private final BundleFile bundlefile;
	private final Iterator<String> entryPaths;
	private final JarOutputStream jarOutput;
	private final ByteArrayOutputStream nextEntryOutput = new ByteArrayOutputStream();

	private ByteArrayInputStream nextEntryInput = null;

	public BundleToJarInputStream(BundleFile bundleFile) throws IOException {
		this.bundlefile = bundleFile;
		List<String> entries = new ArrayList<>();
		int signatureFileCnt = 0;
		for (Enumeration<String> ePaths = bundleFile.getEntryPaths("", true); ePaths.hasMoreElements();) { //$NON-NLS-1$
			String entry = ePaths.nextElement();
			if (entry.equals(JarFile.MANIFEST_NAME)) {
				// this is always read into the stream first and entries follow
				entries.add(0, entry);
				signatureFileCnt++;

			} else if (isSignatureFile(entry)) {
				// Add signature files directly after manifest.
				entries.add(signatureFileCnt++, entry);
			} else {
				// everything else is just at the end in order of the enumeration
				entries.add(entry);
			}
		}

		entryPaths = entries.iterator();

		jarOutput = new JarOutputStream(nextEntryOutput);
	}

	private boolean isSignatureFile(String entry) {
		entry = entry.toUpperCase();
		if (entry.startsWith("META-INF/") && entry.indexOf('/', "META-INF/".length()) == -1) { //$NON-NLS-1$ //$NON-NLS-2$
			return entry.endsWith(".SF") //$NON-NLS-1$
					|| entry.endsWith(".DSA") //$NON-NLS-1$
					|| entry.endsWith(".RSA") //$NON-NLS-1$
					|| entry.endsWith(".EC"); //$NON-NLS-1$
		}
		return false;
	}

	public int read() throws IOException {
		if (nextEntryInput != null) {
			int result = nextEntryInput.read();
			if (result != -1) {
				return result;
			}

			// this entry is done force a new one to be read if there is a next
			nextEntryInput = null;
			return read();

		}

		if (entryPaths.hasNext()) {
			readNext(entryPaths.next());

			if (!entryPaths.hasNext()) {
				jarOutput.close();
			}
		} else {
			jarOutput.close();
			return -1;
		}

		return read();
	}

	private void readNext(String path) throws IOException {
		BundleEntry found = bundlefile.getEntry(path);
		if (found == null) {
			throw new IOException("No entry found: " + path); //$NON-NLS-1$
		}

		nextEntryOutput.reset();
		JarEntry entry = new JarEntry(path);
		jarOutput.putNextEntry(entry);
		if (!entry.isDirectory()) {
			try (InputStream source = found.getInputStream()) {
				byte[] buf = new byte[8192];
				int length;
				while ((length = source.read(buf)) > 0) {
					jarOutput.write(buf, 0, length);
				}
			}
		}


		jarOutput.closeEntry();
		jarOutput.flush();

		// now save off the entry we just wrote
		nextEntryInput = new ByteArrayInputStream(nextEntryOutput.toByteArray());
	}

}
