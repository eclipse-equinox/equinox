/*******************************************************************************
 * Copyright (c) 2005  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package org.eclipse.osgi.profile;

import org.eclipse.osgi.framework.debug.DebugOptions;

public class Profile {
	public static final boolean PROFILE = true; // enable profile compiling
	public static boolean STARTUP = false; // enable startup profiling
	public static boolean BENCHMARK = false; // enable all benchmarking
	public static boolean DEBUG = false; // enable general debug profiling

	private static final String OSGI_PROP = "osgi.profile."; //$NON-NLS-1$
	private static final String PROP_STARTUP = OSGI_PROP + "startup"; //$NON-NLS-1$
	private static final String PROP_BENCHMARK = OSGI_PROP + "benchmark"; //$NON-NLS-1$
	private static final String PROP_DEBUG = OSGI_PROP + "debug"; //$NON-NLS-1$
	private static final String PROP_IMPL = OSGI_PROP + "impl"; //$NON-NLS-1$
	
	private static final String OSGI_OPTION = "org.eclipse.osgi/profile/"; //$NON-NLS-1$
	private static final String OPTION_STARTUP = OSGI_OPTION + "startup"; //$NON-NLS-1$
	private static final String OPTION_BENCHMARK = OSGI_OPTION + "benchmark"; //$NON-NLS-1$
	private static final String OPTION_DEBUG = OSGI_OPTION + "debug"; //$NON-NLS-1$
	private static final String OPTION_IMPL = OSGI_OPTION + "impl"; //$NON-NLS-1$
	
	public static final int FLAG_NONE = 0;
	public static final int FLAG_ENTER = 1;
	public static final int FLAG_EXIT = 2;
	public static final String ENTER_DESCRIPTION = "enter"; //$NON-NLS-1$
	public static final String EXIT_DESCRIPTION = "exit"; //$NON-NLS-1$
	
	private static ProfileLogger profileLogger = null;
	private static String profileLoggerClassName = null;

	static {
		initProps();
	}

	public static void initProps() {
		String prop;
		DebugOptions dbgOptions = null;

		// if osgi.debug is not available, don't force DebugOptions
		//  to init as this variable may be set later on where 
		//  DebugOptions will succeed.
		if (System.getProperty("osgi.debug") != null) { //$NON-NLS-1$
			dbgOptions = DebugOptions.getDefault();
			if (dbgOptions != null) {
				STARTUP = dbgOptions.getBooleanOption(OPTION_STARTUP, false);
				BENCHMARK = dbgOptions.getBooleanOption(OPTION_BENCHMARK, false);
				DEBUG = dbgOptions.getBooleanOption(OPTION_DEBUG, false);
				if (profileLogger == null)
					profileLoggerClassName = dbgOptions.getOption(OPTION_IMPL);
			}
		}

		// System properties will always override anything in .options file
		if ((prop = System.getProperty(PROP_STARTUP)) != null) {
			STARTUP = Boolean.valueOf(prop).booleanValue();
			if (dbgOptions != null)
				dbgOptions.setOption(OPTION_STARTUP, new Boolean(STARTUP).toString());
		}
		if ((prop = System.getProperty(PROP_BENCHMARK)) != null) {
			BENCHMARK = Boolean.valueOf(prop).booleanValue();
			if (dbgOptions != null)
				dbgOptions.setOption(OPTION_BENCHMARK, new Boolean(BENCHMARK).toString());
		}
		if ((prop = System.getProperty(PROP_DEBUG)) != null) {
			DEBUG = Boolean.valueOf(prop).booleanValue();
			if (dbgOptions != null)
				dbgOptions.setOption(OPTION_DEBUG, new Boolean(DEBUG).toString());
		}

		if (profileLogger == null) { 
			if ((prop = System.getProperty(PROP_IMPL)) != null) {
				profileLoggerClassName = prop;
				if (dbgOptions != null)
					dbgOptions.setOption(OPTION_IMPL, profileLoggerClassName);
			}
		} else {
			profileLogger.initProps();
		}
	}
	
	public static void logEnter(String id) {
		logTime(FLAG_ENTER, id, ENTER_DESCRIPTION, null);
	}
	public static void logEnter(String id, String description) {
		logTime(FLAG_ENTER, id, ENTER_DESCRIPTION, description);
	}
	public static void logExit(String id) {
		logTime(FLAG_EXIT, id, EXIT_DESCRIPTION, null);
	}	
	public static void logExit(String id, String description) {
		logTime(FLAG_EXIT, id, EXIT_DESCRIPTION, description);
	}
	public static void logTime(String id, String msg) {
		logTime(FLAG_NONE, id, msg, null);
	}
	public static void logTime(String id, String msg, String description) {
		logTime(FLAG_NONE, id, msg, description);
	}
	
	public static void logTime(int flag, String id, String msg, String description) {
		if (profileLogger == null) {
			if (profileLoggerClassName != null) {
				Class profileImplClass = null;
				try {
					profileImplClass = Class.forName(profileLoggerClassName);
					profileLogger = (ProfileLogger) profileImplClass.newInstance();
				} catch (Exception e) {
					// could not find the class
					e.printStackTrace();
				}
			}
			if (profileLogger == null)
				profileLogger = new DefaultProfileLogger();
		}
		profileLogger.logTime(flag, id, msg, description);
	}
	
	public static String getProfileLog() {
		if (profileLogger != null)
			return profileLogger.getProfileLog();
		return ""; //$NON-NLS-1$
	}

}
