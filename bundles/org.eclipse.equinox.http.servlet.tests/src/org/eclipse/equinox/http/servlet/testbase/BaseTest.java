/*******************************************************************************
 * Copyright (c) 2014, 2019 Raymond Augé and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.equinox.http.servlet.context.ContextPathCustomizer;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleAdvisor;
import org.eclipse.equinox.http.servlet.tests.bundle.BundleInstaller;
import org.eclipse.equinox.http.servlet.tests.util.ServletRequestAdvisor;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedErrorPageDTO;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.runtime.dto.FailedServletContextDTO;
import org.osgi.service.http.runtime.dto.FailedServletDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class BaseTest {

	public static final String	DEFAULT	= HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;

	@Before
	public void setUp() throws Exception {
		// Quiet logging for tests
		System.setProperty("log.LEVEL", "OFF");
		System.setProperty("org.eclipse.jetty.server.LEVEL", "OFF");
		System.setProperty("org.eclipse.jetty.servlet.LEVEL", "OFF");
		System.setProperty("org.osgi.service.http.port", "0");
		System.setProperty("org.eclipse.equinox.http.jetty.context.sessioninactiveinterval", "1");
		System.setProperty("org.eclipse.equinox.http.jetty.housekeeper.interval", "10");
		BundleContext bundleContext = getBundleContext();
		installer = new BundleInstaller(TEST_BUNDLES_BINARY_DIRECTORY, bundleContext);
		advisor = new BundleAdvisor(bundleContext);
		stopJetty();
		startBundles();
		runtimeTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, new ServiceTrackerCustomizer<HttpServiceRuntime,ServiceReference<HttpServiceRuntime>>() {

			@Override
			public ServiceReference<HttpServiceRuntime> addingService(
					ServiceReference<HttpServiceRuntime> reference) {
				final Object obj = reference
						.getProperty(Constants.SERVICE_CHANGECOUNT);
				if (obj != null) {
					httpRuntimeChangeCount.set(Long.valueOf(obj.toString()));
				}
				return reference;
			}

			@Override
			public void modifiedService(
					ServiceReference<HttpServiceRuntime> reference,
					ServiceReference<HttpServiceRuntime> service) {
				addingService(reference);
			}

			@Override
			public void removedService(
					ServiceReference<HttpServiceRuntime> reference,
					ServiceReference<HttpServiceRuntime> service) {
				httpRuntimeChangeCount.set(-1);
			}

		});
		runtimeTracker.open();
		runtimeTracker.waitForService(100);
		startJetty();
	}

	@After
	public void tearDown() throws Exception {
		runtimeTracker.close();
		stopBundles();
		requestAdvisor = null;
		advisor = null;
		try {
			installer.shutdown();
		} finally {
			installer = null;
		}

		for (ServiceRegistration<? extends Object> serviceRegistration : registrations) {
			try {
				serviceRegistration.unregister();
			}
			catch (Exception e) {
				// ignore
			}
		}
		registrations.clear();
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
		return FrameworkUtil.getBundle(BaseTest.class).getBundleContext();
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

	protected void setJettyProperty(String key, String value) {
		String qualifiedKey = JETTY_PROPERTY_PREFIX + key;
		System.setProperty(qualifiedKey, value);
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
			List<String> list = new ArrayList<>();
			for (Object o : ((Collection) property)) {

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

	@SuppressWarnings("unused")
	protected void startBundles() throws BundleException {
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

	protected void startJettyWithSSL(String port, String ksPath, String ksPassword, String keyPassword) throws Exception {
		if(port == null) {
			throw new IllegalArgumentException("Port cannot be null");
		}
		if (ksPath == null) {
			throw new IllegalArgumentException("Keystore path  cannot be null");
		}
		setJettyProperty(JettyConstants.HTTP_ENABLED, "false");
		setJettyProperty(JettyConstants.HTTPS_ENABLED, "true");

		setJettyProperty(JettyConstants.HTTPS_PORT, port);

		setJettyProperty(JettyConstants.SSL_KEYSTORE, ksPath);

		if(ksPassword != null) {
			setJettyProperty(JettyConstants.SSL_PASSWORD, ksPassword);
		}
		if(keyPassword != null) {
			setJettyProperty(JettyConstants.SSL_KEYPASSWORD, keyPassword);
		}

		advisor.startBundle(EQUINOX_JETTY_BUNDLE);
		String contextPath = getContextPath();
		requestAdvisor = new ServletRequestAdvisor(port, contextPath, ksPath, ksPassword);
	}

	@SuppressWarnings("unused")
	protected void stopBundles() throws BundleException {
	}

	protected void stopJetty() throws BundleException {
		advisor.stopBundle(EQUINOX_JETTY_BUNDLE);
	}

	protected void stopJettyWithSSL() throws BundleException {
		advisor.stopBundle(EQUINOX_JETTY_BUNDLE);
		setJettyProperty(JettyConstants.HTTP_ENABLED, "true");
		setJettyProperty(JettyConstants.HTTPS_ENABLED, "false");
	}

	protected void uninstallBundle(Bundle bundle) throws BundleException {
		installer.uninstallBundle(bundle);
	}

	protected void write(OutputStream outputStream, String string) throws IOException {
		outputStream.write(string.getBytes(StandardCharsets.UTF_8));
	}

	protected FailedServletDTO getFailedServletDTOByName(String name) {
		for (FailedServletDTO failedServletDTO : getFailedServletDTOs()) {
			if (name.equals(failedServletDTO.name)) {
				return failedServletDTO;
			}
		}

		return null;
	}

	protected FailedServletDTO[] getFailedServletDTOs() {
		HttpServiceRuntime httpServiceRuntime = getHttpServiceRuntime();

		return httpServiceRuntime.getRuntimeDTO().failedServletDTOs;
	}

	protected HttpServiceRuntime getHttpServiceRuntime() {
		ServiceReference<HttpServiceRuntime> serviceReference =
				getBundleContext().getServiceReference(HttpServiceRuntime.class);

		assertNotNull(serviceReference);

		return getBundleContext().getService(serviceReference);
	}

	protected ListenerDTO getListenerDTOByServiceId(String contextName, long serviceId) {
		ServletContextDTO servletContextDTO = getServletContextDTOByName(contextName);

		if (servletContextDTO == null) {
			return null;
		}

		for (ListenerDTO listenerDTO : servletContextDTO.listenerDTOs) {
			if (serviceId == listenerDTO.serviceId) {
				return listenerDTO;
			}
		}

		return null;
	}

	protected long getServiceId(ServiceRegistration<?> sr) {
		return (Long) sr.getReference().getProperty(Constants.SERVICE_ID);
	}

	protected RequestInfoDTO calculateRequestInfoDTO(String string) {
		HttpServiceRuntime httpServiceRuntime = getHttpServiceRuntime();

		return httpServiceRuntime.calculateRequestInfoDTO(string);
	}

	protected ServletDTO getServletDTOByName(String context, String name) {
		ServletContextDTO servletContextDTO = getServletContextDTOByName(context);

		if (servletContextDTO == null) {
			return null;
		}

		for (ServletDTO servletDTO : servletContextDTO.servletDTOs) {
			if (name.equals(servletDTO.name)) {
				return servletDTO;
			}
		}

		return null;
	}

	protected ServletContextDTO getServletContextDTOByName(String name) {
		for (ServletContextDTO servletContextDTO : getServletContextDTOs()) {
			if (name.equals(servletContextDTO.name)) {
				return servletContextDTO;
			}
		}

		return null;
	}

	protected ServletContextDTO[] getServletContextDTOs() {
		return getHttpServiceRuntime().getRuntimeDTO().servletContextDTOs;
	}

	protected ErrorPageDTO getErrorPageDTOByName(String context, String name) {
		ServletContextDTO servletContextDTO = getServletContextDTOByName(context);

		if (servletContextDTO == null) {
			return null;
		}

		for (ErrorPageDTO errorPageDTO : servletContextDTO.errorPageDTOs) {
			if (name.equals(errorPageDTO.name)) {
				return errorPageDTO;
			}
		}

		return null;
	}

	protected FailedErrorPageDTO getFailedErrorPageDTOByName(String name) {
		for (FailedErrorPageDTO failedErrorPageDTO : getFailedErrorPageDTOs()) {
			if (name.equals(failedErrorPageDTO.name)) {
				return failedErrorPageDTO;
			}
		}

		return null;
	}

	protected FailedErrorPageDTO[] getFailedErrorPageDTOs() {
		HttpServiceRuntime httpServiceRuntime = getHttpServiceRuntime();

		return httpServiceRuntime.getRuntimeDTO().failedErrorPageDTOs;
	}

	protected FilterDTO getFilterDTOByName(String contextName, String name) {
		ServletContextDTO servletContextDTO = getServletContextDTOByName(contextName);

		if (servletContextDTO == null) {
			return null;
		}

		for (FilterDTO filterDTO : servletContextDTO.filterDTOs) {
			if (name.equals(filterDTO.name)) {
				return filterDTO;
			}
		}

		return null;
	}

	protected FailedFilterDTO getFailedFilterDTOByName(String name) {
		for (FailedFilterDTO failedFilterDTO : getFailedFilterDTOs()) {
			if (name.equals(failedFilterDTO.name)) {
				return failedFilterDTO;
			}
		}

		return null;
	}

	protected FailedFilterDTO[] getFailedFilterDTOs() {
		return getHttpServiceRuntime().getRuntimeDTO().failedFilterDTOs;
	}

	protected FailedListenerDTO getFailedListenerDTOByServiceId(long serviceId) {
		for (FailedListenerDTO failedListenerDTO : getFailedListenerDTOs()) {
			if (serviceId == failedListenerDTO.serviceId) {
				return failedListenerDTO;
			}
		}

		return null;
	}

	protected FailedListenerDTO[] getFailedListenerDTOs() {
		return getHttpServiceRuntime().getRuntimeDTO().failedListenerDTOs;
	}

	protected FailedServletContextDTO getFailedServletContextDTOByName(String name) {
		for (FailedServletContextDTO failedServletContextDTO : getFailedServletContextDTOs()) {
			if (name.equals(failedServletContextDTO.name)) {
				return failedServletContextDTO;
			}
		}

		return null;
	}

	protected FailedServletContextDTO[] getFailedServletContextDTOs() {
		return getHttpServiceRuntime().getRuntimeDTO().failedServletContextDTOs;
	}

	protected ResourceDTO getResourceDTOByServiceId(String contextName, long serviceId) {
		ServletContextDTO servletContextDTO = getServletContextDTOByName(contextName);

		if (servletContextDTO == null) {
			return null;
		}

		for (ResourceDTO resourceDTO : servletContextDTO.resourceDTOs) {
			if (serviceId == resourceDTO.serviceId) {
				return resourceDTO;
			}
		}

		return null;
	}

	protected FailedResourceDTO getFailedResourceDTOByServiceId(long serviceId) {
		for (FailedResourceDTO failedResourceDTO : getFailedResourceDTOs()) {
			if (serviceId == failedResourceDTO.serviceId) {
				return failedResourceDTO;
			}
		}

		return null;
	}

	protected FailedResourceDTO[] getFailedResourceDTOs() {
		return getHttpServiceRuntime().getRuntimeDTO().failedResourceDTOs;
	}

	protected long waitForRegistration(final long previousCount) {
		return waitForRegistration(previousCount, 100);
	}

	protected long waitForRegistration(final long previousCount,
			int maxAttempts) {
		while (this.httpRuntimeChangeCount.longValue() == previousCount) {
			if (maxAttempts <= 0) {
				throw new IllegalStateException("Max attempts exceeded");
			}
			try {
				Thread.sleep(20L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			maxAttempts--;
		}
		return this.httpRuntimeChangeCount.longValue();
	}

	protected long getHttpRuntimeChangeCount() {
		return httpRuntimeChangeCount.longValue();
	}

	protected void registerDummyServletInHttpService()
			throws ServletException, NamespaceException {
		final String path = "/tesths";
		final HttpService service = this.getHttpService();
		service.registerServlet(path, new HttpServlet() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws IOException {
				resp.getWriter().print("helloworld");
				resp.flushBuffer();
			}
		}, null, null);
	}

	protected void unregisterDummyServletFromHttpService() {
		this.getHttpService().unregister("/tesths");
	}

	protected ServletContextDTO getServletContextDTOForDummyServlet() {
		for (final ServletContextDTO dto : this.getHttpServiceRuntime()
				.getRuntimeDTO().servletContextDTOs) {
			for (final ServletDTO sd : dto.servletDTOs) {
				if (sd.patterns.length > 0
						&& "/tesths".equals(sd.patterns[0])) {
					return dto;
				}
			}
		}
		fail("Servlet context for http service not found");
		return null;
	}

	protected FailedErrorPageDTO getFailedErrorPageDTOByException(
			String exception) {
		for (FailedErrorPageDTO failedErrorPageDTO : getFailedErrorPageDTOs()) {
			for (String ex : failedErrorPageDTO.exceptions) {
				if (exception.equals(ex)) {
					return failedErrorPageDTO;
				}
			}
		}

		return null;
	}

	protected ErrorPageDTO getErrorPageDTOByException(String context,
			String exception) {
		ServletContextDTO servletContextDTO = getServletContextDTOByName(
				context);

		if (servletContextDTO == null) {
			return null;
		}

		for (ErrorPageDTO errorPageDTO : servletContextDTO.errorPageDTOs) {
			for (String ex : errorPageDTO.exceptions) {
				if (exception.equals(ex)) {
					return errorPageDTO;
				}
			}
		}

		return null;
	}

	final AtomicLong httpRuntimeChangeCount	= new AtomicLong(-1);

	protected static final String PROTOTYPE = "prototype/";
	protected static final String CONFIGURE = "configure";
	protected static final String UNREGISTER = "unregister";
	protected static final String ERROR = "error";
	protected static final String STATUS_PARAM = "servlet.init.status";
	protected static final String TEST_PROTOTYPE_NAME = "test.prototype.name";
	protected static final String TEST_PATH_CUSTOMIZER_NAME = "test.path.customizer.name";
	protected static final String TEST_ERROR_CODE = "test.error.code";

	protected static final String EQUINOX_DS_BUNDLE = "org.apache.felix.scr";
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
	protected final Collection<ServiceRegistration<? extends Object>> registrations = new ArrayList<>();
	protected ServiceTracker<HttpServiceRuntime, ServiceReference<HttpServiceRuntime>> runtimeTracker;

	protected static class TestFilter implements Filter {
		AtomicInteger called = new AtomicInteger(0);

		public TestFilter() {}

		@Override
		public void init(FilterConfig filterConfig) {
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
			}}

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

	protected class TestContextPathAdaptor extends ContextPathCustomizer {
		protected final String defaultFilter;
		protected final String contextPrefix;
		protected final String testName;

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
			throws IOException {

			if (response.isCommitted()) {
				System.out.println("Problem?");

				return;
			}

			PrintWriter writer = response.getWriter();

			String requestURI = (String)request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
			Integer status = (Integer)request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

			writer.print(errorCode + " : " + status + " : ERROR : " + requestURI);
		}

	}

}
