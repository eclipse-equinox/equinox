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
package org.eclipse.equinox.internal.util.ref;

import java.io.*;
import java.util.Vector;
import org.eclipse.equinox.internal.util.UtilActivator;
import org.eclipse.equinox.internal.util.event.Queue;
import org.eclipse.equinox.internal.util.hash.HashIntObjNS;
import org.eclipse.equinox.internal.util.security.PrivilegedRunner;
import org.eclipse.equinox.internal.util.security.SecurityUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Log class is responsible for forwarding bundles's messages to the LogService
 * or console output.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class Log implements LogInterface, ServiceTrackerCustomizer, PrivilegedRunner.PrivilegedDispatcher {

	/**
	 * Flag, pointing if printingOnConsole is allowed
	 * 
	 * @deprecated since osgilib verion 1.3.9 use set/get PrintOnConsole
	 */
	public boolean printOnConsole = false;

	/**
	 * Flag, pointing whether printing on console should be done if log service
	 * is not available
	 */
	public boolean autoPrintOnConsole = false;

	/**
	 * Flag, pointing if logging debuging info is allowed
	 * 
	 * @deprecated since osgilib verion 1.3.9 use set/get Debug
	 */
	public boolean debug = false;

	/**
	 * Flag, pointing whether printing on console should be done for errors
	 * (exceptions) and warnings
	 */
	private boolean logErrorLevel = false;

	private ServiceTracker logTracker;
	private ServiceReference traceRef;

	protected static final SecurityUtil securityUtil = new SecurityUtil();

	private boolean isClosed = false;

	private long bundleId;
	/** BundleContext to get LogService and service owner of Log object BundleId */
	protected BundleContext bc;
	private static Vector logs = new Vector();
	private static Log listener;

	public Log(BundleContext bc) {
		this(bc, true);
	}

	/**
	 * Constructs a log object, used for logging information in the LogService.
	 * If the LogService is unavailble, the information is printed on the
	 * server's console.
	 * 
	 * @param bc
	 *            BundleContext, necessary to get LogService, in case it is
	 *            started.
	 */
	public Log(BundleContext bc, boolean initDebug) {
		if (initDebug) {
			debug = securityUtil.getBooleanProperty("equinox.util.ref.log.debug");
			logErrorLevel = securityUtil.getBooleanProperty("equinox.log.errorlevel");
			autoPrintOnConsole = securityUtil.getBooleanProperty("equinox.util.ref.log.autoPrintOnConsole");
			printOnConsole = securityUtil.getBooleanProperty("equinox.util.ref.log.printOnConsole");
		}
		if (bc != null) {
			this.bc = bc;
			bundleId = bc.getBundle().getBundleId();

			if (UtilActivator.startup && UtilActivator.points != null)
				UtilActivator.points[0] = System.currentTimeMillis();

			initSysServices();

			if (UtilActivator.startup && UtilActivator.points != null)
				UtilActivator.points[1] = System.currentTimeMillis();
			synchronized (logs) {
				if (listener == null)
					initListener();
				logs.addElement(this);
			}

			if (UtilActivator.startup && UtilActivator.points != null)
				UtilActivator.points[2] = System.currentTimeMillis();
		} else
			printOnConsole = true;
	}

	private void initListener() {
		try {
			securityUtil.doPrivileged(this, OPEN_TYPE, null);
		} catch (IllegalStateException ise) {
			/* must be rethrown */
			throw ise;
		} catch (Throwable ignore) {
			ignore.printStackTrace();
		}
	}

	void initListener0() {
		synchronized (logs) {
			initListenerNS();
		}
	}

	void initListenerNS() {
		logTracker = new ServiceTracker(bc, "org.osgi.service.log.LogService", this);
		logTracker.open();
		listener = this;
	}

	/**
	 * Logs error messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param str
	 *            Message description of the error.
	 * @param ex
	 *            Throwable object, containing the stack trace; may be null.
	 */
	public void error(String str, Throwable ex) {
		if (isClosed)
			return;

		boolean logResult = logMessage(LogService.LOG_ERROR, str, ex);
		if (printOnConsole || (!logResult && autoPrintOnConsole) || logErrorLevel) {
			dumpOnConsole("ERROR ", str, bundleId, ex);
		}
	}

	/**
	 * Logs error messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param moduleID
	 * @param msgID
	 * @param msg
	 *            Message description of the error.
	 * @param t
	 *            Throwable object, containing the stack trace; may be null.
	 * @param synch
	 *            Weather tracer to work synchronious or not
	 */
	public void error(int moduleID, int msgID, String msg, Throwable t, boolean synch) {
		if (isClosed)
			return;

		boolean logResult = true;
		if (msg != null || t != null) {
			logResult = logMessage(LogService.LOG_ERROR, msg, t);
		}
		if (printOnConsole || (!logResult && autoPrintOnConsole) || logErrorLevel) {
			dumpOnConsole(buildDebugString(moduleID, msgID, msg, "ERROR " + bundleId + " "), t);
		}
	}

	/**
	 * Logs warning messages. If <code>printOnConsole</code> is true, or if
	 * the <code>LogService</code> is unavailable, log info is printed on
	 * console.
	 * 
	 * @param str
	 *            Message description of the error.
	 * @param ex
	 *            Throwable object, containing the stack trace; may be null.
	 */
	public void warning(String str, Throwable ex) {
		if (isClosed)
			return;

		boolean logResult = logMessage(LogService.LOG_WARNING, str, ex);
		if (printOnConsole || (!logResult && autoPrintOnConsole) || logErrorLevel) {
			dumpOnConsole("WARNING ", str, bundleId, ex);
		}
	}

	/**
	 * Logs warning messages. If <code>printOnConsole</code> is true, or if
	 * the <code>LogService</code> is unavailable, log info is printed on
	 * console.
	 * 
	 * @param moduleID
	 * @param msgID
	 * @param msg
	 *            Message description of the error.
	 * @param t
	 *            Throwable object, containing the stack trace; may be null.
	 * @param synch
	 *            Weather tracer to work synchronious or not
	 */
	public void warning(int moduleID, int msgID, String msg, Throwable t, boolean synch) {
		if (isClosed)
			return;

		boolean logResult = true;
		if (msg != null || t != null) {
			logResult = logMessage(LogService.LOG_WARNING, msg, t);
		}
		if (printOnConsole || (!logResult && autoPrintOnConsole) || logErrorLevel) {
			dumpOnConsole(buildDebugString(moduleID, msgID, msg, "WARNING " + bundleId + " "), t);
		}
	}

	/**
	 * Logs info messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, message is printed on console.
	 * 
	 * @param str
	 *            Message to be logged.
	 */
	public void info(String str) {
		if (isClosed)
			return;

		boolean logResult = logMessage(LogService.LOG_INFO, str, null);
		if (printOnConsole || (!logResult && autoPrintOnConsole)) {
			dumpOnConsole("INFO ", str, bundleId, null);
		}
	}

	/**
	 * Logs info messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param moduleID
	 * @param msgID
	 * @param msg
	 *            Message description of the error.
	 * @param synch
	 *            Weather tracer to work synchronious or not
	 */
	public void info(int moduleID, int msgID, String msg, boolean synch) {
		if (isClosed)
			return;

		boolean logResult = true;
		if (msg != null) {
			logResult = logMessage(LogService.LOG_INFO, msg, null);
		}
		if (printOnConsole || (!logResult && autoPrintOnConsole)) {
			dumpOnConsole(buildDebugString(moduleID, msgID, msg, "INFO " + bundleId + " "), null);
		}
	}

	/**
	 * Logs debug information if <code>debug</code> flag is true. If
	 * LogService is unaccessible or printOnConsole flag is true, log info is
	 * printed on console.
	 * 
	 * @param str
	 *            Message description.
	 * @param ex
	 *            Throwable object, containing the stack trace; may be null.
	 */
	public void debug(String str, Throwable ex) {
		if (!debug || isClosed)
			return;

		boolean logResult = logMessage(LogService.LOG_DEBUG, str, ex);
		if (printOnConsole || (!logResult && autoPrintOnConsole)) {
			dumpOnConsole("DEBUG ", str, bundleId, ex);
		}
	}

	/**
	 * Logs debug messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param moduleID
	 * @param msgID
	 * @param msg
	 *            Message description of the error.
	 * @param t
	 *            Throwable object, containing the stack trace; may be null.
	 * @param synch
	 *            Indicates whether tracer should log the message synchronously
	 *            or not
	 */
	public void debug(int moduleID, int msgID, String msg, Throwable t, boolean synch) {
		debug(moduleID, msgID, msg, t, synch, false, false, true);
	}

	/**
	 * Logs debug messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param moduleID
	 * @param msgID
	 * @param msg
	 *            Message description of the error.
	 * @param t
	 *            Throwable object, containing the stack trace; may be null.
	 * @param synch
	 *            Indicates whether tracer should log the message synchronously
	 *            or not
	 * @param measurement
	 *            Indicates whether the message is a measurement or not
	 */
	public void debug(int moduleID, int msgID, String msg, Throwable t, boolean synch, boolean measurement) {
		debug(moduleID, msgID, msg, t, synch, measurement, false, true);
	}

	/**
	 * Logs debug messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param moduleID
	 * @param msgID
	 * @param msg
	 *            Message description of the error.
	 * @param t
	 *            Throwable object, containing the stack trace; may be null.
	 * @param synch
	 *            Indicates whether tracer should log the message synchronously
	 *            or not
	 * @param measurement
	 *            Indicates whether the message is a measurement or not
	 * @param display
	 *            Indicates whether the message should be displayed in native
	 *            GUI
	 */
	public void debug(int moduleID, int msgID, String msg, Throwable t, boolean synch, boolean measurement, boolean display) {
		debug(moduleID, msgID, msg, t, synch, measurement, display, true);
	}

	/**
	 * Logs debug messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param moduleID
	 * @param msgID
	 * @param msg
	 *            Message description of the error.
	 * @param t
	 *            Throwable object, containing the stack trace; may be null.
	 * @param synch
	 *            Indicates whether tracer should log the message synchronously
	 *            or not
	 * @param measurement
	 *            Indicates whether the message is a measurement or not
	 * @param display
	 *            Indicates whether the message should be displayed in native
	 *            GUI
	 * @param logInFile
	 *            Indicates whether the message should be logged into the log
	 *            file or not. Used for measurements' logs.
	 */
	public void debug(int moduleID, int msgID, String msg, Throwable t, boolean synch, boolean measurement, boolean display, boolean logInFile) {
		if (!debug && !measurement || isClosed)
			return;

		String message = msg;
		if (measurement) {
			message = buildDebugString(moduleID, msgID, msg, "DEBUG " + bundleId + " ");
		}

		boolean logResult = logInFile ? true : (message == null && t == null);
		if (logInFile && (message != null || t != null)) {
			logResult = logMessage(LogService.LOG_DEBUG, message, t);
		}

		/* Checks are added for different framework implementations. */
		if (printOnConsole || (!logResult && autoPrintOnConsole)) {
			message = buildDebugString(moduleID, msgID, msg, "DEBUG " + bundleId + " ");
			dumpOnConsole(message, t);
		}
	}

	private void initSysServices() {
		if (security) {
			try {
				securityUtil.doPrivileged(this, GET_SYS_SERVICES_TYPE, null);
			} catch (Throwable ignore) {
				ignore.printStackTrace();
			}
		}
	}

	LogService getService0() throws IllegalArgumentException {
		synchronized (logs) {
			ServiceReference logRef = (ServiceReference) listener.logTracker.getService();
			LogService ls = null;
			if (logRef != null) {
				ls = (LogService) bc.getService(logRef);
			}
			return ls;
		}
	}

	private LogService getService() throws IllegalArgumentException {
		if (bc == null)
			return null; // standalone hack
		try {
			return (LogService) securityUtil.doPrivileged(this, GET_SERVICE_TYPE, null);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			/* this will not happen */
			e.printStackTrace();
			throw new IllegalArgumentException(e.toString());
		}
	}

	private boolean logMessage(int messageType, String messageText, Throwable t) {
		LogService ls = null;

		try {
			ls = getService();
		} catch (IllegalStateException ise) { // invalid bundle context
			synchronized (logs) {
				close0();
			}
			return false;
		}
		boolean result = true;
		if (ls == null) {
			result = false;
			testClose(); // test the listener
		} else {
			try {
				ls.log(messageType, messageText, t);
			} catch (IllegalStateException ise) { // the log service instance
				// is not valid
				result = false;
				testClose(); // test the listener
			}
		}
		return result;
	}

	private void dumpOnConsole(String prefix, String msg, long bundleId, Throwable t) {
		System.out.println(prefix + bundleId + " " + msg);
		if (t != null) {
			t.printStackTrace();
		}
	}

	/**
	 * Prints a message to the console or a log dispatcher
	 * 
	 * @param msg -
	 *            the message to print, which contains the prefix and the
	 *            bundleID in itself
	 * @param t
	 *            throwable object, which stack trace should be printed
	 */
	private void dumpOnConsole(String msg, Throwable t) {
		System.out.println(msg);
		if (t != null) {
			t.printStackTrace();
		}
	}

	private void testClose() {
		synchronized (logs) {
			if (listener != null)
				try {
					listener.bc.getBundle();
					return;
				} catch (IllegalStateException ise) {
					listener.close0();
				}
		}
	}

	/**
	 * Releases the Log's resources: ungets LogService, removes the
	 * ServiceListener from the framework and nulls references. After invocation
	 * of this method, this Log object can be used no longer.
	 */
	public void close() {
		if (bc != null) {
			if (traceRef != null) {
				bc.ungetService(traceRef);
				traceRef = null;
			}
			synchronized (logs) {
				close0();
			}
		}
	}

	private boolean close0() {
		logs.removeElement(this);
		isClosed = true;
		if (listener == this) {
			try {
				logTracker.close();
				logTracker = null;
			} catch (IllegalStateException ise) {
			}
			Log ls = null;
			while (logs.size() > 0) {
				ls = (Log) logs.elementAt(0);
				try {
					ls.initListener();
					break;
				} catch (IllegalStateException ise) {
					logs.removeElementAt(0);
					ls = null;
				}
			}
			listener = ls;
		}
		return (listener != null);
	}

	/**
	 * enable/disable print on console
	 * 
	 * @param value
	 *            boolean if true enables print on console else disables it
	 */
	public void setPrintOnConsole(boolean value) {
		printOnConsole = value;
	}

	/**
	 * enable/disable loging of debug info
	 * 
	 * @param value
	 *            boolean if true enables loging of debug info else disables it
	 */
	public void setDebug(boolean value) {
		debug = value;
	}

	/**
	 * Gets the flag, which enables logging debug messages.
	 * 
	 * @return true if debugging is enabled
	 */
	public boolean getDebug() {
		return debug;
	}

	/**
	 * Gets the flag, which enables printing log messages on the console.
	 * 
	 * @return true if printingon console is enabled
	 */
	public boolean getPrintOnConsole() {
		return printOnConsole;
	}

	public Object addingService(ServiceReference reference) {
		return reference;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
	}

	private HashIntObjNS map = null;
	private HashIntObjNS starts = null;

	public void setMaps(HashIntObjNS map0, HashIntObjNS starts0) {
		map = map0;
		starts = starts0;
	}

	private String getModuleName(int moduleID) {
		return (String) map.get(-moduleID);
	}

	private String getMsgValue(int msgID) {
		String res = (String) map.get(msgID);
		if (res == null && starts != null) {
			try {
				int startID = ((Integer) starts.get(msgID)).intValue();
				res = "END OF " + (String) map.get(startID);
			} catch (Exception e) {
			}
		}
		return res;
	}

	private static final char[] chars = {']', ' ', ':', ' '};

	private String buildDebugString(int moduleID, int msgID, String message, String prefix) {

		if (map == null) {
			return prefix + " [" + moduleID + "] " + msgID + " : " + (message == null ? "" : message);
		}

		StringBuffer sBuf = new StringBuffer(prefix).append("[");

		String module = getModuleName(moduleID);
		sBuf.append(module != null ? module : "" + moduleID);

		sBuf.append(chars, 0, 2);

		if (msgID != 0) {
			// map msgID to String
			String msg = getMsgValue(msgID);
			sBuf.append(msg != null ? msg : "" + msgID);
		}

		if (message != null)
			sBuf.append(chars, 2, 2).append(message);

		return sBuf.toString();
	}

	// public String toString() {
	// return bc.toString();
	// }

	private static boolean security = false;

	public static boolean security() {
		if (UtilActivator.bc != null) {
			try {
				Object ssecurity = UtilActivator.bc.getProperty("equinox.security");
				security = ssecurity != null && !"none".equals(ssecurity);
			} catch (Throwable t) {
				// no security implementation.
			}
		} else {
			try {
				Object ssecurity = System.getProperty("equinox.security");
				security = ssecurity != null && !"none".equals(ssecurity);
			} catch (Throwable t) {
				// no security implementation.
			}
		}
		return security;
	}

	String baseName;
	boolean synch;
	FileOutputStream fos = null;
	static String logsdir = null;

	static Queue queue;
	static boolean running = false;
	static Saver saver;
	// boolean date = false;
	long lastTime;

	// byte[] bdate;

	public Log(String baseName, boolean synch, BundleContext bc) {
		this(bc);
		this.baseName = baseName;
		this.synch = synch;
		if (!synch)
			synchronized (Log.class) {
				if (queue == null) {
					queue = new Queue(20);
					saver = new Saver();
				}
			}
	}

	public void trace(byte[] bytes) {
		if (!synch)
			synchronized (queue) {
				queue.put(this);
				queue.put(bytes);
				if (!running) {
					running = true;
					UtilActivator.thMan.execute(saver, "File Log Thread");
				}
			}
		else
			trace(bytes, 0, bytes.length);
	}

	public void trace(byte[] bytes, int off, int len) {
		if (logsdir == null)
			synchronized (Log.class) {
				if (logsdir == null) {
					try {
						logsdir = UtilActivator.bc.getProperty("equinox.logsDir");
						if (logsdir == null)
							logsdir = "./logs";
						File logs = new File(logsdir);
						logs.mkdirs();
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}
		if (fos == null)
			synchronized (this) {
				if (fos == null) {
					StringBuffer fname = new StringBuffer(logsdir.length() + baseName.length() + 1);
					fname.append(logsdir).append(File.separatorChar).append(baseName);
					try {
						fos = new FileOutputStream(fname.toString(), true);
					} catch (IOException ioExc) {
						ioExc.printStackTrace();
					}
				}
			}
		try {
			fos.write(bytes, off, len);
			fos.write(10);
		} catch (IOException ioExc) {
			ioExc.printStackTrace();
		}
	}

	public void finalize() {
		if (fos != null)
			try {
				fos.close();
			} catch (IOException ioExc) {
				ioExc.printStackTrace();
			}
	}

	/**
	 * Checks the auto print on console flag
	 * 
	 * @return true, if autoPrintOnConsole is enabled
	 */
	public boolean isAutoPrintOnConsole() {
		return autoPrintOnConsole;
	}

	/**
	 * Enables/disables auto printing on console. This flag points whether
	 * printing on console should be done if log service is not available
	 * 
	 * @param autoPrintOnConsole
	 *            if true enables auto print on console else disables it.
	 */
	public void setAutoPrintOnConsole(boolean autoPrintOnConsole) {
		this.autoPrintOnConsole = autoPrintOnConsole;
	}

	/**
	 * Returns whether printing on console should be done for errors
	 * (exceptions) and warnings
	 * 
	 * @return Returns the error log level flag - if true the error and warnings
	 *         will be print on console
	 */
	public boolean isLogErrorLevel() {
		return logErrorLevel;
	}

	private static final byte OPEN_TYPE = 0;
	private static final byte GET_SERVICE_TYPE = 1;
	private static final byte GET_SYS_SERVICES_TYPE = 2;

	/**
	 * @see org.eclipse.equinox.internal.util.security.PrivilegedRunner.PrivilegedDispatcher#dispatchPrivileged(int,
	 *      java.lang.Object, java.lang.Object, java.lang.Object,
	 *      java.lang.Object)
	 */
	public Object dispatchPrivileged(int type, Object arg1, Object arg2, Object arg3, Object arg4) throws Exception {
		switch (type) {
			case OPEN_TYPE :
				initListener0();
				break;
			case GET_SERVICE_TYPE :
				return getService0();
			case GET_SYS_SERVICES_TYPE :
				break;
		}
		return null;
	}

}// Log class

class Saver implements Runnable {

	public void run() {
		byte[] bytes = null;
		Log log = null;
		while (true) {
			synchronized (Log.queue) {
				log = (Log) Log.queue.get();
				if (log == null) {
					Log.running = false;
					bytes = null;
					return;
				}
				bytes = (byte[]) Log.queue.get();
			}
			log.trace(bytes, 0, bytes.length);
		}
	}
}
