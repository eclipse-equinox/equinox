/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lazar Kirchev, SAP AG - derivative implementation 
 *******************************************************************************/
package org.eclipse.equinox.console.commands;

import org.eclipse.osgi.util.NLS;

public class ConsoleMsg extends NLS {
	public static final String BUNDLE_NAME = "org.eclipse.equinox.console.commands.ConsoleMessages"; //$NON-NLS-1$

	public static String CONSOLE_INVALID_INPUT;
	public static String CONSOLE_NO_BUNDLE_SPECIFIED_ERROR;
	public static String CONSOLE_NOTHING_TO_INSTALL_ERROR;
	public static String CONSOLE_BUNDLE_ID_MESSAGE;
	public static String CONSOLE_NO_INSTALLED_BUNDLES_ERROR;
	public static String CONSOLE_REGISTERED_SERVICES_MESSAGE;
	public static String CONSOLE_FRAMEWORK_IS_LAUNCHED_MESSAGE;
	public static String CONSOLE_FRAMEWORK_IS_SHUTDOWN_MESSAGE;
	public static String CONSOLE_ID;
	public static String CONSOLE_BUNDLE_LOCATION_MESSAGE;
	public static String CONSOLE_STATE_BUNDLE_FILE_NAME_HEADER;
	public static String CONSOLE_BUNDLES_USING_SERVICE_MESSAGE;
	public static String CONSOLE_NO_REGISTERED_SERVICES_MESSAGE;
	public static String CONSOLE_NO_BUNDLES_USING_SERVICE_MESSAGE;
	public static String CONSOLE_REGISTERED_BY_BUNDLE_MESSAGE;
	public static String CONSOLE_IMPORTS_MESSAGE;
	public static String CONSOLE_STALE_MESSAGE;
	public static String CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE;
	public static String CONSOLE_NO_EXPORTED_PACKAGES_NO_PLATFORM_ADMIN_MESSAGE;
	public static String CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE;
	public static String CONSOLE_REMOVAL_PENDING_MESSAGE;
	public static String CONSOLE_SERVICES_IN_USE_MESSAGE;
	public static String CONSOLE_NO_SERVICES_IN_USE_MESSAGE;
	public static String CONSOLE_ID_MESSAGE;
	public static String CONSOLE_STATUS_MESSAGE;
	public static String CONSOLE_DATA_ROOT_MESSAGE;

	public static String CONSOLE_IMPORTED_PACKAGES_MESSAGE;
	public static String CONSOLE_NO_IMPORTED_PACKAGES_MESSAGE;
	public static String CONSOLE_HOST_MESSAGE;
	public static String CONSOLE_EXPORTED_PACKAGES_MESSAGE;
	public static String CONSOLE_EXPORTED_REMOVAL_PENDING_MESSAGE;
	public static String CONSOLE_EXPORTED_MESSAGE;
	public static String CONSOLE_NO_HOST_MESSAGE;
	public static String CONSOLE_FRAGMENT_MESSAGE;
	public static String CONSOLE_NO_FRAGMENT_MESSAGE;
	public static String CONSOLE_NO_NAMED_CLASS_SPACES_MESSAGE;
	public static String CONSOLE_NAMED_CLASS_SPACE_MESSAGE;
	public static String CONSOLE_PROVIDED_MESSAGE;
	public static String CONSOLE_REQUIRED_BUNDLES_MESSAGE;
	public static String CONSOLE_NO_REQUIRED_BUNDLES_MESSAGE;
	public static String CONSOLE_TOTAL_MEMORY_MESSAGE;
	public static String CONSOLE_FREE_MEMORY_BEFORE_GARBAGE_COLLECTION_MESSAGE;
	public static String CONSOLE_FREE_MEMORY_AFTER_GARBAGE_COLLECTION_MESSAGE;
	public static String CONSOLE_MEMORY_GAINED_WITH_GARBAGE_COLLECTION_MESSAGE;
	public static String CONSOLE_FRAMEWORK_LAUNCHED_PLEASE_SHUTDOWN_MESSAGE;
	public static String CONSOLE_CAN_NOT_REFRESH_NO_PACKAGE_ADMIN_ERROR;
	public static String CONSOLE_NO_COMMAND_SPECIFIED_ERROR;
	public static String CONSOLE_STARTED_IN_MESSAGE;
	public static String CONSOLE_EXECUTED_RESULT_CODE_MESSAGE;
	public static String CONSOLE_BUNDLE_HEADERS_TITLE;
	public static String CONSOLE_SYSTEM_PROPERTIES_TITLE;
	public static String CONSOLE_NO_PARAMETERS_SPECIFIED_TITLE;
	public static String CONSOLE_SETTING_PROPERTIES_TITLE;
	public static String CONSOLE_STATE_BUNDLE_TITLE;
	public static String CONSOLE_THREADGROUP_TITLE;
	public static String CONSOLE_THREADTYPE_TITLE;
	public static String CONSOLE_REQUIRES_MESSAGE;
	public static String CONSOLE_CANNOT_ACCESS_SYSTEM_PROPERTIES;
	public static String CONSOLE_NO_CONSTRAINTS_NO_PLATFORM_ADMIN_MESSAGE;
	public static String CONSOLE_CANNOT_FIND_BUNDLE_ERROR;
	public static String CONSOLE_NO_CONSTRAINTS;
	public static String CONSOLE_DIRECT_CONSTRAINTS;
	public static String CONSOLE_LEAF_CONSTRAINTS;
	public static String CONSOLE_MISSING_IMPORTED_PACKAGE;
	public static String CONSOLE_MISSING_OPTIONAL_IMPORTED_PACKAGE;
	public static String CONSOLE_MISSING_DYNAMIC_IMPORTED_PACKAGE;
	public static String CONSOLE_MISSING_OPTIONAL_REQUIRED_BUNDLE;
	public static String CONSOLE_MISSING_REQUIRED_BUNDLE;
	public static String CONSOLE_MISSING_HOST;
	public static String CONSOLE_MISSING_NATIVECODE;
	public static String CONSOLE_MISSING_REQUIRED_CAPABILITY;
	public static String CONSOLE_MISSING_REQUIREMENT;
	public static String CONSOLE_CONSOLE_BUNDLE_DISABLED_MESSAGE;
	public static String CONSOLE_CANNOT_ENABLE_NO_PLATFORM_ADMIN_MESSAGE;
	public static String CONSOLE_CANNOT_DISABLE_NO_PLATFORM_ADMIN_MESSAGE;
	public static String CONSOLE_CANNOT_LIST_DISABLED_NO_PLATFORM_ADMIN_MESSAGE;
	public static String CONSOLE_DISABLED_COUNT_MESSAGE;
	public static String CONSOLE_DISABLED_BUNDLE_HEADER;
	public static String CONSOLE_DISABLED_BUNDLE_REASON;
	public static String CONSOLE_STOP_MESSAGE;
	public static String CONSOLE_STOP_CONFIRMATION_YES;
	public static String CONSOLE_STOP_ERROR_READ_CONFIRMATION;

	public static String STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL;
	public static String STARTLEVEL_BUNDLE_STARTLEVEL;
	public static String STARTLEVEL_NO_STARTLEVEL_GIVEN;
	public static String STARTLEVEL_NO_STARTLEVEL_OR_BUNDLE_GIVEN;
	public static String STARTLEVEL_INITIAL_BUNDLE_STARTLEVEL;
	public static String STARTLEVEL_POSITIVE_INTEGER;
	
	public static final String CONSOLE_HELP_EXIT_COMMAND_DESCRIPTION = "exit immediately (System.exit)";
	public static final String CONSOLE_HELP_LAUNCH_COMMAND_DESCRIPTION = "start the OSGi Framework";
	public static final String CONSOLE_HELP_SHUTDOWN_COMMAND_DESCRIPTION = "shutdown the OSGi Framework";
	public static final String CONSOLE_HELP_START_COMMAND_DESCRIPTION = "start the specified bundle(s)";
	public static final String CONSOLE_HELP_START_COMMAND_ARGUMENT_DESCRIPTION = "bundle(s) to start";
	public static final String CONSOLE_HELP_STOP_COMMAND_DESCRIPTION = "stop the specified bundle(s)";
	public static final String CONSOLE_HELP_STOP_COMMAND_ARGUMENT_DESCRIPTION = "bundle(s) to stop";
	public static final String CONSOLE_HELP_INSTALL_COMMAND_DESCRIPTION = "install and optionally start bundle from the given URL";
	public static final String CONSOLE_HELP_INSTALL_START_OPTION_DESCRIPTION = "spedify if the bundle should be started after installation";
	public static final String CONSOLE_HELP_INSTALL_START_ARGUMENT_DESCRIPTION = "Location of bundle to install";
	public static final String CONSOLE_HELP_UPDATE_COMMAND_DESCRIPTION = "update the specified bundle(s)";
	public static final String CONSOLE_HELP_UPDATE_COMMAND_ARGUMENT_DESCRIPTION = "bundle(s) to update";
	public static final String CONSOLE_HELP_UPDATE_SOURCE_COMMAND_DESCRIPTION = "Update the specified bundle from the specified location";
	public static final String CONSOLE_HELP_UPDATE_SOURCE_COMMAND_BUNDLE_ARGUMENT_DESCRIPTION = "Bundle to update";
	public static final String CONSOLE_HELP_UPDATE_SOURCE_COMMAND_URL_ARGUMENT_DESCRIPTION = "Location of the new bundle content";
	public static final String CONSOLE_HELP_UNINSTALL_COMMAND_DESCRIPTION = "uninstall the specified bundle(s)";
	public static final String CONSOLE_HELP_UNINSTALL_COMMAND_ARGUMENT_DESCRIPTION = "bundle(s) to uninstall";
	public static final String CONSOLE_HELP_STATUS_COMMAND_DESCRIPTION = "display installed bundles and registered services";
	public static final String CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION = "[-s <comma separated list of bundle states>] [segment of bsn]";
	public static final String CONSOLE_HELP_FILTER_ARGUMENT_DESCRIPTION = "Optional filter for filtering the displayed services. Examples for the filter: (objectClass=com.xyz.Person); (&(objectClass=com.xyz.Person)(sn=Jensen)); passing only com.xyz.Person is a shortcut for (objectClass=com.xyz.Person). The filter syntax specification is available at http://www.ietf.org/rfc/rfc1960.txt";
	public static final String CONSOLE_HELP_SERVICES_COMMAND_DESCRIPTION = "display registered service details. Examples for [filter]: (objectClass=com.xyz.Person); (&(objectClass=com.xyz.Person)(sn=Jensen)); passing only com.xyz.Person is a shortcut for (objectClass=com.xyz.Person). The filter syntax specification is available at http://www.ietf.org/rfc/rfc1960.txt";
	public static final String CONSOLE_HELP_PACKAGES_BUNDLE_ARGUMENT_DESCRIPTION = "Bundle whose packages to display. If not present displays all exported packages";
	public static final String CONSOLE_HELP_PACKAGES_PACKAGE_ARGUMENT_DESCRIPTION = "Package name of the package to display";
	public static final String CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION = "display imported/exported package details";
	public static final String CONSOLE_HELP_BUNDLES_COMMAND_DESCRIPTION = "display details for all installed bundles";
	public static final String CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION = "(<id>|<location>)";
	public static final String CONSOLE_HELP_BUNDLE_COMMAND_DESCRIPTION = "display details for the specified bundle(s)";
	public static final String CONSOLE_HELP_GC_COMMAND_DESCRIPTION = "perform a garbage collection";
	public static final String CONSOLE_HELP_INIT_COMMAND_DESCRIPTION = "uninstall all bundles";
	public static final String CONSOLE_HELP_CLOSE_COMMAND_DESCRIPTION = "shutdown and exit";
	public static final String CONSOLE_HELP_REFRESH_COMMAND_DESCRIPTION = "refresh the packages of the specified bundles; if -all option is specified refresh packages of all installed bundles";
	public static final String CONSOLE_HELP_REFRESH_ALL_OPTION_DESCRIPTION = "specify to refresh the packages of all installed bundles";
	public static final String CONSOLE_HELP_REFRESH_COMMAND_ARGUMENT_DESCRIPTION = "list of bundles whose packages to be refreshed; if not present refreshes all bundles";
	public static final String CONSOLE_HELP_EXEC_COMMAND_DESCRIPTION = "execute a command in a separate process and wait";
	public static final String CONSOLE_HELP_EXEC_COMMAND_ARGUMENT_DESCRIPTION = "command to be executed";
	public static final String CONSOLE_HELP_FORK_COMMAND_DESCRIPTION = "execute a command in a separate process";
	public static final String CONSOLE_HELP_FORK_COMMAND_ARGUMENT_DESCRIPTION = "command to be executed";
	public static final String CONSOLE_HELP_HEADERS_COMMAND_DESCRIPTION = "print bundle headers";
	public static final String CONSOLE_HELP_HEADERS_COMMAND_ARGUMENT_DESCRIPTION = "bundles to print headers for";
	public static final String CONSOLE_PROPS_COMMAND_DESCRIPTION = "Display system properties";
	public static final String CONSOLE_HELP_SETPROP_COMMAND_DESCRIPTION = "set OSGi properties";
	public static final String CONSOLE_HELP_SETPROP_COMMAND_ARGUMENTS_DESCRIPTION = "list of properties with values to be set; the format is <key>=<value> and the pairs are separated with space if more than one";
	public static final String CONSOLE_HELP_SS_COMMAND_DESCRIPTION = "display installed bundles (short status)";
	public static final String CONSOLE_THREADS_COMMAND_DESCRIPTION = "display threads and thread groups";
	public static final String CONSOLE_HELP_SL_COMMAND_DESCRIPTION = "display the start level for the specified bundle, or for the framework if no bundle specified";
	public static final String CONSOLE_HELP_SL_COMMAND_ARGUMENT_DESCRIPTION = "bundle to get the start level";
	public static final String CONSOLE_HELP_SETFWSL_COMMAND_DESCRIPTION = "set the framework start level";
	public static final String CONSOLE_HELP_SETFWSL_COMMAND_ARGUMENT_DESCRIPTION = "new start level";
	public static final String CONSOLE_HELP_SETBSL_COMMAND_DESCRIPTION = "set the start level for the bundle(s)";
	public static final String CONSOLE_HELP_SETBSL_COMMAND_ARGUMENT_DESCRIPTION = "bundle(s) to change startlevel";
	public static final String CONSOLE_HELP_SETIBSL_COMMAND_DESCRIPTION = "set the initial bundle start level";
	public static final String CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_DESCRIPTION = "lists required bundles having the specified symbolic name";
	public static final String CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_ARGUMENT_DESCRIPTION = "symbolic name for required bundles to be listed; if not specified all required bundles will be listed";
	public static final String CONSOLE_HELP_PROFILELOG_COMMAND_DESCRIPTION = "Display & flush the profile log messages";
	public static final String CONSOLE_HELP_VISIBLE_PACKAGES_COMMAND_DESCRIPTION = "lists all packages visible from the specified bundle";
	public static final String CONSOLE_HELP_VISIBLE_PACKAGES_COMMAND_ARGUMENTS_DESCRIPTION = "bundle to list the visible packages";
	public static final String CONSOLE_HELP_GETPROP_COMMAND_DESCRIPTION = "displays the system properties with the given name, or all of them";
	public static final String CONSOLE_HELP_GETPROP_COMMAND_ARGUMENT_DESCRIPTION = "name of system property to dispaly";
	public static final String CONSOLE_HELP_DIAG_COMMAND_DESCRIPTION = "Displays unsatisfied constraints for the specified bundle(s)";
	public static final String CONSOLE_HELP_DIAG_COMMAND_ARGUMENT_DESCRIPTION = "IDs of bundle(s), for which to display unsatisfied constraints";
	public static final String CONSOLE_HELP_ENABLE_COMMAND_DESCRIPTION="Enable the specified bundle(s)";
	public static final String CONSOLE_HELP_ENABLE_COMMAND_ARGUMENT_DESCRIPTION="IDs of bundle(s) to enable";
	public static final String CONSOLE_HELP_DISABLE_COMMAND_DESCRIPTION="Disable the specified bundle(s)";
	public static final String CONSOLE_HELP_DISABLE_COMMAND_ARGUMENT_DESCRIPTION="IDs of bundle(s) to disable";
	public static final String CONSOLE_HELP_LD_COMMAND_DESCRIPTION="List disabled bundles in the system";

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, ConsoleMsg.class);
	}
}
