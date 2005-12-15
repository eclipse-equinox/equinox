/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.socket.https;

import java.io.*;
import java.net.*;
import org.eclipse.equinox.socket.SocketInterface;

public class HttpsSocket implements SocketInterface {
	protected Socket socket;
	protected boolean closed = false;
	private boolean active = false;
	private InputStream in = null;

	public HttpsSocket(Socket socket) {
		this.socket = socket;
	}

	public void close() throws IOException {
		closed = true;
		socket.close();
	}

	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}

	public InetAddress getLocalAddress() {
		return socket.getLocalAddress();
	}

	public int getLocalPort() {
		return socket.getLocalPort();
	}

	/**
	 * Wrap the real socket input stream in a buffered input stream
	 *
	 * @return a buffered InputStream which wraps the real input stream.
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		if (in == null) {
			synchronized (this) {
				if (in == null) {
					in = new BufferedInputStream(socket.getInputStream());
				}
			}
		}

		return in;
	}

	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	public void setSoTimeout(int timeout) throws SocketException {
		socket.setSoTimeout(timeout);
	}

	public int getSoTimeout() throws SocketException {
		return socket.getSoTimeout();
	}

	/**
	 * Return the scheme this socket is using.
	 *
	 * @return "https".
	 */
	public String getScheme() {
		return "https"; //$NON-NLS-1$
	}

	/**
	 * Test to see if the socket is closed.
	 *
	 * @return true if close has been called.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Test to see if the socket is active.
	 *
	 * @return true if markActive has been called.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Mark the socket active.
	 *
	 */
	public void markActive() {
		active = true;
	}

	/**
	 * Mark the socket inactive.
	 *
	 */
	public void markInactive() {
		active = false;
	}

}
