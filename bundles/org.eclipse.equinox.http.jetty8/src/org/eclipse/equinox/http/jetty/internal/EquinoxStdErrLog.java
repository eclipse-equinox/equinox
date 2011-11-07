/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.jetty.internal;

import org.eclipse.jetty.util.log.*;

// NOTE: This class simply allows us to override the StdErrLog built into jetty
public class EquinoxStdErrLog implements Logger {

	private static int DEBUG = 0;
	private static int INFO = 1;
	private static int WARN = 2;
	private static int ERROR = 3;
	private static int OFF = 4;

	private static volatile int threshold = WARN;
	private static EquinoxStdErrLog root;

	private final Logger realLogger;
	private final String localName;

	public static synchronized EquinoxStdErrLog getRootLogger() {
		if (root != null)
			return root;

		root = new EquinoxStdErrLog(null, null);
		return root;
	}

	public static void setThresholdLogger(String property) {
		threshold = parseThresholdProperty(property);
		// this is a hack to make sure the built-in jetty StdErrLog is not being used
		org.eclipse.jetty.util.log.Logger rootLogger = Log.getRootLogger();
		if (rootLogger == null || (rootLogger instanceof StdErrLog)) {
			// The built-in jetty StdErrLog is be used; replace with ours.
			Log.setLog(getRootLogger());
		}

	}

	private static int parseThresholdProperty(String property) {
		if (property == null)
			return WARN;

		if (property.equals("debug")) //$NON-NLS-1$
			return DEBUG;

		if (property.equals("info")) //$NON-NLS-1$
			return INFO;

		if (property.equals("warn")) //$NON-NLS-1$
			return WARN;

		if (property.equals("error")) //$NON-NLS-1$
			return ERROR;

		if (property.equals("none")) //$NON-NLS-1$
			return OFF;

		return WARN;
	}

	public EquinoxStdErrLog(String name, Logger realLogger) {
		this.localName = name;
		this.realLogger = realLogger == null ? new StdErrLog(name) : realLogger;
		if (threshold == DEBUG)
			this.realLogger.setDebugEnabled(true);
	}

	public org.eclipse.jetty.util.log.Logger getLogger(String name) {
		if ((name == null && this.localName == null) || (name != null && name.equals(this.localName)))
			return this;
		return new EquinoxStdErrLog(name, realLogger.getLogger(name));
	}

	// debugSOO = slf4j.getMethod("debug", new Class[]{String.class,Object.class,Object.class});
	public void debug(String msg, Object... arg0) {
		if (threshold > DEBUG)
			return;

		realLogger.debug(msg, arg0);
	}

	// debugST = slf4j.getMethod("debug", new Class[]{String.class,Throwable.class});
	public void debug(String msg, Throwable th) {
		if (threshold > DEBUG)
			return;

		realLogger.debug(msg, th);
	}

	// infoSOO = slf4j.getMethod("info", new Class[]{String.class,Object.class,Object.class});
	public void info(String msg, Object... arg0) {
		if (threshold > INFO)
			return;

		realLogger.info(msg, arg0);
	}

	// warnSOO = slf4j.getMethod("warn", new Class[]{String.class,Object.class,Object.class});
	public void warn(String msg, Object... arg0) {
		if (threshold > WARN)
			return;

		realLogger.warn(msg, arg0);
	}

	// warnST = slf4j.getMethod("warn", new Class[]{String.class,Throwable.class});
	public void warn(String msg, Throwable th) {
		if (threshold > WARN)
			return;

		// we treat RuntimeException and Error as an error
		if (th instanceof RuntimeException || th instanceof Error)
			realLogger.warn("ERROR:  " + msg, th); //$NON-NLS-1$
		else if (threshold != ERROR)
			realLogger.warn(msg, th);
	}

	// errorST = slf4j.getMethod("error", new Class[]{String.class,Throwable.class});
	public void error(String msg, Throwable th) {
		if (threshold > ERROR)
			return;

		realLogger.warn("ERROR:  " + msg, th); //$NON-NLS-1$
	}

	public String getName() {
		return realLogger.getName();
	}

	public void warn(Throwable thrown) {
		if (threshold > WARN)
			return;
		realLogger.warn(thrown);
	}

	public void info(Throwable thrown) {
		if (threshold > INFO)
			return;
		realLogger.info(thrown);
	}

	public void info(String msg, Throwable thrown) {
		if (threshold > INFO)
			return;
		realLogger.info(msg, thrown);
	}

	public boolean isDebugEnabled() {
		return threshold == DEBUG;
	}

	public void setDebugEnabled(boolean enabled) {
		threshold = DEBUG;
	}

	public void debug(Throwable thrown) {
		if (threshold > DEBUG)
			return;
		realLogger.debug(thrown);
	}

	public void ignore(Throwable ignored) {
		// Just post this to debug
		debug(ignored);
	}
}
