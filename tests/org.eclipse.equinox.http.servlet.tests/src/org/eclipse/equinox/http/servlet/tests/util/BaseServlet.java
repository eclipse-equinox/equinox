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

package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Raymond Augé
 */
public class BaseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static final String ATTRIBUTE = "content";

	public BaseServlet() {
		this("");
	}

	public BaseServlet(String content) {
		this.content = content;
	}

	@SuppressWarnings("unused")
	@Override
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		request.setAttribute(ATTRIBUTE, content);
		request.setAttribute(ATTRIBUTE, "replaced");
		request.removeAttribute(ATTRIBUTE);

		ServletContext servletContext = request.getServletContext();

		if (servletContext.getAttribute(ATTRIBUTE) == null) {
			servletContext.setAttribute(ATTRIBUTE, content);
		}
		else if (servletContext.getAttribute(ATTRIBUTE).equals(content)) {
			servletContext.setAttribute(ATTRIBUTE, "replaced");
		}
		else {
			servletContext.removeAttribute(ATTRIBUTE);
		}

		HttpSession session = request.getSession();

		if (session.getAttribute(ATTRIBUTE) == null) {
			session.setAttribute(ATTRIBUTE, content);
		}
		else if (session.getAttribute(ATTRIBUTE).equals(content)) {
			session.setAttribute(ATTRIBUTE, "replaced");
		}
		else {
			session.removeAttribute(ATTRIBUTE);
		}

		response.getWriter().print(content);
	}

	protected String content;

}