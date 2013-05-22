/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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

	public static String SERVICE_OBJECTS_UNGET_ARGUMENT_EXCEPTION;

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

	public static String ECLIPSE_CANNOT_CHANGE_LOCATION;
	public static String fileManager_cannotLock;
	public static String fileManager_updateFailed;
	public static String fileManager_illegalInReadOnlyMode;
	public static String fileManager_notOpen;
	public static String ECLIPSE_CLASSLOADER_CONCURRENT_STARTUP;
	public static String ECLIPSE_CLASSLOADER_ACTIVATION;
	public static String ECLIPSE_STARTUP_ALREADY_RUNNING;
	public static String ECLIPSE_STARTUP_STARTUP_ERROR;
	public static String ECLIPSE_STARTUP_SHUTDOWN_ERROR;
	public static String ECLIPSE_STARTUP_ERROR_CHECK_LOG;
	public static String ECLIPSE_STARTUP_NOT_RUNNING;
	public static String ECLIPSE_STARTUP_ERROR_NO_APPLICATION;
	public static String ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_ACTIVE;
	public static String ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED;
	public static String ECLIPSE_STARTUP_BUNDLE_NOT_FOUND;
	public static String ECLIPSE_STARTUP_FAILED_UNINSTALL;
	public static String ECLIPSE_STARTUP_FAILED_INSTALL;
	public static String ECLIPSE_STARTUP_FAILED_START;
	public static String ECLIPSE_STARTUP_APP_ERROR;
	public static String ECLIPSE_STARTUP_PROPS_NOT_SET;
	public static String error_badNL;
	public static String location_cannotLock;
	public static String location_cannotLockNIO;
	public static String location_folderReadOnly;
	public static String location_notSet;
	public static String location_notFileProtocol;
	public static String location_noLockFile;
	public static String ECLIPSE_PLUGIN_EXTRACTION_PROBLEM;
}
