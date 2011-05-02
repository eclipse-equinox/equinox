/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

    public static final String ASPECTJ_OSGI = "org.eclipse.equinox.weaving.hook"; //$NON-NLS-1$

    public static boolean DEBUG_BUNDLE;

    public static String DEBUG_BUNDLENAME;

    public static boolean DEBUG_CACHE;

    public static boolean DEBUG_GENERAL;

    public static boolean DEBUG_SUPPLEMENTS;

    public static boolean DEBUG_WEAVE;

    public static final String OPTION_DEBUG_BUNDLE = ASPECTJ_OSGI
            + "/debug/bundle"; //$NON-NLS-1$

    public static final String OPTION_DEBUG_BUNDLENAME = ASPECTJ_OSGI
            + "/debug/bundleName"; //$NON-NLS-1$

    public static final String OPTION_DEBUG_CACHE = ASPECTJ_OSGI
            + "/debug/cache"; //$NON-NLS-1$

    public static final String OPTION_DEBUG_GENERAL = ASPECTJ_OSGI + "/debug"; //$NON-NLS-1$

    public static final String OPTION_DEBUG_SUPPLEMENTS = ASPECTJ_OSGI
            + "/debug/supplements"; //$NON-NLS-1$

    public static final String OPTION_DEBUG_WEAVE = ASPECTJ_OSGI
            + "/debug/weave"; //$NON-NLS-1$

    static {
        final FrameworkDebugOptions fdo = FrameworkDebugOptions.getDefault();
        if (fdo != null) {
            DEBUG_GENERAL = fdo.getBooleanOption(OPTION_DEBUG_GENERAL, false);
            DEBUG_BUNDLE = fdo.getBooleanOption(OPTION_DEBUG_BUNDLE, false);
            DEBUG_WEAVE = fdo.getBooleanOption(OPTION_DEBUG_WEAVE, false);
            DEBUG_CACHE = fdo.getBooleanOption(OPTION_DEBUG_CACHE, false);
            DEBUG_BUNDLENAME = fdo.getOption(OPTION_DEBUG_BUNDLENAME, "");
            DEBUG_SUPPLEMENTS = fdo.getBooleanOption(OPTION_DEBUG_SUPPLEMENTS,
                    false);
        }
    }

    public static boolean bundleNameMatches(final String name) {
        return name.equals(DEBUG_BUNDLENAME);
    }

    public static void println(final String s) {
        /* if (s.indexOf("org.eclipse.osgi.tests") != -1) */System.err
                .println(s);
    }
}
