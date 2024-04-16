/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.eclipse.equinox.http.servlet.context.ContextPathCustomizer;
import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.BaseServlet;
import org.eclipse.equinox.http.servlet.tests.util.MockSCL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class ContextHelperCustomizerTests extends BaseTest {
	@Rule
	public TestName testName = new TestName();
	private ServiceReference<HttpService> httpServiceReference;
	private HttpService httpService;
	private BundleContext context;

	@Before
	public void begin() {
		httpServiceReference = getBundleContext().getServiceReference(HttpService.class);
		context = httpServiceReference.getBundle().getBundleContext();
		httpService = context.getService(httpServiceReference);
	}

	@After
	public void end() {
		context.ungetService(httpServiceReference);
	}

	@Test
	public void testCreateDefaultHttpContextCreatesNewServletContextHelper() {
		HttpContext context1 = httpService.createDefaultHttpContext();
		HttpContext context2 = httpService.createDefaultHttpContext();
		Assert.assertNotEquals(context1, context2);
	}

	@Test
	public void testServletContextHelpersNotHiddenWhenRegisteredUsingConsumingContext() {
		ServiceRegistration<ServletContextHelper> helperReg = null;
		ServiceRegistration<FindHook> findHookReg = null;

		try {
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "context1");
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/context1");
			// register a ServletContextHelper using the consuming bundle context
			// "org.eclipse.equinox.http.servlet"
			helperReg = context.registerService(ServletContextHelper.class, new ServletContextHelper() {
			}, properties);

			FindHook findHook = (bundleContext, name, filter, allServices, references) -> {

				if (bundleContext != context) {
					return;
				}

				// don't show ServletContextHelper
				for (Iterator<ServiceReference<?>> iterator = references.iterator(); iterator.hasNext();) {
					ServiceReference<?> sr = iterator.next();

					if ("context1".equals(sr.getProperty("osgi.http.whiteboard.context.name"))) {
						iterator.remove();
					}
				}
			};

			findHookReg = context.registerService(FindHook.class, findHook, null);
			AtomicReference<ServletContext> sc1 = new AtomicReference<>();

			properties = new Hashtable<>();
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
			properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
					"(osgi.http.whiteboard.context.name=context1)");
			context.registerService(ServletContextListener.class, new MockSCL(sc1), properties);

			// ServletContextHelpers registered using the consuming context should not be
			// hidden
			assertNotNull(sc1.get());
		} finally {
			if (helperReg != null) {
				helperReg.unregister();
			}
			if (findHookReg != null) {
				findHookReg.unregister();
			}
		}

	}

	@Test
	public void testWBServletContextPathCustomizerContextPrefix() throws Exception {
		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		ServiceRegistration<ServletContextHelper> helperReg = null;
		ServiceRegistration<Servlet> servlet = null;
		ServiceRegistration<FindHook> findHookReg = null;

		try {
			Dictionary<String, String> helperProps = new Hashtable<>();
			helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
					"testContext" + testName.getMethodName());
			helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/helperContext");
			helperProps.put(TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
			helperReg = context.registerService(ServletContextHelper.class, new TestServletContextHelperFactory(),
					helperProps);

			// Pass the context path prefix paramater
			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor(null, "testPrefix",
					testName.getMethodName());
			pathAdaptorReg = context.registerService(ContextPathCustomizer.class, pathAdaptor, null);

			FindHook findHook = (bundleContext, name, filter, allServices, references) -> {

				if (bundleContext != context) {
					return;
				}

				// don't show ServletContextHelper
				for (Iterator<ServiceReference<?>> iterator = references.iterator(); iterator.hasNext();) {
					ServiceReference<?> sr = iterator.next();
					if (("testContext" + testName.getMethodName())
							.equals(sr.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME))) {
						iterator.remove();
					}
				}
			};

			findHookReg = context.registerService(FindHook.class, findHook, null);

			// Register a servlet service with a matching context helper
			BaseServlet baseServlet = new BaseServlet("content");
			Dictionary<String, Object> serviceProps = new Hashtable<>();
			serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet");
			serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
					"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext"
							+ testName.getMethodName() + ")");
			servlet = context.registerService(Servlet.class, baseServlet, serviceProps);

			String actual = requestAdvisor.request("testPrefix/helperContext/servlet");
			Assert.assertEquals("content", actual);
		} finally {
			if (helperReg != null) {
				helperReg.unregister();
			}
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
			if (servlet != null) {
				servlet.unregister();
			}
			if (findHookReg != null) {
				findHookReg.unregister();
			}
		}
	}

	@Test
	public void testWBServletContextPathCustomizerDefaultFilter() throws Exception {
		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		ServiceRegistration<ServletContextHelper> helperReg = null;
		ServiceRegistration<Servlet> servlet = null;
		ServiceRegistration<FindHook> findHookReg = null;

		try {
			Dictionary<String, String> helperProps = new Hashtable<>();
			helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
					"testContext" + testName.getMethodName());
			helperProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/helperContext");
			helperProps.put(TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
			helperReg = context.registerService(ServletContextHelper.class, new TestServletContextHelperFactory(),
					helperProps);

			// Pass the filter parameter
			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor(
					"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + "testContext"
							+ testName.getMethodName() + ")",
					null, testName.getMethodName());
			pathAdaptorReg = context.registerService(ContextPathCustomizer.class, pathAdaptor, null);

			FindHook findHook = (bundleContext, name, filter, allServices, references) -> {

				if (bundleContext != context) {
					return;
				}

				// don't show ServletContextHelper
				for (Iterator<ServiceReference<?>> iterator = references.iterator(); iterator.hasNext();) {
					ServiceReference<?> sr = iterator.next();
					if (("testContext" + testName.getMethodName())
							.equals(sr.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME))) {
						iterator.remove();
					}
				}
			};

			findHookReg = context.registerService(FindHook.class, findHook, null);

			// Register a servlet service with a matching context helper
			BaseServlet baseServlet = new BaseServlet("content");
			Dictionary<String, Object> serviceProps = new Hashtable<>();
			serviceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/servlet");
			// Filter property
			serviceProps.put("servlet.init." + TEST_PATH_CUSTOMIZER_NAME, testName.getMethodName());
			servlet = context.registerService(Servlet.class, baseServlet, serviceProps);

			String actual = requestAdvisor.request("helperContext/servlet");
			Assert.assertEquals("content", actual);
		} finally {
			if (helperReg != null) {
				helperReg.unregister();
			}
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
			if (servlet != null) {
				servlet.unregister();
			}
			if (findHookReg != null) {
				findHookReg.unregister();
			}
		}
	}

	@Test
	public void testLegacyServletContextPathCustomizerContextPrefix() throws Exception {
		ServiceRegistration<ContextPathCustomizer> pathAdaptorReg = null;
		ServiceRegistration<FindHook> findHookReg = null;

		try {
			// Pass the context path prefix paramater
			ContextPathCustomizer pathAdaptor = new TestContextPathAdaptor(null, "testPrefix",
					testName.getMethodName()) {

				@Override
				public String getContextPathPrefix(ServiceReference<ServletContextHelper> helper) {
					if (Boolean.TRUE.equals(helper.getProperty("equinox.legacy.context.helper"))) {
						return contextPrefix;
					}
					return null;
				}
			};
			pathAdaptorReg = context.registerService(ContextPathCustomizer.class, pathAdaptor, null);

			FindHook findHook = (bundleContext, name, filter, allServices, references) -> {

				if (bundleContext != context) {
					return;
				}

				// don't show ServletContextHelper
				for (Iterator<ServiceReference<?>> iterator = references.iterator(); iterator.hasNext();) {
					ServiceReference<?> sr = iterator.next();
					if (Boolean.TRUE.equals(sr.getProperty("equinox.legacy.context.helper"))) {
						iterator.remove();
					}
					iterator.remove();
				}
			};

			findHookReg = context.registerService(FindHook.class, findHook, null);
			// Register a servlet service using HttpService
			BaseServlet baseServlet = new BaseServlet("content");
			httpService.registerServlet("/servlet", baseServlet, null, null);

			String actual = requestAdvisor.request("testPrefix/servlet");
			Assert.assertEquals("content", actual);
		} finally {
			if (pathAdaptorReg != null) {
				pathAdaptorReg.unregister();
			}
			if (findHookReg != null) {
				findHookReg.unregister();
			}
			httpService.unregister("/servlet");
		}
	}
}
