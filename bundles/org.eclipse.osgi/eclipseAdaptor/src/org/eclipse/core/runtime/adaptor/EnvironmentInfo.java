/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.util.*;
import org.eclipse.osgi.service.environment.Constants;

public class EnvironmentInfo implements org.eclipse.osgi.service.environment.EnvironmentInfo {
	private static EnvironmentInfo singleton;
	private static String nl;
	private static String os;
	private static String ws;
	private static String arch;
	static String[] allArgs;
	static String[] frameworkArgs;
	static String[] appArgs;

	// While we recognize the SunOS operating system, we change
	// this internally to be Solaris.
	private static final String INTERNAL_OS_SUNOS = "SunOS"; //$NON-NLS-1$
	// While we recognize the i386 architecture, we change
	// this internally to be x86.
	private static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$

	private EnvironmentInfo() {
		super();
		setupSystemContext();
	}

	public static EnvironmentInfo getDefault() {
		if (singleton == null)
			singleton = new EnvironmentInfo();
		return singleton;
	}

	public boolean inDevelopmentMode() {
		return System.getProperty("osgi.dev") != null;
	}

	public boolean inDebugMode() {
		return System.getProperty("osgi.debug") != null;
	}

	public String[] getCommandLineArgs() {
		return allArgs;
	}

	public String[] getFrameworkArgs() {
		return frameworkArgs;
	}

	public String[] getNonFrameworkArgs() {
		return appArgs;
	}

	public String getOSArch() {
		return arch;
	}

	public String getNL() {
		return nl;
	}

	public String getOS() {
		return os;
	}

	public String getWS() {
		return ws;
	}

	/**
	 * Initializes the execution context for this run of the platform.  The context
	 * includes information about the locale, operating system and window system.
	 * 
	 * NOTE: The OS, WS, and ARCH values should never be null. The executable should
	 * be setting these values and therefore this code path is obsolete for Eclipse
	 * when run from the executable.
	 */
	private void setupSystemContext() {
		// if the user didn't set the locale with a command line argument then
		// use the default.
		nl = System.getProperty("osgi.nl");
		if (nl != null) {
			StringTokenizer tokenizer = new StringTokenizer(nl, "_"); //$NON-NLS-1$
			int segments = tokenizer.countTokens();
			try {
				Locale userLocale = null;
				switch (segments) {
					case 1:
						// use the 2 arg constructor to maintain compatibility with 1.3.1
						userLocale = new Locale(tokenizer.nextToken(), ""); //$NON-NLS-1$
						break;
					case 2:
						userLocale = new Locale(tokenizer.nextToken(), tokenizer.nextToken());
						break;
					case 3:
						userLocale = new Locale(tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken());
						break;
					default:
						// if the user passed us in a bogus value then log a message and use the default
						System.err.println(EclipseAdaptorMsg.formatter.getString("error.badNL", nl)); //$NON-NLS-1$
						userLocale = Locale.getDefault();
						break;
				}
				Locale.setDefault(userLocale);
			} catch (NoSuchElementException e) {
				// fall through and use the default
			}
		}
		nl = Locale.getDefault().toString();
		System.getProperties().put("osgi.nl", nl);

		// if the user didn't set the operating system with a command line 
		// argument then use the default.
		os = System.getProperty("osgi.os");
		if (os == null) {
			String name = System.getProperty("os.name");//$NON-NLS-1$
			// check to see if the VM returned "Windows 98" or some other
			// flavour which should be converted to win32.
			if (name.regionMatches(true, 0, Constants.OS_WIN32, 0, 3))
				os = Constants.OS_WIN32;
			// EXCEPTION: All mappings of SunOS convert to Solaris
			if (os == null)
				os = name.equalsIgnoreCase(INTERNAL_OS_SUNOS) ? Constants.OS_SOLARIS : Constants.OS_UNKNOWN;
		}
		System.getProperties().put("osgi.os", os);

		// if the user didn't set the window system with a command line 
		// argument then use the default.
		ws = System.getProperty("osgi.ws");
		if (ws == null) {
			// setup default values for known OSes if nothing was specified
			if (os.equals(Constants.OS_WIN32))
				ws = Constants.WS_WIN32;
			else if (os.equals(Constants.OS_LINUX))
				ws = Constants.WS_MOTIF;
			else if (os.equals(Constants.OS_MACOSX))
				ws = Constants.WS_CARBON;
			else if (os.equals(Constants.OS_HPUX))
				ws = Constants.WS_MOTIF;
			else if (os.equals(Constants.OS_AIX))
				ws = Constants.WS_MOTIF;
			else if (os.equals(Constants.OS_SOLARIS))
				ws = Constants.WS_MOTIF;
			else
				ws = Constants.WS_UNKNOWN;
		}
		System.getProperties().put("osgi.ws", ws);

		// if the user didn't set the system architecture with a command line 
		// argument then use the default.
		arch = System.getProperty("osgi.arch");
		if (arch == null) {
			String name = System.getProperty("os.arch");//$NON-NLS-1$
			// Map i386 architecture to x86
			arch = name.equalsIgnoreCase(INTERNAL_ARCH_I386) ? Constants.ARCH_X86 : name;
		}
		System.getProperties().put("osgi.arch", arch);
	}

}