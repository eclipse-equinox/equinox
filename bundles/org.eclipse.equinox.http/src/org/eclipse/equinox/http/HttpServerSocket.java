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
package org.eclipse.equinox.http;

import java.io.IOException;
import java.net.Socket;
import org.eclipse.equinox.socket.ServerSocketInterface;
import org.eclipse.equinox.socket.SocketInterface;

public class HttpServerSocket extends java.net.ServerSocket implements ServerSocketInterface {
	String address = null;

	/**
	 * HttpServerSocket constructor comment.
	 * @param port int
	 * @exception java.io.IOException The exception description.
	 */
	public HttpServerSocket(int port) throws java.io.IOException {
		super(port);
	}

	/**
	 * HttpServerSocket constructor comment.
	 * @param port int
	 * @param backlog int
	 * @exception java.io.IOException The exception description.
	 */
	public HttpServerSocket(int port, int backlog) throws java.io.IOException {
		super(port, backlog);
	}

	/**
	 * HttpServerSocket constructor comment.
	 * @param port int
	 * @param backlog int
	 * @param bindAddr java.net.InetAddress
	 * @exception java.io.IOException The exception description.
	 */
	public HttpServerSocket(int port, int backlog, java.net.InetAddress bindAddr) throws java.io.IOException {
		super(port, backlog, bindAddr);
	}

	/**
	 * This method was created in VisualAge.
	 */
	public Socket accept() throws IOException {
		return (Socket) acceptSock();
	}

	public SocketInterface acceptSock() throws IOException {
		HttpSocket socket = new HttpSocket(getScheme());
		implAccept(socket);
		return (socket);
	}

	public String getScheme() {
		return ("http"); //$NON-NLS-1$
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

}
