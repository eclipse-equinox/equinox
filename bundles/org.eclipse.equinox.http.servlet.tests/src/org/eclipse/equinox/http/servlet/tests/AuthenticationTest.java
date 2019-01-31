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

package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.context.ServletContextHelper;

public class AuthenticationTest extends BaseTest {

	@Test
	public void test_forwardSecurity() throws Exception {
		final List<String> callStack = new ArrayList<>();
		setup(callStack);

		// request without auth -> no servlet invoked
		requestAdvisor.request("context1/servlet?forward=true");
		assertTrue(callStack.isEmpty());

		// request with auth and forward -> servlet invoked
		requestAdvisor.request("context1/servlet?forward=true&auth=true");
		assertEquals(6, callStack.size());
		assertEquals(Arrays.asList("handle1", "servlet/context1", "handle1", "servlet/context1", "finish1", "finish1"), callStack);
		callStack.clear();
	}

	@Test
	public void test_handleFinishSecurity() throws Exception {
		final List<String> callStack = new ArrayList<>();
		setup(callStack);

		// request without auth -> no servlet invoked
		requestAdvisor.request("context1/servlet");
		assertTrue(callStack.isEmpty());

		// request with auth -> servlet invoked
		requestAdvisor.request("context1/servlet?auth=true");
		assertEquals(3, callStack.size());
		assertEquals(Arrays.asList("handle1", "servlet/context1", "finish1"), callStack);
		callStack.clear();

		// request to context2, no auth required
		requestAdvisor.request("context2/servlet");
		assertEquals(3, callStack.size());
		assertEquals(Arrays.asList("handle2", "servlet/context2", "finish2"), callStack);
	}

	private static final String	AUTH_PAR	= "auth";

	private static final String	REC_PAR		= "rec";

	private void setup(final List<String> callStack) throws Exception {
		final BundleContext context = getBundleContext();

		// setup context 1
		final Dictionary<String,Object> ctx1Props = new Hashtable<String,Object>();
		ctx1Props.put(HTTP_WHITEBOARD_CONTEXT_NAME, "context1");
		ctx1Props.put(HTTP_WHITEBOARD_CONTEXT_PATH, "/context1");
		registrations.add(context.registerService(
			ServletContextHelper.class,
			new ServletContextHelper() {

				@Override
				public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
					throws IOException {

					if (request.getParameter(AUTH_PAR) != null) {
						callStack.add("handle1");
						return true;
					}
					return false;
				}

				@Override
				public void finishSecurity(final HttpServletRequest request, final HttpServletResponse response) {
					callStack.add("finish1");
				}

			},
			ctx1Props)
		);

		// setup context 2
		final Dictionary<String,Object> ctx2Props = new Hashtable<String,Object>();
		ctx2Props.put(HTTP_WHITEBOARD_CONTEXT_NAME, "context2");
		ctx2Props.put(HTTP_WHITEBOARD_CONTEXT_PATH, "/context2");
		registrations.add(context.registerService(
			ServletContextHelper.class,
			new ServletContextHelper() {

				@Override
				public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response)
					throws IOException {

					callStack.add("handle2");
					return true;
				}

				@Override
				public void finishSecurity(final HttpServletRequest request, final HttpServletResponse response) {
					callStack.add("finish2");
				}

			},
			ctx2Props)
		);

		// servlet for both contexts
		@SuppressWarnings("serial")
		class AServlet extends HttpServlet {

			@Override
			protected void service(HttpServletRequest request,
					HttpServletResponse response)
				throws ServletException, IOException {

				callStack.add("servlet" + request.getContextPath());

				if (request.getContextPath().equals("/context1")
						&& request.getAttribute(REC_PAR) == null) {
					if (request.getParameter("forward") != null) {
						request.setAttribute(REC_PAR, "true");
						request.getRequestDispatcher("/servlet")
									.forward(request, response);
						return;
					} else if (request.getParameter("include") != null) {
						request.setAttribute(REC_PAR, "true");
						request.getRequestDispatcher("/servlet")
									.include(request, response);
					} else if (request.getParameter("throw") != null) {
						callStack.add("throw");
						throw new ServletException("throw");
					}
				}
				response.setStatus(200);
			}

		};

		final Dictionary<String,Object> servletProps = new Hashtable<String,Object>();

		servletProps.put(HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"/servlet"});
		servletProps.put(HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=context1)");
		registrations.add(context.registerService(Servlet.class, new AServlet(), servletProps));

		servletProps.put(HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=context2)");
		registrations.add(context.registerService(Servlet.class, new AServlet(), servletProps));
	}

}
