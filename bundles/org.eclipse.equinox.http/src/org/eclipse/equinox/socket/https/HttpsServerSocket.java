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

import java.io.IOException;
import java.net.*;
import javax.net.ServerSocketFactory;
import org.eclipse.equinox.socket.ServerSocketInterface;
import org.eclipse.equinox.socket.SocketInterface;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class HttpsServerSocket implements ServerSocketInterface, ServiceTrackerCustomizer {

	protected String address = null;
	protected BundleContext context = null;
	protected int port = 443;
	protected int backlog = 50;
	protected InetAddress bindAddr = null;

	protected ServiceTracker st = null;
	protected ServerSocketFactory ssf = null;
	protected ServerSocket ss = null;
	protected ServerSocket dummyss = null;

	/**
	 * Constructor for HttpsServerSockets
	 */
	public HttpsServerSocket(BundleContext context, int port) throws IOException {
		this(context, port, 50);
	}

	/**
	 * HttpServerSocket constructor comment.
	 * @param port int
	 * @param backlog int
	 * @exception java.io.IOException The exception description.
	 */
	public HttpsServerSocket(BundleContext context, int port, int backlog) throws java.io.IOException {
		this(context, port, backlog, null);
	}

	/**
	 * HttpServerSocket constructor comment.
	 * @param port int
	 * @param backlog int
	 * @param bindAddr java.net.InetAddress
	 * @exception java.io.IOException The exception description.
	 */
	public HttpsServerSocket(BundleContext context, int port, int backlog, InetAddress bindAddr) throws java.io.IOException {
		this.context = context;
		this.port = port;
		this.backlog = backlog;
		this.bindAddr = bindAddr;
		// Try to configure a normal ServerSocket with the settings to see
		// if the ServerSocket can be created.  If no exception is thrown
		// then the port is available
		dummyss = new ServerSocket(port, backlog, bindAddr);

		st = new ServiceTracker(context, "javax.net.ssl.SSLServerSocketFactory", this); //$NON-NLS-1$
		st.open();
	}

	/**
	 * @see ServerSocketInterface#acceptSock()
	 */
	public synchronized SocketInterface acceptSock() throws IOException {
		while (this.ssf == null) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new IOException();
			}
		}
		if (ss == null) {
			if (dummyss != null) {
				dummyss.close();
				dummyss = null;
			}
			ss = ssf.createServerSocket(port, backlog, bindAddr);
		}
		Socket socket = ss.accept();
		return new HttpsSocket(socket);
	}

	/**
	 * @see ServerSocketInterface#close()
	 */
	public void close() throws IOException {
		if (ss != null) {
			ss.close();
		}
	}

	/**
	 * @see ServerSocketInterface#getLocalPort()
	 */
	public int getLocalPort() {
		if (ss != null) {
			return ss.getLocalPort();
		}
		return 0;
	}

	/**
	 * @see ServerSocketInterface#getScheme()
	 */
	public String getScheme() {
		return ("https"); //$NON-NLS-1$
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public synchronized Object addingService(ServiceReference reference) {
		if (ssf == null) {
			ssf = (ServerSocketFactory) context.getService(reference);
			notify();
			return ssf;
		}
		return null;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public synchronized void removedService(ServiceReference reference, Object service) {
		ssf = null;
		if (ss != null) {
			try {
				ss.close();
			} catch (IOException e) {
			}
			ss = null;
		}
	}

}
