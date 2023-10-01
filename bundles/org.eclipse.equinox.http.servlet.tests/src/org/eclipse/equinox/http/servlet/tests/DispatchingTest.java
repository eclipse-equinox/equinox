/*******************************************************************************
 * Copyright (c) 2014, 2016 Raymond Augé.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - initial implementation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.BaseServlet;
import org.eclipse.equinox.http.servlet.tests.util.DispatchResultServlet;
import org.eclipse.equinox.http.servlet.tests.util.EventHandler;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class DispatchingTest extends BaseTest {

	@Test
	@Ignore("https://github.com/eclipse/jetty.project/issues/10617")
	public void test_crossContextDispatch1() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

				StringWriter writer = new StringWriter();

				writer.write(request.getContextPath());
				writer.write("|");

				ServletContext servletContext = getServletContext().getContext("/");

				writer.write(servletContext.getContextPath());

				response.getWriter().write(writer.toString());
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		String response = requestAdvisor.request("a/s1/d");

		Assert.assertEquals("/a|", response);
	}

	@Test
	public void test_forwardDepth1() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").forward(request, response);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/b|u=5|/a/s2/b|/s2|/a|/d|p=1|/a/s1/d|/s1", response);
	}

	@Test
	public void test_forwardDepth1_WithRequestFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").forward(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				response.getWriter().write('b');

				super.doFilter(request, response, chain);

				response.getWriter().write('b');
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/b|u=5|/a/s2/b|/s2|/a|/d|p=1|/a/s1/d|/s1", response);
		Assert.assertTrue(filter.getCalled());
	}

	@Test
	public void test_forwardDepth1_WithRequestAndForwardFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/i4?u=5").forward(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				response.getWriter().write('b');

				super.doFilter(request, response, chain);

				response.getWriter().write('b');
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.FORWARD.toString(), DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("b/a|/i4|u=5|/a/s2/i4|/s2|/a|/d|p=1|/a/s1/d|/s1b", response);
		Assert.assertEquals(2, filter.getCount());
	}

	@Test
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

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i3|p3=3|/c1/s3/i3|/s3|/c1|/i1|p1=1|/c1/s1/i1|/s1", response);
	}

	@Test
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

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet3, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s4/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i4|p4=4|/c1/s4/i4|/s4|/c1|/i1|p1=1|/c1/s1/i1|/s1", response);
	}

	@Test
	public void test_forwardNamedParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.forward(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(String.valueOf(req.getQueryString()));
				writer.write("|");
				writer.write(String.valueOf(req.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING)));
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("p=1&p=2|null|1|[1, 2]", result);
	}

	@Test
	public void test_forwardNamed() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.forward(req, resp);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("/c1|/a|p=1&p=2|/c1/s1/a|/s1|null|null|null|null|null", result);
	}

	@Test
	public void test_forwardParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				RequestDispatcher requestDispatcher = req.getRequestDispatcher("/Servlet13B/a?p=3&p=4");

				requestDispatcher.forward(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write((String) req.getAttribute(RequestDispatcher.FORWARD_QUERY_STRING));
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13A");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13A/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13B");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13B/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("Servlet13A/a?p=1&p=2");

		Assert.assertEquals("p=3&p=4|p=1&p=2|3|[3, 4, 1, 2]", result);
	}

	@Test
	public void test_forwardStreamed() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").forward(request, response);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/b|u=5|/a/s2/b|/s2|/a|/d|p=1|/a/s1/d|/s1", response);
	}

	@Test
	public void test_forwardStreamed_WithRequestFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").forward(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				write(response.getOutputStream(), "b");

				super.doFilter(request, response, chain);

				write(response.getOutputStream(), "b");
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/b|u=5|/a/s2/b|/s2|/a|/d|p=1|/a/s1/d|/s1", response);
		Assert.assertTrue(filter.getCalled());
	}

	@Test
	public void test_forwardStreamed_WithRequestAndForwardFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").forward(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				write(response.getOutputStream(), "b");

				super.doFilter(request, response, chain);

				write(response.getOutputStream(), "b");
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.FORWARD.toString(), DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("b/a|/b|u=5|/a/s2/b|/s2|/a|/d|p=1|/a/s1/d|/s1b", response);
		Assert.assertEquals(2, filter.getCount());
	}

	@Test
	public void test_includeBasic() throws Exception {
		Servlet servlet8 = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				RequestDispatcher requestDispatcher = request.getRequestDispatcher("/S8/target");

				requestDispatcher.include(request, response);
			}

		};

		Servlet servlet8Target = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {

				response.getWriter().print("s8target");
			}

		};

		HttpService httpService = getHttpService();

		HttpContext httpContext = httpService.createDefaultHttpContext();

		httpService.registerServlet("/S8", servlet8, null, httpContext);
		httpService.registerServlet("/S8/target", servlet8Target, null, httpContext);

		Assert.assertEquals("s8target", requestAdvisor.request("S8"));
	}

	@Test
	public void test_includeDepth1() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").include(request, response);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/d|p=1|/a/s1/d|/s1|/a|/b|u=5|/a/s2/b|/s2", response);
	}

	@Test
	public void test_includeDepth1_WithRequestFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").include(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				response.getWriter().write('b');

				super.doFilter(request, response, chain);

				response.getWriter().write('b');
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("b/a|/d|p=1|/a/s1/d|/s1|/a|/b|u=5|/a/s2/b|/s2b", response);
		Assert.assertTrue(filter.getCalled());
	}

	@Test
	public void test_includeDepth1_WithRequestAndIncludeFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").include(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				response.getWriter().write('b');

				super.doFilter(request, response, chain);

				response.getWriter().write('b');
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.INCLUDE.toString(), DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("bb/a|/d|p=1|/a/s1/d|/s1|/a|/b|u=5|/a/s2/b|/s2bb", response);
		Assert.assertEquals(2, filter.getCount());
	}

	@Test
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

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i1|p1=1|/c1/s1/i1|/s1|/c1|/i3|p3=3|/c1/s3/i3|/s3", response);
	}

	@Test
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

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s3/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet3, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s4/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("c1/s1/i1?p1=1");

		Assert.assertEquals("/c1|/i1|p1=1|/c1/s1/i1|/s1|/c1|/i4|p4=4|/c1/s4/i4|/s4", response);
	}

	@Test
	public void test_includeNamedParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.include(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

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

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("p=1&p=2|null|1|[1, 2]", result);
	}

	@Test
	public void test_includeNamed() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				ServletContext servletContext = getServletContext();

				RequestDispatcher requestDispatcher = servletContext.getNamedDispatcher("s2");

				requestDispatcher.include(req, resp);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=c1)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String result = requestAdvisor.request("c1/s1/a?p=1&p=2");

		Assert.assertEquals("/c1|/a|p=1&p=2|/c1/s1/a|/s1|null|null|null|null|null", result);
	}

	@Test
	public void test_includeParameterAggregationAndPrecedence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws ServletException, IOException {

				RequestDispatcher requestDispatcher = req.getRequestDispatcher("/Servlet13B/a?p=3&p=4");

				requestDispatcher.include(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write((String) req.getAttribute(RequestDispatcher.INCLUDE_QUERY_STRING));
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13A");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13A/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sA, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13B");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13B/*");
		registrations.add(getBundleContext().registerService(Servlet.class, sB, props));

		String result = requestAdvisor.request("Servlet13A/a?p=1&p=2");

		Assert.assertEquals("p=1&p=2|p=3&p=4|3|[3, 4, 1, 2]", result);
	}

	@Test
	public void test_includeStreamed() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").include(request, response);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("/a|/d|p=1|/a/s1/d|/s1|/a|/b|u=5|/a/s2/b|/s2", response);
	}

	@Test
	public void test_includeStreamed_WithRequestFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").include(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				write(response.getOutputStream(), "b");

				super.doFilter(request, response, chain);

				write(response.getOutputStream(), "b");
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("b/a|/d|p=1|/a/s1/d|/s1|/a|/b|u=5|/a/s2/b|/s2b", response);
		Assert.assertTrue(filter.getCalled());
	}

	@Test
	public void test_includeStreamed_WithRequestAndIncludeFilter() throws Exception {
		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("/s2/b?u=5").include(request, response);
			}
		};

		TestFilter filter = new TestFilter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				write(response.getOutputStream(), "b");

				super.doFilter(request, response, chain);

				write(response.getOutputStream(), "b");
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(Servlet.class, new DispatchResultServlet(), props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER,
				new String[] { DispatcherType.INCLUDE.toString(), DispatcherType.REQUEST.toString() });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("a/s1/d?p=1");

		Assert.assertEquals("bb/a|/d|p=1|/a/s1/d|/s1|/a|/b|u=5|/a/s2/b|/s2bb", response);
		Assert.assertEquals(2, filter.getCount());
	}

	@Test
	public void test_Bug479115() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

				final AtomicReference<String[]> results = new AtomicReference<>();

				Thread thread = new Thread() {

					@Override
					public void run() {
						String[] parts = new String[2];

						parts[0] = req.getContextPath();
						parts[1] = req.getRequestURI();

						results.set(parts);
					}

				};

				thread.start();

				try {
					thread.join();
				} catch (InterruptedException ie) {
					throw new IOException(ie);
				}

				Assert.assertNotNull(results.get());

				PrintWriter writer = resp.getWriter();
				writer.write(results.get()[0]);
				writer.write("|");
				writer.write(results.get()[1]);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "Bug479115");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Bug479115/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		String result = requestAdvisor.request("Bug479115/a");

		Assert.assertEquals("|/Bug479115/a", result);
	}

	@Test
	public void test_headers_include() throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		final long date1 = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365 * 30);
		final long date2 = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365 * 40);

		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				response.addCookie(new Cookie("foo", "bar"));
				response.addDateHeader("X-date", date1);
				response.addHeader("X-colour", "blue");
				response.addIntHeader("X-size", 20);
				response.setCharacterEncoding("UTF-8");
				response.setContentType("text/plain");
				response.setHeader("X-animal", "cat");
				response.setLocale(Locale.CANADA);
				response.setBufferSize(1024);

				request.getRequestDispatcher("/s2").include(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) {

				response.addCookie(new Cookie("foo", "baz"));
				response.addDateHeader("X-date", date2);
				response.addHeader("X-colour", "green");
				response.addIntHeader("X-size", 30);
				response.setCharacterEncoding("UTF-16");
				response.setContentType("text/json");
				response.setHeader("X-animal", "bog");
				response.setLocale(Locale.US);
				response.setBufferSize(0);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		Map<String, List<String>> response = requestAdvisor.request("s1", null);

		Assert.assertNotNull(response.get("Set-Cookie"));
		Assert.assertEquals("foo=bar", response.get("Set-Cookie").get(0));
		Assert.assertNotNull(response.get("X-date"));
		Assert.assertEquals(format.format(new Date(date1)), response.get("X-date").get(0));
		Assert.assertNotNull(response.get("X-colour"));
		Assert.assertEquals("blue", response.get("X-colour").get(0));
		Assert.assertNotNull(response.get("X-size"));
		Assert.assertEquals("20", response.get("X-size").get(0));

		String contentType = response.get("Content-Type").get(0);

		Assert.assertTrue(contentType.contains("text/plain;"));
		Assert.assertTrue(contentType.toLowerCase().contains("charset=utf-8"));
		Assert.assertEquals("en-CA", response.get("Content-Language").get(0));
		Assert.assertNotNull(response.get("X-animal"));
		Assert.assertEquals("cat", response.get("X-animal").get(0));
	}

	@Test
	public void test_headers_forward() throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		final long date1 = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365 * 30);
		final long date2 = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365 * 40);

		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				response.addCookie(new Cookie("foo", "bar"));
				response.addDateHeader("X-date", date1);
				response.addHeader("X-colour", "blue");
				response.addIntHeader("X-size", 20);
				response.setContentType("text/plain");
				response.setCharacterEncoding("UTF-8");
				response.setHeader("X-animal", "cat");
				response.setLocale(Locale.CANADA);
				response.setBufferSize(1024);

				request.getRequestDispatcher("/s2").forward(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) {

				response.addCookie(new Cookie("foo", "baz"));
				response.addDateHeader("X-date", date2);
				response.addHeader("X-colour", "green");
				response.addIntHeader("X-size", 30);
				response.setContentType("text/json");
				response.setCharacterEncoding("UTF-16");
				response.setHeader("X-animal", "dog");
				response.setLocale(Locale.US);
				response.setBufferSize(0);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s2/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		Map<String, List<String>> response = requestAdvisor.request("s1", null);

		// On Java 18 the order changed on the client for headers with multiple values.
		int multiValueIdx = new Version(18, 0, 0).compareTo(getJavaVersion()) > 0 ? 0 : 1;
		Assert.assertNotNull(response.get("Set-Cookie"));
		Assert.assertEquals("foo=baz", response.get("Set-Cookie").get(multiValueIdx));
		Assert.assertNotNull(response.get("X-date"));
		Assert.assertEquals(format.format(new Date(date2)), response.get("X-date").get(multiValueIdx));
		Assert.assertNotNull(response.get("X-colour"));
		Assert.assertEquals("green", response.get("X-colour").get(multiValueIdx));
		Assert.assertNotNull(response.get("X-size"));
		Assert.assertEquals("30", response.get("X-size").get(multiValueIdx));

		String contentType = response.get("Content-Type").get(0);

		Assert.assertTrue(contentType.contains("text/json;"));
		Assert.assertTrue(contentType.toLowerCase().contains("charset=utf-16"));
		Assert.assertEquals("en-US", response.get("Content-Language").get(0));
		Assert.assertNotNull(response.get("X-animal"));
		Assert.assertEquals("dog", response.get("X-animal").get(0));
	}

	private Version getJavaVersion() {
		Bundle system = getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		BundleWiring systemWiring = system.adapt(BundleWiring.class);
		@SuppressWarnings("unchecked")
		List<Version> versions = systemWiring
				.getCapabilities(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE). //
				stream()
				.filter(c -> c.getAttributes().get(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)
						.equals("JavaSE")) //
				.findFirst().map(c -> (List<Version>) c.getAttributes()
						.get(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE))
				.get();
		return versions.get(versions.size() - 1);
	}

	// Bug 493583
	@Test
	public void test_streamed_response_outputstream() throws Exception {
		final long interval = 100L;

		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				response.setContentType("text/event-stream");
				response.setCharacterEncoding("UTF-8");

				try (ServletOutputStream out = response.getOutputStream()) {
					for (int i = 1; i <= 10; ++i) {
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							throw new ServletException(e);
						}

						out.print("data: ");
						out.print(System.currentTimeMillis());
						out.print("\n\n");
						out.flush();
					}
				}
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		final AtomicLong previousTime = new AtomicLong(System.currentTimeMillis());
		final AtomicInteger counter = new AtomicInteger();
		final AtomicBoolean result = new AtomicBoolean(true);

		EventHandler handler = new EventHandler() {

			@Override
			public void handle(Map<String, String> eventMap) {
				super.handle(eventMap);

				long currentTime = System.currentTimeMillis();

				long diff = (currentTime - previousTime.get());

				System.out.println("Differential: " + diff);

				// check that there is at least a differential of half the interval
				// because we can't really guarantee that machine time will accurately
				// reflect the timeouts we've set
				if (diff < (interval / 2)) {
					result.set(false);
				}

				previousTime.set(currentTime);
				counter.incrementAndGet();
			}

		};

		requestAdvisor.eventSource("s1", null, handler);

		handler.close();

		Assert.assertTrue(
				"The interval between events was too short. It means that the response was not properly streamed.",
				result.get());
		Assert.assertEquals(10, counter.get());
	}

	// Bug 493583
	@Test
	public void test_streamed_response_writer() throws Exception {
		final long interval = 100L;

		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				response.setContentType("text/event-stream");
				response.setCharacterEncoding("UTF-8");

				try (PrintWriter writer = response.getWriter()) {
					for (int i = 1; i <= 10; ++i) {
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							throw new ServletException(e);
						}

						writer.print("data: ");
						writer.print(System.currentTimeMillis());
						writer.print("\n\n");
						writer.flush();
					}
				}
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		final AtomicLong previousTime = new AtomicLong(System.currentTimeMillis());
		final AtomicInteger counter = new AtomicInteger();
		final AtomicBoolean result = new AtomicBoolean(true);

		EventHandler handler = new EventHandler() {

			@Override
			public void handle(Map<String, String> eventMap) {
				super.handle(eventMap);

				long currentTime = System.currentTimeMillis();

				long diff = (currentTime - previousTime.get());

				System.out.println("Differential: " + diff);

				// check that there is at least a differential of half the interval
				// because we can't really guarantee that machine time will accurately
				// reflect the timeouts we've set
				if (diff < (interval / 2)) {
					result.set(false);
				}

				previousTime.set(currentTime);
				counter.incrementAndGet();
			}

		};

		requestAdvisor.eventSource("s1", null, handler);

		handler.close();

		Assert.assertTrue(
				"The interval between events was too short. It means that the response was not properly streamed.",
				result.get());
		Assert.assertEquals(10, counter.get());
	}

	@Test
	public void test_Bug497510_1() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

				String requestURI = req.getRequestURI();

				Assert.assertNotNull(requestURI);

				PrintWriter writer = resp.getWriter();
				writer.write(requestURI);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "Bug497510");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Bug497510/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		String result = requestAdvisor.request("Bug497510/a%20b%20c");

		Assert.assertEquals("/Bug497510/a%20b%20c", result);
	}

	@Test
	public void test_Bug497510_2() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

				String requestURI = req.getRequestURI();

				Assert.assertNotNull(requestURI);

				PrintWriter writer = resp.getWriter();
				writer.write(requestURI);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "Bug 497510");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Bug 497510/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		String result = requestAdvisor.request("Bug%20497510/a%20b%20c");

		Assert.assertEquals("/Bug%20497510/a%20b%20c", result);
	}
}
