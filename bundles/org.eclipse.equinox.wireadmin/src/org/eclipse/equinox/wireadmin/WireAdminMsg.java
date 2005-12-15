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

public class WireAdminMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.wireadmin.ExternalMessages"; //$NON-NLS-1$

	public static String BACKING_STORE_READ_EXCEPTION;
	public static String WIREADMIN_UNREGISTERED_EXCEPTION;
	public static String WIREADMIN_EVENT_DISPATCH_ERROR;
	public static String WIREADMIN_PROP_KEY_MUST_BE_STRING;
	public static String WIREADMIN_KEYS_CASE_INSENSITIVE_MATCH;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, WireAdminMsg.class);
	}
}