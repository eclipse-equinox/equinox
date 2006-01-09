/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.wireadmin;

import org.eclipse.osgi.util.NLS;

public class LogTrackerMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.wireadmin.LogMessages"; //$NON-NLS-1$

	public static String Unknown_Log_level;
	public static String Info;
	public static String Warning;
	public static String Error;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, LogTrackerMsg.class);
	}
}