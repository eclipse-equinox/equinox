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
package org.eclipse.equinox.socket;

/** SocketInterface.java
 *
 *
 */

import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;

public interface SocketInterface {
	/* Standard java.net.Socket methods */
	public void close() throws IOException;

	public InetAddress getInetAddress();

	public InetAddress getLocalAddress();

	public int getLocalPort();

	public InputStream getInputStream() throws IOException;

	public OutputStream getOutputStream() throws IOException;

	public void setSoTimeout(int timeout) throws SocketException;

	public int getSoTimeout() throws SocketException;

	/* Http Methods */
	/**
	 * Return the scheme this socket is using.
	 *
	 * @return Either "http" or "https".
	 */
	public String getScheme();

	/**
	 * Test to see if the socket has been closed.
	 *
	 * @return true if close has been called on this socket.
	 */
	public boolean isClosed();

	/**
	 * Test to see if the socket is active.
	 *
	 * @return true if markActive has been called.
	 */
	public boolean isActive();

	/**
	 * Mark the socket active.
	 *
	 */
	public void markActive();

	/**
	 * Mark the socket inactive.
	 *
	 */
	public void markInactive();
}
