/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;

//This class unwraps the request so it can be processed by the underlying servlet container.
public class NamedDispatcherAdaptor implements RequestDispatcher {

	private final DispatchTargets dispatchTargets;

	public NamedDispatcherAdaptor(DispatchTargets dispatchTargets) {
		this.dispatchTargets = dispatchTargets;
	}

	public void forward(ServletRequest req, ServletResponse resp)
		throws IOException, ServletException {

		if (req instanceof HttpServletRequestBuilder)
			req = ((HttpServletRequestBuilder) req).getRequest();

		doDispatch((HttpServletRequest)req, (HttpServletResponse)resp);
	}

	public void include(ServletRequest req, ServletResponse resp)
		throws IOException, ServletException {

		if (req instanceof HttpServletRequestBuilder)
			req = ((HttpServletRequestBuilder) req).getRequest();

		doDispatch((HttpServletRequest)req, (HttpServletResponse)resp);
	}

	private void doDispatch(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {

		DispatcherType dispatcherType = DispatcherType.REQUEST;

		if (request.getAttribute("javax.servlet.include.request_uri") != null) {
			request.setAttribute(
				"javax.servlet.include.request_uri", request.getRequestURI());
			request.setAttribute(
				"javax.servlet.include.context_path",
				dispatchTargets.getContextController().getContextPath());
			request.setAttribute(
				"javax.servlet.include.servlet_path",
				dispatchTargets.getServletPath());
			request.setAttribute(
				"javax.servlet.include.path_info",
				dispatchTargets.getPathInfo());

			dispatcherType = DispatcherType.INCLUDE;
		}

		HttpServletRequest wrappedRequest = new HttpServletRequestBuilder(
			request, dispatchTargets).build();
		HttpServletResponseWrapper wrapperResponse =
			new HttpServletResponseWrapperImpl(response);

		ResponseStateHandler responseStateHandler = new ResponseStateHandler(
			wrappedRequest, wrapperResponse, dispatchTargets, dispatcherType);

		responseStateHandler.processRequest();
	}

}
