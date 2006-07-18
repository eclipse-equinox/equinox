/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.io.*;

/**
 * Provides buffered read from a java.io.RandomAccessFile.
 */
public class BufferedRandomInputStream extends InputStream {

	private RandomAccessFile inputFile;
	private String filePath; // Canonical path to the underlying file used for logging
	private int buffer_size; // Current size of the buffer 
	private int buffer_pos; // Current read position in the buffer
	/**
	 * The absolute position in the file where the buffered region starts.
	 */
	private long buffer_start = 0;

	/**
	 * The current value of the RAF's file pointer.
	 */
	private long file_pointer;

	private byte buffer[];

	public BufferedRandomInputStream(File file) throws IOException {
		this(file, 2048); // default buffer size
	}

	public BufferedRandomInputStream(File file, int bufferSize) throws IOException {
		filePath = file.getCanonicalPath();
		inputFile = new RandomAccessFile(file, "r"); //$NON-NLS-1$
		buffer = new byte[bufferSize];
		file_pointer = 0;
		resetBuffer();
	}

	private void resetBuffer() {
		buffer_pos = 0;
		buffer_size = 0;
		buffer_start = 0;
	}

	private int fillBuffer() throws IOException {
		buffer_pos = 0;
		buffer_start = file_pointer;
		buffer_size = inputFile.read(buffer, 0, buffer.length);
		file_pointer += buffer_size;
		return buffer_size;
	}

	public int read() throws IOException {
		if (buffer_pos >= buffer_size) {
			if (fillBuffer() <= 0)
				return -1;
		}
		return buffer[buffer_pos++] & 0xFF;
	}

	public int read(byte b[], int off, int len) throws IOException {
		int available = buffer_size - buffer_pos;
		if (available < 0)
			return -1;
		//the buffer contains all the bytes we need, so copy over and return
		if (len <= available) {
			System.arraycopy(buffer, buffer_pos, b, off, len);
			buffer_pos += len;
			return len;
		}
		// Use portion remaining in the buffer
		System.arraycopy(buffer, buffer_pos, b, off, available);
		if (fillBuffer() <= 0)
			return available;
		//recursive call to read again until we have the bytes we need
		return available + read(b, off + available, len - available);
	}

	public long skip(long n) throws IOException {
		if (n <= 0)
			return 0;

		int available = buffer_size - buffer_pos;
		if (n <= available) {
			buffer_pos += n;
			return n;
		}
		resetBuffer();
		final int skipped = inputFile.skipBytes((int) (n - available));
		file_pointer += skipped;
		return available + skipped;
	}

	public int available() throws IOException {
		return (buffer_size - buffer_pos);
	}

	public void close() throws IOException {
		inputFile.close();
		inputFile = null;
		buffer = null;
	}

	public String toString() {
		return filePath;
	}

	/**
	 * Supplies functionality of the {@link java.io.RandomAccessFile#seek(long)} in
	 * a buffer-friendly manner.
	 * 
	 * @param pos offset
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException {
		if (pos >= buffer_start && pos < buffer_start + buffer_size) {
			//seeking within the current buffer
			buffer_pos = (int) (pos - buffer_start);
		} else {
			//seeking outside the buffer - just discard the buffer
			inputFile.seek(pos);
			file_pointer = pos;
			resetBuffer();
		}
	}

	/**
	 * Supplies functionality of the {@link java.io.RandomAccessFile#length()}.
	 * @return file length
	 * @throws IOException
	 */
	public long length() throws IOException {
		return inputFile.length();
	}

}
