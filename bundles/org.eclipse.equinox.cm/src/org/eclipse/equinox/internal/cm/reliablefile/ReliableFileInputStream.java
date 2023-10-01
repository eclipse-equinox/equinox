/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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

package org.eclipse.equinox.internal.cm.reliablefile;

import java.io.*;

//This is a copy of org.eclipse.osgi.framework.internal.reliablefile.ReliableFileInputStream

/**
 * A ReliableFile FileInputStream replacement class. This class can be used just
 * like FileInputStream. The class is in partnership with
 * ReliableFileOutputStream to avoid losing file data by using multiple files.
 *
 * @see ReliableFileOutputStream
 */
public class ReliableFileInputStream extends FilterInputStream {
	/**
	 * ReliableFile object for this file.
	 */
	private ReliableFile reliable;

	/**
	 * size of crc and signature
	 */
	private int sigSize;

	/**
	 * current position reading from file
	 */
	private int readPos;

	/**
	 * total file length available for reading
	 */
	private int length;

	/**
	 * Constructs a new ReliableFileInputStream on the file named <code>name</code>.
	 * If the file does not exist, the <code>FileNotFoundException</code> is thrown.
	 * The <code>name</code> may be absolute or relative to the System property
	 * <code>"user.dir"</code>.
	 *
	 * @param name the file on which to stream reads.
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileInputStream(String name) throws IOException {
		this(ReliableFile.getReliableFile(name), ReliableFile.GENERATION_LATEST, ReliableFile.OPEN_BEST_AVAILABLE);
	}

	/**
	 * Constructs a new ReliableFileInputStream on the File <code>file</code>. If
	 * the file does not exist, the <code>FileNotFoundException</code> is thrown.
	 *
	 * @param file the File on which to stream reads.
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileInputStream(File file) throws IOException {
		this(ReliableFile.getReliableFile(file), ReliableFile.GENERATION_LATEST, ReliableFile.OPEN_BEST_AVAILABLE);
	}

	/**
	 * Constructs a new ReliableFileInputStream on the File <code>file</code>. If
	 * the file does not exist, the <code>FileNotFoundException</code> is thrown.
	 * 
	 * @param file       the File on which to stream reads.
	 * @param generation a specific generation requested.
	 * @param openMask   mask used to open data. are invalid (corrupt, missing,
	 *                   etc).
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileInputStream(File file, int generation, int openMask) throws IOException {
		this(ReliableFile.getReliableFile(file), generation, openMask);
	}

	/**
	 * 
	 * @param reliable   The ReliableFile on which to read.
	 * @param generation a specific generation requested.
	 * @param openMask   mask used to open data. are invalid (corrupt, missing,
	 *                   etc).
	 * @throws IOException If an error occurs opening the file.
	 */
	private ReliableFileInputStream(ReliableFile reliable, int generation, int openMask) throws IOException {
		super(reliable.getInputStream(generation, openMask));

		this.reliable = reliable;
		sigSize = reliable.getSignatureSize();
		readPos = 0;
		length = super.available();
		if (sigSize > length)
			length = 0; // shouldn't ever happen
		else
			length -= sigSize;
	}

	/**
	 * Closes this input stream and releases any system resources associated with
	 * the stream.
	 *
	 * @exception java.io.IOException If an error occurs closing the file.
	 */
	@Override
	public synchronized void close() throws IOException {
		if (reliable != null) {
			try {
				super.close();
			} finally {
				reliable.closeInputFile();
				reliable = null;
			}
		}
	}

	/**
	 * Override default FilterInputStream method.
	 * 
	 * @see FilterInputStream#read(byte[], int, int)
	 */
	@Override
	public synchronized int read(byte b[], int off, int len) throws IOException {
		if (readPos >= length) {
			return -1;
		}
		int num = super.read(b, off, len);

		if (num != -1) {
			if (num + readPos > length) {
				num = length - readPos;
			}
			readPos += num;
		}
		return num;
	}

	/**
	 * Override default FilterInputStream method.
	 * 
	 * @see FilterInputStream#read(byte[])
	 */
	@Override
	public synchronized int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	/**
	 * Override default FilterInputStream method.
	 * 
	 * @see FilterInputStream#read()
	 */
	@Override
	public synchronized int read() throws IOException {
		if (readPos >= length) {
			return -1;
		}
		int num = super.read();

		if (num != -1) {
			readPos++;
		}
		return num;
	}

	/**
	 * Override default available method.
	 * 
	 * @see FilterInputStream#available()
	 */
	@Override
	public synchronized int available() {
		if (readPos < length) // just in case
			return (length - readPos);
		return 0;
	}

	/**
	 * Override default skip method.
	 * 
	 * @see FilterInputStream#skip(long)
	 */
	@Override
	public synchronized long skip(long n) throws IOException {
		long len = super.skip(n);
		if (readPos + len > length)
			len = length - readPos;
		readPos += len;
		return len;
	}

	/**
	 * Override default markSupported method.
	 * 
	 * @see FilterInputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return false;
	}

	/**
	 * Override default mark method.
	 * 
	 * @see FilterInputStream#mark(int)
	 */
	@Override
	public void mark(int readlimit) {
		// ignore
	}

	/**
	 * Override default reset method.
	 * 
	 * @see FilterInputStream#reset()
	 */
	@Override
	public void reset() throws IOException {
		throw new IOException("reset not supported."); //$NON-NLS-1$
	}
}
