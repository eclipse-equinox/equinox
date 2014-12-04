/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.tests.bundle.Activator;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleAdvisor;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleInstaller;
import org.eclipse.equinox.http.servlet.tests.util.BaseAsyncServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseHttpContext;
import org.eclipse.equinox.http.servlet.tests.util.BaseHttpSessionAttributeListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServlet;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletContextAttributeListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletContextListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletRequestAttributeListener;
import org.eclipse.equinox.http.servlet.tests.util.BaseServletRequestListener;
import org.eclipse.equinox.http.servlet.tests.util.ServletRequestAdvisor;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
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
		stopJetty();
		stopBundles();
		requestAdvisor = null;
		advisor = null;
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

		ServletDTO servletDTO = servletContextDTOs[0].servletDTOs[0];

		Assert.assertFalse(servletDTO.asyncSupported);
		Assert.assertEquals(servlet.getClass().getName(), servletDTO.name);
		Assert.assertEquals("/blah1", servletDTO.patterns[0]);
		Assert.assertTrue(servletDTO.serviceId < 0);
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

			Assert.assertNotNull(servletContextDTO.contextName);
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

		ResourceDTO resourceDTO = servletContextDTOs[0].resourceDTOs[0];

		Assert.assertEquals("/blah1/*", resourceDTO.patterns[0]);
		Assert.assertEquals("/foo", resourceDTO.prefix);
		Assert.assertTrue(resourceDTO.serviceId < 0);
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

			ServletContextDTO servletContextDTO = servletContextDTOs[0];

			Assert.assertNotNull(servletContextDTO.contextName);
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
		String expected = "Equinox Jetty-based Http Service";
		String actual;
		Bundle bundle = installBundle(ServletTest.TEST_BUNDLE_1);
		try {
			bundle.start();
			actual = requestAdvisor.request("TestServlet8");
		} finally {
			uninstallBundle(bundle);
		}
		Assert.assertEquals(expected, actual);
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

	public void test_Listener1() throws Exception {
		BaseServletContextListener scl1 =
			new BaseServletContextListener();

		Dictionary<String, String> listenerProps = new Hashtable<String, String>();
		ServiceRegistration<EventListener> registration = getBundleContext().registerService(EventListener.class, scl1, listenerProps);
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
			ServiceRegistration<EventListener> registration = getBundleContext().registerService(EventListener.class, scl1, listenerProps);
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
			registrations.add(bundleContext.registerService(EventListener.class, scl1, listenerProps));

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
			registrations.add(getBundleContext().registerService(EventListener.class, scal1, listenerProps));

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
			registrations.add(getBundleContext().registerService(EventListener.class, srl1, listenerProps));

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
			registrations.add(getBundleContext().registerService(EventListener.class, sral1, listenerProps));

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
			registrations.add(getBundleContext().registerService(EventListener.class, hsal1, listenerProps));

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

	private static final String PROTOTYPE = "prototype/";
	private static final String CONFIGURE = "configure";
	private static final String UNREGISTER = "unregister";
	private static final String STATUS_PARAM = "servlet.init.status";
	private static final String TEST_PROTOTYPE_NAME = "test.prototype.name";
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

	private String doRequest(String action, Map<String, String> params) throws IOException {
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
		return requestAdvisor.request(requestInfo.toString());
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

	class EmptyFilter implements Filter {
		@Override
		public void destroy() {/**/}
		@Override
		public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException, ServletException {/**/}
		@Override
		public void init(FilterConfig arg0) throws ServletException {/**/}
	}

}
