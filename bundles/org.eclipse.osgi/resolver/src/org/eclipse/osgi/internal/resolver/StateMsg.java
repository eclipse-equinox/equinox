/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.util.NLS;

public class StateMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.osgi.internal.resolver.StateMessages"; //$NON-NLS-1$

	public static String BUNDLE_NOT_IN_STATE;
	public static String BUNDLE_IN_OTHER_STATE;
	public static String BUNDLE_PENDING_REMOVE_STATE;

	public static String HEADER_REQUIRED;
	public static String HEADER_PACKAGE_DUPLICATES;
	public static String HEADER_PACKAGE_JAVA;
	public static String HEADER_VERSION_ERROR;
	public static String HEADER_EXPORT_ATTR_ERROR;
	public static String HEADER_DIRECTIVE_DUPLICATES;
	public static String HEADER_ATTRIBUTE_DUPLICATES;
	public static String HEADER_EXTENSION_ERROR;

	public static String RES_ERROR_DISABLEDBUNDLE;
	public static String RES_ERROR_MISSING_PERMISSION;
	public static String RES_ERROR_MISSING_CONSTRAINT;
	public static String RES_ERROR_FRAGMENT_CONFLICT;
	public static String RES_ERROR_USES_CONFLICT;
	public static String RES_ERROR_SINGLETON_CONFLICT;
	public static String RES_ERROR_PLATFORM_FILTER;
	public static String RES_ERROR_NO_NATIVECODE_MATCH;
	public static String RES_ERROR_NATIVECODE_PATH_INVALID;
	public static String RES_ERROR_UNKNOWN;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, StateMsg.class);
	}
}