/*******************************************************************************
 * Copyright (c) 2005, 2015 Cognos Incorporated, IBM Corporation and others.
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;

//This class unwraps the request so it can be processed by the underlying servlet container.
public class RequestDispatcherAdaptor implements RequestDispatcher {

	private final DispatchTargets dispatchTargets;
	private final String path;
	private final String string;

	public RequestDispatcherAdaptor(
		DispatchTargets dispatchTargets, String path) {

		this.dispatchTargets = dispatchTargets;
		this.path = path;

		this.string = getClass().getSimpleName() + '[' + path + ", " + dispatchTargets + ']'; //$NON-NLS-1$
	}

	public void forward(ServletRequest request, ServletResponse response)
		throws IOException, ServletException {

		response.resetBuffer();

		dispatchTargets.doDispatch(
			(HttpServletRequest)request, (HttpServletResponse)response,
			path, DispatcherType.FORWARD);
	}

	public void include(ServletRequest request, ServletResponse response)
		throws IOException, ServletException {

		dispatchTargets.doDispatch(
			(HttpServletRequest)request, (HttpServletResponse)response,
			path, DispatcherType.INCLUDE);
	}

	@Override
	public String toString() {
		return string;
	}

}
