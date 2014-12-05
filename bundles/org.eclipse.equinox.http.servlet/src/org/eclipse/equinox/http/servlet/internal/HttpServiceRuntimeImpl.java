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
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.*;
import org.eclipse.equinox.http.servlet.internal.error.*;
import org.eclipse.equinox.http.servlet.internal.servlet.*;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
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
		ServiceTrackerCustomizer<ServletContextHelper, AtomicReference<ContextController>> {

	public HttpServiceRuntimeImpl(
		BundleContext trackingContext, BundleContext consumingContext,
		ServletContext parentServletContext, Map<String, Object> attributes) {

		this.trackingContext = trackingContext;
		this.consumingContext = consumingContext;
		this.parentServletContext = parentServletContext;
		this.attributes = Collections.unmodifiableMap(attributes);
		this.targetFilter = "(" + Activator.UNIQUE_SERVICE_ID + "=" + attributes.get(Activator.UNIQUE_SERVICE_ID) + ")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		contextServiceTracker =
			new ServiceTracker<ServletContextHelper, AtomicReference<ContextController>>(
				trackingContext, ServletContextHelper.class, this);

		contextServiceTracker.open();

		Hashtable<String, Object> defaultContextProps = new Hashtable<String, Object>();
		defaultContextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
		defaultContextProps.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		defaultContextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, Const.SLASH);
		defaultContextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, this.targetFilter);
		defaultContextReg = consumingContext.registerService(ServletContextHelper.class, new DefaultServletContextHelperFactory(), defaultContextProps);
	}

	@Override
	public synchronized AtomicReference<ContextController> addingService(
		ServiceReference<ServletContextHelper> serviceReference) {

		AtomicReference<ContextController> result = new AtomicReference<ContextController>();
		if (!matches(serviceReference)) {
			return result;
		}

		String contextName = (String) serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);

		if (contextName == null) {
			parentServletContext.log(
				"This context's property " +
					HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +
						" is null. Ignoring!");

			return result;
		}

		if (registeredContextNames.contains(contextName)) {
			parentServletContext.log(
				"ContextName " + contextName + " is already in use. Ignoring!");
				return result;
		}

		String contextPath = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);

		if (contextPath == null || contextPath.equals(Const.SLASH)) {
			contextPath = Const.BLANK;
		}

		long serviceId = (Long)serviceReference.getProperty(
			Constants.SERVICE_ID);

		Map<String, Object> properties = new HashMap<String, Object>();

		for (String key : serviceReference.getPropertyKeys()) {
			properties.put(key, serviceReference.getProperty(key));
		}

		properties.putAll(attributes);

		result.set(addServletContextHelper(
			serviceReference, contextName,
			contextPath, serviceId, properties));
		return result;
	}

	public ContextController addServletContextHelper(
		ServiceReference<ServletContextHelper> servletContextHelperRef,
		String contextName, String contextPath, long serviceId,
		Map<String, Object> properties) {

		if (servletContextHelperRef == null) {
			throw new NullServletContextHelperException();
		}

		if (contextName == null) {
			throw new NullContextNamesException();
		}

		if (contextPath == null) {
			contextPath = Const.BLANK;
		}

		ContextController contextController = createContextController(
			servletContextHelperRef, contextName, contextPath, serviceId,
			properties);

		if (contextController != null) {
			registeredContextNames.add(contextName);
		}

		controllerMap.putIfAbsent(contextController, servletContextHelperRef);

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
		defaultContextReg.unregister();
		contextServiceTracker.close();
		controllerMap.clear();
		contextPathMap.clear();
		registeredObjects.clear();

		attributes = null;
		trackingContext = null;
		consumingContext = null;
		contextPathMap = null;
		legacyServiceIdGenerator = null;
		parentServletContext = null;
		registeredObjects = null;
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

	public ContextController getContextController(
		org.osgi.framework.Filter targetFilter) {

		for (ContextController contextController : controllerMap.keySet()) {
			if (contextController.matches(targetFilter)) {
				return contextController;
			}
		}

		return null;
	}

	public org.osgi.framework.Filter getFilterFilter() {
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
			return trackingContext.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	public Set<Object> getRegisteredObjects() {
		return registeredObjects;
	}

	public List<String> getHttpServiceEndpoints() {
		return StringPlus.from(
			attributes.get(
				HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT_ATTRIBUTE));
	}

	public org.osgi.framework.Filter getResourceFilter() {
		StringBuilder sb = new StringBuilder();

		sb.append("(&("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN);
		sb.append("=*))"); //$NON-NLS-1$

		try {
			return trackingContext.createFilter(sb.toString());
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
			return trackingContext.createFilter(sb.toString());
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
		AtomicReference<ContextController> contextController) {

		removedService(serviceReference, contextController);
		AtomicReference<ContextController> added = addingService(serviceReference);
		contextController.set(added.get());
	}

	@Override
	public synchronized void removedService(
		ServiceReference<ServletContextHelper> serviceReference,
		AtomicReference<ContextController> contextControllerRef) {

		ContextController contextController = contextControllerRef.get();
		if (contextController != null) {
			removeContextController(contextController);
		}
		trackingContext.ungetService(serviceReference);
	}

	public void removeContextController(ContextController contextController) {
		Set<ContextController> contextControllers = getContextControllerPathSet(
			contextController.getContextPath(), false);

		if (contextControllers != null) {
			contextControllers.remove(contextController);
		}

		registeredContextNames.remove(contextController.getContextName());

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
		ServiceReference<ServletContextHelper> servletContextHelperRef,
		String contextName, String contextPath, long serviceId,
		Map<String, Object> initParams) {

		ContextController contextController = new ContextController(
			trackingContext, consumingContext, servletContextHelperRef,
			new ProxyContext(parentServletContext),
			this, contextName, contextPath, serviceId, initParams);

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

		if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
			request.setAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH, contextController.getContextPath());
			request.setAttribute(RequestDispatcher.INCLUDE_PATH_INFO, dispatchTargets.getPathInfo());
			request.setAttribute(RequestDispatcher.INCLUDE_QUERY_STRING, request.getQueryString());
			request.setAttribute(RequestDispatcher.INCLUDE_REQUEST_URI, requestURI);
			request.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH, dispatchTargets.getServletPath());

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
		String pathInfo = null;

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

			if ((match == Match.EXACT) || (extension != null)) {
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


	public void registerHttpServiceFilter(
		Bundle bundle, String alias, Filter filter, Dictionary<String, String> initparams, HttpContext httpContext) throws ServletException {

		if (alias == null) {
			throw new IllegalArgumentException("Alias cannot be null");
		}
		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null");
		}

		ContextController.checkPattern(alias);

		synchronized (legacyMappings) {
			if (getRegisteredObjects().contains(filter)) {
				throw new RegisteredFilterException(filter);
			}
			HttpServiceObjectRegistration existing = legacyMappings.get(filter);
			if (existing != null) {
				throw new RegisteredFilterException(filter);
			}
			String filterName = filter.getClass().getName();
			if ((initparams != null) && (initparams.get(Const.FILTER_NAME) != null)) {
				filterName = initparams.get(Const.FILTER_NAME);
			}
			HttpContextHelperFactory factory = getOrRegisterHttpContextHelperFactory(httpContext);
			HttpServiceObjectRegistration objectRegistration = null;
			try {
				Dictionary<String, Object> props = new Hashtable<String, Object>();
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, targetFilter);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, alias);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, filterName);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + Const.EQUINOX_LEGACY_CONTEXT_HELPER + "=true)"); //$NON-NLS-1$ //$NON-NLS-2$
				props.put(Const.EQUINOX_LEGACY_CONTEXT_SELECT, factory.getFilter());
				props.put(Const.EQUINOX_LEGACY_MATCHING_PROP, Boolean.TRUE);
				props.put(Constants.SERVICE_RANKING, findFilterPriority(initparams));
				fillInitParams(props, initparams, Const.FILTER_INIT_PREFIX);
				ServiceRegistration<Filter> registration = bundle.getBundleContext().registerService(Filter.class, new LegacyFilterFactory(filter), props);
				objectRegistration = new HttpServiceObjectRegistration(filter, registration, factory, bundle);
				Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
				if (objectRegistrations == null) {
					objectRegistrations = new HashSet<HttpServiceObjectRegistration>();
					bundleRegistrations.put(bundle, objectRegistrations);
				}
				objectRegistrations.add(objectRegistration);
				legacyMappings.put(objectRegistration.serviceKey, objectRegistration);
			} finally {
				if (objectRegistration == null || !legacyMappings.containsKey(objectRegistration.serviceKey)) {
					// something bad happened above (likely going to throw a runtime exception)
					// need to clean up the factory reference
					decrementFactoryUseCount(factory);
				}
			}
		}
	}

	private void fillInitParams(
		Dictionary<String, Object> props,
		Dictionary<String, String> initparams, String prefix) {
		if (initparams != null) {
			for (Enumeration<String> eKeys = initparams.keys(); eKeys.hasMoreElements();) {
				String key = eKeys.nextElement();
				String value = initparams.get(key);
				if (value != null) {
					props.put(prefix + key, value);
				}
			}
		}
	}

	private static int findFilterPriority(Dictionary<String, String> initparams) {
		if (initparams == null) {
			return 0;
		}

		String filterPriority = initparams.get(Const.FILTER_PRIORITY);

		if (filterPriority == null) {
			return 0;
		}

		try {
			int result = Integer.parseInt(filterPriority);
			if (result >= -1000 && result <= 1000) {
				return result;
			}
		}
		catch (NumberFormatException e) {
			// fall through
		}

		throw new IllegalArgumentException(
			"filter-priority must be an integer between -1000 and 1000 but " +
				"was: " + filterPriority);
	}

	public void registerHttpServiceResources(
		Bundle bundle, String alias, String name, HttpContext httpContext) throws NamespaceException {
		if (alias == null) {
			throw new IllegalArgumentException("Alias cannot be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null");
		}
		String pattern = alias;
		if (pattern.startsWith("/*.")) { //$NON-NLS-1$
			pattern = pattern.substring(1);
		}
		else if (!pattern.contains("*.") && //$NON-NLS-1$
				 !pattern.endsWith(Const.SLASH_STAR) &&
				 !pattern.endsWith(Const.SLASH)) {
			pattern += Const.SLASH_STAR;
		}

		ContextController.checkPattern(alias);

		synchronized (legacyMappings) {
			HttpServiceObjectRegistration existing = legacyMappings.get(alias);
			if (existing != null) {
				throw new PatternInUseException(alias);
			}

			HttpServiceObjectRegistration objectRegistration = null;
			HttpContextHelperFactory factory = getOrRegisterHttpContextHelperFactory(httpContext);
			try {
				Dictionary<String, Object> props = new Hashtable<String, Object>();
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, targetFilter);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, pattern);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, name);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, factory.getFilter());
				props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
				props.put(Const.EQUINOX_LEGACY_MATCHING_PROP, Boolean.TRUE);
				ServiceRegistration<?> registration = bundle.getBundleContext().registerService(String.class, "resource", props); //$NON-NLS-1$
				objectRegistration = new HttpServiceObjectRegistration(alias, registration, factory, bundle);
				Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
				if (objectRegistrations == null) {
					objectRegistrations = new HashSet<HttpServiceObjectRegistration>();
					bundleRegistrations.put(bundle, objectRegistrations);
				}
				objectRegistrations.add(objectRegistration);
				legacyMappings.put(objectRegistration.serviceKey, objectRegistration);
			} finally {
				if (objectRegistration == null || !legacyMappings.containsKey(objectRegistration.serviceKey)) {
					// something bad happened above (likely going to throw a runtime exception)
					// need to clean up the factory reference
					decrementFactoryUseCount(factory);
				}
			}
		}
	}

	public void registerHttpServiceServlet(
		Bundle bundle, String alias, Servlet servlet, Dictionary<String, String> initparams, HttpContext httpContext) throws NamespaceException, ServletException{
		if (alias == null) {
			throw new IllegalArgumentException("Alias cannot be null");
		}
		if (servlet == null) {
			throw new IllegalArgumentException("Servlet cannot be null");
		}

		ContextController.checkPattern(alias);

		synchronized (legacyMappings) {
			if (getRegisteredObjects().contains(servlet)) {
				throw new ServletAlreadyRegisteredException(servlet);
			}
			HttpServiceObjectRegistration existing = legacyMappings.get(alias);
			if (existing != null) {
				throw new PatternInUseException(alias);
			}
			String servletName = servlet.getClass().getName();
			if ((initparams != null) && (initparams.get(Const.SERVLET_NAME) != null)) {
				servletName = initparams.get(Const.SERVLET_NAME);
			}
			HttpServiceObjectRegistration objectRegistration = null;
			HttpContextHelperFactory factory = getOrRegisterHttpContextHelperFactory(httpContext);
			try {
				Dictionary<String, Object> props = new Hashtable<String, Object>();
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, targetFilter);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, alias);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, servletName);
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, factory.getFilter());
				props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
				props.put(Const.EQUINOX_LEGACY_MATCHING_PROP, Boolean.TRUE);
				fillInitParams(props, initparams, Const.SERVLET_INIT_PREFIX);
				ServiceRegistration<Servlet> registration = bundle.getBundleContext().registerService(Servlet.class, servlet, props);
				objectRegistration = new HttpServiceObjectRegistration(alias, registration, factory, bundle);
				Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
				if (objectRegistrations == null) {
					objectRegistrations = new HashSet<HttpServiceObjectRegistration>();
					bundleRegistrations.put(bundle, objectRegistrations);
				}
				objectRegistrations.add(objectRegistration);
				legacyMappings.put(objectRegistration.serviceKey, objectRegistration);
			} finally {
				if (objectRegistration == null || !legacyMappings.containsKey(objectRegistration.serviceKey)) {
					// something bad happened above (likely going to throw a runtime exception)
					// need to clean up the factory reference
					decrementFactoryUseCount(factory);
				}
			}
		}
	}

	public void unregisterHttpServiceAlias(Bundle bundle, String alias) {
		synchronized (legacyMappings) {
			HttpServiceObjectRegistration objectRegistration = legacyMappings.get(alias);
			if (objectRegistration == null) {
				throw new IllegalArgumentException("No registration found for alias: " + alias); //$NON-NLS-1$
			}
			Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
			if (objectRegistrations == null || !objectRegistrations.remove(objectRegistration))
			{
				throw new IllegalArgumentException("The bundle did not register the alias: " + alias); //$NON-NLS-1$
			}
			try {
				objectRegistration.registration.unregister();
			} catch (IllegalStateException e) {
				// ignore; already unregistered
			}
			decrementFactoryUseCount(objectRegistration.factory);
			legacyMappings.remove(alias);
			
		}
	}

	public void unregisterHttpServiceFilter(Bundle bundle, Filter filter) {
		synchronized (legacyMappings) {
			HttpServiceObjectRegistration objectRegistration = legacyMappings.get(filter);
			if (objectRegistration == null) {
				throw new IllegalArgumentException("No registration found for filter: " + filter); //$NON-NLS-1$
			}
			Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
			if (objectRegistrations == null || !objectRegistrations.remove(objectRegistration))
			{
				throw new IllegalArgumentException("The bundle did not register the filter: " + filter); //$NON-NLS-1$
			}
			try {
				objectRegistration.registration.unregister();
			} catch (IllegalStateException e) {
				// ignore; already unregistered
			}
			decrementFactoryUseCount(objectRegistration.factory);
			legacyMappings.remove(filter);
		}
	}

	public void unregisterHttpServiceObjects(Bundle bundle) {
		synchronized (legacyMappings) {
			Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.remove(bundle);
			if (objectRegistrations != null) {
				for (HttpServiceObjectRegistration objectRegistration : objectRegistrations) {
					try {
						objectRegistration.registration.unregister();
					} catch (IllegalStateException e) {
						// ignore; already unregistered
					}
					decrementFactoryUseCount(objectRegistration.factory);
					legacyMappings.remove(objectRegistration.serviceKey);
				}
			}
		}
	}

	private HttpContextHelperFactory getOrRegisterHttpContextHelperFactory(HttpContext httpContext) {
		if (httpContext == null) {
			throw new NullPointerException("A null HttpContext is not allowed."); //$NON-NLS-1$
		}
		synchronized (httpContextHelperFactories) {
			HttpContextHelperFactory factory = httpContextHelperFactories.get(httpContext);
			if (factory == null) {
				factory = new HttpContextHelperFactory(httpContext);
				Dictionary<String, Object> props = new Hashtable<String, Object>();
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, httpContext.toString());
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/"); //$NON-NLS-1$
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_TARGET, targetFilter);
				props.put(Const.EQUINOX_LEGACY_CONTEXT_HELPER, Boolean.TRUE);
				factory.setRegistration(consumingContext.registerService(ServletContextHelper.class, factory, props));
				httpContextHelperFactories.put(httpContext, factory);
			}
			factory.incrementUseCount();
			return factory;
		}
	}

	private void decrementFactoryUseCount(HttpContextHelperFactory factory) {
		synchronized (httpContextHelperFactories) {
			if (factory.decrementUseCount() == 0) {
				httpContextHelperFactories.remove(factory.getHttpContext());
			}
		}
	}

	private Map<String, Object> attributes;
	private final String targetFilter;
	private final ServiceRegistration<ServletContextHelper> defaultContextReg;
	private ServletContext parentServletContext;

	private BundleContext trackingContext;
	private BundleContext consumingContext;

	// BEGIN of old HttpService support
	private Map<HttpContext, HttpContextHelperFactory> httpContextHelperFactories =
		Collections.synchronizedMap(new HashMap<HttpContext, HttpContextHelperFactory>());
	private Map<Object, HttpServiceObjectRegistration> legacyMappings = 
		Collections.synchronizedMap(new HashMap<Object, HttpServiceObjectRegistration>());
	private Map<Bundle, Set<HttpServiceObjectRegistration>> bundleRegistrations =
		new HashMap<Bundle, Set<HttpServiceObjectRegistration>>();
	// END of old HttpService support

	private ConcurrentMap<String, Set<ContextController>> contextPathMap =
		new ConcurrentHashMap<String, Set<ContextController>>();
	private ConcurrentMap<ContextController, ServiceReference<ServletContextHelper>> controllerMap =
		new ConcurrentHashMap<ContextController, ServiceReference<ServletContextHelper>>();

	private AtomicLong legacyServiceIdGenerator = new AtomicLong(0);

	private Set<Object> registeredObjects = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
	private Set<String> registeredContextNames = new ConcurrentSkipListSet<String>();

	private ServiceTracker<ServletContextHelper, AtomicReference<ContextController>> contextServiceTracker;

	static class DefaultServletContextHelperFactory implements ServiceFactory<ServletContextHelper> {
		@Override
		public ServletContextHelper getService(
			Bundle bundle,
			ServiceRegistration<ServletContextHelper> registration) {
			return new DefaultServletContextHelper(bundle);
		}

		@Override
		public void ungetService(
			Bundle bundle,
			ServiceRegistration<ServletContextHelper> registration,
			ServletContextHelper service) {
			// do nothing
		}
	}

	static class DefaultServletContextHelper extends ServletContextHelper {
		public DefaultServletContextHelper(Bundle b) {
			super(b);
		}
	}

	static class LegacyFilterFactory implements PrototypeServiceFactory<Filter> {
		final Filter filter;
		
		public LegacyFilterFactory(Filter filter) {
			this.filter = filter;
		}

		@Override
		public Filter getService(
			Bundle bundle, ServiceRegistration<Filter> registration) {
			return new Filter() {
				
				@Override
				public void init(FilterConfig filterConfig) throws ServletException {
					filter.init(filterConfig);
				}
				
				@Override
				public void doFilter(
					ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
					filter.doFilter(request, response, chain);
				}
				
				@Override
				public void destroy() {
					filter.destroy();
				}
			};
		}

		@Override
		public void ungetService(
			Bundle bundle, ServiceRegistration<Filter> registration,
			Filter service) {
		}
		
	}
}
