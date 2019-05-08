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

package org.eclipse.equinox.http.servlet.tests.tb1;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * @author Raymond Augé
 */
public class TestServlet9 extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;

	private final DispatchTo dispatchTo = new DispatchTo();
	@Override
	public void activate(ComponentContext componentContext) throws ServletException, NamespaceException {
		HttpService service = getHttpService();
		HttpContext context = service.createDefaultHttpContext();
		String alias = getAlias();
		service.registerServlet(alias, this, null, context);
		service.registerServlet(alias + "DispatchTo", dispatchTo, null, context);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		RequestDispatcher requestDispatcher =
			request.getServletContext().getNamedDispatcher(DispatchTo.class.getName());

		requestDispatcher.include(request, response);
	}

	@Override
	protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
		//
	}

	class DispatchTo extends AbstractTestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected void handleDoGet(HttpServletRequest request, PrintWriter writer) throws ServletException, IOException {
			writer.print(TestServlet9.this.getProperties().get(Constants.SERVICE_DESCRIPTION));
		}
		
	}
}
