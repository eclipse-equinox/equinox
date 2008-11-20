/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.storagemanager;

import java.io.*;

/**
 * Represents a managed output stream for target managed by a storage manager.
 * @see StorageManager#getOutputStream(String)
 * @see StorageManager#getOutputStreamSet(String[])
 * <p>
 * Clients may not extend this class.
 * </p>
 * @since 3.2
 */
// Note the implementation of this class originated from the following deprecated classes:
// /org.eclipse.osgi/eclipseAdaptor/src/org/eclipse/core/runtime/adaptor/StreamManagerOutputStream.java
public final class ManagedOutputStream extends FilterOutputStream {
	static final int ST_OPEN = 0;
	static final int ST_CLOSED = 1;
	private String target;
	private StorageManager manager;
	private File outputFile;
	private int state;
	private ManagedOutputStream[] streamSet = null;

	ManagedOutputStream(OutputStream out, StorageManager manager, String target, File outputFile) {
		super(out);
		this.manager = manager;
		this.target = target;
		this.outputFile = outputFile;
		this.state = ST_OPEN;
	}

	/** 
	 * Instructs this output stream to be closed and storage manager to 
	 * be updated as appropriate.  If this managed output stream is part of 
	 * a set returned by {@link StorageManager#getOutputStreamSet(String[])} then
	 * the storage manager will only be updated with the new content after all 
	 * of the managed output streams in the set are closed successfully.
	 * @see FilterOutputStream#close()
	 */
	public void close() throws IOException {
		manager.closeOutputStream(this);
	}

	/**
	 * Instructs this output stream to be closed and the contents discarded.
	 * If this managed output stream is part of a set returned by 
	 * {@link StorageManager#getOutputStreamSet(String[])} then the new 
	 * content of all managed output streams in the set will be discarded.
	 */
	public void abort() {
		manager.abortOutputStream(this);
	}

	OutputStream getOutputStream() {
		return out;
	}

	String getTarget() {
		return target;
	}

	File getOutputFile() {
		return outputFile;
	}

	int getState() {
		return state;
	}

	void setState(int state) {
		this.state = state;
	}

	void setStreamSet(ManagedOutputStream[] set) {
		streamSet = set;
	}

	ManagedOutputStream[] getStreamSet() {
		return streamSet;
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 * Override this method to prevent single byte writes to the output stream
	 * which is done by the default implementation of FilteredOutputStream
	 */
	public void write(byte[] bytes, int off, int len) throws IOException {
		out.write(bytes, off, len);
	}
}
