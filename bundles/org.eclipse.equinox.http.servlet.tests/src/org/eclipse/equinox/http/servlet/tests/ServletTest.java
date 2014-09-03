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

import java.util.Arrays;
import java.util.Collections;
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

import junit.framework.Assert;
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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@SuppressWarnings("deprecation")
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

	public void test_Registration1_1() throws Exception {
		String expected = "Patterns or servletNames must contain a value.";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				new BaseServlet(), "S1", null, null, false, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration2_1() throws Exception {
		String pattern = "blah";
		String expected = "Invalid pattern '" + pattern + "'";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				new BaseServlet(), "S1", new String[] {pattern}, null, false, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration3_1() throws Exception {
		String pattern = "/blah/";
		String expected = "Invalid pattern '" + pattern + "'";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				new BaseServlet(), "S1", new String[] {pattern}, null, false, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration4_1() throws Exception {
		String pattern = "/blah";
		String expected = "Pattern already in use: " + pattern;
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				new BaseServlet(), "S1", new String[] {pattern}, null, false, null, null);
			extendedHttpService.registerServlet(
				new BaseServlet(), "S1", new String[] {pattern}, null, false, null, null);
		}
		catch(NamespaceException ne) {
			Assert.assertEquals(expected, ne.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration5_1() throws Exception {
		String pattern = "/blah";
		String expected = "Servlet cannot be null";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServlet(
				null, "S1", new String[] {pattern}, null, false, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration6_1() throws Exception {
		String expected = "Servlet has already been registered:";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Servlet servlet = new BaseServlet();

			extendedHttpService.registerServlet(
				servlet, "S1", new String[] {"/blah1"}, null, false, null, null);
			extendedHttpService.registerServlet(
				servlet, "S1", new String[] {"/blah2"}, null, false, null, null);
		}
		catch(ServletException se) {
			Assert.assertTrue(se.getMessage().startsWith(expected));

			return;
		}

		Assert.fail();
	}

	public void test_Registration7() throws Exception {
		String expected = "Filter cannot be null";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerFilter("/*", null, new Hashtable<String, String>(), null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration7_1() throws Exception {
		String expected = "Filter cannot be null";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerFilter(
				null, "F1", new String[] {"/*"}, null, null, false, 0, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration7_2() throws Exception {
		String expected = "Patterns or servletNames must contain a value.";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Filter f1 = new EmptyFilter();

			extendedHttpService.registerFilter(
				f1, "F1", null, null, null, false, 0, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration7_3() throws Exception {
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Filter f1 = new EmptyFilter();

			extendedHttpService.registerFilter(
				f1, "F1", null, new String[] {"blah"}, null, false, 0, null, null);
		}
		catch(Exception e) {
			Assert.fail();
		}
	}

	public void test_Registration7_4() throws Exception {
		String dispatcher = "BAD";
		String expected = "Invalid dispatcher '" + dispatcher + "'";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Filter f1 = new EmptyFilter();

			extendedHttpService.registerFilter(
				f1, "F1", new String[] {"/*"}, null, new String[] {dispatcher}, false, 0, null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration7_5() throws Exception {
		String contextSelector = "blah";
		String expected = "No valid ServletContextHelper for filter '(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + contextSelector + ")'";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Filter f1 = new EmptyFilter();

			extendedHttpService.registerFilter(
				f1, "F1", new String[] {"/*"}, null, null, false, 0, null, contextSelector);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration8() throws Exception {
		String expected = "Filter has already been registered: ";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Filter f1 = new Filter() {
				@Override
				public void destroy() {/**/}
				@Override
				public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException, ServletException {/**/}
				@Override
				public void init(FilterConfig arg0) throws ServletException {/**/}
			};

			extendedHttpService.registerFilter("/*", f1, new Hashtable<String, String>(), null);
			extendedHttpService.registerFilter("/*", f1, new Hashtable<String, String>(), null);
		}
		catch(ServletException se) {
			Assert.assertTrue(se.getMessage().startsWith(expected));

			return;
		}

		Assert.fail();
	}

	public void test_Registration8_1() throws Exception {
		String expected = "Filter has already been registered: ";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Filter f1 = new EmptyFilter();

			extendedHttpService.registerFilter(
				f1, "F1", new String[] {"/*"}, null, null, false, 0, null, null);
			extendedHttpService.registerFilter(
				f1, "F1", new String[] {"/*"}, null, null, false, 0, null, null);
		}
		catch(ServletException se) {
			Assert.assertTrue(se.getMessage().startsWith(expected));

			return;
		}

		Assert.fail();
	}

	public void test_Registration9() throws Exception {
		String expected = "Prefix cannot be null";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerResources("/blah", null, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Registration10() throws Exception {
		String prefix = "/blah2/";
		String expected = "Invalid prefix '" + prefix + "'";
		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerResources("/blah1", prefix, null);
		}
		catch(IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

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
			Assert.assertTrue(servletContextDTO.names.length > 0);
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
		String expected = "Patterns must contain a value.";
		String prefix =
			"/" + getClass().getPackage().getName().replaceAll("\\.", "/");

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerResources((String[])null, prefix, null);
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Resource5_1() throws Exception {
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

	public void test_Resource6() throws Exception {
		String expected = "Patterns must contain a value.";
		String prefix =
			"/" + getClass().getPackage().getName().replaceAll("\\.", "/");

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerResources(new String[0], prefix, null);
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Resource7() throws Exception {
		String expected = "Prefix cannot be null";

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerResources(
				new String[] {"/files/*"}, null, null);
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Resource8() throws Exception {
		String pattern = "files/*";
		String expected = "Invalid pattern '" + pattern + "'";

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerResources(
				new String[] {pattern}, "/tmp", null);
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_Resource9() throws Exception {
		String pattern = "/files/";
		String expected = "Invalid pattern '" + pattern + "'";

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerResources(
				new String[] {pattern}, "/tmp", null);
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
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

	public void test_ServletContextHelper1() throws Exception {
		String expected = "ServletContexHelper cannot be null.";

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			extendedHttpService.registerServletContextHelper(null, null, null, null, null);
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_ServletContextHelper2() throws Exception {
		String expected = "ContextNames must contain a value.";

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			BundleContext bundleContext = getBundleContext();
			Bundle bundle = bundleContext.getBundle();

			ServletContextHelper servletContextHelper = new ServletContextHelper(bundle) {};

			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, null, null, null);
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_ServletContextHelper3() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};

		try {
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, null, null);
		}
		catch (Exception e) {
			Assert.fail();
		}
		finally {
			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);
		}
	}

	public void test_ServletContextHelper4() throws Exception {
		String expected = "ServletContextHelper not found: ";
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};

		try {
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, null, null);

			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);

			try {
				extendedHttpService.unregisterServletContextHelper(
					servletContextHelper);

				Assert.fail();
			}
			catch (IllegalArgumentException iae) {
				Assert.assertTrue(iae.getMessage().startsWith(expected));
			}
		}
		catch (Exception e) {
			Assert.fail();
		}
	}

	public void test_ServletContextHelper5() throws Exception {
		String contextSelector = "a";
		String expected = "No valid ServletContextHelper for filter '(" +
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" +
				contextSelector + ")'";

		try {
			ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

			Servlet s1 = new BaseServlet();
			extendedHttpService.registerServlet(
				s1, "S1", new String[] {"/s1"}, null, false, null, "a");
		}
		catch (IllegalArgumentException iae) {
			Assert.assertEquals(expected, iae.getMessage());

			return;
		}

		Assert.fail();
	}

	public void test_ServletContextHelper6() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet();

		try {
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, null, null);

			extendedHttpService.registerServlet(
				s1, "S1", new String[] {"/s1"}, null, false, null, "a");
		}
		catch (IllegalArgumentException iae) {
			Assert.fail();
		}
		finally {
			extendedHttpService.unregisterServlet(s1, "a");
			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);
		}
	}

	public void test_ServletContextHelper7() throws Exception {
		String expected = "a";

		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet("a");

		try {
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, null, null);

			extendedHttpService.registerServlet(
				s1, "S1", new String[] {"/s1"}, null, false, null, "a");

			String actual = requestAdvisor.request("s1");

			Assert.assertEquals(expected, actual);
		}
		finally {
			extendedHttpService.unregisterServlet(s1, "a");
			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);
		}
	}

	public void test_ServletContextHelper8() throws Exception {
		String expected = "b";

		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();
		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet("b");

		try {
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, "/a", null);

			extendedHttpService.registerServlet(
				s1, "S1", new String[] {"/s1"}, null, false, null, "a");

			String actual = requestAdvisor.request("a/s1");

			Assert.assertEquals(expected, actual);
		}
		finally {
			extendedHttpService.unregisterServlet(s1, "a");
			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);
		}
	}

	public void test_ServletContextHelper9() throws Exception {
		String expected1 = "c";
		String expected2 = "d";

		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		Servlet s1 = new BaseServlet(expected1);
		Servlet s2 = new BaseServlet(expected2);

		try {
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, "/a", null);

			extendedHttpService.registerServlet(
				s1, "S1", new String[] {"/s"}, null, false, null, null);

			extendedHttpService.registerServlet(
				s2, "S1", new String[] {"/s"}, null, false, null, "a");

			String actual = requestAdvisor.request("s");

			Assert.assertEquals(expected1, actual);

			actual = requestAdvisor.request("a/s");

			Assert.assertEquals(expected2, actual);
		}
		finally {
			extendedHttpService.unregisterServlet(s1, null);
			extendedHttpService.unregisterServlet(s2, "a");
			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);
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
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		BaseServletContextListener scl1 =
			new BaseServletContextListener();

		extendedHttpService.registerListener(scl1, null);
		extendedHttpService.unregisterListener(scl1, null);

		Assert.assertTrue(scl1.initialized.get());
		Assert.assertTrue(scl1.destroyed.get());
	}

	public void test_Listener2() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};

		try{
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, "/a", null);

			BaseServletContextListener scl1 =
				new BaseServletContextListener();

			extendedHttpService.registerListener(scl1, "a");
			extendedHttpService.unregisterListener(scl1, "a");

			Assert.assertTrue(scl1.initialized.get());
			Assert.assertTrue(scl1.destroyed.get());
		}
		finally {
			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);
		}
	}

	public void test_Listener3() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();
		BundleContext bundleContext = getBundleContext();
		Bundle bundle = bundleContext.getBundle();

		ServletContextHelper servletContextHelper = new ServletContextHelper(bundle){};
		BaseServletContextListener scl1 = new BaseServletContextListener();

		try {
			extendedHttpService.registerServletContextHelper(
				servletContextHelper, bundle, new String[] {"a"}, "/a", null);

			extendedHttpService.registerListener(scl1, "a");

			Assert.assertTrue(scl1.initialized.get());
		}
		finally {
			extendedHttpService.unregisterListener(scl1, "a");
			extendedHttpService.unregisterServletContextHelper(
				servletContextHelper);
			Assert.assertTrue(scl1.destroyed.get());
		}
	}

	public void test_Listener4() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		BaseServletContextAttributeListener scal1 =
			new BaseServletContextAttributeListener();

		extendedHttpService.registerListener(scal1, null);

		Servlet s1 = new BaseServlet("a");
		extendedHttpService.registerServlet(
			s1, "S1", new String[] {"/s"}, null, false, null, null);

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

	public void test_Listener5() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		BaseServletRequestListener srl1 = new BaseServletRequestListener();

		extendedHttpService.registerListener(srl1, null);

		Servlet s1 = new BaseServlet("a");
		extendedHttpService.registerServlet(
			s1, "S1", new String[] {"/s"}, null, false, null, null);

		requestAdvisor.request("s");

		Assert.assertTrue(srl1.initialized.get());
		Assert.assertTrue(srl1.destroyed.get());
	}

	public void test_Listener6() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		BaseServletRequestAttributeListener sral1 = new BaseServletRequestAttributeListener();

		extendedHttpService.registerListener(sral1, null);

		Servlet s1 = new BaseServlet("a");
		extendedHttpService.registerServlet(
			s1, "S1", new String[] {"/s"}, null, false, null, null);

		requestAdvisor.request("s");

		Assert.assertTrue(sral1.added.get());
		Assert.assertTrue(sral1.replaced.get());
		Assert.assertTrue(sral1.removed.get());
	}

	public void test_Listener7() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		BaseHttpSessionAttributeListener hsal1 =
			new BaseHttpSessionAttributeListener();

		extendedHttpService.registerListener(hsal1, null);

		Servlet s1 = new BaseServlet("test_Listener7");
		extendedHttpService.registerServlet(
			s1, "S1", new String[] {"/s"}, null, false, null, null);

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

	public void test_Listener8() throws Exception {
		ExtendedHttpService extendedHttpService = (ExtendedHttpService)getHttpService();

		Servlet s1 = new BaseAsyncServlet("test_Listener8");
		extendedHttpService.registerServlet(
			s1, "S1", new String[] {"/s"}, null, true, null, null);

		String output1 = requestAdvisor.request("s");

		Assert.assertTrue(output1, output1.endsWith("test_Listener8"));
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
