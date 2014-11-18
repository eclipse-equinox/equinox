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

package org.eclipse.equinox.http.servlet.internal.context;

import java.io.IOException;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.customizer.*;
import org.eclipse.equinox.http.servlet.internal.error.RegisteredFilterException;
import org.eclipse.equinox.http.servlet.internal.registration.*;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.eclipse.equinox.http.servlet.internal.servlet.*;
import org.eclipse.equinox.http.servlet.internal.util.*;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Raymond Augé
 */
public class ContextController {

	public static final class ServiceHolder<S> {
		final ServiceObjects<S> serviceObjects;
		final S service;
		public ServiceHolder(ServiceObjects<S> serviceObjects) {
			this.serviceObjects = serviceObjects;
			this.service = serviceObjects.getService();
		}
		public ServiceHolder(S service) {
			this.service = service;
			this.serviceObjects = null;
		}
		public S get() {
			return service;
		}
		public void release() {
			if (serviceObjects != null && service != null) {
				serviceObjects.ungetService(service);
			}
		}
	}

	public ContextController(
		Bundle bundle, BundleContext trackingContextParam, ServletContextHelper servletContextHelper,
		ProxyContext proxyContext, HttpServiceRuntimeImpl httpServiceRuntime,
		String contextName, String contextPath, long serviceId,
		Set<Servlet> registeredServlets, Map<String, Object> attributes) {

		this.bundle = bundle;
		this.servletContextHelper = servletContextHelper;
		this.proxyContext = proxyContext;
		this.httpServiceRuntime = httpServiceRuntime;
		this.contextName = contextName;
		this.contextPath = contextPath;
		this.contextServiceId = serviceId;
		this.registeredServlets = registeredServlets;

		attributes = new HashMap<String, Object>(attributes);
		attributes.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextName);
		attributes.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);
		attributes.put(Constants.SERVICE_ID, serviceId);

		this.attributes = attributes;

		Map<String, String> initParams = new HashMap<String, String>();

		for (String key : this.attributes.keySet()) {
			initParams.put(key, String.valueOf(attributes.get(key)));
		}

		this.initParams = Collections.unmodifiableMap(initParams);

		trackingContext = trackingContextParam;

		listenerServiceTracker = new ServiceTracker<EventListener, AtomicReference<ListenerRegistration>>(
			trackingContext, EventListener.class,
			new ContextListenerTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		listenerServiceTracker.open();

		filterServiceTracker = new ServiceTracker<Filter, AtomicReference<FilterRegistration>>(
			trackingContext, getFilteFilter(),
			new ContextFilterTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		filterServiceTracker.open();

		servletServiceTracker =  new ServiceTracker<Servlet, AtomicReference<ServletRegistration>>(
			trackingContext, getServletFilter(),
			new ContextServletTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		servletServiceTracker.open();

		resourceServiceTracker = new ServiceTracker<Servlet, AtomicReference<ResourceRegistration>>(
			trackingContext, getResourceFilter(),
			new ContextResourceTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		resourceServiceTracker.open();
	}

	public FilterRegistration addFilterRegistration(
			String alias, ServiceHolder<Filter> filterHolder, Dictionary<String, String> initparams,
			long serviceId)
		throws ServletException {

		String filterName = initparams.get(Const.FILTER_NAME);

		filterName = (filterName != null) ? filterName :
			filterHolder.get().getClass().getName();

		int filterPriority = findFilterPriority(initparams);

		return addFilterRegistration(
			filterHolder, false, DISPATCHER, filterPriority,
			new UMDictionaryMap<String, String>(initparams), filterName,
			new String[] {alias}, serviceId, Const.EMPTY_ARRAY);
	}

	public FilterRegistration addFilterRegistration(
			ServiceHolder<Filter> filterHolder, boolean asyncSupported, String[] dispatcher,
			int filterPriority, Map<String, String> initparams, String name,
			String[] patterns, long serviceId, String[] servletNames)
		throws ServletException {

		FilterRegistration result = null;
		Filter filter = filterHolder.get();
		try {
			checkShutdown();

			if (((patterns == null) || (patterns.length == 0)) &&
				((servletNames == null) || servletNames.length == 0)) {

				throw new IllegalArgumentException(
					"Patterns or servletNames must contain a value.");
			}

			if (patterns != null) {
				for (String pattern : patterns) {
					checkPattern(pattern);
				}
			}

			if (filter == null) {
				throw new IllegalArgumentException("Filter cannot be null");
			}

			if (name == null) {
				name = filter.getClass().getName();
			}

			for (FilterRegistration filterRegistration : filterRegistrations) {
				if (filterRegistration.getT().equals(filter)) {
					throw new RegisteredFilterException(filter);
				}
			}

			dispatcher = checkDispatcher(dispatcher);

			FilterDTO filterDTO = new FilterDTO();

			filterDTO.asyncSupported = asyncSupported;
			filterDTO.dispatcher = sort(dispatcher);
			filterDTO.initParams = initparams;
			filterDTO.name = name;
			filterDTO.patterns = sort(patterns);
			// TODO
			//filterDTO.regexps = sort(regexps);
			filterDTO.serviceId = serviceId;
			filterDTO.servletContextId = contextServiceId;
			filterDTO.servletNames = sort(servletNames);

			ServletContextHelper curServletContextHelper = getServletContextHelper(
				bundle);

			ServletContext servletContext = createServletContext(
				bundle, curServletContextHelper);
			FilterRegistration newRegistration  = new FilterRegistration(
				filterHolder, filterDTO, filterPriority, curServletContextHelper, this);
			FilterConfig filterConfig = new FilterConfigImpl(
				name, initparams, servletContext);

			newRegistration.init(filterConfig);

			filterRegistrations.add(newRegistration);
			result = newRegistration;
		} finally {
			if (result == null) {
				filterHolder.release();
			}
		}
		return result;
	}

	public ListenerRegistration addListenerRegistration(
			ServiceHolder<EventListener> listenerHolder, long serviceId)
		throws ServletException {

		checkShutdown();

		EventListener eventListener = listenerHolder.get();
		List<Class<? extends EventListener>> classes = getListenerClasses(
			eventListener);

		if (classes.isEmpty()) {
			throw new IllegalArgumentException(
				"EventListener does not implement a supported type.");
		}

		for (ListenerRegistration listenerRegistration : listenerRegistrations) {
			if (listenerRegistration.getT().equals(eventListener)) {
				throw new ServletException(
					"EventListener has already been registered.");
			}
		}

		ListenerDTO listenerDTO = new ListenerDTO();

		listenerDTO.serviceId = serviceId;
		listenerDTO.servletContextId = contextServiceId;
		listenerDTO.types = asStringArray(classes);

		ServletContextHelper curServletContextHelper = getServletContextHelper(
			bundle);

		ServletContext servletContext = createServletContext(
			bundle, curServletContextHelper);
		ListenerRegistration listenerRegistration = new ListenerRegistration(
			listenerHolder, classes, listenerDTO, servletContext,
			curServletContextHelper, this);

		if (classes.contains(ServletContextListener.class)) {
			ServletContextListener servletContextListener =
				(ServletContextListener)listenerRegistration.getT();

			servletContextListener.contextInitialized(
				new ServletContextEvent(servletContext));
		}

		listenerRegistrations.add(listenerRegistration);

		eventListeners.put(classes, listenerRegistration);

		return listenerRegistration;
	}

	public ResourceRegistration addResourceRegistration(
		String pattern, String prefix, long serviceId) {

		checkShutdown();

		if (pattern.startsWith("/*.")) { //$NON-NLS-1$
			pattern = pattern.substring(1);
		}
		else if (!pattern.contains("*.") && //$NON-NLS-1$
				 !pattern.endsWith(Const.SLASH_STAR) &&
				 !pattern.endsWith(Const.SLASH)) {
			pattern += Const.SLASH_STAR;
		}

		return addResourceRegistration(
			new String[] {pattern}, prefix, serviceId, true);
	}

	public ResourceRegistration addResourceRegistration(
		String[] patterns, String prefix, long serviceId, boolean legacyMatching) {

		checkShutdown();

		checkPrefix(prefix);

		if ((patterns == null) || (patterns.length < 1)) {
			throw new IllegalArgumentException(
				"Patterns must contain a value.");
		}

		for (String pattern : patterns) {
			checkPattern(pattern);
		}

		Servlet servlet = new ResourceServlet(
			prefix, servletContextHelper, AccessController.getContext());

		ResourceDTO resourceDTO = new ResourceDTO();

		resourceDTO.patterns = sort(patterns);
		resourceDTO.prefix = prefix;
		resourceDTO.serviceId = serviceId;
		resourceDTO.servletContextId = contextServiceId;

		ServletContextHelper curServletContextHelper = getServletContextHelper(
			bundle);

		ServletContext servletContext = createServletContext(
			bundle, curServletContextHelper);
		ResourceRegistration resourceRegistration = new ResourceRegistration(
			new ServiceHolder<Servlet>(servlet), resourceDTO, curServletContextHelper, this, legacyMatching);
		ServletConfig servletConfig = new ServletConfigImpl(
			resourceRegistration.getName(), new HashMap<String, String>(),
			servletContext);

		try {
			resourceRegistration.init(servletConfig);
		}
		catch (ServletException e) {
			return null;
		}

		endpointRegistrations.add(resourceRegistration);

		registeredServlets.add(servlet);

		return resourceRegistration;
	}

	public ServletRegistration addServletRegistration(
			String pattern, ServiceHolder<Servlet> servletHolder,
			Dictionary<String, String> initparams, long serviceId)
		throws ServletException {

		checkShutdown();

		if (servletHolder.get() == null) {
			throw new IllegalArgumentException("Servlet cannot be null");
		}

		String servletName = servletHolder.get().getClass().getName();

		if ((initparams != null) && (initparams.get(Const.SERVLET_NAME) != null)) {
			servletName = initparams.get(Const.SERVLET_NAME);
		}

		if (pattern == null) {
			throw new IllegalArgumentException("Pattern cannot be null");
		}

		return addServletRegistration(
			servletHolder, false, Const.EMPTY_ARRAY,
			new UMDictionaryMap<String, String>(initparams),
			new String[] {pattern}, serviceId, servletName, true);
	}

	public ServletRegistration addServletRegistration(
			ServiceHolder<Servlet> servletHolder, boolean asyncSupported, String[] errorPages,
			Map<String, String> initparams, String[] patterns, long serviceId,
			String servletName, boolean legacyMatching)
		throws ServletException {

		checkShutdown();

		if (((patterns == null) || (patterns.length == 0)) &&
			((errorPages == null) || errorPages.length == 0)) {

			throw new IllegalArgumentException(
				"Either patterns or errorPages must contain a value.");
		}

		if (patterns != null) {
			for (String pattern : patterns) {
				checkPattern(pattern);
			}
		}

		Servlet servlet = servletHolder.get();
		if (servletHolder.get() == null) {
			throw new IllegalArgumentException("Servlet cannot be null");
		}

		ServletDTO servletDTO = new ServletDTO();

		servletDTO.asyncSupported = asyncSupported;
		servletDTO.initParams = initparams;
		servletDTO.name = servletName;
		servletDTO.patterns = sort(patterns);
		servletDTO.serviceId = serviceId;
		servletDTO.servletContextId = contextServiceId;
		servletDTO.servletInfo = servlet.getServletInfo();

		ErrorPageDTO errorPageDTO = null;

		if ((errorPages != null) && (errorPages.length > 0)) {
			errorPageDTO = new ErrorPageDTO();

			errorPageDTO.asyncSupported = asyncSupported;
			long[] errorCodes = new long[0];
			List<String> exceptions = new ArrayList<String>();

			for(String errorPage : errorPages) {
				try {
					long longValue = Long.parseLong(errorPage);

					errorCodes = Arrays.copyOf(errorCodes, errorCodes.length + 1);

					errorCodes[errorCodes.length - 1] = longValue;
				}
				catch (NumberFormatException nfe) {
					exceptions.add(errorPage);
				}
			}

			errorPageDTO.errorCodes = errorCodes;
			errorPageDTO.exceptions = exceptions.toArray(new String[exceptions.size()]);
			errorPageDTO.initParams = initparams;
			errorPageDTO.name = servletName;
			errorPageDTO.serviceId = serviceId;
			errorPageDTO.servletContextId = contextServiceId;
			errorPageDTO.servletInfo = servlet.getServletInfo();
		}

		ServletContextHelper curServletContextHelper = getServletContextHelper(
			bundle);

		ServletContext servletContext = createServletContext(
			bundle, curServletContextHelper);
		ServletRegistration servletRegistration = new ServletRegistration(
			new ServiceHolder<Servlet>(servlet), servletDTO, errorPageDTO, curServletContextHelper, this, legacyMatching);
		ServletConfig servletConfig = new ServletConfigImpl(
			servletName, initparams, servletContext);

		servletRegistration.init(servletConfig);

		endpointRegistrations.add(servletRegistration);

		registeredServlets.add(servlet);

		return servletRegistration;
	}

	public void destroy() {
		resourceServiceTracker.close();
		servletServiceTracker.close();
		filterServiceTracker.close();
		listenerServiceTracker.close();

		endpointRegistrations.clear();
		filterRegistrations.clear();
		listenerRegistrations.clear();
		eventListeners.clear();
		proxyContext.destroy();

		eventListeners = null;
		registeredServlets = null;
		contextName = null;
		proxyContext = null;

		shutdown = true;
	}

	public ClassLoader getClassLoader() {
		return bundle.adapt(BundleWiring.class).getClassLoader();
	}

	public String getContextName() {
		checkShutdown();

		return contextName;
	}

	public String getContextPath() {
		checkShutdown();

		return contextPath;
	}

	public DispatchTargets getDispatchTargets(
		HttpServletRequest request, String servletName, String requestURI,
		String servletPath, String pathInfo, String extension, Match match,
		RequestInfoDTO requestInfoDTO) {

		checkShutdown();

		getProxyContext().initializeServletPath(request);

		EndpointRegistration<?> endpointRegistration = null;
		String pattern = null;

		for (EndpointRegistration<?> curEndpointRegistration : endpointRegistrations) {
			if ((pattern = curEndpointRegistration.match(
					servletName, servletPath, pathInfo, extension, match)) != null) {

				endpointRegistration = curEndpointRegistration;

				break;
			}
		}

		if (endpointRegistration == null) {
			return null;
		}

		endpointRegistration.addReference();

		addEnpointRegistrationsToRequestInfo(
			endpointRegistration, requestInfoDTO);

		if (filterRegistrations.isEmpty()) {
			return new DispatchTargets(
				this, endpointRegistration, servletPath, pathInfo, pattern);
		}

		String originalServletPath = servletPath;
		String originalPathInfo = pathInfo;

		List<FilterRegistration> matchingFilterRegistrations =
			new ArrayList<FilterRegistration>();

		for (Match curMatch : Match.values()) {
			collectFilters(
				matchingFilterRegistrations, servletName, requestURI, curMatch);
		}

		addFilterRegistrationsToRequestInfo(
			matchingFilterRegistrations, requestInfoDTO);

		return new DispatchTargets(
			this, endpointRegistration, matchingFilterRegistrations,
			originalServletPath, originalPathInfo, pattern);
	}

	private void collectFilters(
		List<FilterRegistration> matchingFilterRegistrations,
		String servletName, String requestURI, Match match) {

		String servletPath = requestURI;
		String pathInfo = "";
		String extension = null;

		int pos = -1;

		if (requestURI != null) {
			pos = requestURI.lastIndexOf('/');

			if (match == Match.EXTENSION) {
				int x = requestURI.lastIndexOf('.');

				if (x != -1) {
					extension = requestURI.substring(x + 1);
				}

				if (extension == null) {
					return;
				}
			}
		}

		do {
			for (FilterRegistration filterRegistration : filterRegistrations) {
				if ((filterRegistration.match(
						servletName, servletPath, pathInfo, extension, match) != null) &&
					!matchingFilterRegistrations.contains(filterRegistration)) {

					matchingFilterRegistrations.add(filterRegistration);

					filterRegistration.addReference();
				}
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
	}

	public Set<EndpointRegistration<?>> getEndpointRegistrations() {
		checkShutdown();

		return endpointRegistrations;
	}

	public EventListeners getEventListeners() {
		checkShutdown();

		return eventListeners;
	}

	public Set<FilterRegistration> getFilterRegistrations() {
		checkShutdown();

		return filterRegistrations;
	}

	public String getFullContextPath() {
		List<String> endpoints = httpServiceRuntime.getHttpServiceEndpoints();

		if (endpoints.isEmpty()) {
			return proxyContext.getServletPath().concat(contextPath);
		}

		String defaultEndpoint = endpoints.get(0);

		if ((defaultEndpoint.length() > 0) && defaultEndpoint.endsWith("/")) {
			defaultEndpoint = defaultEndpoint.substring(
				0, defaultEndpoint.length() - 1);
		}

		return defaultEndpoint + contextPath;
	}

	public HttpServiceRuntimeImpl getHttpServiceRuntime() {
		checkShutdown();

		return httpServiceRuntime;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public Set<ListenerRegistration> getListenerRegistrations() {
		checkShutdown();

		return listenerRegistrations;
	}

	public ProxyContext getProxyContext() {
		checkShutdown();

		return proxyContext;
	}

	public Set<Servlet> getRegisteredServlets() {
		checkShutdown();

		return registeredServlets;
	}

	public long getServiceId() {
		checkShutdown();

		return contextServiceId;
	}

	public synchronized ServletContextDTO getServletContextDTO(){
		checkShutdown();

		ServletContextDTO servletContextDTO = new ServletContextDTO();

		ServletContext servletContext = getProxyContext().getServletContext();

		servletContextDTO.attributes = getAttributes(servletContext);
		servletContextDTO.contextName = servletContext.getServletContextName();
		servletContextDTO.contextPath = servletContext.getContextPath();
		servletContextDTO.initParams = initParams;
		servletContextDTO.name = getContextName();
		servletContextDTO.serviceId = getServiceId();

		// TODO
		servletContextDTO.errorPageDTOs = new ErrorPageDTO[0];

		collectEndpointDTOs(servletContextDTO);
		collectFilterDTOs(servletContextDTO);
		collectListenerDTOs(servletContextDTO);

		return servletContextDTO;
	}

	public ServletContextHelper getServletContextHelper() {
		return servletContextHelper;
	}

	public boolean matches(String contextSelector) {
		if (contextSelector == null) {
			return true;
		}

		if (!contextSelector.startsWith(Const.OPEN_PAREN)) {
			contextSelector = Const.OPEN_PAREN +
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME +
					Const.EQUAL + contextSelector + Const.CLOSE_PAREN;
		}

		org.osgi.framework.Filter targetFilter;

		try {
			targetFilter = FrameworkUtil.createFilter(contextSelector);
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}

		return matches(targetFilter);
	}

	public boolean matches(org.osgi.framework.Filter targetFilter) {
		return targetFilter.matches(attributes);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + getContextName() + ']';
	}

	private void addEnpointRegistrationsToRequestInfo(
		EndpointRegistration<?> endpointRegistration,
		RequestInfoDTO requestInfoDTO) {

		if (requestInfoDTO == null) {
			return;
		}

		requestInfoDTO.servletContextId = getServiceId();

		if (endpointRegistration instanceof ResourceRegistration) {
			requestInfoDTO.resourceDTO =
				(ResourceDTO)endpointRegistration.getD();
		}
		else {
			requestInfoDTO.servletDTO =
				(ServletDTO)endpointRegistration.getD();
		}
	}

	private void addFilterRegistrationsToRequestInfo(
		List<FilterRegistration> matchedFilterRegistrations,
		RequestInfoDTO requestInfoDTO) {

		if (requestInfoDTO == null) {
			return;
		}

		FilterDTO[] filterDTOs =
			new FilterDTO[matchedFilterRegistrations.size()];

		for (int i = 0; i < filterDTOs.length ; i++) {
			FilterRegistration filterRegistration =
				matchedFilterRegistrations.get(i);

			filterDTOs[i] = filterRegistration.getD();
		}

		requestInfoDTO.filterDTOs = filterDTOs;
	}

	private String[] asStringArray(
		List<Class<? extends EventListener>> clazzes) {

		String[] classesArray = new String[clazzes.size()];

		for (int i = 0; i < classesArray.length; i++) {
			classesArray[i] = clazzes.get(i).getName();
		}

		Arrays.sort(classesArray);

		return classesArray;
	}

	private String[] checkDispatcher(String[] dispatcher) {
		if ((dispatcher == null) || (dispatcher.length == 0)) {
			return DISPATCHER;
		}

		for (String type : dispatcher) {
			try {
				Const.Dispatcher.valueOf(type);
			}
			catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException(
					"Invalid dispatcher '" + type + "'", iae);
			}
		}

		Arrays.sort(dispatcher);

		return dispatcher;
	}

	private void checkPattern(String pattern) {
		if (pattern == null) {
			throw new IllegalArgumentException("Pattern cannot be null");
		}

		if (pattern.indexOf("*.") == 0) { //$NON-NLS-1$
			return;
		}

		if (!pattern.startsWith(Const.SLASH) ||
			(pattern.endsWith(Const.SLASH) && !pattern.equals(Const.SLASH))) {

			throw new IllegalArgumentException(
				"Invalid pattern '" + pattern + "'");
		}
	}

	private void checkPrefix(String prefix) {
		if (prefix == null) {
			throw new IllegalArgumentException("Prefix cannot be null");
		}

		if (prefix.endsWith(Const.SLASH) && !prefix.equals(Const.SLASH)) {
			throw new IllegalArgumentException("Invalid prefix '" + prefix + "'");
		}
	}

	private void checkShutdown() {
		if (shutdown) {
			throw new IllegalStateException(
				"Context is already shutdown"); //$NON-NLS-1$
		}
	}

	private ServletContext createServletContext(
		Bundle curBundle, ServletContextHelper curServletContextHelper) {

		ServletContextAdaptor adaptor = new ServletContextAdaptor(
			this, curBundle, curServletContextHelper, eventListeners,
			AccessController.getContext());

		return adaptor.createServletContext();
	}

	private void collectEndpointDTOs(
		ServletContextDTO servletContextDTO) {

		List<ResourceDTO> resourceDTOs = new ArrayList<ResourceDTO>();
		List<ServletDTO> servletDTOs = new ArrayList<ServletDTO>();

		for (EndpointRegistration<?> endpointRegistration : endpointRegistrations) {
			if (endpointRegistration instanceof ResourceRegistration) {
				resourceDTOs.add((ResourceDTO)endpointRegistration.getD());
			}
			else {
				servletDTOs.add((ServletDTO)endpointRegistration.getD());
			}
		}

		servletContextDTO.resourceDTOs = resourceDTOs.toArray(
			new ResourceDTO[resourceDTOs.size()]);
		servletContextDTO.servletDTOs = servletDTOs.toArray(
			new ServletDTO[servletDTOs.size()]);
	}

	private void collectFilterDTOs(
		ServletContextDTO servletContextDTO) {

		List<FilterDTO> filterDTOs = new ArrayList<FilterDTO>();

		for (FilterRegistration filterRegistration : filterRegistrations) {
			filterDTOs.add(filterRegistration.getD());
		}

		servletContextDTO.filterDTOs = filterDTOs.toArray(
			new FilterDTO[filterDTOs.size()]);
	}

	private void collectListenerDTOs(
		ServletContextDTO servletContextDTO) {

		List<ListenerDTO> listenerDTOs = new ArrayList<ListenerDTO>();

		for (ListenerRegistration listenerRegistration : listenerRegistrations) {
			listenerDTOs.add(listenerRegistration.getD());
		}

		servletContextDTO.listenerDTOs = listenerDTOs.toArray(
			new ListenerDTO[listenerDTOs.size()]);
	}

	private int findFilterPriority(Dictionary<String, String> initparams) {
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

	private Map<String, Object> getAttributes(ServletContext servletContext) {
		Map<String, Object> map = new HashMap<String, Object>();

		for (Enumeration<String> names = servletContext.getAttributeNames();
				names.hasMoreElements();) {

			String name = names.nextElement();

			map.put(name, servletContext.getAttribute(name));
		}

		return Collections.unmodifiableMap(map);
	}

	private List<Class<? extends EventListener>> getListenerClasses(
		EventListener eventListener) {

		List<Class<? extends EventListener>> classes =
			new ArrayList<Class<? extends EventListener>>();

		if (ServletContextListener.class.isInstance(eventListener)) {
			classes.add(ServletContextListener.class);
		}
		if (ServletContextAttributeListener.class.isInstance(eventListener)) {
			classes.add(ServletContextAttributeListener.class);
		}
		if (ServletRequestListener.class.isInstance(eventListener)) {
			classes.add(ServletRequestListener.class);
		}
		if (ServletRequestAttributeListener.class.isInstance(eventListener)) {
			classes.add(ServletRequestAttributeListener.class);
		}
		if (HttpSessionListener.class.isInstance(eventListener)) {
			classes.add(HttpSessionListener.class);
		}
		if (HttpSessionAttributeListener.class.isInstance(eventListener)) {
			classes.add(HttpSessionAttributeListener.class);
		}

		return classes;
	}

	private ServletContextHelper getServletContextHelper(Bundle curBundle) {
		if (curBundle == this.bundle) {
			return servletContextHelper;
		}

		return new ServletContextHelper(curBundle) {

			@Override
			public String getMimeType(String name) {
				return servletContextHelper.getMimeType(name);
			}

			@Override
			public boolean handleSecurity(
					HttpServletRequest request, HttpServletResponse response)
				throws IOException {

				return servletContextHelper.handleSecurity(request, response);
			}

		};
	}

	private org.osgi.framework.Filter getFilteFilter() {
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

	private org.osgi.framework.Filter getResourceFilter() {
		StringBuilder sb = new StringBuilder();

		sb.append("(&(objectClass="); //$NON-NLS-1$
		sb.append(Servlet.class.getName());
		sb.append(")("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
		sb.append("=*))"); //$NON-NLS-1$

		try {
			return trackingContext.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	private org.osgi.framework.Filter getServletFilter() {
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

	private String[] sort(String[] values) {
		if (values == null) {
			return null;
		}

		Arrays.sort(values);

		return values;
	}

	private static final String[] DISPATCHER =
		new String[] {Const.Dispatcher.REQUEST.toString()};

	private Map<String, Object> attributes;
	private Map<String, String> initParams;
	private final Bundle bundle;
	private final BundleContext trackingContext;
	private String contextName;
	private final String contextPath;
	private final long contextServiceId;
	private final Set<EndpointRegistration<?>> endpointRegistrations = new HashSet<EndpointRegistration<?>>();
	private EventListeners eventListeners = new EventListeners();
	private final Set<FilterRegistration> filterRegistrations = new HashSet<FilterRegistration>();
	private ServiceTracker<Filter, AtomicReference<FilterRegistration>> filterServiceTracker;
	private HttpServiceRuntimeImpl httpServiceRuntime;
	private ServiceTracker<EventListener, AtomicReference<ListenerRegistration>> listenerServiceTracker;
	private final Set<ListenerRegistration> listenerRegistrations = new HashSet<ListenerRegistration>();
	private ProxyContext proxyContext;
	private Set<Servlet> registeredServlets;
	private ServiceTracker<Servlet, AtomicReference<ServletRegistration>> servletServiceTracker;
	private ServiceTracker<Servlet, AtomicReference<ResourceRegistration>> resourceServiceTracker;
	ServletContextHelper servletContextHelper;
	private boolean shutdown;

}