/*******************************************************************************
 * Copyright (c) 2015 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 460720
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.util;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class BaseChangeSessionIdServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	public BaseChangeSessionIdServlet(String content) {
		super(content);
	}

	@Override
	protected void service(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException {

		request.getSession(true);

		request.changeSessionId();

		response.getWriter().print(content);
	}

}
