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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;

/**
 * Converts a BundleFile into an input stream that is appropriate for creating a
 * JarInputStream. This class is intended to only be used for directory bundles
 * in order to parse out the singer information. For Jar'ed bundles the JarFile
 * class should be used to read the singer information.
 * <p>
 * To do this a JarOutStream is used to write entries from the bundle file one
 * at a time and the output of that is then feed into the input stream which can
 * be used with a JarInputStream to read the directory as if it is a JarFile.
 * <p>
 * Unfortunately to do this without holding the complete content of an entry in
 * memory the content of each entry must be read 3 times. 1) for calculating the
 * CRC. 2) for writing to the JarOutputStream so it can also calculate the CRC
 * 3) for the actual content that will be read by the JarInputStream as it is
 * iterating over the entries.
 * <p>
 * This is required to use the STORED method for each entry to avoid holding the
 * complete content in memory.
 */
public final class BundleToJarInputStream extends InputStream {
	static final ByteArrayInputStream directoryInputStream = new ByteArrayInputStream(new byte[0]);
	private final BundleFile bundlefile;
	private final Iterator<String> entryPaths;
	private final JarOutputStream jarOutput;
	private final NextEntryOutputStream nextEntryOutput = new NextEntryOutputStream();

	private InputStream nextEntryInput = null;

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
		jarOutput.setLevel(Deflater.NO_COMPRESSION);
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
			nextEntryInput.close();
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
		nextEntryOutput.setCurrent(found);

		// create the JarEntry, this will read non-directory entries to calculate the
		// CRC
		JarEntry entry = getJarEntry(path, found);
		nextEntryOutput.setCurrentLen(entry.getSize());

		jarOutput.putNextEntry(entry);
		nextEntryOutput.prefixWritten();

		if (!entry.isDirectory()) {
			// Have to read the non-directory entry again so that the JarOutputStream
			// can confirm the CRC we calculated above is correct.
			try (InputStream source = new BufferedInputStream(found.getInputStream())) {
				byte[] buf = new byte[8192];
				int length;
				while ((length = source.read(buf)) > 0) {
					jarOutput.write(buf, 0, length);
				}
			}
		} else {
			nextEntryOutput.setCurrentLen(0);
		}

		jarOutput.closeEntry();
		jarOutput.flush();

		// now save off the entry we just wrote, this is the 3rd time
		// we will read the content of non-directory entries!
		nextEntryInput = nextEntryOutput.getNextInputStream();
	}

	private JarEntry getJarEntry(String path, BundleEntry found) throws IOException {
		JarEntry entry = new JarEntry(path);
		if (!entry.isDirectory()) {
			entry.setMethod(ZipEntry.STORED);
			entry.setCompressedSize(-1);

			CRC32 crc = new CRC32();
			long entryLen = 0;
			try (InputStream source = new BufferedInputStream(found.getInputStream())) {
				byte[] buf = new byte[8192];
				int length;
				while ((length = source.read(buf)) > 0) {
					entryLen += length;
					crc.update(buf, 0, length);
				}
			}
			entry.setCrc(crc.getValue());
			entry.setSize(entryLen);
		}
		return entry;
	}

	@Override
	public void close() throws IOException {
		nextEntryOutput.close();
	}

	static class NextEntryOutputStream extends OutputStream {
		private BundleEntry current = null;
		private long currentLen = -1;
		private long currentWritten = 0;
		private boolean writingPrefix = true;
		final ByteArrayOutputStream currentPrefix = new ByteArrayOutputStream();
		final ByteArrayOutputStream currentPostfix = new ByteArrayOutputStream();

		void setCurrent(BundleEntry entry) {
			this.current = entry;
			this.currentWritten = 0;
			this.currentPrefix.reset();
			this.currentPostfix.reset();
			this.writingPrefix = true;
		}

		void prefixWritten() {
			writingPrefix = false;
		}

		void setCurrentLen(long currentLen) {
			this.currentLen = currentLen;
		}

		InputStream getCurrentInputStream() throws IOException {
			if (current.getName().endsWith("/")) { //$NON-NLS-1$
				return directoryInputStream;
			}
			return new BufferedInputStream(current.getInputStream());
		}

		InputStream getNextInputStream() throws IOException {
			return new FilterInputStream(getCurrentInputStream()) {
				ByteArrayInputStream prefix = new ByteArrayInputStream(currentPrefix.toByteArray());
				ByteArrayInputStream postfix = new ByteArrayInputStream(currentPostfix.toByteArray());

				@Override
				public int read() throws IOException {
					int read = prefix.read();
					if (read != -1) {
						return read;
					}
					read = super.read();
					if (read != -1) {
						return read;
					}
					return postfix.read();
				}

				@Override
				public int read(byte[] b) throws IOException {
					int read = prefix.read(b);
					if (read != -1) {
						return read;
					}
					read = super.read(b);
					if (read != -1) {
						return read;
					}
					return postfix.read(b);
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					int read = prefix.read(b, off, len);
					if (read != -1) {
						return read;
					}
					read = super.read(b, off, len);
					if (read != -1) {
						return read;
					}
					return postfix.read(b, off, len);
				}
			};
		}

		@Override
		public void write(int b) throws IOException {
			if (writingPrefix) {
				currentPrefix.write(b);
				return;
			}
			if (currentWritten < currentLen) {
				currentWritten++;
				return;
			}
			currentPostfix.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			if (writingPrefix) {
				currentPrefix.write(b);
				return;
			}
			if (currentWritten < currentLen) {
				currentWritten += b.length;
				return;
			}
			currentPostfix.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (writingPrefix) {
				currentPrefix.write(b, off, len);
				return;
			}
			if (currentWritten < currentLen) {
				currentWritten += len;
				return;
			}
			currentPostfix.write(b, off, len);
		}
	}

}
