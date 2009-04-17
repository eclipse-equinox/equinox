/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.slf4j;

import org.mortbay.log.Log;
import org.mortbay.log.StdErrLog;

// NOTE: This class is not a real SLF4J implementation and MUST NOT be exported as a general implementation!
// It is a place-holder to allow overriding the default logging done in Jetty
// See org.mortbay.log.Log and org.mortbay.log.Slf4jLog 
public class Logger extends StdErrLog {

	private static int DEBUG = 0;
	private static int INFO = 1;
	private static int WARN = 2;
	private static int ERROR = 3;
	private static int OFF = 4;

	private static volatile int threshold = WARN;
	private static Logger root;

	private String localName;

	public static synchronized Logger getRootLogger() {
		if (root != null)
			return root;

		root = new Logger(null);
		return root;
	}

	public static void setThresholdLogger(String property) {
		threshold = parseThresholdProperty(property);
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(Logger.class.getClassLoader());
		try {
			// if this logger class is being used this call will trigger the LoggerFactory to call getRootLogger
			Log.getLog();
			if (root != null)
				Log.setLog(root);
		} finally {
			Thread.currentThread().setContextClassLoader(current);
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

	public Logger(String name) {
		super(name);
		localName = name;
		if (threshold == DEBUG)
			setDebugEnabled(true);
	}

	public org.mortbay.log.Logger getLogger(String name) {
		if ((name == null && this.localName == null) || (name != null && name.equals(this.localName)))
			return this;
		return new Logger(name);
	}

	// debugSOO = slf4j.getMethod("debug", new Class[]{String.class,Object.class,Object.class});
	public void debug(String msg, Object arg0, Object arg1) {
		if (threshold > DEBUG)
			return;

		super.debug(msg, arg0, arg1);
	}

	// debugST = slf4j.getMethod("debug", new Class[]{String.class,Throwable.class});
	public void debug(String msg, Throwable th) {
		if (threshold > DEBUG)
			return;

		super.debug(msg, th);
	}

	// infoSOO = slf4j.getMethod("info", new Class[]{String.class,Object.class,Object.class});
	public void info(String msg, Object arg0, Object arg1) {
		if (threshold > INFO)
			return;

		super.info(msg, arg0, arg1);
	}

	// warnSOO = slf4j.getMethod("warn", new Class[]{String.class,Object.class,Object.class});
	public void warn(String msg, Object arg0, Object arg1) {
		if (threshold > WARN)
			return;

		super.warn(msg, arg0, arg1);
	}

	// warnST = slf4j.getMethod("warn", new Class[]{String.class,Throwable.class});
	public void warn(String msg, Throwable th) {
		if (threshold > WARN)
			return;

		// we treat RuntimeException and Error as an error
		if (th instanceof RuntimeException || th instanceof Error)
			super.warn("ERROR:  " + msg, th); //$NON-NLS-1$
		else if (threshold != ERROR)
			super.warn(msg, th);
	}

	// errorST = slf4j.getMethod("error", new Class[]{String.class,Throwable.class});
	public void error(String msg, Throwable th) {
		if (threshold > ERROR)
			return;

		super.warn("ERROR:  " + msg, th); //$NON-NLS-1$
	}
}
