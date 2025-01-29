/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.equinox.internal.transforms;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that is based off of another stream. This other stream is
 * provided as needed by an {@link InputStreamProvider} so that the underlying
 * stream is not eagerly loaded.
 */
public class LazyInputStream extends InputStream {

	private final InputStreamProvider provider;

	private InputStream original = null;

	/**
	 * Construct a new lazy stream based off the given provider.
	 * 
	 * @param provider the input stream provider. Must not be <code>null</code>.
	 */
	public LazyInputStream(InputStreamProvider provider) {
		if (provider == null)
			throw new IllegalArgumentException();
		this.provider = provider;
	}

	private void initOriginal() throws IOException {
		if (original == null)
			original = provider.getInputStream();
	}

	@Override
	public int available() throws IOException {
		initOriginal();
		return original.available();
	}

	@Override
	public void close() throws IOException {
		initOriginal();
		original.close();
	}

	@Override
	public boolean equals(Object obj) {
		try {
			initOriginal();
			return original.equals(obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		try {
			initOriginal();
			return original.hashCode();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void mark(int readlimit) {
		try {
			initOriginal();
			original.mark(readlimit);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean markSupported() {
		try {
			initOriginal();
			return original.markSupported();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int read() throws IOException {
		initOriginal();
		return original.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		initOriginal();
		return original.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		initOriginal();
		return original.read(b);
	}

	@Override
	public void reset() throws IOException {
		initOriginal();
		original.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		initOriginal();
		return original.skip(n);
	}

	@Override
	public String toString() {
		try {
			initOriginal();
			return original.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * An interface to be implemented by clients that wish to utilize
	 * {@link LazyInputStream}s. The implementation of this interface should defer
	 * obtaining the desired input stream until absolutely necessary.
	 */
	public static interface InputStreamProvider {
		/**
		 * Return the input stream.
		 * 
		 * @return the input stream
		 * @throws IOException thrown if there is an issue obtaining the stream
		 */
		InputStream getInputStream() throws IOException;
	}
}
