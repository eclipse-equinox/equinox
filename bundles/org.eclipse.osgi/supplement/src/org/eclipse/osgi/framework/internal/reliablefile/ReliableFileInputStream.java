/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.reliablefile;

import java.io.*;

/**
 * A ReliableFile FileInputStream replacement class.
 * This class can be used just like FileInputStream. The class
 * is in partnership with ReliableFileOutputStream to avoid losing
 * file data by using multiple files.
 *
 * @see			ReliableFileOutputStream
 */
public class ReliableFileInputStream extends FilterInputStream {
	/**
	 * ReliableFile object for this file.
	 */
	private ReliableFile reliable;

	/**
	 * Constructs a new ReliableFileInputStream on the file named <code>name</code>.  If the
	 * file does not exist, the <code>FileNotFoundException</code> is thrown.
	 * The <code>name</code> may be absolute or relative
	 * to the System property <code>"user.dir"</code>.
	 *
	 * @param		name	the file on which to stream reads.
	 * @exception 	java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileInputStream(String name) throws IOException {
		this(ReliableFile.getReliableFile(name));
	}

	/**
	 * Constructs a new ReliableFileInputStream on the File <code>file</code>.  If the
	 * file does not exist, the <code>FileNotFoundException</code> is thrown.
	 *
	 * @param		file		the File on which to stream reads.
	 * @exception 	java.io.IOException If an error occurs opening the file.
	 */
	public ReliableFileInputStream(File file) throws IOException {
		this(ReliableFile.getReliableFile(file));
	}

	/**
	 * Private constructor used by other constructors.
	 *
	 * @param		reliable		the ReliableFile on which to read.
	 * @exception 	java.io.IOException If an error occurs opening the file.
	 */
	private ReliableFileInputStream(ReliableFile reliable) throws IOException {
		super(reliable.getInputStream());

		this.reliable = reliable;
	}

	/**
	 * Closes this input stream and releases any system resources associated
	 * with the stream.
	 *
	 * @exception 	java.io.IOException If an error occurs closing the file.
	 */
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
	 * Call close to finalize the underlying ReliableFile.
	 */
	protected void finalize() throws IOException {
		close();
	}
}
