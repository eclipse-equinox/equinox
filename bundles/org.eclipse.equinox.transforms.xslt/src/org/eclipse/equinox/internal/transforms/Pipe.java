/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.transforms;

import java.io.*;

/**
 * This class facilitates the moving of data from one input stream to another.  
 * Subclasses may customize the behavior of this move by overriding the {@link #pipeInput(InputStream, OutputStream)} method.
 */
public class Pipe {

	protected InputStream input;
	private PipedInputStream pipedInputStream;
	protected PipedOutputStream pipedOutputStream;

	/**
	 * Create a new Pipe based on the provided input stream.
	 * @param original the original stream.
	 * @throws IOException thrown if there is an issue establishing the pipe.
	 */
	public Pipe(InputStream original) throws IOException {
		this.input = original;

		// The following streams do the majority of the work.  
		// The first operation on the input stream will provoke a new thread to start.
		// This thread will invoke pipeInput and push the data from the original input stream to the output stream.
		// The output stream is tied to this input stream via PipedI/OStream properties so the data is available to callers on the input stream.
		// Any IOException thrown from within the thread will be caught and rethrown to callers of methods on this stream.
		this.pipedInputStream = new PipedInputStream() {
			protected IOException failure;
			private boolean started = false;
			protected Object lock = this;

			private void start() throws IOException {
				synchronized (lock) {
					if (failure != null) {
						IOException e = new IOException("Problem piping the stream."); //$NON-NLS-1$
						e.fillInStackTrace();
						e.initCause(failure);
						throw e;
					}
					if (!started) {
						started = true;
						Thread pipeThread = new Thread(new Runnable() {
							public void run() {
								try {
									pipeInput(input, pipedOutputStream);
									pipedOutputStream.close();
								} catch (IOException e) {
									synchronized (lock) {
										failure = e;
									}
								}
							}
						});
						pipeThread.start();
					}
				}
			}

			public synchronized int available() throws IOException {
				start();
				return super.available();
			}

			public synchronized int read() throws IOException {
				start();
				int c = super.read();
				return c;
			}

			public int read(byte[] b) throws IOException {
				start();
				return super.read(b);
			}

			public synchronized int read(byte[] b, int off, int len) throws IOException {
				start();
				return super.read(b, off, len);
			}

			public synchronized void reset() throws IOException {
				started = false;
				failure = null;
				input.reset();
				super.reset();
			}
		};
		this.pipedOutputStream = new PipedOutputStream(pipedInputStream);

	}

	/**
	 * Get the stream that has resulted from piping the original input stream through {@link #pipeInput(InputStream, OutputStream)}.
	 * @return the new stream.
	 */
	public InputStream getPipedInputStream() {
		return pipedInputStream;
	}

	/**
	 * Pipe the input stream to the output stream.
	 * The default implementation of this method does a simple copy operations.  
	 * Subclasses may elaborate on this behavior.
	 * @param original the original stream
	 * @param result the result stream
	 * @throws IOException thrown if there is an issue reading from the input stream or writing to the output stream.
	 */
	protected void pipeInput(InputStream original, OutputStream result) throws IOException {
		byte[] buffer = new byte[2048];
		int len = 0;
		while ((len = original.read(buffer)) != 0) {
			result.write(buffer, 0, len);
		}
	}
}
