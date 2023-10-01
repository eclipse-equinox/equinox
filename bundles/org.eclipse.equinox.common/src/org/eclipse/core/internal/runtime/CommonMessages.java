/**********************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Christoph Läubrich - Bug 577645 - [Adapters] provide a method that returns an Optional for an adapted type
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

	// URL
	public static String url_badVariant;
	public static String url_createConnection;
	public static String url_invalidURL;
	public static String url_noaccess;
	public static String url_noOutput;
	public static String url_resolveFragment;
	public static String url_resolvePlugin;

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

	public static String activator_not_available;
	public static String activator_resourceBundleNotFound;
	public static String activator_resourceBundleNotStarted;

	public static String adapters_internal_error_of;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, CommonMessages.class);
	}
}
