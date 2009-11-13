/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import org.eclipse.osgi.util.NLS;

// Runtime plugin message catalog
public class RegistryMessages extends NLS {
	/**
	 * The unique identifier constant of this plug-in.
	 */
	public static final String OWNER_NAME = "org.eclipse.equinox.registry"; //$NON-NLS-1$

	private static final String BUNDLE_NAME = "org.eclipse.core.internal.registry.messages"; //$NON-NLS-1$

	// Bundle
	public static String bundle_not_activated;

	// Extension Registry
	public static String meta_registryCacheWriteProblems;
	public static String meta_registryCacheReadProblems;
	public static String meta_regCacheIOExceptionReading;
	public static String meta_registryCacheInconsistent;
	public static String meta_unableToCreateCache;
	public static String meta_unableToReadCache;
	public static String registry_no_default;
	public static String registry_default_exists;
	public static String registry_bad_cache;
	public static String registry_non_multi_lang;

	// parsing/resolve
	public static String parse_error;
	public static String parse_errorNameLineColumn;
	public static String parse_internalStack;
	public static String parse_missingAttribute;
	public static String parse_missingAttributeLine;
	public static String parse_unknownAttribute;
	public static String parse_unknownAttributeLine;
	public static String parse_unknownElement;
	public static String parse_unknownElementLine;
	public static String parse_unknownTopElement;
	public static String parse_xmlParserNotAvailable;
	public static String parse_process;
	public static String parse_failedParsingManifest;
	public static String parse_nonSingleton;
	public static String parse_nonSingletonFragment;
	public static String parse_problems;
	public static String parse_duplicateExtension;
	public static String parse_duplicateExtensionPoint;

	// direct creation
	public static String create_failedExtensionPoint;

	// executable extensions
	public static String exExt_findClassError;
	public static String exExt_instantiateClassError;
	public static String exExt_initObjectError;
	public static String exExt_extDefNotFound;

	// plugins
	public static String plugin_eventListenerError;
	public static String plugin_initObjectError;
	public static String plugin_instantiateClassError;
	public static String plugin_loadClassError;

	// logging
	public static String log_error;
	public static String log_warning;
	public static String log_log;

	// Adapter manager
	public static String adapters_badAdapterFactory;
	public static String adapters_cantInstansiate;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, RegistryMessages.class);
	}
}
