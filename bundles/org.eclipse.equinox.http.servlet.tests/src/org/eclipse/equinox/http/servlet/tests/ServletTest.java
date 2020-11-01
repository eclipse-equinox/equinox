/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Raymond Augé - bug fixes and enhancements
 *     Juan Gonzalez <juan.gonzalez@liferay.com> - Bug 486412
 *     Peter Nehrer <pnehrer@eclipticalsoftware.com> - Bug 515912
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
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
import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.RangeAwareServletContextHelper;
import org.eclipse.equinox.http.servlet.context.ContextPathCustomizer;
import org.eclipse.equinox.http.servlet.session.HttpSessionInvalidator;
import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.AsyncOutputServlet;
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
import org.eclipse.equinox.http.servlet.tests.util.TestServletPrototype;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
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
import org.osgi.util.tracker.ServiceTracker;

public class ServletTest extends BaseTest {
	@Rule
	public TestName testName = new TestName();

	@Override
	protected void startBundles() throws BundleException {
		for (String bundle : BUNDLES) {
			advisor.startBundle(bundle);
		}
	}

	@Override
	protected void stopBundles() throws BundleException {
		for (int i = BUNDLES.length - 1; i >= 0; i--) {
			String bundle = BUNDLES[i];
			advisor.stopBundle(bundle);
		}
	}

	@Test
	public void test_ErrorPage1() throws Exception {
		String expected = "403 ERROR :";
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(TEST_BUNDLE_1);
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

	@Test
	public void test_ErrorPage2() throws Exception {
		String expected = "org.eclipse.equinox.http.servlet.tests.tb1.TestErrorPage2$MyException ERROR :";
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(TEST_BUNDLE_1);
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

	@Test
	public void test_ErrorPage3() throws Exception {
		String expected = "400 ERROR :";
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(TEST_BUNDLE_1);
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
		Assert.assertTrue("Expected <" + expected + "*> but got <" + actual + ">", actual.startsWith(expected));
	}

	/**
	 * This case should NOT hit the error servlet because the response is already
	 * committed. However, setting the response code is perfectly allowed.
	 */
	@Test
	public void test_ErrorPage4() throws Exception {
		String actual = null;
		Map<String, List<String>> response = Collections.emptyMap();
		Bundle bundle = installBundle(TEST_BUNDLE_1);
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

	@Test
	public void test_ErrorPage5() throws Exception {
		BundleContext bundleContext = getBundleContext();
		Dictionary<String, Object> serviceProps = new Hashtable<>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		registrations.add(bundleContext.registerService(Servlet.class, testDriver, serviceProps));

		Dictionary<String, Object> errorProps = new Hashtable<>();
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E5.4xx");
		errorProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, "4xx");
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("4xx"), errorProps));
		errorProps = new Hashtable<>();
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
	@Test
	public void test_ErrorPage6() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response)
				throws IOException {

				response.getWriter().write("Hello!");
				response.setStatus(444);
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E6");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage6/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<>();
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
	@Test
	public void test_ErrorPage7() throws Exception {
		final int status = 422;

		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

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

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E7");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage7/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E7.error");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, String.valueOf(status));
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet(String.valueOf(status)), props));

		Map<String, List<String>> response = requestAdvisor.request("TestErrorPage7/a", null);

		String responseCode = response.get("responseCode").get(0);
		String responseBody = response.get("responseBody").get(0);

		Assert.assertEquals(String.valueOf(status), responseCode);
		Assert.assertNotEquals(status + " : " + status + " : ERROR : /TestErrorPage7/a", responseBody);
	}

	@Test
	public void test_ErrorPage8() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp) {

				throw new RuntimeException();
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E8");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage8/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E8.error");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, RuntimeException.class.getName());
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("500"), props));

		Map<String, List<String>> response = requestAdvisor.request("TestErrorPage8/a", null);

		String responseCode = response.get("responseCode").get(0);
		String responseBody = response.get("responseBody").get(0);

		Assert.assertEquals("500", responseCode);
		Assert.assertEquals("500 : 500 : ERROR : /TestErrorPage8/a", responseBody);
	}

	@Test
	public void test_ErrorPage9() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

				throw new IOException();
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E9");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/TestErrorPage9/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "E9.error");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, IOException.class.getName());
		registrations.add(getBundleContext().registerService(Servlet.class, new ErrorServlet("500"), props));

		Map<String, List<String>> response = requestAdvisor.request("TestErrorPage9/a", null);

		String responseCode = response.get("responseCode").get(0);
		String responseBody = response.get("responseBody").get(0);

		Assert.assertEquals("500", responseCode);
		Assert.assertEquals("500 : 500 : ERROR : /TestErrorPage9/a", responseBody);
	}

	@Test
	public void test_ErrorPage10() {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

				resp.getWriter().write("some output");
				resp.flushBuffer();

				throw new IOException();
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
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

	@Test
	public void test_ErrorPage11() {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

				resp.sendError(403);
				resp.getOutputStream().flush();
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
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

	@Test
	public void test_Filter1() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter1/bab");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter2() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter2/cbabc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter3() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter3/cbdadbc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter4() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter4/dcbabcd");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter5() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/bab.TestFilter5");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter6() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/cbabc.TestFilter6");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter7() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/cbdadbc.TestFilter7");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter8() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/dcbabcd.TestFilter8");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter9() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter9/bab");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter10() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter10/cbabc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter11() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter11/cbdadbc");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter12() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter12/dcbabcd");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter13() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter13");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter14() throws Exception {
		String expected = "cbabc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter14");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter15() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter15");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter16() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestFilter16");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter17() throws Exception {
		String expected = "ebcdadcbe";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter17/foo/bar/baz");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter18() throws Exception {
		String expected = "dbcacbd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter18/foo/bar/baz");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Filter19() throws Exception {
		String expected = "dfbcacbfd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestFilter18/foo/bar/baz/with/path/info");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
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

	@Test
	public void test_Filter21() throws Exception {
		// Make sure exact path matching is honored by filters registrations
		String expected = "a";
		TestFilter testFilter1 = new TestFilter();
		TestFilter testFilter2 = new TestFilter();
		Servlet testServlet = new BaseServlet(expected);

		Dictionary<String, String> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/hello");
		registrations.add(getBundleContext().registerService(Filter.class, testFilter1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/hello/*");
		registrations.add(getBundleContext().registerService(Filter.class, testFilter2, props));

		props = new Hashtable<>();
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
		final AtomicReference<HttpServletRequestWrapper> httpServletRequestWrapper = new AtomicReference<>();
		final AtomicReference<HttpServletResponseWrapper> httpServletResponseWrapper = new AtomicReference<>();

		Servlet servlet1 = new BaseServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {
				request.getRequestDispatcher("index.jsp").forward(request, response);
			}
		};

		Servlet servlet2 = new BaseServlet("a") {
			private static final long serialVersionUID = 1L;

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

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servlet1Pattern);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet1, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servlet2Pattern);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet2, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F22");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, dispatchers);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, filterPattern);
		registrations.add(getBundleContext().registerService(Filter.class, filter, props));

		String response = requestAdvisor.request("f22/a");

		Assert.assertEquals(expected, response);
	}

	@Test
	public void test_Filter22a() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "/f22/*", "a", new String[] {"REQUEST"} );
	}

	@Test
	public void test_Filter22b() throws Exception {
		basicFilterTest22 ( "/*", "*.jsp", "/*", "a", new String[] {"REQUEST"} );
	}

	@Test
	public void test_Filter22c() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "*.jsp", "a", new String[] {"REQUEST"} );
	}

	@Test
	public void test_Filter22d() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "/f22/*", "bab", new String[] {"FORWARD"} );
	}

	@Test
	public void test_Filter22e() throws Exception {
		basicFilterTest22 ( "/*", "*.jsp", "/*", "bab", new String[] {"FORWARD"} );
	}

	@Test
	public void test_Filter22f() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "*.jsp", "bab", new String[] {"FORWARD"} );
	}

	@Test
	public void test_Filter22g() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "/f22/*", "bab", new String[] {"REQUEST", "FORWARD"} );
	}

	@Test
	public void test_Filter22h() throws Exception {
		basicFilterTest22 ( "/*", "*.jsp", "/*", "bab", new String[] {"REQUEST", "FORWARD"} );
	}

	@Test
	public void test_Filter22i() throws Exception {
		basicFilterTest22 ( "/f22/*", "*.jsp", "*.jsp", "bab", new String[] {"REQUEST", "FORWARD"} );
	}

	@Test
	public void test_Filter23a() throws Exception {
		// Make sure legacy filter registrations match as if they are prefix matching with extension matching
		String expected = "a";
		TestFilter testFilter1 = new TestFilter();
		TestFilter testFilter2 = new TestFilter();
		TestFilter testFilter3 = new TestFilter();
		Servlet testServlet = new BaseServlet(expected);
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		extendedHttpService.registerFilter("*.ext", testFilter1, null, extendedHttpService.createDefaultHttpContext());
		extendedHttpService.registerFilter("/hello/*.ext", testFilter2, null, extendedHttpService.createDefaultHttpContext());
		extendedHttpService.registerFilter("/hello/test/*.ext", testFilter3, null, extendedHttpService.createDefaultHttpContext());
		extendedHttpService.registerServlet("/hello", testServlet, null, extendedHttpService.createDefaultHttpContext());

		String actual = requestAdvisor.request("hello/test/request");
		Assert.assertEquals(expected, actual);
		Assert.assertFalse("testFilter1 did get called.", testFilter1.getCalled());
		Assert.assertFalse("testFilter2 did get called.", testFilter2.getCalled());
		Assert.assertFalse("testFilter3 did get called.", testFilter3.getCalled());

		testFilter1.clear();
		testFilter2.clear();
		testFilter3.clear();
		actual = requestAdvisor.request("hello/test/request.ext");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertTrue("testFilter2 did not get called.", testFilter2.getCalled());
		Assert.assertTrue("testFilter3 did not get called.", testFilter3.getCalled());
	}

	@Test
	public void test_Filter23b() throws Exception {
		// Make sure legacy filter registrations match as if they are prefix matching wildcard, but make sure the prefix is checked
		String expected = "a";
		TestFilter testFilter1 = new TestFilter();
		TestFilter testFilter2 = new TestFilter();
		Servlet testServlet = new BaseServlet(expected);
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		extendedHttpService.registerFilter("/", testFilter1, null, extendedHttpService.createDefaultHttpContext());
		extendedHttpService.registerFilter("/hello", testFilter2, null, extendedHttpService.createDefaultHttpContext());
		extendedHttpService.registerServlet("/", testServlet, null, extendedHttpService.createDefaultHttpContext());

		String actual = requestAdvisor.request("hello_test/request");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertFalse("testFilter2 did get called.", testFilter2.getCalled());

		actual = requestAdvisor.request("hello/request");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertTrue("testFilter2 did not get called.", testFilter2.getCalled());
	}

	@Test
	public void test_Filter23c() throws Exception {
		// Test WB servlet with default servlet pattern "/" and filter matching against it.
		String expected = "a";
		TestFilter testFilter1 = new TestFilter();
		TestFilter testFilter2 = new TestFilter();
		Servlet testServlet = new BaseServlet(expected);

		registrations.add(getBundleContext().registerService(Filter.class, testFilter1, new Hashtable<>(Collections.singletonMap(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*"))));
		registrations.add(getBundleContext().registerService(Filter.class, testFilter2, new Hashtable<>(Collections.singletonMap(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/hello/*"))));
		registrations.add(getBundleContext().registerService(Servlet.class, testServlet, new Hashtable<>(Collections.singletonMap(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/"))));

		String actual = requestAdvisor.request("hello_test/request");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertFalse("testFilter2 did get called.", testFilter2.getCalled());

		actual = requestAdvisor.request("hello/request");
		Assert.assertEquals(expected, actual);
		Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());
		Assert.assertTrue("testFilter2 did not get called.", testFilter2.getCalled());
	}

	@Test
	public void test_Filter24() throws Exception {
		// Test WB servlet and WB testfilter matching against it.
		// Test filter gets called.
		// Unregister WB filter.
		// test filter is NOT called
		String expected = "a";
		TestFilter testFilter1 = new TestFilter();
		Servlet testServlet = new BaseServlet(expected);

		ServiceRegistration<Filter> filterReg = getBundleContext().registerService(Filter.class, testFilter1, new Hashtable<>(Collections.singletonMap(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/hello/*")));
		try {
			registrations.add(getBundleContext().registerService(Servlet.class, testServlet, new Hashtable<>(Collections.singletonMap(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/hello/*"))));

			String actual = requestAdvisor.request("hello/request");
			Assert.assertEquals(expected, actual);
			Assert.assertTrue("testFilter1 did not get called.", testFilter1.getCalled());

			filterReg.unregister();
			filterReg = null;
			testFilter1.clear();

			actual = requestAdvisor.request("hello/request");
			Assert.assertEquals(expected, actual);
			Assert.assertFalse("testFilter1 did get called.", testFilter1.getCalled());
		} finally {
			if (filterReg != null) {
				filterReg.unregister();
			}
		}
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void test_unregister() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		Servlet servlet = new BaseServlet();
		Filter filter = new TestFilter();

		extendedHttpService.registerServlet("/s1", servlet, null, null);
		extendedHttpService.registerFilter("/f1", filter, null, null);
		extendedHttpService.registerResources("/r1", "/resources", null);

		extendedHttpService.unregister("/s1");
		extendedHttpService.unregisterFilter(filter);
		extendedHttpService.unregister("/r1");
	}

	@Test
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

	@Test
	public void test_Registration12() throws Exception {
		Bundle bundle = installBundle(TEST_BUNDLE_1);
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

	@Test
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

	@Test
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

	@Test
	public void test_Registration15() throws Exception {
		Servlet initError = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			public void init(ServletConfig config) {
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

	@Test
	public void test_Registration16() throws Exception {
		Filter initError = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) {
				throw new IllegalStateException("Init error.");
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
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

	@Test
	public void test_Registration17() throws Exception {
		Filter initError = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				throw new ServletException("Init error.");
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
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

	@Test
	public void test_Registration18_WhiteboardServletByNameOnly() throws Exception {
		String expected = "a";
		final String servletName = "hello_servlet";
		Servlet namedServlet = new BaseServlet(expected);
		Servlet targetServlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

				request.getServletContext().getNamedDispatcher(servletName).forward(request, response);
			}

		};

		Hashtable<String, String> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, servletName);
		registrations.add(getBundleContext().registerService(Servlet.class, namedServlet, props));

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
		registrations.add(getBundleContext().registerService(Servlet.class, targetServlet, props));

		String actual = requestAdvisor.request("s");
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_RegistrationTCCL1() {
		final Set<String> filterTCCL = Collections.synchronizedSet(new HashSet<String>());
		final Set<String> servletTCCL = Collections.synchronizedSet(new HashSet<String>());
		Filter tcclFilter = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) {
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
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

	@Test
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
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
		Dictionary<String, Object> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sessions");
		String actual = null;
		CookieHandler previous = CookieHandler.getDefault();
		CookieHandler.setDefault(new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );
		try {
			servletReg = getBundleContext().registerService(Servlet.class, sessionServlet, servletProps);
			Dictionary<String, String> listenerProps = new Hashtable<>();
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

	@Test
	public void test_Sessions02() {
		final AtomicReference<HttpSession> sessionReference = new AtomicReference<>();

		HttpServlet sessionServlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
				HttpSession session = request.getSession();
				sessionReference.set(session);
				if (session.getAttribute("test.attribute") == null) {
					session.setAttribute("test.attribute", "foo");
					response.getWriter().print("created");
				} else {
					session.setAttribute("test.attribute", null);
					response.getWriter().print("attribute set to null");
				}
			}
		};
		ServiceRegistration<Servlet> servletReg = null;
		Dictionary<String, Object> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sessions");
		String actual = null;
		CookieHandler previous = CookieHandler.getDefault();
		CookieHandler.setDefault(new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );
		try {
			servletReg = getBundleContext().registerService(Servlet.class, sessionServlet, servletProps);

			// first call will create the session
			actual = requestAdvisor.request("sessions");
			assertEquals("Wrong result", "created", actual);

			// second call will set parameter to null
			actual = requestAdvisor.request("sessions");
			assertEquals("Wrong result", "attribute set to null", actual);

			HttpSession httpSession = sessionReference.get();

			Enumeration<String> names = httpSession.getAttributeNames();

			boolean exist = false;

			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (name.equals("test.attribute")) {
					exist = true;
				}
			}

			assertFalse("Session atribute was not removed", exist);
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		} finally {
			if (servletReg != null) {
				servletReg.unregister();
			}
			CookieHandler.setDefault(previous);
		}
	}

	@Test
	public void test_Sessions03_HttpSessionInvalidator() throws Exception {
		ServiceTracker<HttpSessionInvalidator, HttpSessionInvalidator> sessionInvalidatorTracker =
			new ServiceTracker<>(getBundleContext(), HttpSessionInvalidator.class, null);
		sessionInvalidatorTracker.open();
		HttpSessionInvalidator invalidator = sessionInvalidatorTracker.waitForService(100);

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
		final AtomicReference<String> sessionId = new AtomicReference<>();
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
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
				HttpSession session = request.getSession();
				if (session.getAttribute("test.attribute") == null) {
					session.setAttribute("test.attribute", bindingListener);
					sessionId.set(session.getId());
					response.getWriter().print("created");
				} else {
					session.invalidate();
					response.getWriter().print("invalidated");
				}
			}

		};
		ServiceRegistration<Servlet> servletReg = null;
		ServiceRegistration<HttpSessionListener> sessionListenerReg = null;
		Dictionary<String, Object> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sessions");
		String actual = null;
		CookieHandler previous = CookieHandler.getDefault();
		CookieHandler.setDefault(new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );
		try {
			servletReg = getBundleContext().registerService(Servlet.class, sessionServlet, servletProps);
			Dictionary<String, String> listenerProps = new Hashtable<>();
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

			assertNotNull(sessionId.get());

			// invalidate using the invalidator
			invalidator.invalidate(sessionId.get(), true);

			// second call should find the session invalidated, and create a new one
			actual = requestAdvisor.request("sessions");
			assertEquals("Wrong result", "created", actual);
			assertTrue("No sessionCreated was called", sessionCreated.get());
			assertTrue("No valueBound was called", valueBound.get());
			assertTrue("No sessionDestroyed called", sessionDestroyed.get());
			assertTrue("No valueUnbound called", valueUnbound.get());

			sessionCreated.set(false);
			sessionDestroyed.set(false);
			valueBound.set(false);
			valueUnbound.set(false);

			// calling again should invalidate the session again
			actual = requestAdvisor.request("sessions");
			assertEquals("Wrong result", "invalidated", actual);
			assertFalse("sessionCreated called", sessionCreated.get());
			assertFalse("valueBound called", valueBound.get());
			assertTrue("No sessionDestroyed called", sessionDestroyed.get());
			assertTrue("No valueUnbound called", valueUnbound.get());
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
			sessionInvalidatorTracker.close();
		}
	}

	@Test
	public void test_Sessions04_inlineSessionId() {
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
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
				HttpSession session = request.getSession();
				if (session.getAttribute("test.attribute") == null) {
					session.setAttribute("test.attribute", bindingListener);
					response.getWriter().print(response.encodeURL(request.getRequestURI()));
				} else {
					session.invalidate();
					response.getWriter().print("invalidated");
				}
			}

		};
		ServiceRegistration<Servlet> servletReg = null;
		ServiceRegistration<HttpSessionListener> sessionListenerReg = null;
		Dictionary<String, Object> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sessions");
		String actual = null;

		try {
			servletReg = getBundleContext().registerService(Servlet.class, sessionServlet, servletProps);
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			sessionListenerReg = getBundleContext().registerService(HttpSessionListener.class, sessionListener, listenerProps);

			sessionCreated.set(false);
			valueBound.set(false);
			sessionDestroyed.set(false);
			valueUnbound.set(false);

			// first call will create the session
			String inlined = requestAdvisor.request("sessions");
			assertTrue("Wrong result: " + inlined, inlined.startsWith("/sessions;jsessionid="));
			assertTrue("No sessionCreated called", sessionCreated.get());
			assertTrue("No valueBound called", valueBound.get());
			assertFalse("sessionDestroyed was called", sessionDestroyed.get());
			assertFalse("valueUnbound was called", valueUnbound.get());

			sessionCreated.set(false);
			valueBound.set(false);
			sessionDestroyed.set(false);
			valueUnbound.set(false);

			// second call will invalidate the session
			actual = requestAdvisor.request(inlined.substring(1));
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
			actual = requestAdvisor.request(inlined.substring(1) + "?bar=2");
			assertTrue("Wrong result: " + actual, actual.startsWith("/sessions;jsessionid="));
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
		}
	}

	@Test
	public void test_Sessions05_Bug541607_MemoryLeak() {
		final List<String> sessionIds = new CopyOnWriteArrayList<>();
		HttpSessionListener sessionListener = new HttpSessionListener() {

			@Override
			public void sessionDestroyed(HttpSessionEvent se) {
				sessionIds.remove(se.getSession().getId());
			}

			@Override
			public void sessionCreated(HttpSessionEvent se) {
				sessionIds.add(se.getSession().getId());
			}
		};
		HttpServlet sessionServlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
				HttpSession session = request.getSession();
				response.getWriter().print("created " + session.getId());
			}

		};
		ServiceRegistration<Servlet> servletReg = null;
		ServiceRegistration<HttpSessionListener> sessionListenerReg = null;
		Dictionary<String, Object> servletProps = new Hashtable<>();
		servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/sessions");

		try {
			servletReg = getBundleContext().registerService(Servlet.class, sessionServlet, servletProps);
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			sessionListenerReg = getBundleContext().registerService(HttpSessionListener.class, sessionListener, listenerProps);

			// call the servet 10 times, we should get 10 sessions
			for (int i = 0; i < 10; i++) {
				requestAdvisor.request("sessions");
			}

			assertEquals("Wrong result", 10, sessionIds.size());
			Thread.sleep(12000); // 12 seconds
			assertEquals("Wrong result", 0, sessionIds.size());
		} catch (Exception e) {
			fail("Unexpected exception: " + e);
		} finally {
			if (servletReg != null) {
				servletReg.unregister();
			}
			if (sessionListenerReg != null) {
				sessionListenerReg.unregister();
			}
		}
	}

	@Test
	public void test_Resource1() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource1/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Resource2() throws Exception {
		String expected = "cbdadbc";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource2/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Resource3() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource3/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Resource4() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource4/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Resource5() throws Exception {
		String expected = "dcbabcd";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestResource5/resource1.txt");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_ResourceAliasNormal() throws Exception {
		HttpService extendedHttpService = getHttpService();

		extendedHttpService.registerResources("/testalias", "/org/eclipse/equinox/http/servlet/tests", null);

		String actual = requestAdvisor.request("testalias/resource2.txt");
		Assert.assertEquals("Wrong value.", "test", actual);
	}

	@Test
	public void test_ResourceAliasSlash() throws Exception {
		HttpService extendedHttpService = getHttpService();

		extendedHttpService.registerResources("/", "/org/eclipse/equinox/http/servlet/tests", null);

		String actual = requestAdvisor.request("resource2.txt");
		Assert.assertEquals("Wrong value.", "test", actual);
	}

	@Test
	public void test_ResourceRangeRequest_Complete() throws Exception {
		Bundle bundle = installBundle(TEST_BUNDLE_2);
		ServletContextHelper customSCH = new ServletContextHelper(bundle) {
			@Override
			public String getMimeType(String filename) {
				if (filename.endsWith(".mp4")) { //$NON-NLS-1$
					return "video/mp4"; //$NON-NLS-1$
				}
				return null;
			}
		};
		Dictionary<String, Object> contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "foo");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/foo");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, customSCH, contextProps));
		Map<String, List<String>> actual;
		Map<String, List<String>> requestHeader = new HashMap<>();
		requestHeader.put("Range", Collections.singletonList("bytes=0-"));
		try {
			bundle.start();
			actual = requestAdvisor.request("foo/TestResource1/rangerequest.mp4", requestHeader);
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals("Response Code", Collections.singletonList("206"), actual.get("responseCode"));
		Assert.assertEquals("Content-Length", Collections.singletonList("20655"), actual.get("Content-Length"));
		Assert.assertEquals("Accept-Ranges", Collections.singletonList("bytes"), actual.get("Accept-Ranges"));
		Assert.assertEquals("Content-Range", Collections.singletonList("bytes 0-20654/20655"), actual.get("Content-Range"));
	}

	@Test
	public void test_ResourceRangeRequest_WithRange() throws Exception {
		Map<String, List<String>> actual;
		Bundle bundle = installBundle(TEST_BUNDLE_2);
		ServletContextHelper customSCH = new ServletContextHelper(bundle) {
			@Override
			public String getMimeType(String filename) {
				if (filename.endsWith(".mp4")) { //$NON-NLS-1$
					return "video/mp4"; //$NON-NLS-1$
				}
				return null;
			}
		};
		Dictionary<String, Object> contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "foo");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/foo");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, customSCH, contextProps));
		Map<String, List<String>> requestHeader = new HashMap<>();
		requestHeader.put("Range", Collections.singletonList("bytes=1000-9999"));
		try {
			bundle.start();
			actual = requestAdvisor.request("foo/TestResource1/rangerequest.mp4", requestHeader);
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals("Response Code", Collections.singletonList("206"), actual.get("responseCode"));
		Assert.assertEquals("Content-Length", Collections.singletonList("9000"), actual.get("Content-Length"));
		Assert.assertEquals("Accept-Ranges", Collections.singletonList("bytes"), actual.get("Accept-Ranges"));
		Assert.assertEquals("Content-Range", Collections.singletonList("bytes 1000-9999/20655"), actual.get("Content-Range"));
		Assert.assertEquals("Response Body Prefix", "901", actual.get("responseBody").get(0).substring(0, 3));
		Assert.assertEquals("Response Body Suffix", "567", actual.get("responseBody").get(0).substring(8997, 9000));
	}

	@Test
	public void test_ResourceRangeRequest_WithRange_customContext() throws Exception {
		Map<String, List<String>> actual;
		Bundle bundle = installBundle(TEST_BUNDLE_2);
		RangeAwareServletContextHelper customSCH = new RangeAwareServletContextHelper(bundle) {
			@Override
			public String getMimeType(String filename) {
				if (filename.endsWith(".mp4")) { //$NON-NLS-1$
					return "video/mp4"; //$NON-NLS-1$
				}
				return null;
			}
			@Override
			public boolean rangeableContentType(String contentType, String userAgent) {
				return userAgent.contains("Foo") && contentType.startsWith("video/");
			}
		};
		Dictionary<String, Object> contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "foo");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/foo");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, customSCH, contextProps));

		Map<String, List<String>> requestHeader = new HashMap<>();
		requestHeader.put("User-Agent", Collections.singletonList("Foo"));
		try {
			bundle.start();
			actual = requestAdvisor.request("foo/TestResource1/rangerequest.mp4", requestHeader);
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals("Response Code", Collections.singletonList("206"), actual.get("responseCode"));
		Assert.assertEquals("Content-Length", Collections.singletonList("20655"), actual.get("Content-Length"));
		Assert.assertEquals("Accept-Ranges", Collections.singletonList("bytes"), actual.get("Accept-Ranges"));
		Assert.assertEquals("Content-Range", Collections.singletonList("bytes 0-20654/20655"), actual.get("Content-Range"));
		Assert.assertEquals("Response Body Prefix", "123", actual.get("responseBody").get(0).substring(0, 3));
		Assert.assertEquals("Response Body Suffix", "789", actual.get("responseBody").get(0).substring(8997, 9000));
	}

	@Test
	public void test_Runtime() throws Exception {
		Bundle bundle = installBundle(TEST_BUNDLE_1);
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

	@Test
	public void test_Servlet1() throws Exception {
		String expected = STATUS_OK;
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet1");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet2() throws Exception {
		String expected = "3";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet2");
			Assert.assertEquals(expected, actual);
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet3() throws Exception {
		String expected = STATUS_OK;
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet3");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet4() throws Exception {
		String expected = System.getProperty(JETTY_PROPERTY_PREFIX + "context.path", "");
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet4");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet5() throws Exception {
		String expected = "Equinox Jetty-based Http Service";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet5");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet6() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("something/a.TestServlet6");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet7() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet7/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet9() throws Exception {
		String expected = "Equinox Jetty-based Http Service";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet9");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet10() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet10");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet11() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet11");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_Servlet12() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws IOException {

				response.getWriter().write('a');
			}

		};

		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws IOException {

				response.getWriter().write('b');
			}

		};

		HttpService httpService = getHttpService();

		HttpContext httpContext = httpService.createDefaultHttpContext();

		httpService.registerServlet("*.txt", sA, null, httpContext);
		httpService.registerServlet("/files/*.txt", sB, null, httpContext);

		Assert.assertEquals("b", requestAdvisor.request("files/help.txt"));
	}

	@Test
	public void test_Servlet13() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

				PrintWriter writer = resp.getWriter();
				writer.write(req.getQueryString());
				writer.write("|");
				writer.write(req.getParameter("p"));
				writer.write("|");
				writer.write(Arrays.toString(req.getParameterValues("p")));
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S13");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet13/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		String result = requestAdvisor.request("Servlet13/a?p=1&p=2");

		Assert.assertEquals("p=1&p=2|1|[1, 2]", result);
	}

	@Test
	public void test_ServletExactMatchPrecidence() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws IOException {

				response.getWriter().write('a');
			}

		};

		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws IOException {

				response.getWriter().write('b');
			}

		};

		HttpService httpService = getHttpService();

		HttpContext httpContext = httpService.createDefaultHttpContext();

		httpService.registerServlet("*.txt", sA, null, httpContext);
		httpService.registerServlet("/files/help.txt", sB, null, httpContext);

		Assert.assertEquals("b", requestAdvisor.request("files/help.txt"));
	}

//	private static String getSubmittedFileName(Part part) {
//		for (String cd : part.getHeader("content-disposition").split(";")) {
//			if (cd.trim().startsWith("filename")) {
//				String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
//				return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1); // MSIE fix.
//			}
//		}
//		return null;
//	}

	/*
	 * 3.1 file uploads
	 */
	@Test
	public void test_Servlet16() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws IOException, ServletException {

				Part part = req.getPart("file");
				Assert.assertNotNull(part);

				String submittedFileName = part.getSubmittedFileName();
				String contentType = part.getContentType();
				long size = part.getSize();

				PrintWriter writer = resp.getWriter();

				writer.write(submittedFileName);
				writer.write("|");
				writer.write(contentType);
				writer.write("|" + size);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		props.put("equinox.http.multipartSupported", Boolean.TRUE);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<Object>> map = new HashMap<>();

		map.put("file", Arrays.<Object>asList(getClass().getResource("resource1.txt")));

		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);

		Assert.assertEquals("200", result.get("responseCode").get(0));
		Assert.assertEquals("resource1.txt|text/plain|25", result.get("responseBody").get(0));
	}

	@Test
	public void test_Servlet16_notEnabled() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws IOException, ServletException {

				req.getPart("file");
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<Object>> map = new HashMap<>();

		map.put("file", Arrays.<Object>asList(getClass().getResource("resource1.txt")));

		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);

		Assert.assertEquals("500", result.get("responseCode").get(0));
	}

	@Test
	public void test_Servlet16_fileuploadWithLocation() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws IOException, ServletException {

				Part part = req.getPart("file");
				Assert.assertNotNull(part);

				String submittedFileName = part.getSubmittedFileName();
				String contentType = part.getContentType();
				long size = part.getSize();

				File tempDir = (File)getServletContext().getAttribute(ServletContext.TEMPDIR);
				File location = new File(tempDir, "file-upload-test");

				File[] listFiles = location.listFiles();

				PrintWriter writer = resp.getWriter();

				writer.write(submittedFileName);
				writer.write("|");
				writer.write(contentType);
				writer.write("|" + size);
				writer.write("|" + listFiles.length);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		props.put("equinox.http.multipartSupported", Boolean.TRUE);
		props.put("equinox.http.whiteboard.servlet.multipart.location", "file-upload-test");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<Object>> map = new HashMap<>();

		map.put("file", Arrays.<Object>asList(getClass().getResource("resource1.txt")));

		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);

		Assert.assertEquals("200", result.get("responseCode").get(0));
		Assert.assertEquals("resource1.txt|text/plain|25|0", result.get("responseBody").get(0));
	}

	@Test
	public void test_Servlet16_fileuploadWithLocationAndThreshold() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws IOException, ServletException {

				Part part = req.getPart("file");
				Assert.assertNotNull(part);

				String submittedFileName = part.getSubmittedFileName();
				String contentType = part.getContentType();
				long size = part.getSize();

				File tempDir = (File)getServletContext().getAttribute(ServletContext.TEMPDIR);
				File location = new File(tempDir, "file-upload-test");

				File[] listFiles = location.listFiles();

				PrintWriter writer = resp.getWriter();

				writer.write(submittedFileName);
				writer.write("|");
				writer.write(contentType);
				writer.write("|" + size);
				writer.write("|" + listFiles.length);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		props.put("equinox.http.multipartSupported", Boolean.TRUE);
		props.put("equinox.http.whiteboard.servlet.multipart.location", "file-upload-test");
		props.put("equinox.http.whiteboard.servlet.multipart.fileSizeThreshold", 10);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<Object>> map = new HashMap<>();

		map.put("file", Arrays.<Object>asList(getClass().getResource("resource1.txt")));

		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);

		Assert.assertEquals("200", result.get("responseCode").get(0));
		Assert.assertEquals("resource1.txt|text/plain|25|1", result.get("responseBody").get(0));
	}

	@Test
	public void test_Servlet16_fileuploadWithLocationMaxFileSize() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws IOException, ServletException {

				req.getPart("file");
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		props.put("equinox.http.multipartSupported", Boolean.TRUE);
		props.put("equinox.http.whiteboard.servlet.multipart.location", "file-upload-test");
		// Note the actual uploaded file size is 25bytes
		props.put("equinox.http.whiteboard.servlet.multipart.maxFileSize", 24L);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<Object>> map = new HashMap<>();

		map.put("file", Arrays.<Object>asList(getClass().getResource("resource1.txt")));

		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);

		Assert.assertEquals("500", result.get("responseCode").get(0));
	}

	@Test
	public void test_Servlet16_fileuploadWithLocationMaxRequestSize() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws IOException, ServletException {

				req.getPart("file");
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		props.put("equinox.http.multipartSupported", Boolean.TRUE);
		props.put("equinox.http.whiteboard.servlet.multipart.location", "file-upload-test");
		// Note the actual uploaded file size is 25bytes, but you also need room for the rest of the headers
		props.put("equinox.http.whiteboard.servlet.multipart.maxRequestSize", 26L);
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<Object>> map = new HashMap<>();

		map.put("file", Arrays.<Object>asList(getClass().getResource("resource1.txt")));

		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);

		Assert.assertEquals("500", result.get("responseCode").get(0));
	}

	/*
	 * 3.0 file uploads
	 */
// This is commented due to a bug in commons-fileupload which was subsequently fixed in later versions.
//	@Test
//	public void test_Servlet17() throws Exception {
//		Servlet servlet = new HttpServlet() {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
//				throws IOException, ServletException {
//
//				Part part = req.getPart("file");
//				Assert.assertNotNull(part);
//
//				String submittedFileName = getSubmittedFileName(part);
//				String contentType = part.getContentType();
//				long size = part.getSize();
//
//				PrintWriter writer = resp.getWriter();
//
//				writer.write(submittedFileName);
//				writer.write("|");
//				writer.write(contentType);
//				writer.write("|" + size);
//			}
//		};
//
//		Dictionary<String, Object> props = new Hashtable<String, Object>();
//		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
//		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
//		props.put("equinox.http.multipartSupported", Boolean.TRUE);
//		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));
//
//		Map<String, List<Object>> map = new HashMap<String, List<Object>>();
//
//		map.put("file", Arrays.<Object>asList(getClass().getResource("blue.png")));
//
//		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);
//
//		Assert.assertEquals("200", result.get("responseCode").get(0));
//		Assert.assertEquals("blue.png|image/png|292", result.get("responseBody").get(0));
//	}

	@Test
	public void test_Servlet18() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest request, HttpServletResponse response)
				throws IOException {
				// get a resource that is imported
				URL url = request.getServletContext().getResource("org/osgi/service/http/HttpService.class");
				response.getWriter().write(url == null ? "null" : url.getProtocol());
			}

		};

		HttpService httpService = getHttpService();

		HttpContext httpContext = httpService.createDefaultHttpContext();

		httpService.registerServlet("/testDefaultHttpContextResource", sA, null, httpContext);

		// just making sure bundleresource protocol is used as proof that Bundle.getResource was called
		Assert.assertEquals("bundleresource", requestAdvisor.request("testDefaultHttpContextResource"));
	}

	@Test
	public void test_commonsFileUpload() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

				boolean isMultipart = ServletFileUpload.isMultipartContent(req);
				Assert.assertTrue(isMultipart);

				DiskFileItemFactory factory = new DiskFileItemFactory();

				ServletContext servletContext = this.getServletConfig().getServletContext();
				File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
				factory.setRepository(repository);
				ServletFileUpload upload = new ServletFileUpload(factory);

				List<FileItem> items = null;
				try {
					List<FileItem> parseRequest = upload.parseRequest(req);
					items = parseRequest;
				} catch (FileUploadException e) {
					e.printStackTrace();
				}

				Assert.assertNotNull(items);
				Assert.assertFalse(items.isEmpty());

				FileItem fileItem = items.get(0);

				String submittedFileName = fileItem.getName();
				String contentType = fileItem.getContentType();
				long size = fileItem.getSize();

				PrintWriter writer = resp.getWriter();

				writer.write(submittedFileName);
				writer.write("|");
				writer.write(contentType);
				writer.write("|" + size);
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<Object>> map = new HashMap<>();

		map.put("file", Arrays.<Object>asList(getClass().getResource("blue.png")));

		Map<String, List<String>> result = requestAdvisor.upload("Servlet16/do", map);

		Assert.assertEquals("200", result.get("responseCode").get(0));
		Assert.assertEquals("blue.png|image/png|292", result.get("responseBody").get(0));
	}

	@Test
	public void test_PathEncodings_Bug540970() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp)
					throws IOException {

				PrintWriter writer = resp.getWriter();

				writer.write(req.getRequestURI());
			}
		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S16");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/Servlet16/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<String>> map = new HashMap<>();

		Map<String, List<String>> result = requestAdvisor.request("Servlet16/NEEO-a5056097%2Fdevice%2Fapt-neeo_io%3Avirtual%3A6jzOoAtL%2FTemperature_GF_Living%2Fnone%2F1%2Fdirectory%2Factor/default", map);

		Assert.assertEquals("200", result.get("responseCode").get(0));
		Assert.assertEquals("/Servlet16/NEEO-a5056097%2Fdevice%2Fapt-neeo_io%3Avirtual%3A6jzOoAtL%2FTemperature_GF_Living%2Fnone%2F1%2Fdirectory%2Factor/default", result.get("responseBody").get(0));
	}

	@Test
	public void test_ServletContext1() throws Exception {
		String expected = "/org/eclipse/equinox/http/servlet/tests/tb1/resource1.txt";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServletContext1");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);

	}

	@Test
	public void test_ServletContext1_2() throws Exception {
		String expected = "/org/eclipse/equinox/http/servlet/tests/tb1/resource1.txt";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
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

	@Test
	public void test_ServletContext2() throws Exception {
		Servlet servlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(
				HttpServletRequest request, HttpServletResponse response) {

				getServletContext().setAttribute("name", null);
			}

		};

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/S1/*");
		registrations.add(getBundleContext().registerService(Servlet.class, servlet, props));

		Map<String, List<String>> response = requestAdvisor.request("S1/a", null);

		String responseCode = response.get("responseCode").get(0);

		Assert.assertEquals("200", responseCode);
	}

	@Test
	public void testServletContextUnsupportedOperations() {
		final AtomicReference<ServletContext> contextHolder = new AtomicReference<>();
		Servlet unsupportedServlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;
			@Override
			public void init(ServletConfig config) {
				contextHolder.set(config.getServletContext());
			}
		};

		ServiceRegistration<Servlet> servletReg = null;
		Dictionary<String, Object> servletProps = new Hashtable<>();
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
		List<Method> methods = new ArrayList<>();
		Class<ServletContext> contextClass = ServletContext.class;
		for(Method m : contextClass.getMethods()) {
			String name = m.getName();
			if (name.equals("addFilter") || name.equals("addListener") || name.equals("addServlet") || name.equals("createFilter") || name.equals("createListener") || name.equals("createServlet") || name.equals("declareRoles")) {
				methods.add(m);
			}
		}
		return methods;
	}

	@Test
	public void test_ServletContextHelper1() {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Dictionary<String, String> contextProps = new Hashtable<>();
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "test.sch.one");
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/test-sch2");
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "Test SCH 3!");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/test-sch3");
		registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

		servletContextHelper = new ServletContextHelper(bundle){};
		contextProps = new Hashtable<>();
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

	@Test
	public void test_ServletContextHelper7() throws Exception {
		String expected = "a";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps = new Hashtable<>();
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

	@Test
	public void test_ServletContextHelper8() throws Exception {
		String expected = "b";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet("b");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps = new Hashtable<>();
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

	@Test
	public void test_ServletContextHelper9() throws Exception {
		String expected1 = "c";
		String expected2 = "d";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet(expected1);
		Servlet s2 = new BaseServlet(expected2);

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S1");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s");
			registrations.add(bundleContext.registerService(Servlet.class, s1, servletProps1));

			Dictionary<String, String> servletProps2 = new Hashtable<>();
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

	@Test
	public void test_ServletContextHelperVisibility() throws Exception {
		String expected1 = "c";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet(expected1);


		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
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

			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps2 = new Hashtable<>();
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

	@Test
	public void test_ServletContextHelper10() throws Exception {
		String expected = "cac";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("a/TestServletContextHelper10/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	private static final String HTTP_CONTEXT_TEST_ROOT = "http.context.test";

	@Test
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

	@Test
	public void test_ServletContextHelper12() throws Exception {
		String expected1 = "a,b,null";

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void service(
					HttpServletRequest request, HttpServletResponse response)
				throws IOException {

				StringBuilder builder = new StringBuilder();
				builder.append(request.getServletContext().getInitParameter("a")).append(',');
				builder.append(request.getServletContext().getInitParameter("b")).append(',');
				builder.append(request.getServletContext().getInitParameter("c"));
				response.getWriter().print(builder.toString());
			}
		};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, Object> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "a", "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "b", "b");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "c", Integer.valueOf(1));
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
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

	@Test
	public void test_ServletContextHelper13() throws Exception {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		// test that the helper handlesecurity is called before the filter by setting an attribute on the request
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				request.setAttribute(testName.getMethodName(), Boolean.TRUE);
				return super.handleSecurity(request, response);
			}

		};
		Filter f1 = new Filter() {

			@Override
			public void init(FilterConfig filterConfig) {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				if (request.getAttribute(testName.getMethodName()) == Boolean.TRUE) {
					request.setAttribute(testName.getMethodName() + ".fromFilter", Boolean.TRUE);
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
			public void service(ServletRequest req, ServletResponse res) throws IOException {
				res.getWriter().print(req.getAttribute(testName.getMethodName() + ".fromFilter"));
			}

		};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> filterProps = new Hashtable<>();
			filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
			filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			registrations.add(bundleContext.registerService(Filter.class, f1, filterProps));

			Dictionary<String, String> servletProps = new Hashtable<>();
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

	@Test
	public void test_ServletContextHelper14_uniqueTempDirs() throws Exception {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelperA = new ServletContextHelper(bundle){};
		ServletContextHelper servletContextHelperB = new ServletContextHelper(bundle){};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			final AtomicReference<File> fileA = new AtomicReference<>();

			Servlet servletA = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp)
					throws IOException {
					fileA.set((File)getServletContext().getAttribute(ServletContext.TEMPDIR));
					new File(fileA.get(), "test").createNewFile();
				}
			};

			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelperA, contextProps));
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SA");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/SA");
			registrations.add(getBundleContext().registerService(Servlet.class, servletA, props));

			requestAdvisor.request("a/SA");
			Assert.assertNotNull(fileA.get());
			Assert.assertTrue(new File(fileA.get(), "test").exists());

			contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "b");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/b");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelperB, contextProps));

			Assert.assertNotNull(fileA.get());
			Assert.assertTrue(new File(fileA.get(), "test").exists());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	@Test
	public void test_ServletContextHelper15_fullContextPath_include() throws Exception {
		try {
			stopJetty();
			System.setProperty("org.eclipse.equinox.http.jetty.context.path", "/foo");
		}
		finally {
			startJetty();
		}

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelperA = new ServletContextHelper(bundle){};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			final AtomicReference<String> path = new AtomicReference<>();

			Servlet servletA = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp)
					throws IOException, ServletException {
					RequestDispatcher rd = req.getRequestDispatcher("/foo/a/SB");
					rd.include(req, resp);
				}
			};
			Servlet servletB = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp) {
					path.set((String)req.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH));
				}
			};

			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelperA, contextProps));
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SA");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/SA");
			registrations.add(getBundleContext().registerService(Servlet.class, servletA, props));
			props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SB");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/SB");
			registrations.add(getBundleContext().registerService(Servlet.class, servletB, props));

			requestAdvisor.request("a/SA");

			Assert.assertEquals("/foo/a", path.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
			try {
				stopJetty();
				System.setProperty("org.eclipse.equinox.http.jetty.context.path", "");
			}
			finally {
				startJetty();
			}
		}
	}

	@Test
	public void test_ServletContextHelper15_fullContextPath_forward() throws Exception {
		try {
			stopJetty();
			System.setProperty("org.eclipse.equinox.http.jetty.context.path", "/foo");
		}
		finally {
			startJetty();
		}

		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelperA = new ServletContextHelper(bundle){};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			final AtomicReference<String> path = new AtomicReference<>();

			Servlet servletA = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp)
					throws IOException, ServletException {
					RequestDispatcher rd = req.getRequestDispatcher("/foo/a/SB");
					rd.forward(req, resp);
				}
			};
			Servlet servletB = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp) {
					path.set((String)req.getAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH));
				}
			};

			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelperA, contextProps));
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SA");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/SA");
			registrations.add(getBundleContext().registerService(Servlet.class, servletA, props));
			props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SB");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/SB");
			registrations.add(getBundleContext().registerService(Servlet.class, servletB, props));

			requestAdvisor.request("a/SA");

			Assert.assertEquals("/foo/a", path.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
			try {
				stopJetty();
				System.setProperty("org.eclipse.equinox.http.jetty.context.path", "");
			}
			finally {
				startJetty();
			}
		}
	}

	@Test
	public void test_getRequestURI_trailingSlash1() throws Exception {
		try {
			stopJetty();
			System.setProperty("org.eclipse.equinox.http.jetty.context.path", "/foo");
		}
		finally {
			startJetty();
		}

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			final AtomicReference<String> getRequestURI = new AtomicReference<>();

			Servlet servletA = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp) {
					getRequestURI.set(req.getRequestURI());
				}
			};

			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SA");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"/*", "/"});
			registrations.add(getBundleContext().registerService(Servlet.class, servletA, props));
			props = new Hashtable<>();

			requestAdvisor.request("a/b/c/");

			Assert.assertEquals("/foo/a/b/c/", getRequestURI.get());
			requestAdvisor.request("a/b/");
			Assert.assertEquals("/foo/a/b/", getRequestURI.get());
			requestAdvisor.request("a/");
			Assert.assertEquals("/foo/a/", getRequestURI.get());
			// Note using empty string here because of the way requestAdvisor works
			// by appending a slash first.
			requestAdvisor.request("");
			Assert.assertEquals("/foo/", getRequestURI.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
			try {
				stopJetty();
				System.setProperty("org.eclipse.equinox.http.jetty.context.path", "");
			}
			finally {
				startJetty();
			}
		}
	}

	@Test
	public void test_getRequestURI_trailingSlash2() throws Exception {
		try {
			stopJetty();
			System.setProperty("org.eclipse.equinox.http.jetty.context.path", "/foo");
		}
		finally {
			startJetty();
		}

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			final AtomicReference<String> getRequestURI = new AtomicReference<>();

			Servlet servletA = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp) {
					HttpSession session = req.getSession();
					session.setAttribute("test", req.getParameter("p1"));
					getRequestURI.set(resp.encodeURL(req.getRequestURI()));
				}
			};

			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SA");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
			registrations.add(getBundleContext().registerService(Servlet.class, servletA, props));
			props = new Hashtable<>();

			requestAdvisor.request("a/b/c/?p1=v1");
			// get the session
			String initialURI = getRequestURI.get();
			int sessionIdx = initialURI.indexOf(";jsessionid=");
			Assert.assertTrue("No session: " + initialURI, sessionIdx > -1);
			String sessionPostfix = initialURI.substring(sessionIdx);
			Assert.assertEquals("/foo/a/b/c/" + sessionPostfix, getRequestURI.get());
			requestAdvisor.request("a/b/" + sessionPostfix);
			Assert.assertEquals("/foo/a/b/" + sessionPostfix, getRequestURI.get());
			requestAdvisor.request("a/" + sessionPostfix);
			Assert.assertEquals("/foo/a/" + sessionPostfix, getRequestURI.get());
			// Note using empty string here because of the way requestAdvisor works
			// by appending a slash first.
			requestAdvisor.request("" + sessionPostfix);
			Assert.assertEquals("/foo/" + sessionPostfix, getRequestURI.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
			try {
				stopJetty();
				System.setProperty("org.eclipse.equinox.http.jetty.context.path", "");
			}
			finally {
				startJetty();
			}
		}
	}

	@Test
	public void test_getRequestURI_trailingSlash3() throws Exception {
		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			final AtomicReference<String> getRequestURI = new AtomicReference<>();

			Servlet servletA = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp) {
					getRequestURI.set(req.getRequestURI());
				}
			};

			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SA");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] {"/*", "/"});
			registrations.add(getBundleContext().registerService(Servlet.class, servletA, props));
			props = new Hashtable<>();

			requestAdvisor.request("a/b/c/");
			Assert.assertEquals("/a/b/c/", getRequestURI.get());
			requestAdvisor.request("a/b/");
			Assert.assertEquals("/a/b/", getRequestURI.get());
			requestAdvisor.request("a/");
			Assert.assertEquals("/a/", getRequestURI.get());
			// Note using empty string here because of the way requestAdvisor works
			// by appending a slash first.
			requestAdvisor.request("");
			Assert.assertEquals("/", getRequestURI.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	@Test
	public void test_getRequestURI_trailingSlash4() throws Exception {
		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			final AtomicReference<String> getRequestURI = new AtomicReference<>();

			Servlet servletA = new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse resp) {
					HttpSession session = req.getSession();
					session.setAttribute("test", req.getParameter("p1"));
					getRequestURI.set(resp.encodeURL(req.getRequestURI()));
				}
			};

			Dictionary<String, Object> props = new Hashtable<>();
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "SA");
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
			registrations.add(getBundleContext().registerService(Servlet.class, servletA, props));
			props = new Hashtable<>();

			requestAdvisor.request("a/b/c/?p1=v1");
			// get the session
			String initialURI = getRequestURI.get();
			int sessionIdx = initialURI.indexOf(";jsessionid=");
			Assert.assertTrue("No session: " + initialURI, sessionIdx > -1);
			String sessionPostfix = initialURI.substring(sessionIdx);
			Assert.assertEquals("/a/b/c/" + sessionPostfix, getRequestURI.get());
			requestAdvisor.request("a/b/" + sessionPostfix);
			Assert.assertEquals("/a/b/" + sessionPostfix, getRequestURI.get());
			requestAdvisor.request("a/" + sessionPostfix);
			Assert.assertEquals("/a/" + sessionPostfix, getRequestURI.get());
			// Note using empty string here because of the way requestAdvisor works
			// by appending a slash first.
			requestAdvisor.request("" + sessionPostfix);
			Assert.assertEquals("/" + sessionPostfix, getRequestURI.get());
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	@Test
	public void test_Listener1() {
		BaseServletContextListener scl1 =
			new BaseServletContextListener();

		Dictionary<String, String> listenerProps = new Hashtable<>();
		listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		ServiceRegistration<ServletContextListener> registration = getBundleContext().registerService(ServletContextListener.class, scl1, listenerProps);
		registration.unregister();


		Assert.assertTrue(scl1.initialized.get());
		Assert.assertTrue(scl1.destroyed.get());
	}

	@Test
	public void test_Listener2() {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			BaseServletContextListener scl1 =
					new BaseServletContextListener();
			Dictionary<String, String> listenerProps = new Hashtable<>();
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

	@Test
	public void test_Listener3() {
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		BaseServletContextListener scl1 = new BaseServletContextListener();
		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> contextProps = new Hashtable<>();
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
			contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
			registrations.add(bundleContext.registerService(ServletContextHelper.class, servletContextHelper, contextProps));

			Dictionary<String, String> listenerProps = new Hashtable<>();
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

	@Test
	public void test_Listener4() throws Exception {

		BaseServletContextAttributeListener scal1 =
			new BaseServletContextAttributeListener();
		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletContextAttributeListener.class, scal1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
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

	@Test
	public void test_Listener5() throws Exception {

		BaseServletRequestListener srl1 = new BaseServletRequestListener();

		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletRequestListener.class, srl1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
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

	@Test
	public void test_Listener6() throws Exception {

		BaseServletRequestAttributeListener sral1 = new BaseServletRequestAttributeListener();

		Servlet s1 = new BaseServlet("a");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletRequestAttributeListener.class, sral1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
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

	@Test
	public void test_Listener7() throws Exception {

		BaseHttpSessionAttributeListener hsal1 =
			new BaseHttpSessionAttributeListener();

		Servlet s1 = new BaseServlet("test_Listener7");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(HttpSessionAttributeListener.class, hsal1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
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

			Map<String, List<String>> requestHeaders = new HashMap<>();
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

	@Test
	public void test_Listener8() throws Exception {
		BaseHttpSessionIdListener hsil1 = new BaseHttpSessionIdListener();

		Servlet s1 = new BaseChangeSessionIdServlet("test_Listener8");

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(HttpSessionIdListener.class, hsil1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
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

	@Test
	public void test_Listener9() throws Exception {
		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

				RequestDispatcher requestDispatcher = req.getRequestDispatcher("/s9B");

				requestDispatcher.include(req, resp);
			}
		};
		Servlet sB = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

				PrintWriter writer = resp.getWriter();
				writer.write("S9 included");
			}
		};

		BaseServletRequestListener srl1 = new BaseServletRequestListener();

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> listenerProps = new Hashtable<>();
			listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletRequestListener.class, srl1, listenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S9A");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s9A");
			registrations.add(getBundleContext().registerService(Servlet.class, sA, servletProps1));

			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S9B");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s9B");
			registrations.add(getBundleContext().registerService(Servlet.class, sB, servletProps1));

			String result = requestAdvisor.request("s9A");
			Assert.assertEquals("S9 included", result);

			Assert.assertEquals(0, srl1.number.get());

		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	@Test
	public void test_Listener10() {
		BaseServletContextListener scl1 = new BaseServletContextListener();
		BaseServletContextListener scl2 = new BaseServletContextListener();
		BaseServletContextListener scl3 = new BaseServletContextListener();

		Dictionary<String, String> listenerProps = new Hashtable<>();
		listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		registrations.add(getBundleContext().registerService(ServletContextListener.class, scl1, listenerProps));

		listenerProps = new Hashtable<>();
		listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		registrations.add(getBundleContext().registerService(ServletContextListener.class, scl2, listenerProps));

		Dictionary<String, String> contextProps = new Hashtable<>();
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "a");
		contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/a");
		registrations.add(getBundleContext().registerService(ServletContextHelper.class, new ServletContextHelper(){}, contextProps));

		listenerProps = new Hashtable<>();
		listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		listenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=a)");
		registrations.add(getBundleContext().registerService(ServletContextListener.class, scl3, listenerProps));

		ServletContext servletContext1 = scl1.servletContext;
		ServletContext servletContext2 = scl2.servletContext;
		ServletContext servletContext3 = scl3.servletContext;

		Assert.assertNotNull(servletContext1);
		Assert.assertNotNull(servletContext2);
		Assert.assertNotNull(servletContext3);

		Assert.assertTrue(servletContext1.equals(servletContext1));
		Assert.assertTrue(servletContext2.equals(servletContext2));
		Assert.assertTrue(servletContext3.equals(servletContext3));

		Assert.assertTrue(servletContext1.equals(servletContext2));
		Assert.assertFalse(servletContext1.equals(servletContext3));
		Assert.assertFalse(servletContext2.equals(servletContext3));

		// Asserts two invocations return the same value
		Assert.assertEquals(servletContext1.hashCode(), servletContext1.hashCode());
		Assert.assertEquals(servletContext2.hashCode(), servletContext2.hashCode());
		Assert.assertEquals(servletContext3.hashCode(), servletContext3.hashCode());

		Assert.assertEquals(servletContext1.hashCode(), servletContext2.hashCode());
		Assert.assertNotEquals(servletContext1.hashCode(), servletContext3.hashCode());
		Assert.assertNotEquals(servletContext2.hashCode(), servletContext3.hashCode());
	}

	@Test
	public void test_Listener11() throws Exception {

		final AtomicInteger listenerBalance = new AtomicInteger(0);
		final AtomicReference<HttpSession> sessionReference = new AtomicReference<>();

		ServletContextListener scl = new ServletContextListener() {

			@Override
			public void contextInitialized(ServletContextEvent arg0) {
			}

			@Override
			public void contextDestroyed(ServletContextEvent arg0) {
				listenerBalance.decrementAndGet();
			}
		};

		HttpSessionListener sl = new HttpSessionListener() {

			@Override
			public void sessionDestroyed(HttpSessionEvent se) {
				listenerBalance.incrementAndGet();
			}

			@Override
			public void sessionCreated(HttpSessionEvent se) {
			}
		};

		Servlet sA = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(
				HttpServletRequest req, HttpServletResponse resp)
				throws IOException {

				HttpSession session = req.getSession();
				sessionReference.set(session);

				session.setAttribute("testAttribute", "testValue");
				PrintWriter writer = resp.getWriter();
				writer.write("S11 requested");
			}
		};

		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> scListenerProps = new Hashtable<>();
			scListenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(ServletContextListener.class, scl, scListenerProps));

			Dictionary<String, String> sListenerProps = new Hashtable<>();
			sListenerProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			registrations.add(getBundleContext().registerService(HttpSessionListener.class, sl, sListenerProps));

			Dictionary<String, String> servletProps1 = new Hashtable<>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "S11");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/s11");
			registrations.add(getBundleContext().registerService(Servlet.class, sA, servletProps1));

			String result = requestAdvisor.request("s11");
			Assert.assertEquals("S11 requested", result);
		}
		finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}

		//Emulate session expiration to check sessionListener
		//is only called once (when unregister)
		HttpSession session = sessionReference.get();

		session.invalidate();

		Assert.assertEquals(0, listenerBalance.get());
	}

	@Test
	public void test_Async1() throws Exception {

		Servlet s1 = new BaseAsyncServlet("test_Listener8");
		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, Object> servletProps1 = new Hashtable<>();
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

	@Test
	public void test_AsyncOutput1() throws Exception {
		Servlet s1 = new AsyncOutputServlet();
		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, Object> servletProps1 = new Hashtable<>();
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "AsyncOutputServlet");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/asyncOutput");
			servletProps1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, true);
			registrations.add(getBundleContext().registerService(Servlet.class, s1, servletProps1));

			String output1 = requestAdvisor.request("asyncOutput?iterations=2");

			Assert.assertEquals("write(int)", "01234567890123456789", output1);

			String output2 = requestAdvisor.request("asyncOutput?bytes=true&iterations=4");

			Assert.assertEquals("write(byte[], int, int)", "0123456789012345678901234567890123456789", output2);
		} finally {
			for (ServiceRegistration<?> registration : registrations) {
				registration.unregister();
			}
		}
	}

	@Test
	public void test_WBServlet1() throws Exception {
		String expected = "a";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("WBServlet1/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_WBServlet2() throws Exception {
		String expected = "bab";
		String actual;
		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("WBServlet2/a");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void test_BufferedOutput() throws Exception {
		Collection<ServiceRegistration<?>> registrations = new ArrayList<>();
		try {
			Dictionary<String, String> servletProps1 = new Hashtable<>();
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

	@Test
	public void testWBServletChangeInitParams() throws Exception{
		BundleContext bundleContext = getBundleContext();
		Dictionary<String, Object> serviceProps = new Hashtable<>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		registrations.add(bundleContext.registerService(Servlet.class, testDriver, serviceProps));

			String actual;

			Map<String, String> params = new HashMap<>();
			params.put(TEST_PROTOTYPE_NAME, testName.getMethodName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + testName.getMethodName());
			params.put(STATUS_PARAM, testName.getMethodName());
			actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(testName.getMethodName(), actual);
			actual = requestAdvisor.request(testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);

			// change the init param
			params.put(STATUS_PARAM, "changed");
			doRequest(CONFIGURE, params);
			actual = requestAdvisor.request(testName.getMethodName());
			Assert.assertEquals("changed", actual);
	}

	@Test
	public void testWBServletChangePattern() throws Exception{
		BundleContext bundleContext = getBundleContext();
		Dictionary<String, Object> serviceProps = new Hashtable<>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		registrations.add(bundleContext.registerService(Servlet.class, testDriver, serviceProps));

		String actual;

		Map<String, String> params = new HashMap<>();
		params.put(TEST_PROTOTYPE_NAME, testName.getMethodName());
		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + testName.getMethodName());
		params.put(STATUS_PARAM, testName.getMethodName());
		actual = doRequest(CONFIGURE, params);
		Assert.assertEquals(testName.getMethodName(), actual);
		actual = requestAdvisor.request(testName.getMethodName());
		Assert.assertEquals(testName.getMethodName(), actual);

		// change the pattern
		params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/changed");
		doRequest(CONFIGURE, params);
		actual = requestAdvisor.request("changed");
		Assert.assertEquals(testName.getMethodName(), actual);
	}

	@Test
	public void testWBServletChangeRanking() throws Exception{
		BundleContext bundleContext = getBundleContext();
		Dictionary<String, Object> serviceProps = new Hashtable<>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		registrations.add(bundleContext.registerService(Servlet.class, testDriver, serviceProps));

		String actual;

		// Configure two servlets with the second one registered ranking higher
		Map<String, String> params1 = new HashMap<>();
		params1.put(TEST_PROTOTYPE_NAME, testName.getMethodName() + 1);
		params1.put(Constants.SERVICE_RANKING, "1");
		params1.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + testName.getMethodName());
		params1.put(STATUS_PARAM, testName.getMethodName() + 1);
		actual = doRequest(CONFIGURE, params1);
		Assert.assertEquals(testName.getMethodName() + 1, actual);

		Map<String, String> params2 = new HashMap<>();
		params2.put(TEST_PROTOTYPE_NAME, testName.getMethodName() + 2);
		params2.put(Constants.SERVICE_RANKING, "2");
		params2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + testName.getMethodName());
		params2.put(STATUS_PARAM, testName.getMethodName() + 2);
		actual = doRequest(CONFIGURE, params2);
		Assert.assertEquals(testName.getMethodName() + 2, actual);

		// Confirm the second registered (higher ranked) gets used
		actual = requestAdvisor.request(testName.getMethodName());
		Assert.assertEquals(testName.getMethodName() + 2, actual);

		// change the ranking to use the first servlet registered
		params2.put(Constants.SERVICE_RANKING, "0");
		doRequest(CONFIGURE, params2);
		actual = requestAdvisor.request(testName.getMethodName());
		Assert.assertEquals(testName.getMethodName() + 1, actual);

		// Unregister the first servlet should cause the second servlet to be used
		actual = doRequest(UNREGISTER, Collections.singletonMap(TEST_PROTOTYPE_NAME, testName.getMethodName() + 1));
		Assert.assertEquals(testName.getMethodName() + 1, actual);

		// Confirm the second registered is used
		actual = requestAdvisor.request(testName.getMethodName());
		Assert.assertEquals(testName.getMethodName() + 2, actual);
	}

	@Test
	public void testWBServletDefaultContextAdaptor1() throws Exception{
		BundleContext bundleContext = getBundleContext();
		Dictionary<String, Object> serviceProps = new Hashtable<>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		registrations.add(bundleContext.registerService(Servlet.class, testDriver, serviceProps));

		Dictionary<String, String> helperProps = new Hashtable<>();
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "testContext" + testName.getMethodName());
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/testContext");
		helperProps.put(TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
		ServiceRegistration<ServletContextHelper> helperReg = getBundleContext().registerService(ServletContextHelper.class, new TestServletContextHelperFactory(), helperProps);

		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		try {
			Map<String, String> params = new HashMap<>();
			params.put(TEST_PROTOTYPE_NAME, testName.getMethodName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + testName.getMethodName());
			params.put(STATUS_PARAM, testName.getMethodName());
			params.put("servlet.init." + TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
			String actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(testName.getMethodName(), actual);

			actual = requestAdvisor.request(testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);

			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor("(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext" + testName.getMethodName() + ")", null, testName.getMethodName());
			pathAdaptorReg = getBundleContext().registerService(ContextPathCustomizer.class, pathAdaptor, null);

			actual = requestAdvisor.request("testContext/" + testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);

			pathAdaptorReg.unregister();
			pathAdaptorReg = null;

			actual = requestAdvisor.request(testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);
			doRequest(UNREGISTER, params);
		} finally {
			helperReg.unregister();
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
		}
	}

	@Test
	public void testWBServletDefaultContextAdaptor2() throws Exception{
		BundleContext bundleContext = getBundleContext();
		Dictionary<String, Object> serviceProps = new Hashtable<>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		registrations.add(bundleContext.registerService(Servlet.class, testDriver, serviceProps));

		Dictionary<String, String> helperProps = new Hashtable<>();
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "testContext" + testName.getMethodName());
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/testContext");
		helperProps.put(TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
		ServiceRegistration<ServletContextHelper> helperReg = getBundleContext().registerService(ServletContextHelper.class, new TestServletContextHelperFactory(), helperProps);

		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		try {
			Map<String, String> params = new HashMap<>();
			params.put(TEST_PROTOTYPE_NAME, testName.getMethodName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + testName.getMethodName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext" + testName.getMethodName() + ")");
			params.put(STATUS_PARAM, testName.getMethodName());
			params.put("servlet.init." + TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
			String actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(testName.getMethodName(), actual);

			actual = requestAdvisor.request("testContext/" + testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);

			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor(null, "testPrefix", testName.getMethodName());
			pathAdaptorReg = getBundleContext().registerService(ContextPathCustomizer.class, pathAdaptor, null);

			actual = requestAdvisor.request("testPrefix/testContext/" + testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);

			pathAdaptorReg.unregister();
			pathAdaptorReg = null;

			actual = requestAdvisor.request("testContext/" + testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);
			doRequest(UNREGISTER, params);
		} finally {
			helperReg.unregister();
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
		}
	}

	@Test
	public void testWBServletDefaultContextAdaptor3() throws Exception{
		BundleContext bundleContext = getBundleContext();
		Dictionary<String, Object> serviceProps = new Hashtable<>();
		serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/prototype/*");
		TestServletPrototype testDriver = new TestServletPrototype(bundleContext);
		registrations.add(bundleContext.registerService(Servlet.class, testDriver, serviceProps));

		// test the ContextPathCustomizer with a ServletContextHelper that has a '/' context path
		Dictionary<String, String> helperProps = new Hashtable<>();
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "testContext" + testName.getMethodName());
		helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
		helperProps.put(TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
		ServiceRegistration<ServletContextHelper> helperReg = getBundleContext().registerService(ServletContextHelper.class, new TestServletContextHelperFactory(), helperProps);

		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		try {
			Map<String, String> params = new HashMap<>();
			params.put(TEST_PROTOTYPE_NAME, testName.getMethodName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, '/' + testName.getMethodName());
			params.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext" + testName.getMethodName() + ")");
			params.put(STATUS_PARAM, testName.getMethodName());
			params.put("servlet.init." + TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
			String actual = doRequest(CONFIGURE, params);
			Assert.assertEquals(testName.getMethodName(), actual);

			actual = requestAdvisor.request(testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);

			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor(null, "testPrefix", testName.getMethodName());
			pathAdaptorReg = getBundleContext().registerService(ContextPathCustomizer.class, pathAdaptor, null);

			actual = requestAdvisor.request("testPrefix/" + testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);

			pathAdaptorReg.unregister();
			pathAdaptorReg = null;

			actual = requestAdvisor.request(testName.getMethodName());
			Assert.assertEquals(testName.getMethodName(), actual);
			doRequest(UNREGISTER, params);
		} finally {
			helperReg.unregister();
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
		}
	}

	@Test
	public void testHttpContextSetUser() throws ServletException, NamespaceException, IOException {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		HttpContext testContext = new HttpContext() {

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
				request.setAttribute(HttpContext.REMOTE_USER, "TEST");
				request.setAttribute(HttpContext.AUTHENTICATION_TYPE, "Basic");
				return true;
			}

			@Override
			public URL getResource(String name) {
				return null;
			}

			@Override
			public String getMimeType(String name) {
				return null;
			}
		};
		HttpServlet testServlet = new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp)
					throws IOException {
				resp.setContentType("text/html");
				PrintWriter out = resp.getWriter();
				out.print("USER: " + req.getRemoteUser() + " AUTH_TYPE: " + req.getAuthType());
			}

		};
		extendedHttpService.registerServlet("/" + testName.getMethodName(), testServlet, null, testContext);

		String expected = "USER: TEST AUTH_TYPE: Basic";
		String actual = requestAdvisor.request(testName.getMethodName());
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testHTTPSEndpoint() throws Exception {
		stopJetty();
		File keyStoreFile = getBundleContext().getDataFile("server-keystore.jks");
		URL keyStoreURL = getClass().getResource("server-keystore.jks");
		if (!keyStoreFile.exists()) {
			Files.copy(keyStoreURL.openStream(), keyStoreFile.toPath());
		}

		startJettyWithSSL("8443", keyStoreFile.getAbsolutePath(), "secret", "secret");

		Bundle bundle = installBundle(TEST_BUNDLE_1);
		try {
			bundle.start();

			String actual = requestAdvisor.requestHttps("TestServlet10");
			assertEquals("Expected output not found", "a", actual);
		} finally {
			uninstallBundle(bundle);
			stopJettyWithSSL();
		}
	}
}
