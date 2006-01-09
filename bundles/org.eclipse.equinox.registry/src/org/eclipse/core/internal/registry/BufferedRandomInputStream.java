/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
	private byte buffer[];

	public BufferedRandomInputStream(File file) throws IOException {
		this(file, 2048); // default buffer size
	}

	public BufferedRandomInputStream(File file, int bufferSize) throws IOException {
		filePath = file.getCanonicalPath();
		inputFile = new RandomAccessFile(file, "r");
		buffer = new byte[bufferSize];
		resetBuffer();
	}

	private void resetBuffer() throws IOException {
		buffer_pos = 0;
		buffer_size = 0;
	}

	private int fillBuffer() throws IOException {
		buffer_pos = 0;
		buffer_size = inputFile.read(buffer, 0, buffer.length);
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
		if (len <= available) {
			System.arraycopy(buffer, buffer_pos, b, off, len);
			buffer_pos += len;
			return len;
		}

		// Use portion remaining in the buffer and read the rest from file
		System.arraycopy(buffer, buffer_pos, b, off, available);
		int read = inputFile.read(b, off + available, len - available);
		fillBuffer(); // nothing left in the buffer, fill it with the next chunk
		return available + read;
	}

	public long skip(long n) throws IOException {
		if (n <= 0)
			return 0;

		int available = buffer_size - buffer_pos;
		if (n <= available) {
			buffer_pos += n;
			return n;
		} else {
			resetBuffer();
			return available + inputFile.skipBytes((int) (n - available));
		}
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
		inputFile.seek(pos);
		resetBuffer();
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
