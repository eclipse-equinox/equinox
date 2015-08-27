/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.eclipse.equinox.http.servlet.tests;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.context.ContextPathCustomizer;
import org.eclipse.equinox.http.servlet.tests.bundle.Activator;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleAdvisor;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleInstaller;
import org.eclipse.equinox.http.servlet.tests.util.BaseServlet;
import org.eclipse.equinox.http.servlet.tests.util.ServletRequestAdvisor;

import org.junit.Assert;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import junit.framework.TestCase;

/**
 * @author Raymond Augé
 */
public class DispatchingTest extends TestCase {

	@Override
	public void setUp() throws Exception {
		BundleContext bundleContext = getBundleContext();
		installer = new BundleInstaller(ServletTest.TEST_BUNDLES_BINARY_DIRECTORY, bundleContext);
		advisor = new BundleAdvisor(bundleContext);
		String port = getPort();
		String contextPath = getContextPath();
		requestAdvisor = new ServletRequestAdvisor(port, contextPath);
		startBundles();
		stopJetty();
		startJetty();
	}

	@Override
	public void tearDown() throws Exception {
		for (ServiceRegistration<? extends Object> serviceRegistration : registrations) {
			serviceRegistration.unregister();
		}
		stopJetty();
		stopBundles();
		requestAdvisor = null;
		advisor = null;
		registrations.clear();
		try {
			installer.shutdown();
		} finally {
			installer = null;
		}
	}

	public void test_forwardDepth1() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").forward(request, response);
			}
		};

		Servlet servlet2 = new HttpServlet() {

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				PrintWriter writer = response.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(request.getPathInfo());
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(request.getRequestURI());
				writer.write("|");
				writer.write(request.getServletPath());
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH)));
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/b|u=5&p=1|/a/s2/b|/s2|/a|/d|p=1|/a/s1/d|/s1", response);
	}

	public void test_forwardDepth2() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/i2?p2=2").forward(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s3/i3?p3=3").forward(request, response);
			}
		};

		Servlet servlet3 = new HttpServlet() {

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				PrintWriter writer = response.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(request.getPathInfo());
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(request.getRequestURI());
				writer.write("|");
				writer.write(request.getServletPath());
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH)));
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet3, props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i3|p3=3&p2=2&p1=1|/c1/s3/i3|/s3|/c1|/i1|p1=1|/c1/s1/i1|/s1", response);
	}

	public void test_forwardDepth3() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/i2?p2=2").forward(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s3/i3?p3=3").forward(request, response);
			}
		};

		Servlet servlet3 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s4/i4?p4=4").forward(request, response);
			}
		};

		Servlet servlet4 = new HttpServlet() {

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				PrintWriter writer = response.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(request.getPathInfo());
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(request.getRequestURI());
				writer.write("|");
				writer.write(request.getServletPath());
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH)));
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet3, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s4/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet4, props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i4|p4=4&p3=3&p2=2&p1=1|/c1/s4/i4|/s4|/c1|/i1|p1=1|/c1/s1/i1|/s1", response);
	}

	public void test_forwardNamedParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.forward(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write(String.valueOf(req.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("p=1&p=2|null|1|[1, 2]", result);
	}

	public void test_forwardNamed() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.forward(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest request, HttpServletResponse resp)
				throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(String.valueOf(request.getPathInfo()));
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(String.valueOf(request.getRequestURI()));
				writer.write("|");
				writer.write(String.valueOf(request.getServletPath()));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH)));
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("/c1|/c1/s1/a|p=1&p=2|/c1/s1/a||null|null|null|null|null", result);
	}

	public void test_forwardParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				RequestDispatcher requestDispatcher = req.getRequestDispatcher("/Servlet13B/a?p=3&p=4");

				requestDispatcher.forward(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write((String)req.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING));
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13A");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13A/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13B");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13B/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("Servlet13A/a?p=1&p=2");

		Assert.assertEquals("p=3&p=4&p=1&p=2|p=1&p=2|3|[3, 4, 1, 2]", result);
	}

	public void test_includeBasic() throws Exception {
		Servlet servlet8 = new HttpServlet() {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				RequestDispatcher requestDispatcher =
					request.getRequestDispatcher("/S8/target");

				requestDispatcher.include(request, response);
			}

		};

		Servlet servlet8Target = new HttpServlet() {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				response.getWriter().print("s8target");
			}

		};

		HttpService httpService = getHttpService();

		HttpContext httpContext = httpService.createDefaultHttpContext();

		httpService.registerServlet("/S8", servlet8, null, httpContext);
		httpService.registerServlet("/S8/target", servlet8Target, null, httpContext);

		Assert.assertEquals("s8target", requestAdvisor.request("S8"));
	}

	public void test_includeDepth1() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").include(request, response);
			}
		};

		Servlet servlet2 = new HttpServlet() {

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				PrintWriter writer = response.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(request.getPathInfo());
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(request.getRequestURI());
				writer.write("|");
				writer.write(request.getServletPath());
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH)));
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/d|u=5&p=1|/a/s1/d|/s1|/a|/b|u=5|/a/s2/b|/s2", response);
	}

	public void test_includeDepth2() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/i2?p2=2").include(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s3/i3?p3=3").include(request, response);
			}
		};

		Servlet servlet3 = new HttpServlet() {

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				PrintWriter writer = response.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(request.getPathInfo());
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(request.getRequestURI());
				writer.write("|");
				writer.write(request.getServletPath());
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH)));
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet3, props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i1|p3=3&p2=2&p1=1|/c1/s1/i1|/s1|/c1|/i3|p3=3|/c1/s3/i3|/s3", response);
	}

	public void test_includeDepth3() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/i2?p2=2").include(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s3/i3?p3=3").include(request, response);
			}
		};

		Servlet servlet3 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s4/i4?p4=4").include(request, response);
			}
		};

		Servlet servlet4 = new HttpServlet() {

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				PrintWriter writer = response.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(request.getPathInfo());
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(request.getRequestURI());
				writer.write("|");
				writer.write(request.getServletPath());
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH)));
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet3, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s4/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet4, props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i1|p4=4&p3=3&p2=2&p1=1|/c1/s1/i1|/s1|/c1|/i4|p4=4|/c1/s4/i4|/s4", response);
	}

	public void test_includeNamedParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.include(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write(String.valueOf(req.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("p=1&p=2|null|1|[1, 2]", result);
	}

	public void test_includeNamed() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.include(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest request, HttpServletResponse resp)
				throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();

				writer.write(request.getContextPath());
				writer.write("|");
				writer.write(request.getPathInfo());
				writer.write("|");
				writer.write(request.getQueryString());
				writer.write("|");
				writer.write(request.getRequestURI());
				writer.write("|");
				writer.write(request.getServletPath());
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI)));
				writer.write("|");
				writer.write(String.valueOf(request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH)));
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {}, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("/c1|/c1/s1/a|p=1&p=2|/c1/s1/a||null|null|null|null|null", result);
	}

	public void test_includeParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				RequestDispatcher requestDispatcher = req.getRequestDispatcher("/Servlet13B/a?p=3&p=4");

				requestDispatcher.include(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write((String)req.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING));
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13A");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13A/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13B");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13B/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("Servlet13A/a?p=1&p=2");

		Assert.assertEquals("p=3&p=4&p=1&p=2|p=3&p=4|3|[3, 4, 1, 2]", result);
	}

	private String doRequest(String action, Map<String, String> params) throws IOException {
		return doRequestGetResponse(action, params).get("responseBody").get(0);
	}

	private Map<String, List<String>> doRequestGetResponse(String action, Map<String, String> params) throws IOException {
		StringBuilder requestInfo = new StringBuilder(ServletTest.PROTOTYPE);
		requestInfo.append(action);
		if (!params.isEmpty()) {
			boolean firstParam = true;
			for (Map.Entry<String, String> param : params.entrySet()) {
				if (firstParam) {
					requestInfo.append('?');
					firstParam = false;
				} else {
					requestInfo.append('&');
				}
				requestInfo.append(param.getKey());
				requestInfo.append('=');
				requestInfo.append(param.getValue());
			}
		}
		return requestAdvisor.request(requestInfo.toString(), null);
	}

	private BundleContext getBundleContext() {
		return Activator.getBundleContext();
	}

	private String getContextPath() {
		return getJettyProperty("context.path", "");
	}

	private HttpService getHttpService() {
		ServiceReference<HttpService> serviceReference = getBundleContext().getServiceReference(HttpService.class);
		return getBundleContext().getService(serviceReference);
	}

	private String getJettyProperty(String key, String defaultValue) {
		String qualifiedKey = ServletTest.JETTY_PROPERTY_PREFIX + key;
		String value = getProperty(qualifiedKey);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	private String getPort() {
		String defaultPort = getProperty(ServletTest.OSGI_HTTP_PORT_PROPERTY);
		if (defaultPort == null) {
			defaultPort = "80";
		}
		return getJettyProperty("port", defaultPort);
	}

	private String getProperty(String key) {
		BundleContext bundleContext = getBundleContext();
		String value = bundleContext.getProperty(key);
		return value;
	}

	private Bundle installBundle(String bundle) throws BundleException {
		return installer.installBundle(bundle);
	}

	private void startBundles() throws BundleException {
		for (String bundle : ServletTest.BUNDLES) {
			advisor.startBundle(bundle);
		}
	}

	private void startJetty() throws BundleException {
		advisor.startBundle(ServletTest.EQUINOX_JETTY_BUNDLE);
	}

	private void stopBundles() throws BundleException {
		for (int i = ServletTest.BUNDLES.length - 1; i >= 0; i--) {
			String bundle = ServletTest.BUNDLES[i];
			advisor.stopBundle(bundle);
		}
	}

	private void stopJetty() throws BundleException {
		advisor.stopBundle(ServletTest.EQUINOX_JETTY_BUNDLE);
	}

	private void uninstallBundle(Bundle bundle) throws BundleException {
		installer.uninstallBundle(bundle);
	}

	private static final String EQUINOX_DS_BUNDLE = "org.eclipse.equinox.ds";
	private static final String EQUINOX_JETTY_BUNDLE = "org.eclipse.equinox.http.jetty";
	private static final String JETTY_PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty.";
	private static final String OSGI_HTTP_PORT_PROPERTY = "org.osgi.service.http.port";
	private static final String STATUS_OK = "OK";
	private static final String TEST_BUNDLES_BINARY_DIRECTORY = "/bundles_bin/";
	private static final String TEST_BUNDLE_1 = "tb1";

	private static final String[] BUNDLES = new String[] {
		ServletTest.EQUINOX_DS_BUNDLE
	};

	private BundleInstaller installer;
	private BundleAdvisor advisor;
	private ServletRequestAdvisor requestAdvisor;
	private final Collection<ServiceRegistration<? extends Object>> registrations = new ArrayList<ServiceRegistration<? extends Object>>();

	static class TestServletContextHelperFactory implements ServiceFactory<ServletContextHelper> {
		static class TestServletContextHelper extends ServletContextHelper {
			public TestServletContextHelper(Bundle bundle) {
				super(bundle);
			}};
		@Override
		public ServletContextHelper getService(Bundle bundle, ServiceRegistration<ServletContextHelper> registration) {
			return new TestServletContextHelper(bundle);
		}

		@Override
		public void ungetService(Bundle bundle, ServiceRegistration<ServletContextHelper> registration,
				ServletContextHelper service) {
			// nothing
		}

	}

	static class TestContextPathAdaptor extends ContextPathCustomizer {
		private final String defaultFilter;
		private final String contextPrefix;
		private final String testName;

		/**
		 * @param defaultFilter
		 * @param contextPrefix
		 */
		public TestContextPathAdaptor(String defaultFilter, String contextPrefix, String testName) {
			super();
			this.defaultFilter = defaultFilter;
			this.contextPrefix = contextPrefix;
			this.testName = testName;
		}

		@Override
		public String getDefaultContextSelectFilter(ServiceReference<?> httpWhiteBoardService) {
			if (testName.equals(httpWhiteBoardService.getProperty("servlet.init." + ServletTest.TEST_PATH_CUSTOMIZER_NAME))) {
				return defaultFilter;
			}
			return null;
		}

		@Override
		public String getContextPathPrefix(ServiceReference<ServletContextHelper> helper) {
			if (testName.equals(helper.getProperty(ServletTest.TEST_PATH_CUSTOMIZER_NAME))) {
				return contextPrefix;
			}
			return null;
		}

	}

	static class ErrorServlet extends HttpServlet{
		private static final long serialVersionUID = 1L;
		private final String errorCode;

		public ErrorServlet(String errorCode) {
			super();
			this.errorCode = errorCode;
		}

		@Override
		protected void service(
				HttpServletRequest request, HttpServletResponse response)
			throws ServletException ,IOException {

			if (response.isCommitted()) {
				System.out.println("Problem?");

				return;
			}

			PrintWriter writer = response.getWriter();

			String requestURI = (String)request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			Integer status = (Integer)request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

			writer.print(errorCode + " : " + status + " : ERROR : " + requestURI);
		}

	};
}