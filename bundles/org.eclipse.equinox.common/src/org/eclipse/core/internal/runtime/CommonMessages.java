/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.runtime;

import org.eclipse.osgi.util.NLS;

// Common runtime plugin message catalog
public class CommonMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.core.internal.runtime.commonMessages"; //$NON-NLS-1$

	public static String ok;

	// metadata
	public static String meta_couldNotCreate;
	public static String meta_instanceDataUnspecified;
	public static String meta_noDataModeSpecified;
	public static String meta_notDir;
	public static String meta_readonly;
	public static String meta_pluginProblems;

	// parsing/resolve
	public static String parse_doubleSeparatorVersion;
	public static String parse_emptyPluginVersion;
	public static String parse_fourElementPluginVersion;
	public static String parse_numericMajorComponent;
	public static String parse_numericMinorComponent;
	public static String parse_numericServiceComponent;
	public static String parse_oneElementPluginVersion;

	public static String parse_postiveMajor;
	public static String parse_postiveMinor;
	public static String parse_postiveService;
	public static String parse_separatorEndVersion;
	public static String parse_separatorStartVersion;

	// FileMananger messages
	public static String fileManager_cannotLock;
	public static String fileManager_couldNotSave;
	public static String fileManager_updateFailed;
	public static String fileManager_illegalInReadOnlyMode;
	public static String fileManager_notOpen;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, CommonMessages.class);
	}
}