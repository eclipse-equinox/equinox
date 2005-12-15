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
package org.eclipse.equinox.http.servlet;

import java.util.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import org.eclipse.equinox.http.HttpMsg;

public class ServletConfigImpl implements ServletConfig {
	protected ServletContext servletContext;
	protected Dictionary initParams;

	public ServletConfigImpl(ServletContext context, Dictionary params) {
		servletContext = context;
		initParams = params;
	}

	/**
	 *
	 * Returns a string containing the value of the named
	 * initialization parameter of the servlet, or null if the
	 * parameter does not exist.  Init parameters have a single string
	 * value; it is the responsibility of the servlet writer to
	 * interpret the string.
	 *
	 * @param name the name of the parameter whose value is requested
	 */
	public String getInitParameter(String name) {
		if (initParams != null) {
			return ((String) initParams.get(name));
		}
		return (null);
	}

	/**
	 * Returns the names of the servlet's initialization parameters
	 * as an enumeration of strings, or an empty enumeration if there
	 * are no initialization parameters.
	 */
	public Enumeration getInitParameterNames() {
		if (initParams != null) {
			return (initParams.keys());
		}
		return (new Vector(0).elements());
	}

	/**
	 * Returns the context for the servlet.
	 */

	public ServletContext getServletContext() {
		return (servletContext);
	}

	/**
	 * @see javax.servlet.ServletConfig#getServletName()
	 */
	public String getServletName() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

}
