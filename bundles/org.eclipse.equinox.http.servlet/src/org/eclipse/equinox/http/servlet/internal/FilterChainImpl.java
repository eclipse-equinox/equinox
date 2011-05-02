/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal;

import java.io.IOException;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FilterChainImpl implements FilterChain {

	private List matchingFilterRegistrations;
	private ServletRegistration registration;
	private int filterIndex = 0;
	private int filterCount;

	public FilterChainImpl(List matchingFilterRegistrations, ServletRegistration registration) {
		this.matchingFilterRegistrations = matchingFilterRegistrations;
		this.registration = registration;
		this.filterCount = matchingFilterRegistrations.size();
	}

	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		if (filterIndex < filterCount) {
			FilterRegistration filterRegistration = (FilterRegistration) matchingFilterRegistrations.get(filterIndex++);
			filterRegistration.doFilter((HttpServletRequest) request, (HttpServletResponse) response, this);
			return;
		}
		registration.service((HttpServletRequest) request, (HttpServletResponse) response);
	}
}
