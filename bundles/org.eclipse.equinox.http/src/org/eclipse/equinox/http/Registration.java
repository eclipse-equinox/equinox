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

import java.io.IOException;
import javax.servlet.*;
import org.eclipse.equinox.http.servlet.HttpServletRequestImpl;
import org.eclipse.equinox.http.servlet.HttpServletResponseImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public interface Registration {
	public abstract Bundle getBundle();

	public abstract HttpContext getHttpContext();

	public abstract void destroy();

	public abstract String getAlias();

	public abstract void service(HttpServletRequestImpl req, HttpServletResponseImpl res) throws ServletException, IOException;

	public abstract void service(ServletRequest req, ServletResponse res) throws ServletException, IOException;
}
