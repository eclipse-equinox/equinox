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
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Vector;
import org.eclipse.equinox.http.servlet.HttpSessionImpl;
import org.eclipse.equinox.socket.ServerSocketInterface;
import org.eclipse.equinox.socket.https.HttpsServerSocket;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class Http {
	public static final boolean DEBUG = false;
	BundleContext context;
	private LogTracker log = null;

	protected HttpConfiguration configuration;

	/** Mapping of Session ID (String) => HttpSessionImpl */
	protected Hashtable sessions;

	protected Vector servlets;
	protected StaticDataReader dataReader;

	HttpSecurityTracker securityTracker;

	protected Http(BundleContext context) throws Exception {
		this.context = context;

		initialize();
	}

	/**
	 * Initializes fields.
	 * <p>
	 * Added for subclassing purposes
	 */
	protected void initialize() throws Exception {
		log = new LogTracker(context, System.out);

		securityTracker = new HttpSecurityTracker(context);

		servlets = new Vector(15);

		//initialize sessions Hashtable
		sessions = new Hashtable(15);

		//get a MIMETypes object to compute MIMETypes
		dataReader = new StaticDataReader(this);
	}

	protected void start() throws Exception {
		if (Http.DEBUG) {
			logDebug("Starting Http Service bundle"); //$NON-NLS-1$
		}

		try {
			configuration = new HttpConfiguration(this);
		} catch (IOException e) {
			logError("Http Service failed to start -- HALTING", e); //$NON-NLS-1$
			stop();
			throw e;
		}

		if (Http.DEBUG) {
			logInfo("Http Service started"); //$NON-NLS-1$
		}
	}

	protected void stop() {
		if (Http.DEBUG) {
			logDebug("Stopping Http Service bundle"); //$NON-NLS-1$
		}

		if (configuration != null) {
			configuration.close();
			configuration = null;
		}

		if (Http.DEBUG) {
			logDebug("Http Service stopped"); //$NON-NLS-1$
		}
	}

	protected void close() {

		if (securityTracker != null) {
			securityTracker.close();
			securityTracker = null;
		}

		if (log != null) {
			log.close();
			log = null;
		}
	}

	/**
	 * @param session javax.servlet.http.HttpSession
	 */
	public void addSession(HttpSessionImpl session) {
		sessions.put(session.getId(), session);
	}

	public void removeSession(HttpSessionImpl session) {
		sessions.remove(session.getId());
	}

	/**
	 * Returns a valid session from the cache.
	 *
	 * @param id ID of requested session.
	 * @return Valid session object.
	 */
	public HttpSessionImpl getSession(String id) {
		HttpSessionImpl session = (HttpSessionImpl) sessions.get(id);

		if ((session != null) && (session.isValid(true))) {
			return (session); /* session is valid */
		}
		/* session is null or invalid and has removed itself from the cache */

		return (null);
	}

	/**
	 * @return MIMETypes
	 */
	public String getMimeType(String name) {
		return (dataReader.computeMimeType(name));
	}

	/**
	 * @return java.lang.String
	 * @param statusCode java.lang.String
	 */
	public String getStatusPhrase(int statusCode) {
		return (dataReader.computeStatusPhrase(statusCode));
	}

	public void logDebug(String message) {
		if (Http.DEBUG) {
			System.out.println(message);

			log.log(log.LOG_DEBUG, message);
		}
	}

	public void logDebug(String message, Throwable t) {
		if (Http.DEBUG) {
			System.out.println(message);
			t.printStackTrace(System.out);

			log.log(log.LOG_DEBUG, message, t);
		}
	}

	public void logError(String message, Throwable t) {
		if (Http.DEBUG) {
			System.out.println(message);
			t.printStackTrace(System.out);
		}

		log.log(log.LOG_ERROR, message, t);
	}

	public void logInfo(String message) {
		if (Http.DEBUG) {
			System.out.println(message);
		}

		log.log(log.LOG_INFO, message);
	}

	public void logWarning(String message, Throwable t) {
		if (Http.DEBUG) {
			System.out.println(message);
			t.printStackTrace(System.out);
		}

		log.log(log.LOG_WARNING, message, t);
	}

	protected ServerSocketInterface createSSLServerSocket(int port, int backlog, InetAddress address) throws IOException {
		HttpsServerSocket socket;
		try {
			socket = new HttpsServerSocket(context, port, backlog, address);
		} catch (UnsupportedOperationException ex) {
			throw new IOException(NLS.bind(HttpMsg.HTTP_INVALID_SCHEME_EXCEPTION, "https"));  //$NON-NLS-1$
		}
		return (socket);
	}

}
