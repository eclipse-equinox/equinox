/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
import java.io.InterruptedIOException;
import java.net.SocketException;
import javax.servlet.ServletException;
import org.eclipse.equinox.socket.SocketInterface;

/* @ThreadSafe */
public class HttpConnection implements Runnable {
	/** Master HTTP object */
	private final Http http;

	/** socket that this thread is operating */
	private final SocketInterface socket;

	/** Listener this thread is working for */
	private final HttpListener listener;

	/** if true, we support Keep-Alive for the socket */
	private volatile boolean supportKeepAlive;

	public HttpConnection(Http http, HttpListener listenerParam, SocketInterface socketParam, int socketTimeout) {
		this.http = http;
		this.listener = listenerParam;
		this.socket = socketParam;
		boolean keepAlive;
		if (socketTimeout > 0) {
			try {
				socketParam.setSoTimeout(socketTimeout);

				keepAlive = true;
			} catch (SocketException e) {
				keepAlive = false;
			}
		} else {
			keepAlive = false;
		}

		setKeepAlive(keepAlive);
	}

	public void run() {
		boolean keepAlive = false;

		try {
			if (Http.DEBUG) {
				http.logDebug(Thread.currentThread().getName() + ": Processing request on socket: " + socket); //$NON-NLS-1$
			}

			socket.markInactive(); /* mark inactive: we are not actively processing a request */

			listener.handleConnection(socket);

			keepAlive = supportKeepAlive && !socket.isClosed();
		} catch (InterruptedIOException e) {
			/*
			 * A read on the socket did not complete within the timeout period.
			 */
			keepAlive = false;

			if (Http.DEBUG) {
				http.logDebug(Thread.currentThread().getName() + ": Read Timeout while processing connection on socket: " + socket, e); //$NON-NLS-1$
			}
		} catch (SocketException e) {
			/*
			 * Most likely the user agent closed the socket.
			 */
			keepAlive = false;

			if (Http.DEBUG) {
				http.logDebug(Thread.currentThread().getName() + ": Socket Exception while processing connection on socket: " + socket, e); //$NON-NLS-1$
			}
		}
		// BUGBUG Need to handle UnavailableException
		// Servlet 2.2 Section 3.3.3.2
		// BUGBUG An unhandled exception should result in flushing the response
		// buff and returning status code 500.
		// Servlet 2.3 Section 9.9.2
		catch (ServletException e) {
			/*
			 * The Servlet threw a ServletException.
			 */
			keepAlive = false;

			http.logWarning(HttpMsg.HTTP_SERVLET_EXCEPTION, e);
		} catch (IOException e) {
			/*
			 * The Servlet threw an IOException.
			 */
			keepAlive = false;

			http.logWarning(HttpMsg.HTTP_CONNECTION_EXCEPTION, e);
		} catch (Throwable t) {
			/*
			 * Some exception has occurred. Log it and keep the thread working.
			 */
			keepAlive = false;

			http.logError(HttpMsg.HTTP_CONNECTION_EXCEPTION, t);
		} finally {
			if (!keepAlive) {
				if (!socket.isClosed()) {
					try {
						if (Http.DEBUG) {
							http.logDebug(Thread.currentThread().getName() + ": Closing socket: " + socket); //$NON-NLS-1$
						}

						socket.close();
					} catch (IOException e) {
						// TODO: consider logging
					}
				}
			}
		}
	}

	public void setKeepAlive(boolean keepAlive) {
		supportKeepAlive = keepAlive;
	}

	public boolean isClosed() {
		return socket.isClosed();
	}

	public void close() throws IOException {
		socket.close();
	}

	public boolean isActive() {
		return socket.isActive();
	}

	public String toString() {
		return socket.toString();
	}
}
