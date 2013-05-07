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
package org.eclipse.osgi.framework.internal.core;

import org.eclipse.osgi.util.NLS;

public class Msg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.osgi.framework.internal.core.ExternalMessages"; //$NON-NLS-1$

	public static String MANIFEST_INVALID_HEADER_EXCEPTION;

	public static String SERVICE_ARGUMENT_NULL_EXCEPTION;
	public static String SERVICE_EMPTY_CLASS_LIST_EXCEPTION;
	public static String SERVICE_NOT_INSTANCEOF_CLASS_EXCEPTION;
	public static String SERVICE_FACTORY_NOT_INSTANCEOF_CLASS_EXCEPTION;
	public static String BUNDLE_ACTIVATOR_EXCEPTION;
	public static String BUNDLE_CONTEXT_INVALID_EXCEPTION;

	public static String FILTER_TERMINATED_ABRUBTLY;
	public static String FILTER_TRAILING_CHARACTERS;
	public static String FILTER_MISSING_LEFTPAREN;
	public static String FILTER_MISSING_RIGHTPAREN;
	public static String FILTER_INVALID_OPERATOR;
	public static String FILTER_MISSING_ATTR;
	public static String FILTER_INVALID_VALUE;
	public static String FILTER_MISSING_VALUE;

	public static String SERVICE_ALREADY_UNREGISTERED_EXCEPTION;

	public static String SERVICE_FACTORY_EXCEPTION;
	public static String SERVICE_OBJECT_NULL_EXCEPTION;
	public static String SERVICE_FACTORY_RECURSION;
	public static String SERVICE_USE_OVERFLOW;

	public static String BUNDLE_SYSTEMBUNDLE_UNINSTALL_EXCEPTION;

	public static String HEADER_DUPLICATE_KEY_EXCEPTION;
	public static String MANIFEST_INVALID_SPACE;
	public static String MANIFEST_INVALID_LINE_NOCOLON;
	public static String MANIFEST_IOEXCEPTION;

	public static String CANNOT_SET_CONTEXTFINDER;

	public static String URL_HANDLER_INCORRECT_TYPE;

	public static String HEADER_PACKAGE_DUPLICATES;
	public static String HEADER_PACKAGE_JAVA;
	public static String HEADER_VERSION_ERROR;
	public static String HEADER_EXPORT_ATTR_ERROR;
	public static String HEADER_DIRECTIVE_DUPLICATES;
	public static String HEADER_ATTRIBUTE_DUPLICATES;
	public static String HEADER_EXTENSION_ERROR;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, Msg.class);
	}
}
