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
import java.util.zip.Checksum;

//This is a copy of org.eclipse.osgi.framework.internal.reliablefile.ReliableFileOutputStream

/**
 * A ReliableFile FileOutputStream replacement class. This class can be used
 * just like FileOutputStream. The class is in partnership with
 * ReliableFileInputStream to avoid losing file data by using multiple files.
 *
 * @see ReliableFileInputStream
 */
public class ReliableFileOutputStream extends FilterOutputStream {
	/**
	 * ReliableFile object for the file.
	 */
	private ReliableFile reliable;

	/**
	 * Checksum calculator
	 */
	private Checksum crc;

	private boolean outputOpen = false;

	/**
	 * Constructs a new ReliableFileOutputStream on the File <code>file</code>. If
	 * the file exists, it is written over. See the constructor which can append to
	 * the file if so desired.
	 *
	 * @param file the File on which to stream reads.
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileOutputStream(File file) throws IOException {
		this(ReliableFile.getReliableFile(file), false);
	}

	/**
	 * Constructs a new ReliableFileOutputStream on the File <code>file</code>.
	 *
	 * @param file   the File on which to stream reads.
	 * @param append a boolean indicating whether or not to append to an existing
	 *               file.
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileOutputStream(File file, boolean append) throws IOException {
		this(ReliableFile.getReliableFile(file), append);
	}

	/**
	 * Constructs a new ReliableFileOutputStream on the file named
	 * <code>name</code>. If the file exists, it is written over. See the
	 * constructor which can append to the file if so desired. The <code>name</code>
	 * may be absolute or relative to the System property <code>"user.dir"</code>.
	 *
	 * @param name the file on which to stream writes.
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileOutputStream(String name) throws IOException {
		this(ReliableFile.getReliableFile(name), false);
	}

	/**
	 * Constructs a new ReliableFileOutputStream on the file named
	 * <code>name</code>. The <code>name</code> may be absolute or relative to the
	 * System property <code>"user.dir"</code>.
	 *
	 * @param name   the file on which to stream writes.
	 * @param append a boolean indicating whether or not to append to an existing
	 *               file.
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileOutputStream(String name, boolean append) throws IOException {
		this(ReliableFile.getReliableFile(name), append);
	}

	/**
	 * Private constructor used by other constructors.
	 *
	 * @param reliable the ReliableFile on which to read.
	 * @param append   a boolean indicating whether or not to append to an existing
	 *                 file.
	 * @exception java.io.IOException If an error occurs opening the file.
	 */
	private ReliableFileOutputStream(ReliableFile reliable, boolean append) throws IOException {
		super(reliable.getOutputStream(append, ReliableFile.GENERATION_LATEST));

		this.reliable = reliable;
		outputOpen = true;
		if (append)
			crc = reliable.getFileChecksum();
		else
			crc = reliable.getChecksumCalculator();
	}

	/**
	 * Closes this output stream and releases any system resources associated with
	 * this stream. The general contract of <code>close</code> is that it closes the
	 * output stream. A closed stream cannot perform output operations and cannot be
	 * reopened.
	 *
	 * @exception java.io.IOException If an error occurs closing the file.
	 */
	@Override
	public synchronized void close() throws IOException {
		closeIntermediateFile();
		reliable.closeOutputFile(crc);
		// if the previouse closeOutpuFile() throws exception,
		// we don't null out reliable to give another opportunity
		// to rename the file.
		reliable = null;
	}

	public File closeIntermediateFile() throws IOException {
		if (reliable == null)
			throw new IOException("ReliableFile stream not open"); //$NON-NLS-1$
		if (outputOpen) {
			// tag on our signature and checksum
			reliable.writeChecksumSignature(out, crc);
			out.flush();
			try {
				((FileOutputStream) out).getFD().sync();
			} catch (IOException e) {
				// just ignore this Exception
				// Debug
				e.printStackTrace();
			}
			out.close();
			outputOpen = false;
		}
		return reliable.getOutputFile();
	}

	/**
	 * Override default FilterOutputStream method.
	 * 
	 * @see FilterOutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	/**
	 * Override default FilterOutputStream method.
	 * 
	 * @see FilterOutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		crc.update(b, off, len);
	}

	/**
	 * Override default FilterOutputStream method.
	 * 
	 * @see FilterOutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		out.write(b);
		crc.update((byte) b);
	}

	public void abort() {
		if (reliable == null)
			return;
		if (outputOpen) {
			try {
				out.close();
			} catch (IOException e) {/* ignore */
			}
			outputOpen = false;
		}
		reliable.abortOutputFile();
		reliable = null;
	}
}
