/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.console.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * This class serves as an input stream, which wraps the actual input (e.g. from the telnet) and buffers the lines.
 */
public class ConsoleInputStream extends InputStream {

	private final ArrayList<byte[]> buffer = new ArrayList<>();
	private byte[] current;
	private int pos;
	private boolean isClosed;

	@Override
	public synchronized int read() {
		while (current == null && buffer.isEmpty() && !isClosed) {
			try {
				wait();
			} catch (InterruptedException e) {
				return -1;
			}
		}
		if (isClosed) {
			return -1;
		}

		try {
			if (current == null) {
				current = buffer.remove(0);
				return current[pos++] & 0xFF;
			} else {

				return current[pos++] & 0xFF;
			}
		} finally {
			if (current != null) {
				if (pos == current.length) {
					current = null;
					pos = 0;
				}
			}
		}

	}

	/*public int read(byte b[], int off, int len) throws IOException {
		if (len == 0) {
			return len;
		}
		int i = read();
		if (i == -1) {
			return -1;
		}
		b[off] = (byte) i;
		return 1;
	}*/
	
	/*public synchronized int read(byte b[], int off, int len) throws IOException {
		if (len == 0) {
			return len;
		}
		
		int currOff = off;
		int readCnt = 0;
		
		if (current != null) {
			int i;
			while (pos > 0 && readCnt < len) {
				i = read();
				if (i == -1) {
					return (readCnt > 0) ? readCnt : i;
				}
				b[currOff] = (byte) i;
				currOff++;
				readCnt++;
			}
		} else {
			int i = read();
			if (i == -1) {
				return i;
			}
			b[currOff] = (byte) i;
			currOff++;
			readCnt++;
			while (pos > 0 && readCnt < len) {
				i = read();
				if (i == -1) {
					return (readCnt > 0) ? readCnt : i;
				}
				b[currOff] = (byte) i;
				currOff++;
				readCnt++;
			}
		}
		
		return readCnt;
	}*/
	
	@Override
	public synchronized int read(byte b[], int off, int len) throws IOException {
		if (len == 0) {
			return len;
		}
		
		int currOff = off;
		int readCnt = 0;
		int i;
		
		if (current == null) {
			i = read();
			if (i == -1) {
				return i;
			}
			b[currOff] = (byte) i;
			currOff++;
			readCnt++;
		}
		
		while ((pos > 0 || !buffer.isEmpty()) && readCnt < len) {
			i = read();
			if (i == -1) {
				return (readCnt > 0) ? readCnt : i;
			}
			b[currOff] = (byte) i;
			currOff++;
			readCnt++;
		}
		
		return readCnt;
	}

	@Override
	public synchronized void close() throws IOException {
		isClosed = true;
		notifyAll();
	}

	public synchronized void add(byte[] data) {
		if (data.length > 0) {
			buffer.add(data);
			notify();
		}
	}

}
