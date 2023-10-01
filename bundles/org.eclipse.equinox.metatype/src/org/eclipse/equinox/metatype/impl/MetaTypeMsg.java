/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import org.eclipse.osgi.util.NLS;

public class MetaTypeMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.metatype.impl.ExternalMessages"; //$NON-NLS-1$

	public static String SERVICE_DESCRIPTION;
	public static String UNEXPECTED_ELEMENT;
	public static String UNEXPECTED_TEXT;
	public static String MISSING_ATTRIBUTE;
	public static String INVALID_TYPE;
	public static String MISSING_DESIGNATE_PID_AND_FACTORYPID;
	public static String OCD_PID_NOT_FOUND;
	public static String OCD_REF_NOT_FOUND;
	public static String MISSING_ELEMENT;
	public static String EXCEPTION_MESSAGE;
	public static String NULL_IS_INVALID;
	public static String VALUE_OUT_OF_RANGE;
	public static String VALUE_OUT_OF_OPTION;
	public static String CARDINALITY_VIOLATION;
	public static String NULL_OPTIONS;
	public static String INCONSISTENT_OPTIONS;
	public static String INVALID_OPTIONS;
	public static String INVALID_DEFAULTS;
	public static String METADATA_NOT_FOUND;
	public static String ASK_INVALID_LOCALE;
	public static String MISSING_REQUIRED_PARAMETER;
	public static String INVALID_PID_METATYPE_PROVIDER_IGNORED;
	public static String METADATA_FILE_PARSE_ERROR;
	public static String METADATA_PARSE_ERROR;
	public static String INVALID_DEFAULTS_XML;
	public static String INVALID_OPTIONS_XML;
	public static String VALUE_NOT_A_NUMBER;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, MetaTypeMsg.class);
	}
}
