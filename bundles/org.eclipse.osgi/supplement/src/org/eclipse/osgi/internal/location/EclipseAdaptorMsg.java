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
package org.eclipse.osgi.internal.location;

import org.eclipse.osgi.util.NLS;

public class EclipseAdaptorMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.osgi.internal.location.EclipseAdaptorMessages"; //$NON-NLS-1$

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

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, EclipseAdaptorMsg.class);
	}

}
