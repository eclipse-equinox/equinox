/*******************************************************************************
 * Copyright (c) 1999, 2006 IBM Corporation and others.
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

/**
 * The class provide a thread for processing HTTP requests.
 */
/* @ThreadSafe */
public class HttpThread extends Thread {
	/** Master HTTP object */
	private final Http http;

	/** if true this thread must terminate */
	private volatile boolean running;

	/** Pool to which this thread belongs. */
	private final HttpThreadPool pool;

	/** connection that this thread is operating */
	private volatile HttpConnection conn;

	/** lock object to wait for work */
	private final Object waitLock = new Object();

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
	public void close() {
		running = false;

		if (conn == null) {
			interrupt();
		} else {
			try {
				conn.close();
			} catch (IOException e) {
				// TODO: consider logging
			}
		}
	}

	/**
	 * recall this thread.
	 */
	public void recall() {
		if (Http.DEBUG) {
			http.logDebug(getName() + ": recall on socket: " + conn); //$NON-NLS-1$
		}

		if (conn != null) {
			conn.setKeepAlive(false);	/* disable keep alive in case the connection is currently processing a request */
			if (!conn.isActive()) {		/* if the connection is not processing a request, close it */
				try {
					if (Http.DEBUG) {
						http.logDebug(getName() + ": Closing socket: " + conn); //$NON-NLS-1$
					}
					conn.close();
				} catch (IOException e) {
					// TODO: consider logging
				}
			}
		}
	}

	/**
	 * Set the connection for this thread to process. The thread must have just been 
	 * retreived from the thread pool.
	 * @param connParam The HttpConnection to process.
	 */
	public void handleConnection(HttpConnection connParam) {
		if (running) {
			this.conn = connParam;
			synchronized (waitLock) {
				waitLock.notify();
			}
		}
	}

	public void run() {
		running = true;

		while (running) {
			if (conn == null) {			/* if we have no work to do, wait in the pool */
				synchronized (waitLock) {
					pool.putThread(this);

					try {
						waitLock.wait();
					} catch (InterruptedException e) {
						// ignore and check exit condition
					}
				}
			}

			if (running && (conn != null)) {
				conn.run();		/* execute the connection */

				if (conn.isClosed()) {	/* if connection is closed */
			    	conn = null;		/* go back to the pool and wait */
			    }
			}
		}
	}
}
