/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Webster           initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.weaving.adaptors;

import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;

public class Debug {

	public static boolean DEBUG_GENERAL;
	public static boolean DEBUG_BUNDLE;
	public static boolean DEBUG_WEAVE;
	public static boolean DEBUG_CACHE;
	public static boolean DEBUG_SUPPLEMENTS;
	
	private static String bundleName;
	
	public static boolean bundleNameMatches (String name) {
		return name.equals(bundleName);
	}
	
	public static final String ASPECTJ_OSGI = "org.aspectj.osgi";
	public static final String OPTION_DEBUG_GENERAL = ASPECTJ_OSGI + "/debug";
	public static final String OPTION_DEBUG_BUNDLE = ASPECTJ_OSGI + "/debug/bundle";
	public static final String OPTION_DEBUG_WEAVE = ASPECTJ_OSGI + "/debug/weave";
	public static final String OPTION_DEBUG_CACHE = ASPECTJ_OSGI + "/debug/cache";
	public static final String OPTION_DEBUG_BUNDLENAME = ASPECTJ_OSGI + "/debug/bundleName";
	public static final String OPTION_DEBUG_SUPPLEMENTS = ASPECTJ_OSGI + "/debug/supplements";
	
	static {
		FrameworkDebugOptions fdo = FrameworkDebugOptions.getDefault();
		if (fdo != null) {
			DEBUG_GENERAL = fdo.getBooleanOption(OPTION_DEBUG_GENERAL,false);
			DEBUG_BUNDLE = fdo.getBooleanOption(OPTION_DEBUG_BUNDLE,false);
			DEBUG_WEAVE = fdo.getBooleanOption(OPTION_DEBUG_WEAVE,false);
			DEBUG_CACHE = fdo.getBooleanOption(OPTION_DEBUG_CACHE,false);
			bundleName = fdo.getOption(OPTION_DEBUG_BUNDLENAME,"");
			DEBUG_SUPPLEMENTS = fdo.getBooleanOption(OPTION_DEBUG_SUPPLEMENTS,false);
		}
	}
	
	public static void println (String s) {
		/*if (s.indexOf("org.eclipse.osgi.tests") != -1)*/ System.err.println(s);
	}
}
