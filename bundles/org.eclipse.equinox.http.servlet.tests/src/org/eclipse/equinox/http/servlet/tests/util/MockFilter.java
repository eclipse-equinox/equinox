/*******************************************************************************
 * Copyright (c) Jan. 26, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - tests
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class MockFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (before != null) {
			response.getWriter().write(before);
		}
		chain.doFilter(request, response);
		if (after != null) {
			response.getWriter().write(after);
		}
	}

	@Override
	public void init(FilterConfig config) {
	}

	public MockFilter after(String after) {
		this.after = after;

		return this;
	}

	public MockFilter around(String around) {
		before(around);
		after(around);

		return this;
	}

	public MockFilter before(String before) {
		this.before = before;

		return this;
	}

	private String after;
	private String before;

}
