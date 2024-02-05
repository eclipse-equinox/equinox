/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Raymond Augé - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.http.jetty;

/**
 * <p>
 * Provides configuration constants for use with JettyConfigurator.
 * </p>
 * 
 * @since 1.1
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface JettyConstants {

	/**
	 * name="http.enabled" type="Boolean" (default: true)
	 */
	public static final String HTTP_ENABLED = "http.enabled"; //$NON-NLS-1$

	/**
	 * name="http.port" type="Integer" (default: 0 -- first available port)
	 */
	public static final String HTTP_PORT = "http.port"; //$NON-NLS-1$

	/**
	 * name="http.host" type="String" (default: 0.0.0.0 -- all network adapters)
	 */
	public static final String HTTP_HOST = "http.host"; //$NON-NLS-1$

	/**
	 * name="http.nio" type="Boolean" (default: true, with some exceptions for JREs
	 * with known NIO problems)
	 * 
	 * @since 1.1
	 */
	public static final String HTTP_NIO = "http.nio"; //$NON-NLS-1$

	/**
	 * name="https.enabled" type="Boolean" (default: false)
	 */
	public static final String HTTPS_ENABLED = "https.enabled"; //$NON-NLS-1$

	/**
	 * name="https.host" type="String" (default: 0.0.0.0 -- all network adapters)
	 */
	public static final String HTTPS_HOST = "https.host"; //$NON-NLS-1$

	/**
	 * name="https.port" type="Integer" (default: 0 -- first available port)
	 */
	public static final String HTTPS_PORT = "https.port"; //$NON-NLS-1$

	/**
	 * name="http.maxThreads" type="Integer" (default: 200 -- max number of threads)
	 * 
	 * @since 1.2
	 */
	public static final String HTTP_MAXTHREADS = "http.maxThreads"; //$NON-NLS-1$

	/**
	 * name="http.maxThreads" type="Integer" (default: 8 -- max number of threads)
	 * 
	 * @since 1.2
	 */
	public static final String HTTP_MINTHREADS = "http.minThreads"; //$NON-NLS-1$

	/**
	 * @deprecated
	 * @since 1.3
	 */
	@Deprecated
	public static final String MULTIPART_FILESIZETHRESHOLD = "multipart.fileSizeThreshold"; //$NON-NLS-1$

	/**
	 * @deprecated
	 * @since 1.3
	 */
	@Deprecated
	public static final String MULTIPART_LOCATION = "multipart.location"; //$NON-NLS-1$

	/**
	 * @deprecated
	 * @since 1.3
	 */
	@Deprecated
	public static final String MULTIPART_MAXFILESIZE = "multipart.maxFileSize"; //$NON-NLS-1$

	/**
	 * @deprecated
	 * @since 1.3
	 */
	@Deprecated
	public static final String MULTIPART_MAXREQUESTSIZE = "multipart.maxRequestSize"; //$NON-NLS-1$

	/**
	 * name="ssl.keystore" type="String"
	 */
	public static final String SSL_KEYSTORE = "ssl.keystore"; //$NON-NLS-1$

	/**
	 * name="ssl.password" type="String"
	 */
	public static final String SSL_PASSWORD = "ssl.password"; //$NON-NLS-1$

	/**
	 * name="ssl.keypassword" type="String"
	 */
	public static final String SSL_KEYPASSWORD = "ssl.keypassword"; //$NON-NLS-1$

	/**
	 * name="ssl.needclientauth" type="Boolean"
	 */
	public static final String SSL_NEEDCLIENTAUTH = "ssl.needclientauth"; //$NON-NLS-1$

	/**
	 * name="ssl.wantclientauth" type="Boolean"
	 */
	public static final String SSL_WANTCLIENTAUTH = "ssl.wantclientauth"; //$NON-NLS-1$

	/**
	 * name="ssl.protocol" type="String"
	 */
	public static final String SSL_PROTOCOL = "ssl.protocol"; //$NON-NLS-1$

	/**
	 * name="ssl.algorithm" type="String"
	 */
	public static final String SSL_ALGORITHM = "ssl.algorithm"; //$NON-NLS-1$

	/**
	 * name="ssl.keystoretype" type="String"
	 */
	public static final String SSL_KEYSTORETYPE = "ssl.keystoretype"; //$NON-NLS-1$

	/**
	 * name="context.path" type="String"
	 */
	public static final String CONTEXT_PATH = "context.path"; //$NON-NLS-1$

	/**
	 * name="context.sessioninactiveinterval" type="Integer"
	 */
	public static final String CONTEXT_SESSIONINACTIVEINTERVAL = "context.sessioninactiveinterval"; //$NON-NLS-1$

	/**
	 * name="housekeeper.interval" type="Integer"
	 * 
	 * @since 1.5
	 */
	public static final String HOUSEKEEPER_INTERVAL = "housekeeper.interval"; //$NON-NLS-1$

	/**
	 * name="customizer.class" type="String" <br>
	 * (full qualified name of the class that implements
	 * <code>org.eclipse.equinox.http.jetty.JettyCustomizer</code> and has a public
	 * no-arg constructor; the class must be supplied via a fragment to this
	 * bundle's classpath)
	 * 
	 * @since 1.1
	 */
	public static final String CUSTOMIZER_CLASS = "customizer.class"; //$NON-NLS-1$

	/**
	 * name="other.info" type="String"
	 */
	public static final String OTHER_INFO = "other.info"; //$NON-NLS-1$

	/**
	 * @since 1.3
	 */
	public static final String PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty."; //$NON-NLS-1$

}
