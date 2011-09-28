/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	public static String OCD_ID_NOT_FOUND;
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
	public static String TOKENIZER_GOT_INVALID_DATA;
	public static String INVALID_PID_METATYPE_PROVIDER_IGNORED;
	public static String METADATA_PARSE_ERROR;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, MetaTypeMsg.class);
	}
}