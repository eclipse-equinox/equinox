/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorMsg;
import org.eclipse.osgi.util.NLS;

public class ConsoleManager {
	public static final String PROP_CONSOLE = "osgi.console"; //$NON-NLS-1$
	private static final String CONSOLE_NAME = "OSGi Console"; //$NON-NLS-1$
	private FrameworkConsole console;

	public static ConsoleManager startConsole(Framework equinox) {
		String consolePort = FrameworkProperties.getProperty(PROP_CONSOLE);
		if (consolePort != null) {
			ConsoleManager consoleMgr = new ConsoleManager();
			consoleMgr.startConsole(equinox, new String[0], consolePort);
			return consoleMgr;
		}

		return null;
	}

	/**
	 *  Invokes the OSGi Console on another thread
	 *
	 * @param equinox The current OSGi instance for the console to attach to
	 * @param consoleArgs An String array containing commands from the command line
	 * for the console to execute
	 * @param consolePort the port on which to run the console.  Empty string implies the default port.
	 */
	public void startConsole(Framework equinox, String[] consoleArgs, String consolePort) {
		try {
			if (consolePort.length() == 0)
				console = new FrameworkConsole(equinox, consoleArgs);
			else
				console = new FrameworkConsole(equinox, Integer.parseInt(consolePort), consoleArgs);
			Thread t = new Thread(console, CONSOLE_NAME);
			t.setDaemon(false);
			t.start();
		} catch (NumberFormatException nfe) {
			// TODO log or something other than write on System.err
			System.err.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_INVALID_PORT, consolePort));
		}
	}

	/**
	 *  Stops the OSGi Command console
	 *
	 */
	public void stopConsole() {
		if (console == null)
			return;
		console.shutdown();
	}
}
