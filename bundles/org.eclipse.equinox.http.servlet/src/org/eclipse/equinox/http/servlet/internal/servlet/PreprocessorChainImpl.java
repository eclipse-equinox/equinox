/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.registration.PreprocessorRegistration;

public class PreprocessorChainImpl implements FilterChain {

	private final ProxyServlet proxyServlet;
	private final List<PreprocessorRegistration> preprocessors;
	private final String alias;
	private final DispatcherType dispatcherType;
	private final int filterCount;
	private int filterIndex = 0;

	public PreprocessorChainImpl(
		List<PreprocessorRegistration> preprocessors,
		String alias, DispatcherType dispatcherType, ProxyServlet proxyServlet) {

		this.preprocessors = preprocessors;
		this.alias = alias;
		this.dispatcherType = dispatcherType;
		this.proxyServlet = proxyServlet;
		this.filterCount = preprocessors.size();
	}

	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		if (filterIndex < filterCount) {
			PreprocessorRegistration registration = preprocessors.get(filterIndex++);

			registration.doFilter(
				(HttpServletRequest) request, (HttpServletResponse) response, this);

			return;
		}

		proxyServlet.dispatch(
			(HttpServletRequest) request, (HttpServletResponse) response, alias, dispatcherType);
	}

}
