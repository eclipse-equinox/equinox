/*******************************************************************************
 * Copyright (c) 2008 Heiko Seeberger and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import static org.eclipse.equinox.weaving.internal.caching.IBundleConstants.BUNDLE_SYMBOLIC_NAME;

/**
 * Logging utility.
 * 
 * @author Heiko Seeberger
 */
public class Log {

    static boolean debugEnabled = false;

    private static final String PREFIX = "[" + BUNDLE_SYMBOLIC_NAME + "] "; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Logging debug information.
     * 
     * @param message The tracing message, optional.
     */
    public static void debug(final String message) {
        if (debugEnabled) {
            if (message != null) {
                System.out.println(PREFIX + message);
            }
        }
    }

    /**
     * Logging an error.
     * 
     * @param message The error message, optional.
     * @param t The Throwable for this error, optional.
     */
    public static void error(final String message, final Throwable t) {
        if (message != null) {
            System.err.println(PREFIX + message);
        }
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * Shows debug toggle state.
     * 
     * @return true, if debug is enabled, else false.
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
}
