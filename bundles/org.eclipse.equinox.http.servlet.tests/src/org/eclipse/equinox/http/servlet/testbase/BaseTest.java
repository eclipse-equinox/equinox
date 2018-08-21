/*******************************************************************************
 * Copyright (c) 2014, 2018 Raymond Augé and others.
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

package org.eclipse.equinox.http.servlet.testbase;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.context.ContextPathCustomizer;
import org.eclipse.equinox.http.servlet.tests.bundle.Activator;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleAdvisor;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleInstaller;
import org.eclipse.equinox.http.servlet.tests.util.ServletRequestAdvisor;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.util.tracker.ServiceTracker;

public class BaseTest {

	@Before
	public void setUp() throws Exception {
		// Quiet logging for tests
		System.setProperty("/.LEVEL", "OFF");
		System.setProperty("org.eclipse.jetty.server.LEVEL", "OFF");
		System.setProperty("org.eclipse.jetty.servlet.LEVEL", "OFF");

		System.setProperty("org.osgi.service.http.port", "0");
		BundleContext bundleContext = getBundleContext();
		installer = new BundleInstaller(TEST_BUNDLES_BINARY_DIRECTORY, bundleContext);
		advisor = new BundleAdvisor(bundleContext);
		startBundles();
		stopJetty();
		runtimeTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, null);
		runtimeTracker.open();
		runtimeTracker.waitForService(100);
		startJetty();
	}

	@After
	public void tearDown() throws Exception {
		for (ServiceRegistration<? extends Object> serviceRegistration : registrations) {
			serviceRegistration.unregister();
		}
		runtimeTracker.close();
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

	protected String doRequest(String action, Map<String, String> params) throws IOException {
		return doRequestGetResponse(action, params).get("responseBody").get(0);
	}

	protected Map<String, List<String>> doRequestGetResponse(String action, Map<String, String> params) throws IOException {
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

	protected BundleContext getBundleContext() {
		return Activator.getBundleContext();
	}

	protected String getContextPath() {
		return getJettyProperty("context.path", "");
	}

	protected HttpService getHttpService() {
		ServiceReference<HttpService> serviceReference = getBundleContext().getServiceReference(HttpService.class);
		return getBundleContext().getService(serviceReference);
	}

	protected String getJettyProperty(String key, String defaultValue) {
		String qualifiedKey = JETTY_PROPERTY_PREFIX + key;
		String value = getProperty(qualifiedKey);
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	protected String getPort() {
		String defaultPort = getProperty(OSGI_HTTP_PORT_PROPERTY);
		if (defaultPort == null) {
			defaultPort = "80";
		}
		return getJettyProperty("port", defaultPort);
	}

	protected String getProperty(String key) {
		BundleContext bundleContext = getBundleContext();
		String value = bundleContext.getProperty(key);
		return value;
	}

	protected List<String> getStringPlus(String key, ServiceReference<?> ref) {
		Object property = ref.getProperty(key);
		if (String.class.isInstance(property)) {
			return Collections.singletonList((String)property);
		}
		else if (String[].class.isInstance(property)) {
			return Arrays.asList((String[])property);
		}
		else if (Collection.class.isInstance(property)) {
			List<String> list = new ArrayList<String>();
			for (@SuppressWarnings("rawtypes")
				 Iterator i = ((Collection)property).iterator(); i.hasNext();) {

				Object o = i.next();
				if (String.class.isInstance(o)) {
					list.add((String)o);
				}
			}
			return list;
		}
		return Collections.emptyList();
	}

	protected Bundle installBundle(String bundle) throws BundleException {
		return installer.installBundle(bundle);
	}

	protected void startBundles() throws BundleException {
		for (String bundle : BUNDLES) {
			advisor.startBundle(bundle);
		}
	}

	protected void startJetty() throws Exception {
		advisor.startBundle(EQUINOX_JETTY_BUNDLE);
		ServiceReference<HttpServiceRuntime> runtimeReference = runtimeTracker.getServiceReference();
		List<String> endpoints = getStringPlus(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT, runtimeReference);
		String port = getPort();
		if (port.equals("0") && !endpoints.isEmpty()) {
			for (String endpoint : endpoints) {
				if (endpoint.startsWith("http://")) {
					port = String.valueOf(new URL(endpoint).getPort());
					break;
				}
			}
			if (port.equals("-1")) {
				port = "80";
			}
		}
		String contextPath = getContextPath();
		requestAdvisor = new ServletRequestAdvisor(port, contextPath);
	}

	protected void stopBundles() throws BundleException {
		for (int i = BUNDLES.length - 1; i >= 0; i--) {
			String bundle = BUNDLES[i];
			advisor.stopBundle(bundle);
		}
	}

	protected void stopJetty() throws BundleException {
		advisor.stopBundle(EQUINOX_JETTY_BUNDLE);
	}

	protected void uninstallBundle(Bundle bundle) throws BundleException {
		installer.uninstallBundle(bundle);
	}

	protected void write(OutputStream outputStream, String string) throws IOException {
		outputStream.write(string.getBytes(StandardCharsets.UTF_8));
	}

	protected static final String PROTOTYPE = "prototype/";
	protected static final String CONFIGURE = "configure";
	protected static final String UNREGISTER = "unregister";
	protected static final String ERROR = "error";
	protected static final String STATUS_PARAM = "servlet.init.status";
	protected static final String TEST_PROTOTYPE_NAME = "test.prototype.name";
	protected static final String TEST_PATH_CUSTOMIZER_NAME = "test.path.customizer.name";
	protected static final String TEST_ERROR_CODE = "test.error.code";

	protected static final String EQUINOX_DS_BUNDLE = "org.eclipse.equinox.ds";
	protected static final String EQUINOX_JETTY_BUNDLE = "org.eclipse.equinox.http.jetty";
	protected static final String JETTY_PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty.";
	protected static final String OSGI_HTTP_PORT_PROPERTY = "org.osgi.service.http.port";
	protected static final String STATUS_OK = "OK";
	protected static final String TEST_BUNDLES_BINARY_DIRECTORY = "/bundles_bin/";
	protected static final String TEST_BUNDLE_1 = "tb1";
	protected static final String TEST_BUNDLE_2 = "tb2";

	protected static final String[] BUNDLES = new String[] {
		EQUINOX_DS_BUNDLE
	};

	protected BundleInstaller installer;
	protected BundleAdvisor advisor;
	protected ServletRequestAdvisor requestAdvisor;
	protected final Collection<ServiceRegistration<? extends Object>> registrations = new ArrayList<ServiceRegistration<? extends Object>>();
	protected ServiceTracker<HttpServiceRuntime, HttpServiceRuntime> runtimeTracker;

	protected static class TestFilter implements Filter {
		AtomicInteger called = new AtomicInteger(0);

		public TestFilter() {}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			// nothing
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			called.incrementAndGet();
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
			// nothing
		}

		public void clear() {
			called.set(0);
		}

		public boolean getCalled() {
			return called.get() >= 1;
		}

		public int getCount() {
			return called.get();
		}
	}

	protected static class TestServletContextHelperFactory implements ServiceFactory<ServletContextHelper> {
		static class TestServletContextHelper extends ServletContextHelper {
			public TestServletContextHelper(Bundle bundle) {
				super(bundle);
			}};

		public TestServletContextHelperFactory() {}

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

	protected static class TestContextPathAdaptor extends ContextPathCustomizer {
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

	protected static class ErrorServlet extends HttpServlet{
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