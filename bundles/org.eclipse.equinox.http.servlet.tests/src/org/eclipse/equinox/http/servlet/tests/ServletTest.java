/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import junit.framework.TestCase;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.context.ContextPathCustomizer;
import org.eclipse.equinox.http.servlet.tests.bundle.Activator;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleAdvisor;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleInstaller;
import org.eclipse.equinox.http.servlet.tests.util.BaseAsyncServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseChangeSessionIdServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseHttpContext;
import org.eclipse.equinox.http.servlet.tests.util.BaseHttpSessionAttributeListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseHttpSessionIdListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletContextAttributeListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletContextListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletRequestAttributeListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletRequestListener;
import org.eclipse.equinox.http.servlet.tests.util.BufferedServlet;
import org.eclipse.equinox.http.servlet.tests.util.ServletRequestAdvisor;

import org.junit.Assert;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class ServletTest extends TestCase {

	@Override
	public void setUp() throws Exception {
		// Quiet logging for tests
		System.setProperty("/.LEVEL", "OFF");
		System.setProperty("org.eclipse.jetty.server.LEVEL", "OFF");
		System.setProperty("org.eclipse.jetty.servlet.LEVEL", "OFF");

		System.setProperty("org.osgi.service.http.port", "8090");
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

	public void test_ErrorPage1() throws Exception {
		String expected = "403 ERROR :";
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			response = requestAdvisor.request("TestErrorPage1/a", null);
		}
		finally {
			uninstallBundle(bundle);
		}
		String responseCode = response.get("responseCode").get(0);
		actual = response.get("responseBody").get(0);

		Assert.assertEquals("403", responseCode);
		Assert.assertTrue(
			"Expected <" + expected + "*> but got <" + actual + ">", actual.startsWith(expected));
	}

	public void test_ErrorPage2() throws Exception {
		String expected = "org.eclipse.equinox.http.servlet.tests.tb1.TestErrorPage2$MyException ERROR :";
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			response = requestAdvisor.request("TestErrorPage2/a", null);
		}
		finally {
			uninstallBundle(bundle);
		}
		String responseCode = response.get("responseCode").get(0);
		actual = response.get("responseBody").get(0);

		Assert.assertEquals("500", responseCode);
		Assert.assertTrue(
			"Expected <" + expected + "*> but got <" + actual + ">", actual.startsWith(expected));
	}

	public void test_ErrorPage3() throws Exception {
		String expected = "400 ERROR :";
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			response = requestAdvisor.request("TestErrorPage3/a", null);
		}
		finally {
			uninstallBundle(bundle);
		}
		String responseCode = response.get("responseCode").get(0);
		actual = response.get("responseBody").get(0);

		Assert.assertEquals("400", responseCode);
		Assert.assertTrue(
			"Expected <" + expected + "*> but got <" + actual + ">", actual.startsWith(expected));
	}

	/**
	 * This case should NOT hit the error servlet because the response is already
	 * committed. However, setting the response code is perfectly allowed.
	 */
	public void test_ErrorPage4() throws Exception {
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			response = requestAdvisor.request("TestErrorPage4/a", null);
		}
		finally {
			uninstallBundle(bundle);
		}
		String responseCode = response.get("responseCode").get(0);
		actual = response.get("responseBody").get(0);

		Assert.assertEquals("401", responseCode);
		Assert.assertEquals("", actual);
	}

	public void test_ErrorPage5() throws Exception {
		Dictionary<String, Object> errorProps = new Hashtable<String, Object>();
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E5.4xx");
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("4xx"), errorProps));
		errorProps = new Hashtable<String, Object>();
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E5.5xx");
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "5xx");
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("5xx"), errorProps));
		for(String expectedCode: Arrays.asList("400", "450", "499", "500", "550", "599")) {
			Map<String, List<String>> response = doRequestGetResponse(ERROR, Collections.singletonMap(TEST_ERROR_CODE, expectedCode));
			String expectedResponse = expectedCode.charAt(0) + "xx : " + expectedCode + " : ERROR";
			String actualCode = response.get("responseCode").get(0);
			String actualResponse = response.get("responseBody").get(0);

			Assert.assertEquals(expectedCode, actualCode);
			Assert.assertTrue(
				"Expected <" + expectedResponse + "*> but got <" + actualResponse + ">", actualResponse.startsWith(expectedResponse));
		}
	}

	/**
	 * This test should also not hit the error servlet as we've only set the
	 * status. As per the Servlet spec this should not trigger error handling.
	 */
	public void test_ErrorPage6() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				response.getWriter().write("Hello!");
				response.setStatus(444);
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E6");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage6/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E6.error");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "444");
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("444"), props));

		Map<String, List<String>> response = requestAdvisor.request("TestErrorPage6/a", null);

		String responseCode = response.get("responseCode").get(0);
		String responseBody = response.get("responseBody").get(0);

		Assert.assertEquals("444", responseCode);
		Assert.assertNotEquals("444 : 444 : ERROR : /TestErrorPage6/a", responseBody);
	}

	/**
	 * This test should also not hit the error servlet as we've only set the
	 * status. As per the Servlet spec this should not trigger error handling.
	 */
	public void test_ErrorPage7() throws Exception {
		final int status = 422;

		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				resp.setStatus(status);

				PrintWriter printWriter = new PrintWriter(
					resp.getOutputStream());

				printWriter.println("{");
				printWriter.println("error: 'An error message',");
				printWriter.println("code: 'An error code'");
				printWriter.println("}");

				printWriter.flush();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E7");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage7/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E7.error");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, String.valueOf(status));
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet(String.valueOf(status)), props));

		Map<String, List<String>> response = requestAdvisor.request("TestErrorPage7/a", null);

		String responseCode = response.get("responseCode").get(0);
		String responseBody = response.get("responseBody").get(0);

		Assert.assertEquals(String.valueOf(status), responseCode);
		Assert.assertNotEquals(status + " : " + status + " : ERROR : /TestErrorPage7/a", responseBody);
	}

	public void test_ErrorPage8() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				throw new RuntimeException();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E8");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage8/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E8.error");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, RuntimeException.class.getName());
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("500"), props));

		Map<String, List<String>> response = requestAdvisor.request("TestErrorPage8/a", null);

		String responseCode = response.get("responseCode").get(0);
		String responseBody = response.get("responseBody").get(0);

		Assert.assertEquals("500", responseCode);
		Assert.assertEquals("500 : 500 : ERROR : /TestErrorPage8/a", responseBody);
	}

	public void test_ErrorPage9() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				throw new IOException();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E9");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage9/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E9.error");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, IOException.class.getName());
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("500"), props));

		Map<String, List<String>> response = requestAdvisor.request("TestErrorPage9/a", null);

		String responseCode = response.get("responseCode").get(0);
		String responseBody = response.get("responseBody").get(0);

		Assert.assertEquals("500", responseCode);
		Assert.assertEquals("500 : 500 : ERROR : /TestErrorPage9/a", responseBody);
	}

	public void test_ErrorPage10() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				resp.getWriter().write("some output");
				resp.flushBuffer();

				throw new IOException();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E10");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage10/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		try {
			requestAdvisor.request("TestErrorPage10/a");
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IOException);

			return;
		}

		Assert.fail("Expecting java.io.IOException: Premature EOF");
	}

	public void test_ErrorPage11() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				resp.sendError(403);
				resp.getOutputStream().flush();
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E10");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage11/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		try {
			requestAdvisor.request("TestErrorPage11/a");
		} catch (IOException e) {
			// This is expected because of old behavior
			// TODO is this really the correct behavior though
		}
	}

	public void test_Filter1() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter1/bab");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter2() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter2/cbabc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter3() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter3/cbdadbc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter4() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter4/dcbabcd");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter5() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/bab.TestFilter5");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter6() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/cbabc.TestFilter6");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter7() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/cbdadbc.TestFilter7");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter8() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/dcbabcd.TestFilter8");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter9() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter9/bab");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter10() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter10/cbabc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter11() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter11/cbdadbc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter12() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter12/dcbabcd");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter13() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter13");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter14() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter14");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter15() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter15");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter16() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter16");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter17() throws Exception {
		String expected = "ebcdadcbe";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter17/foo/bar/baz");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter18() throws Exception {
		String expected = "dbcacbd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter18/foo/bar/baz");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Filter19() throws Exception {
		String expected = "dfbcacbfd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter18/foo/bar/baz/with/path/info");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	static class TestFilter implements Filter {
		AtomicBoolean called = new AtomicBoolean(false);
		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			// nothing
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			called.set(true);
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
			// nothing
		}
		void clear() {
			called.set(false);
		}

		public boolean getCalled() {
			return called.get();
		}
	}
	public void test_Filter20() throws Exception {
		// Make sure legacy filter registrations match against all controllers that are for legacy HttpContext
		// Make sure legacy filter registrations match as if they are prefix matching with wildcards
		String expected = "a";
		TestFilter testFilter1 = new TestFilter();
		TestFilter testFilter2 = new TestFilter();
		Servlet testServlet = new BaseServlet(expected);
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		extendedHttpService.registerFilter("/hello", testFilter1, null, extendedHttpService.createDefaultHttpContext());
		extendedHttpService.registerFilter("/hello/*", testFilter2, null, extendedHttpService.createDefaultHttpContext());
		extendedHttpService.registerServlet("/hello", testServlet, null, extendedHttpService.createDefaultHttpContext());

		String actual = requestAdvisor.request("hello");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertTrue("testFilter2 did not get called.", testFilter2.getCalled());

		testFilter1.clear();
		testFilter2.clear();
		actual = requestAdvisor.request("hello/test");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertTrue("testFilter2 did not get called.", testFilter2.getCalled());
	}

	public void test_Filter21() throws Exception {
		// Make sure exact path matching is honored by filters registrations
		String expected = "a";
		TestFilter testFilter1 = new TestFilter();
		TestFilter testFilter2 = new TestFilter();
		Servlet testServlet = new BaseServlet(expected);

		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/hello");
		registrations.add(getBundleContext().registerService(Filter.class, testFilter1, props));

		props = new Hashtable<String, String>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/hello/*");
		registrations.add(getBundleContext().registerService(Filter.class, testFilter2, props));

		props = new Hashtable<String, String>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/hello/*");
		registrations.add(getBundleContext().registerService(Servlet.class, testServlet, props));

		String actual = requestAdvisor.request("hello");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertTrue("testFilter2 did not get called.", testFilter2.getCalled());

		testFilter1.clear();
		testFilter2.clear();
		actual = requestAdvisor.request("hello/test");
		Assert.assertEquals(expected, actual);
		Assert.assertFalse("testFilter1 got called.", testFilter1.getCalled());
		Assert.assertTrue("testFilter2 did not get called.", testFilter2.getCalled());
	}



	public void basicFilterTest22( String servlet1Pattern, String servlet2Pattern, String filterPattern, String expected, String[] dispatchers ) throws Exception {
		final AtomicReference<HttpServletRequestWrapper> httpServletRequestWrapper = new AtomicReference<HttpServletRequestWrapper>();
		final AtomicReference<HttpServletResponseWrapper> httpServletResponseWrapper = new AtomicReference<HttpServletResponseWrapper>();

		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("index.jsp").forward(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet("a") {

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				if ((httpServletRequestWrapper.get() != null) && !request.equals(httpServletRequestWrapper.get())) {
					throw new ServletException("not the same request");
				}
				if ((httpServletResponseWrapper.get() != null) && !response.equals(httpServletResponseWrapper.get())) {
					throw new ServletException("not the same response");
				}

				response.getWriter().print(content);
			}

		};

		Filter filter = new TestFilter() {

			@Override
			public void doFilter(
					ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

				response.getWriter().write('b');

				httpServletRequestWrapper.set(new HttpServletRequestWrapper((HttpServletRequest) request));
				httpServletResponseWrapper.set(new HttpServletResponseWrapper((HttpServletResponse) response));

				chain.doFilter(httpServletRequestWrapper.get(), httpServletResponseWrapper.get());

				response.getWriter().write('b');
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servlet1Pattern);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servlet2Pattern);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F22");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, dispatchers);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, filterPattern);
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("f22/a");

		Assert.assertEquals(expected, response);
	}

	public void test_Filter22a() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "/f22/*", "a", new String[] {"REQUEST"} );
	}

	public void test_Filter22b() throws Exception {
		basicFilterTest22 ( "/*", "*.jsp", "/*", "a", new String[] {"REQUEST"} );
	}

	public void test_Filter22c() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "*.jsp", "a", new String[] {"REQUEST"} );
	}

	public void test_Filter22d() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "/f22/*", "bab", new String[] {"FORWARD"} );
	}

	public void test_Filter22e() throws Exception {
		basicFilterTest22 ( "/*", "*.jsp", "/*", "bab", new String[] {"FORWARD"} );
	}

	public void test_Filter22f() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "*.jsp", "bab", new String[] {"FORWARD"} );
	}

	public void test_Filter22g() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "/f22/*", "bab", new String[] {"REQUEST", "FORWARD"} );
	}

	public void test_Filter22h() throws Exception {
		basicFilterTest22 ( "/*", "*.jsp", "/*", "bab", new String[] {"REQUEST", "FORWARD"} );
	}

	public void test_Filter22i() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "*.jsp", "bab", new String[] {"REQUEST", "FORWARD"} );
	}

	public void test_Registration1() throws Exception {
		String expected = "Alias cannot be null";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				null, new BaseServlet(), null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration2() throws Exception {
		String pattern = "blah";
		String expected = "Invalid pattern '" + pattern + "'";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				pattern, new BaseServlet(), null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration3() throws Exception {
		String pattern = "/blah/";
		String expected = "Invalid pattern '" + pattern + "'";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				pattern, new BaseServlet(), null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration4() throws Exception {
		String pattern = "/blah";
		String expected = "Pattern already in use: " + pattern;
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				pattern, new BaseServlet(), null, null);
			extendedHttpService.registerServlet(
				pattern, new BaseServlet(), null, null);
		}
		catch(NamespaceException ne) {
			Assert.assertEquals(expected, ne.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration5() throws Exception {
		String alias = "/blah";
		String expected = "Servlet cannot be null";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				alias, null, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration6() throws Exception {
		String expected = "Servlet has already been registered:";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Servlet servlet = new BaseServlet();

			extendedHttpService.registerServlet("/blah1", servlet, null, null);
			extendedHttpService.registerServlet("/blah2", servlet, null, null);
		}
		catch(ServletException se) {
			Assert.assertTrue(se.getMessage().startsWith(expected));

			return;
		}

		Assert.fail();
	}

	public void test_Registration11() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		Servlet servlet = new BaseServlet();

		extendedHttpService.registerServlet("/blah1", servlet, null, null);

		BundleContext bundleContext = getBundleContext();

		ServiceReference<HttpServiceRuntime> serviceReference =
			bundleContext.getServiceReference(HttpServiceRuntime.class);
		HttpServiceRuntime runtime = bundleContext.getService(serviceReference);

		RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

		ServletContextDTO[] servletContextDTOs = runtimeDTO.servletContextDTOs;

		for (ServletContextDTO servletContextDTO : servletContextDTOs) {
			if (servletContextDTO.name.startsWith("org.eclipse.equinox.http.servlet.internal.HttpServiceImpl$")) {
				ServletDTO servletDTO = servletContextDTO.servletDTOs[0];

				Assert.assertFalse(servletDTO.asyncSupported);
				Assert.assertEquals(servlet.getClass().getName(), servletDTO.name);
				Assert.assertEquals("/blah1", servletDTO.patterns[0]);
				Assert.assertTrue(servletDTO.serviceId < 0);
			}
		}
	}

	public void test_Registration12() throws Exception {
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			BundleContext bundleContext = getBundleContext();

			ServiceReference<HttpServiceRuntime> serviceReference =
				bundleContext.getServiceReference(HttpServiceRuntime.class);
			HttpServiceRuntime runtime = bundleContext.getService(serviceReference);

			RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

			ServletContextDTO[] servletContextDTOs = runtimeDTO.servletContextDTOs;

			ServletContextDTO servletContextDTO = servletContextDTOs[0];

			Assert.assertNotNull(servletContextDTO.name);
		}
		finally {
			uninstallBundle(bundle);
		}
	}

	public void test_Registration13() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		extendedHttpService.registerResources("/blah1", "/foo", null);

		BundleContext bundleContext = getBundleContext();

		ServiceReference<HttpServiceRuntime> serviceReference =
			bundleContext.getServiceReference(HttpServiceRuntime.class);
		HttpServiceRuntime runtime = bundleContext.getService(serviceReference);

		RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

		ServletContextDTO[] servletContextDTOs = runtimeDTO.servletContextDTOs;

		for (ServletContextDTO servletContextDTO : servletContextDTOs) {
			if (servletContextDTO.name.startsWith("org.eclipse.equinox.http.servlet.internal.HttpServiceImpl$")) {
				ResourceDTO resourceDTO = servletContextDTO.resourceDTOs[0];

				Assert.assertEquals("/blah1/*", resourceDTO.patterns[0]);
				Assert.assertEquals("/foo", resourceDTO.prefix);
				Assert.assertTrue(resourceDTO.serviceId < 0);
			}
		}
	}

	public void test_Registration14() throws Exception {
		Servlet initError = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			public void init(ServletConfig config) throws ServletException {
				throw new ServletException("Init error.");
			}

		};
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		try {
			extendedHttpService.registerServlet("/foo", initError, null, null);
			fail("Expected an init failure.");
		} catch (ServletException e) {
			//expected
			assertEquals("Wrong exception message.", "Init error.", e.getMessage());
		}
	}

	public void test_Registration15() throws Exception {
		Servlet initError = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			public void init(ServletConfig config) throws ServletException {
				throw new IllegalStateException("Init error.");
			}

		};
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		try {
			extendedHttpService.registerServlet("/foo", initError, null, null);
			fail("Expected an init failure.");
		} catch (IllegalStateException e) {
			//expected
			assertEquals("Wrong exception message.", "Init error.", e.getMessage());
		}
	}

	public void test_Registration16() throws Exception {
		Filter initError = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				throw new IllegalStateException("Init error.");
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
					ServletException {
				// nothing
			}

			@Override
			public void destroy() {
				// nothing
			}
		};
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		try {
			extendedHttpService.registerFilter("/foo", initError, null, null);
			fail("Expected an init failure.");
		} catch (IllegalStateException e) {
			//expected
			assertEquals("Wrong exception message.", "Init error.", e.getMessage());
		}
	}

	public void test_Registration17() throws Exception {
		Filter initError = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				throw new ServletException("Init error.");
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
					ServletException {
				// nothing
			}

			@Override
			public void destroy() {
				// nothing
			}
		};
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		try {
			extendedHttpService.registerFilter("/foo", initError, null, null);
			fail("Expected an init failure.");
		} catch (ServletException e) {
			//expected
			assertEquals("Wrong exception message.", "Init error.", e.getMessage());
		}
	}

	public void test_RegistrationTCCL1() {
		final Set<String> filterTCCL = Collections.synchronizedSet(new HashSet<String>());
		final Set<String> servletTCCL = Collections.synchronizedSet(new HashSet<String>());
		Filter tcclFilter = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				filterTCCL.add(Thread.currentThread().getContextClassLoader().getClass().getName());
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
					ServletException {
				filterTCCL.add(Thread.currentThread().getContextClassLoader().getClass().getName());
				chain.doFilter(request, response);
			}

			@Override
			public void destroy() {
				filterTCCL.add(Thread.currentThread().getContextClassLoader().getClass().getName());
			}
		};
		HttpServlet tcclServlet = new HttpServlet() {

			@Override
			public void destroy() {
				super.destroy();
				servletTCCL.add(Thread.currentThread().getContextClassLoader().getClass().getName());
			}

			@Override
			public void init(ServletConfig config) throws ServletException {
				super.init(config);
				servletTCCL.add(Thread.currentThread().getContextClassLoader().getClass().getName());
			}

			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
					IOException {
				servletTCCL.add(Thread.currentThread().getContextClassLoader().getClass().getName());
				response.getWriter().print(Thread.currentThread().getContextClassLoader().getClass().getName());
			}

		};

		ClassLoader originalTCCL = Thread.currentThread().getContextClassLoader();
		ClassLoader dummy = new ClassLoader() {
		};
		Thread.currentThread().setContextClassLoader(dummy);
		String expected = dummy.getClass().getName();
		String actual = null;
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		try {
			extendedHttpService.registerFilter("/tccl", tcclFilter, null, null);
			extendedHttpService.registerServlet("/tccl", tcclServlet, null, null);
			actual = requestAdvisor.request("tccl");
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		} finally {
			Thread.currentThread().setContextClassLoader(originalTCCL);
			try {
				extendedHttpService.unregister("/tccl");
				extendedHttpService.unregisterFilter(tcclFilter);
			} catch (IllegalArgumentException e) {
				// ignore
			}
		}
		assertEquals(expected, actual);
		assertEquals("Wrong filterTCCL size: " + filterTCCL, 1, filterTCCL.size());
		assertTrue("Wrong filterTCCL: " + filterTCCL, filterTCCL.contains(expected));
		assertEquals("Wrong httpTCCL size: " + servletTCCL, 1, servletTCCL.size());
		assertTrue("Wrong servletTCCL: " + servletTCCL, servletTCCL.contains(expected));

	}

	public void test_Sessions01() {
		final AtomicBoolean valueBound = new AtomicBoolean(false);
		final AtomicBoolean valueUnbound = new AtomicBoolean(false);
		final HttpSessionBindingListener bindingListener = new HttpSessionBindingListener() {

			@Override
			public void valueUnbound(HttpSessionBindingEvent event) {
				valueUnbound.set(true);
			}

			@Override
			public void valueBound(HttpSessionBindingEvent event) {
				valueBound.set(true);
			}
		};
		final AtomicBoolean sessionCreated = new AtomicBoolean(false);
		final AtomicBoolean sessionDestroyed = new AtomicBoolean(false);
		HttpSessionListener sessionListener = new HttpSessionListener() {

			@Override
			public void sessionDestroyed(HttpSessionEvent se) {
				sessionDestroyed.set(true);
			}

			@Override
			public void sessionCreated(HttpSessionEvent se) {
				sessionCreated.set(true);
			}
		};
		HttpServlet sessionServlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
					IOException {
				HttpSession session = request.getSession();
				if (session.getAttribute("test.attribute") == null) {
					session.setAttribute("test.attribute", bindingListener);
					response.getWriter().print("created");
				} else {
					session.invalidate();
					response.getWriter().print("invalidated");
				}
			}

		};
		ServiceRegistration<Servlet> servletReg = null;
		ServiceRegistration<HttpSessionListener> sessionListenerReg = null;
		Dictionary<String, Object> servletProps = new Hashtable<String, Object>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sessions");
		String actual = null;
		CookieHandler previous = CookieHandler.getDefault();
		CookieHandler.setDefault(new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );
		try {
			servletReg = getBundleContext().registerService(Servlet.class, sessionServlet, servletProps);
			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			sessionListenerReg = getBundleContext().registerService(HttpSessionListener.class, sessionListener, listenerProps);

			sessionCreated.set(false);
			valueBound.set(false);
			sessionDestroyed.set(false);
			valueUnbound.set(false);
			// first call will create the session
			actual = requestAdvisor.request("sessions");
			assertEquals("Wrong result", "created", actual);
			assertTrue("No sessionCreated called", sessionCreated.get());
			assertTrue("No valueBound called", valueBound.get());
			assertFalse("sessionDestroyed was called", sessionDestroyed.get());
			assertFalse("valueUnbound was called", valueUnbound.get());

			sessionCreated.set(false);
			valueBound.set(false);
			sessionDestroyed.set(false);
			valueUnbound.set(false);
			// second call will invalidate the session
			actual = requestAdvisor.request("sessions");
			assertEquals("Wrong result", "invalidated", actual);
			assertFalse("sessionCreated was called", sessionCreated.get());
			assertFalse("valueBound was called", valueBound.get());
			assertTrue("No sessionDestroyed called", sessionDestroyed.get());
			assertTrue("No valueUnbound called", valueUnbound.get());

			sessionCreated.set(false);
			sessionDestroyed.set(false);
			valueBound.set(false);
			valueUnbound.set(false);
			// calling again should create the session again
			actual = requestAdvisor.request("sessions");
			assertEquals("Wrong result", "created", actual);
			assertTrue("No sessionCreated called", sessionCreated.get());
			assertTrue("No valueBound called", valueBound.get());
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		} finally {
			if (servletReg != null) {
				servletReg.unregister();
			}
			if (sessionListenerReg != null) {
				sessionListenerReg.unregister();
			}
			CookieHandler.setDefault(previous);
		}
	}

	public void test_Resource1() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource1/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Resource2() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource2/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Resource3() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource3/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Resource4() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource4/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Resource5() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource5/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Runtime() throws Exception {
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();

			BundleContext bundleContext = getBundleContext();

			ServiceReference<HttpServiceRuntime> serviceReference = bundleContext.getServiceReference(HttpServiceRuntime.class);
			HttpServiceRuntime runtime = bundleContext.getService(serviceReference);

			Assert.assertNotNull(runtime);

			RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();

			ServletContextDTO[] servletContextDTOs = runtimeDTO.servletContextDTOs;

			Assert.assertTrue(servletContextDTOs.length > 0);
		} finally {
			uninstallBundle(bundle);
		}
	}

	public void test_Servlet1() throws Exception {
		String expected = ServletTest.STATUS_OK;
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet1");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet2() throws Exception {
		String expected = "3";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet2");
			Assert.assertEquals(expected, actual);
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet3() throws Exception {
		String expected = ServletTest.STATUS_OK;
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet3");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet4() throws Exception {
		String expected = System.getProperty(ServletTest.JETTY_PROPERTY_PREFIX + "context.path", "");
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet4");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet5() throws Exception {
		String expected = "Equinox Jetty-based Http Service";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet5");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet6() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestServlet6");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet7() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet7/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet8() throws Exception {
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

	public void test_Servlet9() throws Exception {
		String expected = "Equinox Jetty-based Http Service";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet9");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet10() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet10");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet11() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet11");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_Servlet12() throws Exception {
		Servlet sA = new HttpServlet() {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				response.getWriter().write('a');
			}

		};

		Servlet sB = new HttpServlet() {

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				response.getWriter().write('b');
			}

		};

		HttpService httpService = getHttpService();

		HttpContext httpContext = httpService.createDefaultHttpContext();

		httpService.registerServlet("*.txt", sA, null, httpContext);
		httpService.registerServlet("/files/*.txt", sB, null, httpContext);

		Assert.assertEquals("b", requestAdvisor.request("files/help.txt"));
	}

	public void test_Servlet13() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		String result = requestAdvisor.request("Servlet13/a?p=1&p=2");

		Assert.assertEquals("p=1&p=2|1|[1, 2]", result);
	}

	public void test_Servlet14() throws Exception {
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

	public void test_Servlet15() throws Exception {
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

	public void test_ServletContext1() throws Exception {
		String expected = "/org/eclipse/equinox/http/servlet/tests/tb1/resource1.txt";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServletContext1");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);

	}

	public void test_ServletContext1_2() throws Exception {
		String expected = "/org/eclipse/equinox/http/servlet/tests/tb1/resource1.txt";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServletContext1");
			Assert.assertEquals(expected, actual);
			bundle.stop();
			bundle.start();
			bundle.stop();
			bundle.start();
			actual = requestAdvisor.request("TestServletContext1");
		} finally {
			uninstallBundle(bundle);
		}

		Assert.assertEquals(expected, actual);
	}

	public void test_ServletContext2() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				getServletContext().setAttribute("name", null);
			}

		};

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/S1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<String>> response = requestAdvisor.request("S1/a", null);

		String responseCode = response.get("responseCode").get(0);

		Assert.assertEquals("200", responseCode);
	}

	public void testServletContextUnsupportedOperations() {
		final AtomicReference<ServletContext> contextHolder = new AtomicReference<ServletContext>();
		Servlet unsupportedServlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;
			@Override
			public void init(ServletConfig config) throws ServletException {
				contextHolder.set(config.getServletContext());
			}
		};

		ServiceRegistration<Servlet> servletReg = null;
		Dictionary<String, Object> servletProps = new Hashtable<String, Object>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sessions");
		try {
			servletReg = getBundleContext().registerService(Servlet.class, unsupportedServlet, servletProps);
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		} finally {
			if (servletReg != null) {
				servletReg.unregister();
			}
		}
		ServletContext context = contextHolder.get();
		assertNotNull("Null context.", context);
		for(Method m : getUnsupportedMethods()) {
			checkMethod(m, context);
		}
	}

	private void checkMethod(Method m, ServletContext context) throws RuntimeException {
		Class<?>[] types = m.getParameterTypes();
		Object[] params = new Object[types.length];
		try {
			m.invoke(context, params);
			fail("Expected an exception.");
		} catch (IllegalAccessException e) {
			fail("unexpected: " + e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getTargetException();
			if (!(cause instanceof UnsupportedOperationException)) {
				fail("unexpected exception for " + m.getName() + ": " + cause);
			}
		}

	}

	static private List<Method> getUnsupportedMethods() {
		List<Method> methods = new ArrayList<Method>();
		Class<ServletContext> contextClass = ServletContext.class;
		for(Method m : contextClass.getMethods()) {
			String name = m.getName();
			if (name.equals("addFilter") || name.equals("addListener") || name.equals("addServlet") || name.equals("createFilter") || name.equals("createListener") || name.equals("createServlet") | name.equals("declareRoles")) {
				methods.add(m);
			}
		}
		return methods;
	}

	public void test_ServletContextHelper1() throws Exception {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Dictionary<String, String> contextProps = new Hashtable<String, String>();
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<String, String>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "test.sch.one");
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<String, String>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/test-sch2");
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<String, String>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "Test SCH 3!");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/test-sch3");
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<String, String>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "test.sch.four");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "test$sch$4");
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		ServiceReference<HttpServiceRuntime> serviceReference =
			bundleContext.getServiceReference(HttpServiceRuntime.class);
		HttpServiceRuntime runtime = bundleContext.getService(serviceReference);

		RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
		Assert.assertEquals(5, runtimeDTO.failedServletContextDTOs.length);

		for (ServiceRegistration<?> registration : registrations) {
			registration.unregister();
		}
		registrations.clear();

		runtimeDTO = runtime.getRuntimeDTO();
		Assert.assertEquals(0, runtimeDTO.failedServletContextDTOs.length);
	}

	public void test_ServletContextHelper7() throws Exception {
		String expected = "a";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<String, String>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps = new Hashtable<String, String>();
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1");
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Servlet.class, s1, servletProps));

			String actual = requestAdvisor.request("s1");

			Assert.assertEquals(expected, actual);
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_ServletContextHelper8() throws Exception {
		String expected = "b";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet("b");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<String, String>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps = new Hashtable<String, String>();
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1");
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Servlet.class, s1, servletProps));

			String actual = requestAdvisor.request("a/s1");

			Assert.assertEquals(expected, actual);
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_ServletContextHelper9() throws Exception {
		String expected1 = "c";
		String expected2 = "d";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet(expected1);
		Servlet s2 = new BaseServlet(expected2);

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<String, String>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			registrations.add(bundleContext.registerService(Servlet.class, s1, servletProps1));

			Dictionary<String, String> servletProps2 = new Hashtable<String, String>();
			servletProps2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			servletProps2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Servlet.class, s2, servletProps2));

			String actual = requestAdvisor.request("s");

			Assert.assertEquals(expected1, actual);

			actual = requestAdvisor.request("a/s");

			Assert.assertEquals(expected2, actual);
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_ServletContextHelperVisibility() throws Exception {
		String expected1 = "c";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet(expected1);


		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			// register a hook that hides the helper from the registering bundle
			registrations.add(bundleContext.registerService(FindHook.class, new FindHook() {

				@Override
				public void find(BundleContext context, String name, String filter, boolean allServices,
						Collection<ServiceReference<?>> references) {
					if (ServletContextHelper.class.getName().equals(name) && context.getBundle().equals(getBundleContext().getBundle())) {
						references.clear();
					}
				}
			}, null));

			Dictionary<String, String> contextProps = new Hashtable<String, String>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps2 = new Hashtable<String, String>();
			servletProps2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			servletProps2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Servlet.class, s1, servletProps2));

			try {
				requestAdvisor.request("a/s");
			} catch (FileNotFoundException e) {
				// expected
			}
		} finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_ServletContextHelper10() throws Exception {
		String expected = "cac";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("a/TestServletContextHelper10/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	private static final String HTTP_CONTEXT_TEST_ROOT = "http.context.test";

	public void test_ServletContextHelper11() throws Exception {
		BaseHttpContext ctx1 = new BaseHttpContext(true, HTTP_CONTEXT_TEST_ROOT + "/1", getBundleContext().getBundle());
		BaseHttpContext ctx2 = new BaseHttpContext(true, HTTP_CONTEXT_TEST_ROOT + "/2", getBundleContext().getBundle());
		String actual;
		try {
			getHttpService().registerResources("/" + HTTP_CONTEXT_TEST_ROOT + "/1", "", ctx1);
			getHttpService().registerResources("/" + HTTP_CONTEXT_TEST_ROOT + "/2", "", ctx2);
			actual = requestAdvisor.request(HTTP_CONTEXT_TEST_ROOT + "/1/test");
			Assert.assertEquals("1", actual);
			actual = requestAdvisor.request(HTTP_CONTEXT_TEST_ROOT + "/2/test");
			Assert.assertEquals("2", actual);
		}
		finally {
			try {
				getHttpService().unregister("/" + HTTP_CONTEXT_TEST_ROOT + "/1");
				getHttpService().unregister("/" + HTTP_CONTEXT_TEST_ROOT + "/2");
			} catch (IllegalArgumentException e) {
				// ignore
			}
		}
	}

	public void test_ServletContextHelper12() throws Exception {
		String expected1 = "a,b,1";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(
					HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				StringBuilder builder = new StringBuilder();
				builder.append(request.getServletContext().getInitParameter("a")).append(',');
				builder.append(request.getServletContext().getInitParameter("b")).append(',');
				builder.append(request.getServletContext().getInitParameter("c"));
				response.getWriter().print(builder.toString());
			}
		};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, Object> contextProps = new Hashtable<String, Object>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "a", "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "b", "b");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "c", new Integer(1));
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Servlet.class, s1, servletProps1));

			String actual = requestAdvisor.request("a/s");

			Assert.assertEquals(expected1, actual);
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_ServletContextHelper13() throws Exception {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		// test that the helper handlesecurity is called before the filter by setting an attribute on the request
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				request.setAttribute(getName(), Boolean.TRUE);
				return super.handleSecurity(request, response);
			}

		};
		Filter f1 = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				if (request.getAttribute(getName()) == Boolean.TRUE) {
					request.setAttribute(getName() + ".fromFilter", Boolean.TRUE);
				}
				chain.doFilter(request, response);
			}

			@Override
			public void destroy() {
			}

		};
		Servlet s1 = new HttpServlet() {
			private static final long serialVersionUID = 1L;
			@Override
			public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
				res.getWriter().print(req.getAttribute(getName() + ".fromFilter"));
			}

		};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<String, String>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> filterProps = new Hashtable<String, String>();
			filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
			filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Filter.class, f1, filterProps));

			Dictionary<String, String> servletProps = new Hashtable<String, String>();
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s1");
			servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Servlet.class, s1, servletProps));

			String actual = requestAdvisor.request("s1");

			Assert.assertEquals(Boolean.TRUE.toString(), actual);
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_Listener1() throws Exception {
		BaseServletContextListener scl1 =
			new BaseServletContextListener();

		Dictionary<String, String> listenerProps = new Hashtable<String, String>();
		listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		ServiceRegistration<ServletContextListener> registration = getBundleContext().registerService(ServletContextListener.class, scl1, listenerProps);
		registration.unregister();


		Assert.assertTrue(scl1.initialized.get());
		Assert.assertTrue(scl1.destroyed.get());
	}

	public void test_Listener2() throws Exception {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<String, String>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			BaseServletContextListener scl1 =
					new BaseServletContextListener();
			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			ServiceRegistration<ServletContextListener> registration = getBundleContext().registerService(ServletContextListener.class, scl1, listenerProps);
			registration.unregister();

			Assert.assertTrue(scl1.initialized.get());
			Assert.assertTrue(scl1.destroyed.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_Listener3() throws Exception {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		BaseServletContextListener scl1 = new BaseServletContextListener();
		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<String, String>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(bundleContext.registerService(ServletContextListener.class, scl1, listenerProps));

			Assert.assertTrue(scl1.initialized.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
			Assert.assertTrue(scl1.destroyed.get());
		}
	}

	public void test_Listener4() throws Exception {

		BaseServletContextAttributeListener scal1 =
			new BaseServletContextAttributeListener();
		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletContextAttributeListener.class, scal1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			registrations.add(getBundleContext().registerService(Servlet.class, s1, servletProps1));

			requestAdvisor.request("s");

			Assert.assertTrue(scal1.added.get());
			Assert.assertFalse(scal1.replaced.get());
			Assert.assertFalse(scal1.removed.get());

			requestAdvisor.request("s");

			Assert.assertTrue(scal1.added.get());
			Assert.assertTrue(scal1.replaced.get());
			Assert.assertFalse(scal1.removed.get());

			requestAdvisor.request("s");

			Assert.assertTrue(scal1.added.get());
			Assert.assertTrue(scal1.replaced.get());
			Assert.assertTrue(scal1.removed.get());

		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_Listener5() throws Exception {

		BaseServletRequestListener srl1 = new BaseServletRequestListener();

		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletRequestListener.class, srl1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			registrations.add(getBundleContext().registerService(Servlet.class, s1, servletProps1));

			requestAdvisor.request("s");

			Assert.assertTrue(srl1.initialized.get());
			Assert.assertTrue(srl1.destroyed.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_Listener6() throws Exception {

		BaseServletRequestAttributeListener sral1 = new BaseServletRequestAttributeListener();

		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletRequestAttributeListener.class, sral1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			registrations.add(getBundleContext().registerService(Servlet.class, s1, servletProps1));

			requestAdvisor.request("s");

			Assert.assertTrue(sral1.added.get());
			Assert.assertTrue(sral1.replaced.get());
			Assert.assertTrue(sral1.removed.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_Listener7() throws Exception {

		BaseHttpSessionAttributeListener hsal1 =
			new BaseHttpSessionAttributeListener();

		Servlet s1 = new BaseServlet("test_Listener7");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(HttpSessionAttributeListener.class, hsal1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			registrations.add(getBundleContext().registerService(Servlet.class, s1, servletProps1));

			Map<String, List<String>> responseMap = requestAdvisor.request("s", null);

			Assert.assertTrue(hsal1.added.get());
			Assert.assertFalse(hsal1.replaced.get());
			Assert.assertFalse(hsal1.removed.get());

			List<String> list = responseMap.get("Set-Cookie");

			String sessionId = "";

			for (String string : list) {
				if (string.startsWith("JSESSIONID=")) {
					sessionId = string;

					int pos = sessionId.indexOf(';');
					if (pos != -1) {
						sessionId = sessionId.substring(0, pos);
					}
				}
			}

			Map<String, List<String>> requestHeaders = new HashMap<String, List<String>>();
			requestHeaders.put("Cookie", Arrays.asList(sessionId));

			requestAdvisor.request("s", requestHeaders);

			Assert.assertTrue(hsal1.added.get());
			Assert.assertTrue(hsal1.replaced.get());
			Assert.assertFalse(hsal1.removed.get());

			requestAdvisor.request("s", requestHeaders);

			Assert.assertTrue(hsal1.added.get());
			Assert.assertTrue(hsal1.replaced.get());
			Assert.assertTrue(hsal1.removed.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_Listener8() throws Exception {
		BaseHttpSessionIdListener hsil1 = new BaseHttpSessionIdListener();

		Servlet s1 = new BaseChangeSessionIdServlet("test_Listener8");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<String, String>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(HttpSessionIdListener.class, hsil1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S8");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			registrations.add(getBundleContext().registerService(Servlet.class, s1, servletProps1));

			requestAdvisor.request("s");

			Assert.assertTrue(hsil1.changed.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_Async1() throws Exception {

		Servlet s1 = new BaseAsyncServlet("test_Listener8");
		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, Object> servletProps1 = new Hashtable<String, Object>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, true);
			registrations.add(getBundleContext().registerService(Servlet.class, s1, servletProps1));

			String output1 = requestAdvisor.request("s");

			Assert.assertTrue(output1, output1.endsWith("test_Listener8"));
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	public void test_WBServlet1() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("WBServlet1/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_WBServlet2() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("WBServlet2/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	public void test_BufferedOutput() throws Exception {
		Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
		try {
			Dictionary<String, String> servletProps1 = new Hashtable<String, String>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S9");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s9");
			registrations.add(getBundleContext().registerService(Servlet.class, new BufferedServlet(), servletProps1));

			Map<String, List<String>> response = requestAdvisor.request(
				"s9", Collections.<String, List<String>>emptyMap());

			String responseCode = response.get("responseCode").get(0);
			Assert.assertEquals("200", responseCode);
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	private static final String PROTOTYPE = "prototype/";
	private static final String CONFIGURE = "configure";
	private static final String UNREGISTER = "unregister";
	private static final String ERROR = "error";
	private static final String STATUS_PARAM = "servlet.init.status";
	private static final String TEST_PROTOTYPE_NAME = "test.prototype.name";
	private static final String TEST_PATH_CUSTOMIZER_NAME = "test.path.customizer.name";
	private static final String TEST_ERROR_CODE = "test.error.code";
	public void testWBServletChangeInitParams() throws Exception{
			String actual;

			Map<String, String> params = new HashMap<String, String>();
			params.put(TEST_PROTOTYPE_NAME, getName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + getName());
			params.put(STATUS_PARAM, getName());
			actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(getName(), actual);
			actual = requestAdvisor.request(getName());
			Assert.assertEquals(getName(), actual);

			// change the init param
			params.put(STATUS_PARAM, "changed");
			doRequest(CONFIGURE, params);
			actual = requestAdvisor.request(getName());
			Assert.assertEquals("changed", actual);
	}

	public void testWBServletChangePattern() throws Exception{
		String actual;

		Map<String, String> params = new HashMap<String, String>();
		params.put(TEST_PROTOTYPE_NAME, getName());
		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + getName());
		params.put(STATUS_PARAM, getName());
		actual = doRequest(CONFIGURE, params);
		Assert.assertEquals(getName(), actual);
		actual = requestAdvisor.request(getName());
		Assert.assertEquals(getName(), actual);

		// change the pattern
		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/changed");
		doRequest(CONFIGURE, params);
		actual = requestAdvisor.request("changed");
		Assert.assertEquals(getName(), actual);
	}

	public void testWBServletChangeRanking() throws Exception{
		String actual;

		// Configure two servlets with the second one registered ranking higher
		Map<String, String> params1 = new HashMap<String, String>();
		params1.put(TEST_PROTOTYPE_NAME, getName() + 1);
		params1.put(Constants.SERVICE_RANKING, "1");
		params1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + getName());
		params1.put(STATUS_PARAM, getName() + 1);
		actual = doRequest(CONFIGURE, params1);
		Assert.assertEquals(getName() + 1, actual);

		Map<String, String> params2 = new HashMap<String, String>();
		params2.put(TEST_PROTOTYPE_NAME, getName() + 2);
		params2.put(Constants.SERVICE_RANKING, "2");
		params2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + getName());
		params2.put(STATUS_PARAM, getName() + 2);
		actual = doRequest(CONFIGURE, params2);
		Assert.assertEquals(getName() + 2, actual);

		// Confirm the second registered (higher ranked) gets used
		actual = requestAdvisor.request(getName());
		Assert.assertEquals(getName() + 2, actual);

		// change the ranking to use the first servlet registered
		params2.put(Constants.SERVICE_RANKING, "0");
		doRequest(CONFIGURE, params2);
		actual = requestAdvisor.request(getName());
		Assert.assertEquals(getName() + 1, actual);

		// Unregister the first servlet should cause the second servlet to be used
		actual = doRequest(UNREGISTER, Collections.singletonMap(TEST_PROTOTYPE_NAME, getName() + 1));
		Assert.assertEquals(getName() + 1, actual);

		// Confirm the second registered is used
		actual = requestAdvisor.request(getName());
		Assert.assertEquals(getName() + 2, actual);
	}

	public void testWBServletDefaultContextAdaptor1() throws Exception{
		Dictionary<String, String> helperProps = new Hashtable<String, String>();
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "testContext" + getName());
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/testContext");
		helperProps.put(TEST_PATH_CUSTOMIZER_NAME, getName());
		ServiceRegistration<ServletContextHelper> helperReg = getBundleContext().registerService(ServletContextHelper.class, new TestServletContextHelperFactory(), helperProps);

		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put(TEST_PROTOTYPE_NAME, getName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + getName());
			params.put(STATUS_PARAM, getName());
			params.put("servlet.init." + TEST_PATH_CUSTOMIZER_NAME, getName());
			String actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(getName(), actual);

			actual = requestAdvisor.request(getName());
			Assert.assertEquals(getName(), actual);

			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext" + getName() + ")", null, getName());
			pathAdaptorReg = getBundleContext().registerService(ContextPathCustomizer.class, pathAdaptor, null);

			actual = requestAdvisor.request("testContext/" + getName());
			Assert.assertEquals(getName(), actual);

			pathAdaptorReg.unregister();
			pathAdaptorReg = null;

			actual = requestAdvisor.request(getName());
			Assert.assertEquals(getName(), actual);
		} finally {
			helperReg.unregister();
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
		}
	}

	public void testWBServletDefaultContextAdaptor2() throws Exception{
		Dictionary<String, String> helperProps = new Hashtable<String, String>();
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "testContext" + getName());
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/testContext");
		helperProps.put(TEST_PATH_CUSTOMIZER_NAME, getName());
		ServiceRegistration<ServletContextHelper> helperReg = getBundleContext().registerService(ServletContextHelper.class, new TestServletContextHelperFactory(), helperProps);

		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put(TEST_PROTOTYPE_NAME, getName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + getName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext" + getName() + ")");
			params.put(STATUS_PARAM, getName());
			params.put("servlet.init." + TEST_PATH_CUSTOMIZER_NAME, getName());
			String actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(getName(), actual);

			actual = requestAdvisor.request("testContext/" + getName());
			Assert.assertEquals(getName(), actual);

			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor(null, "testPrefix", getName());
			pathAdaptorReg = getBundleContext().registerService(ContextPathCustomizer.class, pathAdaptor, null);

			actual = requestAdvisor.request("testPrefix/testContext/" + getName());
			Assert.assertEquals(getName(), actual);

			pathAdaptorReg.unregister();
			pathAdaptorReg = null;

			actual = requestAdvisor.request("testContext/" + getName());
			Assert.assertEquals(getName(), actual);
		} finally {
			helperReg.unregister();
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
		}
	}

	public void testWBServletDefaultContextAdaptor3() throws Exception{
		// test the ContextPathCustomizer with a ServletContextHelper that has a '/' context path
		Dictionary<String, String> helperProps = new Hashtable<String, String>();
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "testContext" + getName());
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
		helperProps.put(TEST_PATH_CUSTOMIZER_NAME, getName());
		ServiceRegistration<ServletContextHelper> helperReg = getBundleContext().registerService(ServletContextHelper.class, new TestServletContextHelperFactory(), helperProps);

		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put(TEST_PROTOTYPE_NAME, getName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + getName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext" + getName() + ")");
			params.put(STATUS_PARAM, getName());
			params.put("servlet.init." + TEST_PATH_CUSTOMIZER_NAME, getName());
			String actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(getName(), actual);

			actual = requestAdvisor.request(getName());
			Assert.assertEquals(getName(), actual);

			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor(null, "testPrefix", getName());
			pathAdaptorReg = getBundleContext().registerService(ContextPathCustomizer.class, pathAdaptor, null);

			actual = requestAdvisor.request("testPrefix/" + getName());
			Assert.assertEquals(getName(), actual);

			pathAdaptorReg.unregister();
			pathAdaptorReg = null;

			actual = requestAdvisor.request(getName());
			Assert.assertEquals(getName(), actual);
		} finally {
			helperReg.unregister();
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
		}
	}

	private String doRequest(String action, Map<String, String> params) throws IOException {
		return doRequestGetResponse(action, params).get("responseBody").get(0);
	}

	private Map<String, List<String>> doRequestGetResponse(String action, Map<String, String> params) throws IOException {
		StringBuilder requestInfo = new StringBuilder(PROTOTYPE);
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
			if (testName.equals(httpWhiteBoardService.getProperty("servlet.init." + TEST_PATH_CUSTOMIZER_NAME))) {
				return defaultFilter;
			}
			return null;
		}

		@Override
		public String getContextPathPrefix(ServiceReference<ServletContextHelper> helper) {
			if (testName.equals(helper.getProperty(TEST_PATH_CUSTOMIZER_NAME))) {
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
