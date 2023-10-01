/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *   Matthew Webster           initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.weaving.adaptors;

import org.eclipse.osgi.service.debug.DebugOptions;

public class Debug {

	public static final String ASPECTJ_OSGI = "org.eclipse.equinox.weaving.hook"; //$NON-NLS-1$

	public static boolean DEBUG_BUNDLE = false;

	public static String DEBUG_BUNDLENAME;

	public static boolean DEBUG_CACHE = false;

	public static boolean DEBUG_GENERAL = false;

	public static boolean DEBUG_SUPPLEMENTS = false;

	public static boolean DEBUG_WEAVE = false;

	public static final String OPTION_DEBUG_BUNDLE = ASPECTJ_OSGI + "/debug/bundle"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_BUNDLENAME = ASPECTJ_OSGI + "/debug/bundleName"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_CACHE = ASPECTJ_OSGI + "/debug/cache"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_GENERAL = ASPECTJ_OSGI + "/debug"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_SUPPLEMENTS = ASPECTJ_OSGI + "/debug/supplements"; //$NON-NLS-1$

	public static final String OPTION_DEBUG_WEAVE = ASPECTJ_OSGI + "/debug/weave"; //$NON-NLS-1$

	public static boolean bundleNameMatches(final String name) {
		return name.equals(DEBUG_BUNDLENAME);
	}

	public static void init(final DebugOptions options) {
		if (options != null) {
			DEBUG_GENERAL = options.getBooleanOption(OPTION_DEBUG_GENERAL, false);
			DEBUG_BUNDLE = options.getBooleanOption(OPTION_DEBUG_BUNDLE, false);
			DEBUG_WEAVE = options.getBooleanOption(OPTION_DEBUG_WEAVE, false);
			DEBUG_CACHE = options.getBooleanOption(OPTION_DEBUG_CACHE, false);
			DEBUG_BUNDLENAME = options.getOption(OPTION_DEBUG_BUNDLENAME, ""); //$NON-NLS-1$
			DEBUG_SUPPLEMENTS = options.getBooleanOption(OPTION_DEBUG_SUPPLEMENTS, false);
		}
	}

	public static void println(final String s) {
		/* if (s.indexOf("org.eclipse.osgi.tests") != -1) */System.err.println(s);
	}
}
