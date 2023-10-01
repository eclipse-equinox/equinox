/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.tb;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.tests.util.CharResponseWrapper;

/**
 * @author Raymond Augé
 */
public abstract class AbstractWhiteboardTestFilter implements Filter {

	public abstract char getChar();

	@Override
	public void destroy() {
		//
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		CharResponseWrapper charResponseWrapper = new CharResponseWrapper((HttpServletResponse) response);

		chain.doFilter(request, charResponseWrapper);

		String output = charResponseWrapper.toString();

		response.setContentLength(output.length() + 2);

		PrintWriter writer = response.getWriter();
		writer.print(getChar());
		writer.print(output);
		writer.print(getChar());
		writer.close();
	}

	@Override
	public void init(FilterConfig arg0) {
		//
	}

}
