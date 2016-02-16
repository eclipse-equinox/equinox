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
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.util.Const;

/**
 * The ProxyMultipartServlet is the private side of a Servlet that when registered (and init() called) in a servlet container
 * will handle all multipart requests targeted toward it's associated ProxyServlet.
 * This class is not meant for extending or even using directly and is purely meant for registering
 * in a servlet container.
 */
public class ProxyMultipartServlet extends HttpServlet {

	private static final long serialVersionUID = -9079427283290998897L;

	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		String alias = HttpServletRequestWrapperImpl.getDispatchPathInfo(request);

		if (alias == null) {
			alias = Const.SLASH;
		}

		DispatchTargets dispatchTargets = (DispatchTargets)request.getAttribute(DispatchTargets.class.getName());

		if (dispatchTargets != null) {
			dispatchTargets.doDispatch(
				request, response, alias, request.getDispatcherType());

			return;
		}

		response.sendError(
			HttpServletResponse.SC_NOT_FOUND, "ProxyMultipartServlet: " + alias); //$NON-NLS-1$
	}

}
