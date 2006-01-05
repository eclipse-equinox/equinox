/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http;

import org.eclipse.osgi.util.NLS;

public class HttpMsg extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.http.ExternalMessages"; //$NON-NLS-1$

	public static String HTTP_ALIAS_ALREADY_REGISTERED_EXCEPTION;
	public static String HTTP_SERVLET_ALREADY_REGISTERED_EXCEPTION;
	public static String HTTP_SERVLET_NULL_EXCEPTION;
	public static String HTTP_SERVLET_EXCEPTION;
	public static String HTTP_ALIAS_UNREGISTER_EXCEPTION;
	public static String HTTP_ALIAS_INVALID_EXCEPTION;
	public static String HTTP_RESOURCE_NAME_INVALID_EXCEPTION;
	public static String HTTP_SERVET_INIT_EXCEPTION;
	public static String HTTP_DEFAULT_MIME_TABLE_ERROR;
	public static String HTTP_STATUS_CODES_TABLE_ERROR;
	public static String HTTP_STATUS_CODE_NOT_FOUND;
	public static String HTTP_ACCEPT_SOCKET_EXCEPTION;
	public static String HTTP_PORT_IN_USE_EXCEPTION;
	public static String HTTP_INVALID_VALUE_RANGE_EXCEPTION;
	public static String HTTP_CONNECTION_EXCEPTION;
	public static String HTTP_INVALID_SCHEME_EXCEPTION;
	public static String HTTP_NO_HEADER_LINE_READ_EXCEPTION;
	public static String HTTP_QUERYDATA_PARSE_EXCEPTION;
	public static String HTTP_INVALID_HEADER_LINE_EXCEPTION;
	public static String HTTP_HEADER_LINE_TOO_LONG_EXCEPTION;
	public static String HTTP_UNEXPECTED_IOEXCEPTION;
	public static String HTTP_UNEXPECTED_RUNTIMEEXCEPTION;
	public static String HTTP_ONLY_SUPPORTS_2_1;
	public static String HTTP_HOST_UNKNOWN;
	public static String OSGi_Http_Service_IBM_Implementation_16;
	public static String IBM_Http_Service_37;
	public static String Jan_1;
	public static String Feb_2;
	public static String Mar_3;
	public static String Apr_4;
	public static String May_5;
	public static String Jun_6;
	public static String Jul_7;
	public static String Aug_8;
	public static String Sep_9;
	public static String Oct_10;
	public static String Nov_11;
	public static String Dec_12;
	public static String Sun_13;
	public static String Mon_14;
	public static String Tue_15;
	public static String Wed_16;
	public static String Thu_17;
	public static String Fri_18;
	public static String Sat_19;
	public static String HTTP_DEFAULT_PORT_FORMAT_EXCEPTION;
	public static String HTTP_THREAD_POOL_CREATE_NUMBER_ERROR;

	static {
		// initialize resource bundles
		NLS.initializeMessages(BUNDLE_NAME, HttpMsg.class);
	}
}