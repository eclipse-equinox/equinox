/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import java.util.*;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;

/**
 * Internal class.
 */
public class EclipseEnvironmentInfo implements EnvironmentInfo {
	private static EclipseEnvironmentInfo singleton;
	private static String nl;
	private static String os;
	private static String ws;
	private static String arch;
	private volatile static String[] allArgs;
	private volatile static String[] frameworkArgs;
	private volatile static String[] appArgs;

	// While we recognize the SunOS operating system, we change
	// this internally to be Solaris.
	private static final String INTERNAL_OS_SUNOS = "SunOS"; //$NON-NLS-1$
	private static final String INTERNAL_OS_LINUX = "Linux"; //$NON-NLS-1$
	private static final String INTERNAL_OS_MACOSX = "Mac OS"; //$NON-NLS-1$
	private static final String INTERNAL_OS_AIX = "AIX"; //$NON-NLS-1$
	private static final String INTERNAL_OS_HPUX = "HP-UX"; //$NON-NLS-1$
	private static final String INTERNAL_OS_QNX = "QNX"; //$NON-NLS-1$
	private static final String INTERNAL_OS_OS400 = "OS/400"; //$NON-NLS-1$
	private static final String INTERNAL_OS_OS390 = "OS/390"; //$NON-NLS-1$
	private static final String INTERNAL_OS_ZOS = "z/OS"; //$NON-NLS-1$

	// While we recognize the i386 architecture, we change
	// this internally to be x86.
	private static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$
	// While we recognize the amd64 architecture, we change
	// this internally to be x86_64.
	private static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$

	private EclipseEnvironmentInfo() {
		super();
		setupSystemContext();
	}

	public static EclipseEnvironmentInfo getDefault() {
		if (singleton == null)
			singleton = new EclipseEnvironmentInfo();
		return singleton;
	}

	public boolean inDevelopmentMode() {
		return FrameworkProperties.getProperty("osgi.dev") != null; //$NON-NLS-1$
	}

	public boolean inDebugMode() {
		return FrameworkProperties.getProperty("osgi.debug") != null; //$NON-NLS-1$
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
	private static void setupSystemContext() {
		// if the user didn't set the locale with a command line argument then use the default.
		nl = FrameworkProperties.getProperty("osgi.nl"); //$NON-NLS-1$
		if (nl != null) {
			StringTokenizer tokenizer = new StringTokenizer(nl, "_"); //$NON-NLS-1$
			int segments = tokenizer.countTokens();
			try {
				Locale userLocale = null;
				switch (segments) {
					case 1 :
						// use the 2 arg constructor to maintain compatibility with 1.3.1
						userLocale = new Locale(tokenizer.nextToken(), ""); //$NON-NLS-1$
						break;
					case 2 :
						userLocale = new Locale(tokenizer.nextToken(), tokenizer.nextToken());
						break;
					case 3 :
						userLocale = new Locale(tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken());
						break;
					default :
						// if the user passed us in a bogus value then log a message and use the default
						System.err.println(NLS.bind(EclipseAdaptorMsg.error_badNL, nl));
						userLocale = Locale.getDefault();
						break;
				}
				Locale.setDefault(userLocale);
				// TODO what the heck is this for?? why not just use osgi.nl
				FrameworkProperties.setProperty("osgi.nl.user", nl); //$NON-NLS-1$
			} catch (NoSuchElementException e) {
				// fall through and use the default
			}
		}
		nl = Locale.getDefault().toString();
		FrameworkProperties.setProperty("osgi.nl", nl); //$NON-NLS-1$

		// if the user didn't set the operating system with a command line 
		// argument then use the default.
		os = FrameworkProperties.getProperty("osgi.os"); //$NON-NLS-1$
		if (os == null) {
			os = guessOS(FrameworkProperties.getProperty("os.name"));//$NON-NLS-1$);
			FrameworkProperties.setProperty("osgi.os", os); //$NON-NLS-1$
		}

		// if the user didn't set the window system with a command line 
		// argument then use the default.
		ws = FrameworkProperties.getProperty("osgi.ws"); //$NON-NLS-1$
		if (ws == null) {
			ws = guessWS(os);
			FrameworkProperties.setProperty("osgi.ws", ws); //$NON-NLS-1$
		}

		// if the user didn't set the system architecture with a command line 
		// argument then use the default.
		arch = FrameworkProperties.getProperty("osgi.arch"); //$NON-NLS-1$
		if (arch == null) {
			String name = FrameworkProperties.getProperty("os.arch");//$NON-NLS-1$
			// Map i386 architecture to x86
			if (name.equalsIgnoreCase(INTERNAL_ARCH_I386))
				arch = Constants.ARCH_X86;
			// Map amd64 architecture to x86_64
			else if (name.equalsIgnoreCase(INTERNAL_AMD64))
				arch = Constants.ARCH_X86_64;
			else
				arch = name;
			FrameworkProperties.setProperty("osgi.arch", arch); //$NON-NLS-1$			
		}
	}

	public static void setAllArgs(String[] allArgs) {
		// do not check if this is set already to allow arguments to change when multiple applications are launched
		EclipseEnvironmentInfo.allArgs = allArgs;
	}

	public static void setAppArgs(String[] appArgs) {
		// do not check if this is set already to allow arguments to change when multiple applications are launched
		EclipseEnvironmentInfo.appArgs = appArgs;
	}

	public static void setFrameworkArgs(String[] frameworkArgs) {
		// do not check if this is set already to allow arguments to change when multiple applications are launched
		EclipseEnvironmentInfo.frameworkArgs = frameworkArgs;
	}

	public static String guessWS(String osName) {
		// setup default values for known OSes if nothing was specified
		if (osName.equals(Constants.OS_WIN32))
			return Constants.WS_WIN32;
		if (osName.equals(Constants.OS_LINUX))
			return Constants.WS_GTK;
		if (osName.equals(Constants.OS_MACOSX))
			return Constants.WS_COCOA;
		if (osName.equals(Constants.OS_HPUX))
			return Constants.WS_MOTIF;
		if (osName.equals(Constants.OS_AIX))
			return Constants.WS_MOTIF;
		if (osName.equals(Constants.OS_SOLARIS))
			return Constants.WS_GTK;
		if (osName.equals(Constants.OS_QNX))
			return Constants.WS_PHOTON;
		return Constants.WS_UNKNOWN;
	}

	public static String guessOS(String osName) {
		// check to see if the OS name is "Windows 98" or some other
		// flavour which should be converted to win32.
		if (osName.regionMatches(true, 0, Constants.OS_WIN32, 0, 3))
			return Constants.OS_WIN32;
		// EXCEPTION: All mappings of SunOS convert to Solaris
		if (osName.equalsIgnoreCase(INTERNAL_OS_SUNOS))
			return Constants.OS_SOLARIS;
		if (osName.equalsIgnoreCase(INTERNAL_OS_LINUX))
			return Constants.OS_LINUX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_QNX))
			return Constants.OS_QNX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_AIX))
			return Constants.OS_AIX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_HPUX))
			return Constants.OS_HPUX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_OS400))
			return Constants.OS_OS400;
		if (osName.equalsIgnoreCase(INTERNAL_OS_OS390))
			return Constants.OS_OS390;
		if (osName.equalsIgnoreCase(INTERNAL_OS_ZOS))
			return Constants.OS_ZOS;
		// os.name on Mac OS can be either Mac OS or Mac OS X
		if (osName.regionMatches(true, 0, INTERNAL_OS_MACOSX, 0, INTERNAL_OS_MACOSX.length()))
			return Constants.OS_MACOSX;
		return Constants.OS_UNKNOWN;
	}

	public String getProperty(String key) {
		return FrameworkProperties.getProperty(key);
	}

	public String setProperty(String key, String value) {
		return FrameworkProperties.setProperty(key, value);
	}
}
