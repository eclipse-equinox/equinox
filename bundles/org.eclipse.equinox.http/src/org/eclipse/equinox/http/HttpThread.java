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
import java.io.InterruptedIOException;
import java.net.SocketException;
import javax.servlet.ServletException;
import org.eclipse.equinox.socket.SocketInterface;

/**
 * The class provide a thread for processing HTTP requests.
 */
public class HttpThread extends Thread {
	/** Master HTTP object */
	protected Http http;

	/** if true this thread must terminate */
	protected volatile boolean running;

	/** Pool to which this thread belongs. */
	protected HttpThreadPool pool;

	/** socket that this thread is operating */
	private SocketInterface socket;

	/** Listener this thread is working for */
	private HttpListener listener;

	/** if true, we support Keep-Alive for the socket */
	private boolean supportKeepAlive;

	/**
	 * HttpThread constructor.
	 */
	public HttpThread(Http http, HttpThreadPool pool, String name) {
		super(pool, name);

		this.http = http;
		this.pool = pool;

		setDaemon(true); /* mark thread as daemon thread */
	}

	/**
	 * Returns true if this thread has been closed.
	 * @return boolean
	 */
	public boolean isClosed() {
		return (!running);
	}

	/**
	 * Close this thread.
	 */
	public synchronized void close() {
		running = false;

		if (socket == null) {
			interrupt();
		} else {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * recall this thread.
	 */
	public synchronized void recall() {
		if (Http.DEBUG) {
			http.logDebug(getName() + ": recall on socket: " + socket); //$NON-NLS-1$

		}

		if ((socket != null) && !socket.isActive()) {
			try {
				if (Http.DEBUG) {
					http.logDebug(getName() + ": Closing socket: " + socket); //$NON-NLS-1$
				}

				socket.close();
			} catch (IOException e) {
			}
		}

		supportKeepAlive = false;
	}

	public synchronized void handleConnection(HttpListener listener, SocketInterface socket, int socketTimeout) {
		if (running) {
			this.listener = listener;
			this.socket = socket;

			if (socketTimeout > 0) {
				try {
					socket.setSoTimeout(socketTimeout);

					supportKeepAlive = true;
				} catch (SocketException e) {
					supportKeepAlive = false;
				}
			} else {
				supportKeepAlive = false;
			}

			notify();
		}
	}

	public void run() {
		running = true;

		while (running) {
			if (socket == null) {
				/*
				 synchronized (this)
				 {
				 pool.putThread(this);

				 * Synchronizing on this before putThread
				 * causes deadlock when security manager causes
				 * Thread.isAlive() calls
				 */

				pool.putThread(this);

				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
			}

			if (running && (socket != null)) {
				boolean keepAlive = false;

				try {
					if (Http.DEBUG) {
						http.logDebug(getName() + ": Processing request on socket: " + socket); //$NON-NLS-1$
					}

					socket.markInactive();

					listener.handleConnection(socket);

					keepAlive = supportKeepAlive && !socket.isClosed();
				} catch (InterruptedIOException e) {
					/* A read on the socket did not complete within the timeout period.
					 */
					keepAlive = false;

					if (Http.DEBUG) {
						http.logDebug(getName() + ": Read Timeout while processing connection on socket: " + socket, e); //$NON-NLS-1$
					}
				} catch (SocketException e) {
					/* Most likely the user agent closed the socket.
					 */
					keepAlive = false;

					if (Http.DEBUG) {
						http.logDebug(getName() + ": Socket Exception while processing connection on socket: " + socket, e); //$NON-NLS-1$
					}
				}
				// BUGBUG Need to handle UnavailableException
				// Servlet 2.2 Section 3.3.3.2
				// BUGBUG An unhandled exception should result in flushing the response
				// buff and returning status code 500.
				// Servlet 2.3 Section 9.9.2
				catch (ServletException e) {
					/* The Servlet threw a ServletException.
					 */
					keepAlive = false;

					http.logWarning(HttpMsg.HTTP_SERVLET_EXCEPTION, e);
				} catch (IOException e) {
					/* The Servlet threw an IOException.
					 */
					keepAlive = false;

					http.logWarning(HttpMsg.HTTP_CONNECTION_EXCEPTION, e);
				} catch (Throwable t) {
					/* Some exception has occurred. Log it and keep
					 * the thread working.
					 */
					keepAlive = false;

					http.logError(HttpMsg.HTTP_CONNECTION_EXCEPTION, t);
				} finally {
					if (!keepAlive) {
						if (!socket.isClosed()) {
							try {
								if (Http.DEBUG) {
									http.logDebug(getName() + ": Closing socket: " + socket); //$NON-NLS-1$
								}

								socket.close();
							} catch (IOException e) {
							}
						}

						socket = null;
						listener = null;
					}
				}
			}
		}
	}
}
