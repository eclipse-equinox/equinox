/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.*;
import org.eclipse.equinox.http.servlet.internal.customizer.*;
import org.eclipse.equinox.http.servlet.internal.error.NullContextNamesException;
import org.eclipse.equinox.http.servlet.internal.error.NullServletContextHelperException;
import org.eclipse.equinox.http.servlet.internal.servlet.*;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.*;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.http.runtime.dto.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Raymond Augé
 */
public class HttpServiceRuntimeImpl
	implements
		HttpServiceRuntime,
		ServiceTrackerCustomizer<ServletContextHelper, ContextController> {

	public HttpServiceRuntimeImpl(
		BundleContext bundleContext, ServletContext parentServletContext,
		Map<String, Object> attributes) {

		this.bundleContext = bundleContext;
		this.parentServletContext = parentServletContext;
		this.attributes = Collections.unmodifiableMap(attributes);

		contextServiceTracker =
			new ServiceTracker<ServletContextHelper, ContextController>(
				bundleContext, ServletContextHelper.class, this);

		contextServiceTracker.open();

		listenerServiceTracker =
			new ServiceTracker<EventListener, EventListener>(
				bundleContext, EventListener.class,
				new ListenerTrackerCustomizer(bundleContext, this));

		listenerServiceTracker.open();

		filterServiceTracker = new ServiceTracker<Filter, Filter>(
			bundleContext, getFilteFilter(),
			new FilterTrackerCustomizer(bundleContext, this));

		filterServiceTracker.open();

		servletServiceTracker = new ServiceTracker<Servlet, Servlet>(
			bundleContext, getServletFilter(),
			new ServletTrackerCustomizer(bundleContext, this));

		servletServiceTracker.open();

		resourceServiceTracker = new ServiceTracker<Servlet, Servlet>(
			bundleContext, getResourceFilter(),
			new ResourceTrackerCustomizer(bundleContext, this));

		resourceServiceTracker.open();
	}

	@Override
	public synchronized ContextController addingService(
		ServiceReference<ServletContextHelper> serviceReference) {

		if (!matches(serviceReference)) {
			return null;
		}

		List<String> contextNames = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME));

		if (contextNames.isEmpty()) {
			parentServletContext.log(
				"This context's property " +
					HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +
						" is null. Ignoring!");

			return null;
		}

		for (String contextName : contextNames) {
			if (registeredContextNames.contains(contextName)) {
				parentServletContext.log(
					"ContextName " + contextName + " is already in use. Ignoring!");

				return null;
			}
		}

		String contextPath = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);

		if (contextPath == null) {
			contextPath = Const.BLANK;
		}

		long serviceId = (Long)serviceReference.getProperty(
			Constants.SERVICE_ID);

		ServletContextHelper servletContextHelper = bundleContext.getService(
			serviceReference);

		Map<String, Object> properties = new HashMap<String, Object>();

		for (String key : serviceReference.getPropertyKeys()) {
			properties.put(key, serviceReference.getProperty(key));
		}

		properties.putAll(attributes);

		return addServletContextHelper(
			serviceReference.getBundle(), servletContextHelper, contextNames,
			contextPath, serviceId, properties);
	}

	public ContextController addServletContextHelper(
		Bundle bundle, ServletContextHelper servletContextHelper,
		List<String> contextNames, String contextPath, long serviceId,
		Map<String, Object> properties) {

		if (servletContextHelper == null) {
			throw new NullServletContextHelperException();
		}

		if (controllerMap.containsKey(servletContextHelper)) {
			throw new IllegalArgumentException(
				"ServletContextHelper is already registered.");
		}

		if ((contextNames == null) || (contextNames.size() < 1)) {
			throw new NullContextNamesException();
		}

		if (contextPath == null) {
			contextPath = "";
		}

		ContextController contextController = createContextController(
			bundle, servletContextHelper, contextNames, contextPath, serviceId,
			properties);

		if (contextController != null) {
			for (String contextName : contextNames) {
				registeredContextNames.add(contextName);
			}
		}

		controllerMap.putIfAbsent(contextController, servletContextHelper);

		return contextController;
	}

	@Override
	public RequestInfoDTO calculateRequestInfoDTO(String path) {
		RequestInfoDTO requestInfoDTO = new RequestInfoDTO();

		requestInfoDTO.path = path;

		try {
			doDispatch(null, null, path, requestInfoDTO);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		return requestInfoDTO;
	}

	public void destroy() {
		resourceServiceTracker.close();
		servletServiceTracker.close();
		filterServiceTracker.close();
		listenerServiceTracker.close();
		contextServiceTracker.close();
		controllerMap.clear();
		contextPathMap.clear();
		registeredServlets.clear();

		attributes = null;
		bundleContext = null;
		contextPathMap = null;
		legacyServiceIdGenerator = null;
		parentServletContext = null;
		registeredServlets = null;
		contextServiceTracker = null;
	}

	public boolean doDispatch(
			HttpServletRequest request, HttpServletResponse response,
			String path)
		throws ServletException, IOException {

		return doDispatch(request, response, path, null);
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Map<String, String> getAttributesAsInitParams() {
		Map<String, String> initParameters = new HashMap<String, String>();

		for (Entry<String, Object> entry : getAttributes().entrySet()) {
			initParameters.put(
				entry.getKey(), String.valueOf(entry.getValue()));
		}

		return initParameters;
	}

	public ContextController getOrAddContextController(
		String contextSelector, BundleContext selectorBundleContext) {

		Bundle bundle = selectorBundleContext.getBundle();

		org.osgi.framework.Filter targetFilter = getContextSelectorFilter(
			bundle, contextSelector);

		ContextController contextController = getContextController(
			targetFilter);

		if (contextController != null) {
			return contextController;
		}

		if (contextSelector == null) {
			Hashtable<String, Object> properties = new Hashtable<String, Object>(
				getAttributes());

			properties.put(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
				String.valueOf(bundle.getBundleId()));

			selectorBundleContext.registerService(
				ServletContextHelper.class, new ServletContextHelper(bundle) {},
				properties);
		}

		return contextController = getContextController(targetFilter);
	}

	public ContextController getContextController(
		org.osgi.framework.Filter targetFilter) {

		for (ContextController contextController : controllerMap.keySet()) {
			if (contextController.matches(targetFilter)) {
				return contextController;
			}
		}

		return null;
	}

	public org.osgi.framework.Filter getContextSelectorFilter(
		Bundle bundle, String contextSelector) {

		if (contextSelector == null) {
			contextSelector = Const.OPEN_PAREN +
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +
					Const.EQUAL + String.valueOf(bundle.getBundleId()) +
						Const.CLOSE_PAREN;
		}
		else if (!contextSelector.startsWith(Const.OPEN_PAREN)) {
			contextSelector = Const.OPEN_PAREN +
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +
					Const.EQUAL + contextSelector + Const.CLOSE_PAREN;
		}

		try {
			return FrameworkUtil.createFilter(contextSelector);
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	public org.osgi.framework.Filter getFilteFilter() {
		StringBuilder sb = new StringBuilder();

		sb.append("(&(objectClass="); //$NON-NLS-1$
		sb.append(Filter.class.getName());
		sb.append(")(|("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET);
		sb.append("=*)))"); //$NON-NLS-1$

		try {
			return bundleContext.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	public Set<Servlet> getRegisteredServlets() {
		return registeredServlets;
	}

	public List<String> getHttpServiceEndpoints() {
		return StringPlus.from(
			attributes.get(
				HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT_ATTRIBUTE));
	}

	public org.osgi.framework.Filter getResourceFilter() {
		StringBuilder sb = new StringBuilder();

		sb.append("(&(objectClass="); //$NON-NLS-1$
		sb.append(Servlet.class.getName());
		sb.append(")("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
		sb.append("=*))"); //$NON-NLS-1$

		try {
			return bundleContext.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	@Override
	public RuntimeDTO getRuntimeDTO() {
		RuntimeDTO runtimeDTO = new RuntimeDTO();

		runtimeDTO.attributes = serializeAttributes();

		// TODO

		runtimeDTO.failedErrorPageDTOs = null;
		runtimeDTO.failedFilterDTOs = null;
		runtimeDTO.failedListenerDTOs = null;
		runtimeDTO.failedResourceDTOs = null;
		runtimeDTO.failedServletContextDTOs = null;
		runtimeDTO.failedServletDTOs = null;
		runtimeDTO.servletContextDTOs = getServletContextDTOs();

		return runtimeDTO;
	}

	public org.osgi.framework.Filter getServletFilter() {
		StringBuilder sb = new StringBuilder();

		sb.append("(&(objectClass="); //$NON-NLS-1$
		sb.append(Servlet.class.getName());
		sb.append(")(|("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
		sb.append("=*))(!("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
		sb.append("=*)))"); //$NON-NLS-1$

		try {
			return bundleContext.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	public void log(String message, Throwable t) {
		parentServletContext.log(message, t);
	}

	public boolean matches(ServiceReference<?> serviceReference) {
		String target = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET);

		if (target == null) {
			return true;
		}

		org.osgi.framework.Filter targetFilter;

		try {
			targetFilter = FrameworkUtil.createFilter(target);
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}

		if (targetFilter.matches(attributes)) {
			return true;
		}

		return false;
	}

	@Override
	public synchronized void modifiedService(
		ServiceReference<ServletContextHelper> serviceReference,
		ContextController contextController) {

		// do nothing
	}

	@Override
	public synchronized void removedService(
		ServiceReference<ServletContextHelper> serviceReference,
		ContextController contextController) {

		bundleContext.ungetService(serviceReference);

		removeContextController(contextController);
	}

	public void removeContextController(ContextController contextController) {
		Set<ContextController> contextControllers = getContextControllerPathSet(
			contextController.getContextPath(), false);

		if (contextControllers != null) {
			contextControllers.remove(contextController);
		}

		for (String contextName : contextController.getContextNames()) {
			registeredContextNames.remove(contextName);
		}

		controllerMap.remove(contextController);

		contextController.destroy();
	}

	Set<ContextController> getContextControllerPathSet(
		String contextPath, boolean add) {

		if (contextPath == null) {
			contextPath = "";
		}

		Set<ContextController> set = contextPathMap.get(contextPath);

		if ((set == null) && add) {
			set = new HashSet<ContextController>();

			Set<ContextController> existingSet =
				contextPathMap.putIfAbsent(contextPath, set);

			if (existingSet != null) {
				set = existingSet;
			}
		}

		return set;
	}

	Set<ContextController> getContextControllers(String requestURI) {
		int pos = requestURI.lastIndexOf('/');

		do {
			Set<ContextController> contextControllers = contextPathMap.get(
				requestURI);

			if (contextControllers != null) {
				return contextControllers;
			}

			if (pos > -1) {
				requestURI = requestURI.substring(0, pos);
				pos = requestURI.lastIndexOf('/');

				continue;
			}

			break;
		}
		while (true);

		return null;
	}

	AtomicLong getLegacyServiceIdGenerator() {
		return legacyServiceIdGenerator;
	}

	private ContextController createContextController(
		Bundle bundle, ServletContextHelper servletContextHelper,
		List<String> contextNames, String contextPath, long serviceId,
		Map<String, Object> initParams) {

		ContextController contextController = new ContextController(
			bundle, servletContextHelper, new ProxyContext(parentServletContext),
			this, contextNames, contextPath, serviceId, registeredServlets,
			initParams);

		Set<ContextController> contextControllers = getContextControllerPathSet(
			contextPath, true);

		contextControllers.add(contextController);

		contextPathMap.put(contextPath, contextControllers);

		return contextController;
	}

	private boolean doDispatch(
			HttpServletRequest request, HttpServletResponse response,
			String path, RequestInfoDTO requestInfoDTO)
		throws ServletException, IOException {

		// perfect match
		if (doDispatch(
				request, response, path, null, Match.EXACT, requestInfoDTO)) {

			return true;
		}

		String extensionAlias = findExtensionAlias(path);

		// extension match
		if (doDispatch(
				request, response, path, extensionAlias, Match.EXTENSION,
				requestInfoDTO)) {

			return true;
		}

		// regex match
		if (doDispatch(
				request, response, path, null, Match.REGEX, requestInfoDTO)) {

			return true;
		}

		// handle '/' aliases
		if (doDispatch(
				request, response, path, null, Match.DEFAULT_SERVLET,
				requestInfoDTO)) {

			return true;
		}

		return false;
	}

	private boolean doDispatch(
			HttpServletRequest request, HttpServletResponse response,
			String requestURI, String extension, Match match,
			RequestInfoDTO requestInfoDTO)
		throws ServletException, IOException {

		DispatchTargets dispatchTargets = getDispatchTargets(
			request, requestURI, extension, match, requestInfoDTO);

		if ((dispatchTargets == null) || (requestInfoDTO != null)) {
			return false;
		}

		ContextController contextController =
			dispatchTargets.getContextController();
		DispatcherType dispatcherType = DispatcherType.REQUEST;

		if (request.getAttribute("javax.servlet.include.request_uri") != null) {
			request.setAttribute(
				"javax.servlet.include.request_uri", requestURI);
			request.setAttribute(
				"javax.servlet.include.context_path",
				contextController.getContextPath());
			request.setAttribute(
				"javax.servlet.include.servlet_path",
				dispatchTargets.getServletPath());
			request.setAttribute(
				"javax.servlet.include.path_info",
				dispatchTargets.getPathInfo());

			dispatcherType = DispatcherType.INCLUDE;
		}

		HttpServletRequest wrappedRequest = new HttpServletRequestBuilder(
			request, dispatchTargets).build();
		HttpServletResponseWrapper wrapperResponse =
			new HttpServletResponseWrapperImpl(response);

		ResponseStateHandler responseStateHandler = new ResponseStateHandler(
			wrappedRequest, wrapperResponse, dispatchTargets, dispatcherType);

		responseStateHandler.processRequest();

		return true;
	}

	private String findExtensionAlias(String alias) {
		String lastSegment = alias.substring(alias.lastIndexOf('/') + 1);

		int dot = lastSegment.lastIndexOf('.');

		if (dot == -1) {
			return null;
		}

		return lastSegment.substring(dot + 1);
	}

	private DispatchTargets getDispatchTargets(
		HttpServletRequest request, String requestURI, String extension,
		Match match, RequestInfoDTO requestInfoDTO) {

		Set<ContextController> contextControllers = getContextControllers(
			requestURI);

		if ((contextControllers == null) || contextControllers.isEmpty()) {
			return null;
		}

		String contextPath =
			contextControllers.iterator().next().getContextPath();

		requestURI = requestURI.substring(contextPath.length());

		int pos = requestURI.lastIndexOf('/');

		String servletPath = requestURI;
		String pathInfo = Const.BLANK;

		if (match == Match.DEFAULT_SERVLET) {
			pathInfo = servletPath;
			servletPath = Const.SLASH;
		}

		do {
			for (ContextController contextController : contextControllers) {
				DispatchTargets dispatchTargets =
					contextController.getDispatchTargets(
						request, null, requestURI, servletPath, pathInfo,
						extension, match, requestInfoDTO);

				if (dispatchTargets != null) {
					return dispatchTargets;
				}
			}

			if (extension != null) {
				break;
			}

			if (pos > -1) {
				String newServletPath = requestURI.substring(0, pos);
				pathInfo = requestURI.substring(pos);
				servletPath = newServletPath;
				pos = servletPath.lastIndexOf('/');

				continue;
			}

			break;
		}
		while (true);

		return null;
	}

	private ServletContextDTO[] getServletContextDTOs() {
		List<ServletContextDTO> servletContextDTOs =
			new ArrayList<ServletContextDTO>();

		for (ContextController contextController : controllerMap.keySet()) {
			servletContextDTOs.add(contextController.getServletContextDTO());
		}

		return servletContextDTOs.toArray(
			new ServletContextDTO[servletContextDTOs.size()]);
	}

	private Map<String, String> serializeAttributes() {
		Map<String, String> temp = new HashMap<String, String>();

		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			temp.put(entry.getKey(), String.valueOf(entry.getValue()));
		}

		return temp;
	}

	private Map<String, Object> attributes;
	private BundleContext bundleContext;
	private ConcurrentMap<String, Set<ContextController>> contextPathMap =
		new ConcurrentHashMap<String, Set<ContextController>>();
	private ServiceTracker<ServletContextHelper, ContextController> contextServiceTracker;
	private ConcurrentMap<ContextController, ServletContextHelper> controllerMap =
		new ConcurrentHashMap<ContextController, ServletContextHelper>();
	private ServiceTracker<Filter, Filter> filterServiceTracker;
	private AtomicLong legacyServiceIdGenerator = new AtomicLong(0);
	private ServiceTracker<EventListener, EventListener> listenerServiceTracker;
	private ServletContext parentServletContext;
	private Set<Servlet> registeredServlets = new HashSet<Servlet>();
	private Set<String> registeredContextNames = new ConcurrentSkipListSet<String>();
	private ServiceTracker<Servlet, Servlet> resourceServiceTracker;
	private ServiceTracker<Servlet, Servlet> servletServiceTracker;

}
