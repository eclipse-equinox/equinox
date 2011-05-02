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

/**
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface LogInterface {

	public boolean printOnConsole = true;

	public boolean debug = false;

	/**
	 * Logs error messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, log info is printed on console.
	 * 
	 * @param str
	 *            Message description of the error.
	 * @param ex
	 *            Throwable object, containing the stack trace; may be null.
	 */
	public void error(String str, Throwable ex);

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
	public void warning(String str, Throwable ex);

	/**
	 * Logs info messages. If <code>printOnConsole</code> is true, or if the
	 * <code>LogService</code> is unavailable, message is printed on console.
	 * 
	 * @param str
	 *            Message to be logged.
	 */
	public void info(String str);

	/**
	 * Logs debug information if <code>debug</code> flag is true. If
	 * LogService is unaccessible or printOnConsole flag is true, log info is
	 * printed on console.
	 * 
	 * @param str
	 *            Message description.
	 * @param e
	 *            Throwable object, containing the stack trace; may be null.
	 */
	public void debug(String str, Throwable e);

	/**
	 * Releases the Log's resources: ungets LogService, removes the
	 * ServiceListener from the framework and nulls references. After invocation
	 * of this method, this Log object can be used no longer.
	 */
	public void close();

	/**
	 * enable/diasable print on console
	 * 
	 * @param value
	 *            boolean if true enables print on console else disables it
	 */
	public void setPrintOnConsole(boolean value);

	/**
	 * enable/diasable loging of debug info
	 * 
	 * @param value
	 *            boolean if true enables loging of debug info else disables it
	 */
	public void setDebug(boolean value);

	/**
	 * Gets the flag, which enables logging debug messages.
	 * 
	 * @return true if debugging is enabled
	 */
	public boolean getDebug();

	/**
	 * Gets the flag, which enables printing log messages on the console.
	 * 
	 * @return true if printingon console is enabled
	 */
	public boolean getPrintOnConsole();

}// Log class
