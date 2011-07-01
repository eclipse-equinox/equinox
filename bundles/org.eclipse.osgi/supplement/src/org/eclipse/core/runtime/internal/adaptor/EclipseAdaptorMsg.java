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
package org.eclipse.core.runtime.internal.adaptor;

import org.eclipse.osgi.util.NLS;

public class EclipseAdaptorMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorMessages"; //$NON-NLS-1$

	public static String ECLIPSE_MISSING_IMPORTED_PACKAGE;
	public static String ECLIPSE_MISSING_OPTIONAL_IMPORTED_PACKAGE;
	public static String ECLIPSE_MISSING_DYNAMIC_IMPORTED_PACKAGE;
	public static String ECLIPSE_MISSING_OPTIONAL_REQUIRED_BUNDLE;
	public static String ECLIPSE_MISSING_REQUIRED_BUNDLE;
	public static String ECLIPSE_MISSING_HOST;
	public static String ECLIPSE_MISSING_NATIVECODE;
	public static String ECLIPSE_MISSING_REQUIRED_CAPABILITY;
	public static String ECLIPSE_MISSING_REQUIREMENT;
	public static String ECLIPSE_CANNOT_CHANGE_LOCATION;
	public static String ECLIPSE_BUNDLESTOPPER_CYCLES_FOUND;
	public static String ECLIPSE_CACHEDMANIFEST_UNEXPECTED_EXCEPTION;

	public static String fileManager_cannotLock;
	public static String fileManager_updateFailed;
	public static String fileManager_illegalInReadOnlyMode;
	public static String fileManager_notOpen;

	public static String ECLIPSE_ADAPTOR_ERROR_XML_SERVICE;
	public static String ECLIPSE_ADAPTOR_RUNTIME_ERROR;
	public static String ECLIPSE_ADAPTOR_EXITING;

	public static String ECLIPSE_DATA_MANIFEST_NOT_FOUND;
	public static String ECLIPSE_CONVERTER_ERROR_CONVERTING;
	public static String ECLIPSE_DATA_ERROR_READING_MANIFEST;
	public static String ECLIPSE_CLASSLOADER_CANNOT_GET_HEADERS;

	public static String ECLIPSE_CLASSLOADER_CONCURRENT_STARTUP;
	public static String ECLIPSE_CLASSLOADER_ACTIVATION;

	public static String ECLIPSE_CONSOLE_COMMANDS_HEADER;
	public static String ECLIPSE_CONSOLE_HELP_DIAG_COMMAND_DESCRIPTION;
	public static String ECLIPSE_CONSOLE_HELP_ENABLE_COMMAND_DESCRIPTION;
	public static String ECLIPSE_CONSOLE_HELP_DISABLE_COMMAND_DESCRIPTION;
	public static String ECLIPSE_CONSOLE_HELP_LD_COMMAND_DESCRIPTION;
	public static String ECLIPSE_CONSOLE_NO_BUNDLE_SPECIFIED_ERROR;
	public static String ECLIPSE_CONSOLE_NO_CONSTRAINTS_NO_PLATFORM_ADMIN_MESSAGE;
	public static String ECLIPSE_CONSOLE_CANNOT_FIND_BUNDLE_ERROR;
	public static String ECLIPSE_CONSOLE_NO_CONSTRAINTS;
	public static String ECLIPSE_CONSOLE_DIRECT_CONSTRAINTS;
	public static String ECLIPSE_CONSOLE_LEAF_CONSTRAINTS;
	public static String ECLIPSE_CONSOLE_BUNDLE_DISABLED_MESSAGE;
	public static String ECLIPSE_CONSOLE_DISABLED_COUNT_MESSAGE;
	public static String ECLIPSE_CONSOLE_DISABLED_BUNDLE_HEADER;
	public static String ECLIPSE_CONSOLE_DISABLED_BUNDLE_REASON1;

	public static String ECLIPSE_STARTUP_ALREADY_RUNNING;
	public static String ECLIPSE_STARTUP_STARTUP_ERROR;
	public static String ECLIPSE_STARTUP_SHUTDOWN_ERROR;
	public static String ECLIPSE_STARTUP_ERROR_CHECK_LOG;
	public static String ECLIPSE_STARTUP_NOT_RUNNING;
	public static String ECLIPSE_STARTUP_ERROR_NO_APPLICATION;
	public static String ECLIPSE_STARTUP_ROOTS_NOT_RESOLVED;
	public static String ECLIPSE_STARTUP_ALL_NOT_RESOLVED;
	public static String ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_ACTIVE;
	public static String ECLIPSE_STARTUP_ERROR_BUNDLE_NOT_RESOLVED;
	public static String ECLIPSE_STARTUP_BUNDLE_NOT_FOUND;
	public static String ECLIPSE_STARTUP_FAILED_UNINSTALL;
	public static String ECLIPSE_STARTUP_FAILED_INSTALL;
	public static String ECLIPSE_STARTUP_FAILED_START;
	public static String ECLIPSE_STARTUP_APP_ERROR;
	public static String ECLIPSE_STARTUP_FILEMANAGER_OPEN_ERROR;
	public static String ECLIPSE_STARTUP_PROPS_NOT_SET;

	public static String error_badNL;

	public static String location_cannotLock;
	public static String location_cannotLockNIO;
	public static String location_folderReadOnly;
	public static String location_notSet;
	public static String location_notFileProtocol;
	public static String location_noLockFile;

	public static String ECLIPSE_CONVERTER_FILENOTFOUND;
	public static String ECLIPSE_CONVERTER_ERROR_CREATING_BUNDLE_MANIFEST;
	public static String ECLIPSE_CONVERTER_PLUGIN_LIBRARY_IGNORED;

	public static String ECLIPSE_CONVERTER_ERROR_PARSING_PLUGIN_MANIFEST;
	public static String ECLIPSE_CONVERTER_MISSING_ATTRIBUTE;
	public static String parse_error;
	public static String parse_errorNameLineColumn;

	public static String ECLIPSE_CONVERTER_NO_SAX_FACTORY;
	public static String ECLIPSE_CONVERTER_PARSE_UNKNOWNTOP_ELEMENT;

	public static String ECLIPSE_PLUGIN_EXTRACTION_PROBLEM;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, EclipseAdaptorMsg.class);
	}

}
