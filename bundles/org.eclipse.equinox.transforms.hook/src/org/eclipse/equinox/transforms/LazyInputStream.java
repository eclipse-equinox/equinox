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

package org.eclipse.equinox.transforms;

import java.io.IOException;
import java.io.InputStream;

public class LazyInputStream extends InputStream {

	private InputStreamProvider provider;

	private InputStream original = null;

	public LazyInputStream(InputStreamProvider provider) {
		this.provider = provider;
	}

	private void initOriginal() throws IOException {
		if (original == null)
			original = provider.getInputStream();
	}

	public int available() throws IOException {
		initOriginal();
		return original.available();
	}

	public void close() throws IOException {
		initOriginal();
		original.close();
	}

	public boolean equals(Object obj) {
		try {
			initOriginal();
			return original.equals(obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int hashCode() {
		try {
			initOriginal();
			return original.hashCode();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void mark(int readlimit) {
		try {
			initOriginal();
			original.mark(readlimit);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean markSupported() {
		try {
			initOriginal();
			return original.markSupported();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int read() throws IOException {
		initOriginal();
		return original.read();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		initOriginal();
		return original.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		initOriginal();
		return original.read(b);
	}

	public void reset() throws IOException {
		initOriginal();
		original.reset();
	}

	public long skip(long n) throws IOException {
		initOriginal();
		return original.skip(n);
	}

	public String toString() {
		try {
			initOriginal();
			return original.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static interface InputStreamProvider {
		InputStream getInputStream() throws IOException;
	}
}
