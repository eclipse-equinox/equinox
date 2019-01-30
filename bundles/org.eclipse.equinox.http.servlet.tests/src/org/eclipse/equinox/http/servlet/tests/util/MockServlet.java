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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class MockServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (content != null) {
			response.getWriter().write(content);
		}
		if (code != null) {
			response.sendError(code, errorMessage);
		}
		if (exception != null) {
			if (exception instanceof IOException) {
				throw (IOException) exception;
			}
			throw (ServletException) exception;
		}
	}

	public MockServlet content(String content) {
		this.content = content;

		return this;
	}

	public MockServlet error(int code, String errorMessage) {
		this.code = new Integer(code);
		this.errorMessage = errorMessage;

		return this;
	}

	public MockServlet exception(Exception exception) {
		if (!(exception instanceof ServletException) &&
				!(exception instanceof IOException)) {
			this.exception = new ServletException(exception);
		}

		this.exception = exception;

		return this;
	}

	private Integer	code;
	private String	content;
	private String	errorMessage;
	private Exception exception;

}
