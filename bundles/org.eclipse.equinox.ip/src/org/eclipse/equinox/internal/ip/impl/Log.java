/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip.impl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Dictionary;
import org.osgi.service.log.LogService;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * Simple debug class for provisioning agent bundle.
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class Log {

	/**
	 * Property for workaround the J9 2.0 problem with Content-Length, the
	 * content length is not set if property is "true"
	 */
	public static boolean j9workAround;

	/** If debug mode is on. */
	public static boolean debug;
	/** If debug mode is on. */
	public static boolean remoteDebug;
	/** If to send trace */
	public static boolean sendTrace;

	/** Message that trace is not send */
	public static byte[] NO_TRACE = "NoTrace".getBytes();

	/** Reference to provisioning service */
	public static ProvisioningService prvSrv;

	/** Log stream to receive logged messages. */
	private static PrintStream logStream;

	/** org.eclipse.equinox.internal.util.ref.Log class to receive logged messages */
	public static org.eclipse.equinox.internal.util.ref.Log log;

	static {
		try {
			String logFile = ProvisioningAgent.bc.getProperty("equinox.provisioning.provisioning.logfile");
			if (logFile != null && (logFile = logFile.trim()).length() != 0) {
				File log = new File(logFile);
				log = log.isDirectory() ? new File(log, "log.txt") : log;
				logStream = new PrintStream(new FileOutputStream(log), true);
			}
		} catch (Exception e) {
			logStream = null;
		}
	}

	/**
	 * Dumps a debug string
	 * 
	 * @param obj
	 *            a debug string
	 */
	public static void debug(String obj) {
		String message = "[PROVISIONING] " + obj;
		if (debug && logStream != null) {
			logStream.println(message);
		} else if (log != null) {
			log.debug(message, null);
		}
	}

	/**
	 * Dumps a debug exception
	 * 
	 * @param e
	 *            a debugged Throwable
	 */
	public static void debug(Throwable e) {
		debug(null, e, remoteDebug);
	}

	/**
	 * Dumps an exception if debug mode is enabled
	 * 
	 * @param e
	 *            the exception to dump
	 */
	public static void debug(String message, Throwable e) {
		debug(message, e, remoteDebug);
	}

	private static void debug(String message, Throwable e, boolean sendRemote) {
		if (debug && logStream != null) {
			logStream.println("[PROVISIONING] " + message);
			e.printStackTrace(logStream);
		} else if (log != null) {
			log.debug(message, e);
		}
		if (sendRemote && e != null && (e.getMessage() == null || e.getMessage().indexOf("Error from Backend") == -1)) {
			log(message, e);
		}
	}

	/**
	 * Logs an exception
	 * 
	 * @param e
	 *            the exception
	 */
	private static void log(String message, Throwable e) {
		debug("Log exception remotely.");
		String logUrl = null;
		try {
			ProvisioningService prvSrv = Log.prvSrv;
			if (prvSrv != null) {
				Dictionary info = prvSrv.getInformation();
				if (info != null) {
					logUrl = (String) info.get("equinox.provisioning.prv.log");
					if (logUrl != null) {
						debug("Log url = " + logUrl);
						HttpURLConnection conn = (HttpURLConnection) new URL(logUrl).openConnection();
						conn.setRequestMethod("POST");
						conn.setRequestProperty("Connection", "close");
						conn.setRequestProperty(ProvisioningService.PROVISIONING_SPID, info.get(ProvisioningService.PROVISIONING_SPID) + "");
						conn.setRequestProperty("msg", (message != null ? message + ": " : "") + e.getMessage());
						conn.setRequestProperty("lvl", LogService.LOG_ERROR + "");

						OutputStream os = new ByteArrayOutputStream();
						if (sendTrace) {
							e.printStackTrace(new PrintStream(os));
						} else {
							os.write(NO_TRACE);
						}
						byte[] stackTrace = ((ByteArrayOutputStream) os).toByteArray();
						if (!j9workAround) {
							conn.setRequestProperty("Content-Length", stackTrace.length + "");
						}

						conn.setDoOutput(true);
						conn.setDoInput(true);
						conn.setUseCaches(false);
						conn.connect();

						os = conn.getOutputStream();
						os.write(stackTrace);
						os.flush();

						InputStream is = conn.getInputStream();
						while (is.read() != -1) {
						}
					}
				}
			}
		} catch (Throwable t) {
			debug("Error while logging remotely to url \"" + logUrl + '"', t, false);
		}
	}
}
