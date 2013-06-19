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
package org.eclipse.osgi.compatibility.plugins;

import org.eclipse.osgi.util.NLS;

public class PluginConverterMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.osgi.compatibility.plugins.PluginConverterMsg"; //$NON-NLS-1$

	public static String ECLIPSE_CONVERTER_ERROR_CONVERTING;

	public static String ECLIPSE_CONVERTER_FILENOTFOUND;
	public static String ECLIPSE_CONVERTER_ERROR_CREATING_BUNDLE_MANIFEST;
	public static String ECLIPSE_CONVERTER_PLUGIN_LIBRARY_IGNORED;

	public static String ECLIPSE_CONVERTER_ERROR_PARSING_PLUGIN_MANIFEST;
	public static String ECLIPSE_CONVERTER_MISSING_ATTRIBUTE;
	public static String parse_error;
	public static String parse_errorNameLineColumn;

	public static String ECLIPSE_CONVERTER_NO_SAX_FACTORY;
	public static String ECLIPSE_CONVERTER_PARSE_UNKNOWNTOP_ELEMENT;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, PluginConverterMsg.class);
	}

}
