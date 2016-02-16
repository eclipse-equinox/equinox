/*******************************************************************************
 * Copyright (c) 2016 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import org.eclipse.equinox.http.servlet.internal.servlet.ProxyMultipartServlet;

/**
 * The HttpServiceMultipartServlet is the "public" side of a Servlet that when registered (and init() called) in a servlet container
 * will be used by the OSGi Http Service implementation to handle multipart requests. This servlet must be paired with,
 * and initialized after an HttpServiceServlet. The HttpServiceServlet must be told the name of the multipart servlet it is paired with
 * using the init-param "multipart.servlet.name".
 * <p>
 * e.g.<br/>
 * <pre>
 * 	&lt;servlet>
 *		&lt;servlet-name>Equinox Http Service Servlet&lt;/servlet-name>
 *		&lt;servlet-class>org.eclipse.equinox.http.servlet.HttpServiceServlet&lt;/servlet-class>
 *		&lt;init-param>
 *			&lt;param-name>multipart.servlet.name&lt;/param-name>
 *			&lt;param-value>Equinox Http Service Multipart Servlet&lt;/param-value>
 *		&lt;/init-param>
 *		&lt;load-on-startup>0&lt;/load-on-startup>
 *	&lt;/servlet>
 *	&lt;servlet>
 *		&lt;servlet-name>Equinox Http Service Multipart Servlet&lt;/servlet-name>
 *		&lt;servlet-class>org.eclipse.equinox.http.servlet.HttpServiceMultipartServlet&lt;/servlet-class>
 *		&lt;load-on-startup>1&lt;/load-on-startup>
 *		&lt;multipart-config>
 *			&lt;location>&lt;/location>
 *			&lt;max-file-size>-1&lt;/max-file-size>
 *			&lt;max-request-size>-1&lt;/max-request-size>
 *			&lt;file-size-threshold>0&lt;/file-size-threshold>
 *		&lt;/multipart-config>
 *	&lt;/servlet>
 * </pre>
 * </p>
 * This class is not meant for extending or even using directly and is purely meant for registering
 * in a servlet container.
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.3
 */
public class HttpServiceMultipartServlet extends ProxyMultipartServlet {
	private static final long serialVersionUID = 2281118780429323631L;
}
