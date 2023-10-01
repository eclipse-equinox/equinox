/*******************************************************************************
 * Copyright (c) 2016, 2019 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.context;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.customizer.*;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedErrorPageDTO;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedErrorPageDTO.ErrorCodeType;
import org.eclipse.equinox.http.servlet.internal.error.*;
import org.eclipse.equinox.http.servlet.internal.registration.*;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.eclipse.equinox.http.servlet.internal.servlet.*;
import org.eclipse.equinox.http.servlet.internal.util.*;
import org.osgi.framework.*;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Raymond Augé
 */
public class ContextController {

	public ContextController(
		BundleContext trackingContextParam, BundleContext consumingContext,
		ServiceReference<ServletContextHelper> serviceReference,
		ServletContext parentServletContext, HttpServiceRuntimeImpl httpServiceRuntime) {

		this.trackingContext = trackingContextParam;
		this.consumingContext = consumingContext;
		this.serviceReference = serviceReference;
		this.httpServiceRuntime = httpServiceRuntime;
		this.contextName = validateName();
		this.contextPath = validatePath();
		this.proxyContext = new ProxyContext(contextName, parentServletContext);
		this.contextServiceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
		this.servletContextHelperRefFilter = createFilter(contextServiceId);

		this.initParams = ServiceProperties.parseInitParams(
			serviceReference, HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX, parentServletContext);

		listenerServiceTracker = new ServiceTracker<>(
			trackingContext, httpServiceRuntime.getListenerFilter(),
			new ContextListenerTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		listenerServiceTracker.open();

		filterServiceTracker = new ServiceTracker<>(
			trackingContext, httpServiceRuntime.getFilterFilter(),
			new ContextFilterTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		filterServiceTracker.open();

		errorPageServiceTracker =  new ServiceTracker<>(
			trackingContext, httpServiceRuntime.getErrorPageFilter(),
			new ContextErrorPageTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		errorPageServiceTracker.open();

		servletServiceTracker =  new ServiceTracker<>(
			trackingContext, httpServiceRuntime.getServletFilter(),
			new ContextServletTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		servletServiceTracker.open();

		resourceServiceTracker = new ServiceTracker<>(
			trackingContext, httpServiceRuntime.getResourceFilter(),
			new ContextResourceTrackerCustomizer(
				trackingContext, httpServiceRuntime, this));

		resourceServiceTracker.open();
	}

	public ErrorPageRegistration addErrorPageRegistration(ServiceReference<Servlet> servletRef) {
		ServiceHolder<Servlet> servletHolder = new ServiceHolder<>(consumingContext.getServiceObjects(servletRef));
		Servlet servlet = servletHolder.get();
		ErrorPageRegistration registration = null;
		//boolean addedRegisteredObject = false;
		try {
			if (servlet == null) {
				throw new IllegalArgumentException("Servlet cannot be null"); //$NON-NLS-1$
			}
			//addedRegisteredObject = httpServiceRuntime.getRegisteredObjects().add(servlet);
			//if (!addedRegisteredObject) {
			//	throw new HttpWhiteboardFailureException("Multiple registration of instance detected. Prototype scope is recommended: " + servletRef, DTOConstants.FAILURE_REASON_SERVICE_IN_USE); //$NON-NLS-1$
			//}
			registration = doAddErrorPageRegistration(servletHolder, servletRef);
		} finally {
			if (registration == null) {
				// Always attempt to release here; even though destroy() may have been called
				// on the registration while failing to add.  There are cases where no
				// ServletRegistration may have even been created at all to call destory() on.
				// Also, addedRegisteredObject may be false which means we never call doAddServletRegistration
				servletHolder.release();
				//if (addedRegisteredObject) {
				//	httpServiceRuntime.getRegisteredObjects().remove(servlet);
				//}
			}
		}
		return registration;
	}

	private ErrorPageRegistration doAddErrorPageRegistration(ServiceHolder<Servlet> servletHolder, ServiceReference<Servlet> servletRef) {
		ExtendedErrorPageDTO errorPageDTO = DTOUtil.assembleErrorPageDTO(servletRef, getServiceId(), true);
		errorPageDTO.servletInfo = servletHolder.get().getServletInfo();
		errorPageDTO.serviceId = servletHolder.getServiceId();

		if (((errorPageDTO.errorCodes == null) || (errorPageDTO.errorCodes.length == 0)) &&
			((errorPageDTO.exceptions == null) || (errorPageDTO.exceptions.length == 0))) {

			throw new HttpWhiteboardFailureException("'errorPage' expects String, String[] or Collection<String>.", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$
		}

		if (errorPageDTO.name == null) {
			errorPageDTO.name = servletHolder.get().getClass().getName();
		}

		ServletContextHelper curServletContextHelper = getServletContextHelper(
			servletHolder.getBundle());

		ServletContext servletContext = createServletContext(
			servletHolder.getBundle(), curServletContextHelper);
		ErrorPageRegistration errorPageRegistration = new ErrorPageRegistration(
			servletHolder, errorPageDTO, curServletContextHelper, this);
		ServletConfig servletConfig = new ServletConfigImpl(
			errorPageDTO.name, errorPageDTO.initParams, servletContext);

		try {
			errorPageRegistration.init(servletConfig);
		}
		catch (Throwable t) {
			errorPageRegistration.destroy();

			return Throw.unchecked(t);
		}

		recordErrorPageShadowing(errorPageRegistration);

		endpointRegistrations.add(errorPageRegistration);

		return errorPageRegistration;
	}

	public FilterRegistration addFilterRegistration(ServiceReference<Filter> filterRef) throws ServletException {
		ServiceHolder<Filter> filterHolder = new ServiceHolder<>(consumingContext.getServiceObjects(filterRef));
		Filter filter = filterHolder.get();
		FilterRegistration registration = null;
		boolean addedRegisteredObject = false;
		try {
			if (filter == null) {
				throw new IllegalArgumentException("Filter cannot be null"); //$NON-NLS-1$
			}
			addedRegisteredObject = httpServiceRuntime.getRegisteredObjects().add(filter);
			if (!addedRegisteredObject) {
				throw new HttpWhiteboardFailureException("Multiple registration of instance detected. Prototype scope is recommended: " + filterRef, DTOConstants.FAILURE_REASON_SERVICE_IN_USE); //$NON-NLS-1$
			}
			registration = doAddFilterRegistration(filterHolder, filterRef);
		} finally {
			if (registration == null) {
				filterHolder.release();
				if (addedRegisteredObject) {
					httpServiceRuntime.getRegisteredObjects().remove(filter);
				}
			}
		}
		return registration;
	}

	private FilterRegistration doAddFilterRegistration(ServiceHolder<Filter> filterHolder, ServiceReference<Filter> filterRef) throws ServletException {
		boolean asyncSupported = ServiceProperties.parseBoolean(
			filterRef, HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);

		List<String> dispatcherList = StringPlus.from(
			filterRef.getProperty(
				HTTP_WHITEBOARD_FILTER_DISPATCHER));
		String[] dispatchers = dispatcherList.toArray(new String[0]);
		Integer filterPriority = (Integer)filterRef.getProperty(
			Constants.SERVICE_RANKING);
		if (filterPriority == null) {
			filterPriority = Integer.valueOf(0);
		}
		Map<String, String> filterInitParams = ServiceProperties.parseInitParams(
			filterRef, HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX);
		List<String> patternList = StringPlus.from(
			filterRef.getProperty(
				HTTP_WHITEBOARD_FILTER_PATTERN));
		String[] patterns = patternList.toArray(new String[0]);
		List<String> regexList = StringPlus.from(
			filterRef.getProperty(
				HTTP_WHITEBOARD_FILTER_REGEX));
		String[] regexs = regexList.toArray(new String[0]);
		List<String> servletList = StringPlus.from(
			filterRef.getProperty(
				HTTP_WHITEBOARD_FILTER_SERVLET));
		String[] servletNames = servletList.toArray(new String[0]);

		String name = ServiceProperties.parseName(filterRef.getProperty(
			HTTP_WHITEBOARD_FILTER_NAME), filterHolder.get());

		if (((patterns == null) || (patterns.length == 0)) &&
			((regexs == null) || (regexs.length == 0)) &&
			((servletNames == null) || (servletNames.length == 0))) {

			throw new HttpWhiteboardFailureException(
				"Patterns, regex or servletNames must contain a value.", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$
		}

		if (patterns != null) {
			for (String pattern : patterns) {
				checkPattern(pattern);
			}
		}

		if (regexs != null) {
			for (String regex : regexs) {
				checkRegex(regex);
			}
		}

		Filter filter = filterHolder.get();

		if (name == null) {
			name = filter.getClass().getName();
		}

		dispatchers = checkDispatcher(dispatchers);

		FilterDTO filterDTO = new FilterDTO();

		filterDTO.asyncSupported = asyncSupported;
		filterDTO.dispatcher = sort(dispatchers);
		filterDTO.initParams = filterInitParams;
		filterDTO.name = name;
		filterDTO.patterns = sort(patterns);
		filterDTO.regexs = regexs;
		filterDTO.serviceId = filterHolder.getServiceId();
		filterDTO.servletContextId = contextServiceId;
		filterDTO.servletNames = sort(servletNames);

		ServletContextHelper curServletContextHelper = getServletContextHelper(
			filterHolder.getBundle());

		ServletContext servletContext = createServletContext(
			filterHolder.getBundle(), curServletContextHelper);
		FilterRegistration newRegistration  = new FilterRegistration(
			filterHolder, filterDTO, filterPriority, this);
		FilterConfig filterConfig = new FilterConfigImpl(
			name, filterInitParams, servletContext);

		newRegistration.init(filterConfig);

		filterRegistrations.add(newRegistration);
		return newRegistration;
	}

	public ListenerRegistration addListenerRegistration(ServiceReference<EventListener> listenerRef) throws ServletException {
		ServiceHolder<EventListener> listenerHolder = new ServiceHolder<>(consumingContext.getServiceObjects(listenerRef));
		EventListener listener = listenerHolder.get();
		ListenerRegistration registration = null;
		try {
			if (listener == null) {
				throw new IllegalArgumentException("EventListener cannot be null"); //$NON-NLS-1$
			}
			registration = doAddListenerRegistration(listenerHolder, listenerRef);
		} finally {
			if (registration == null) {
				listenerHolder.release();
			}
		}
		return registration;
	}

	private ListenerRegistration doAddListenerRegistration(
		ServiceHolder<EventListener> listenerHolder,
		ServiceReference<EventListener> listenerRef) throws ServletException {

		EventListener eventListener = listenerHolder.get();
		List<Class<? extends EventListener>> classes = getListenerClasses(listenerRef);

		if (classes.isEmpty()) {
			throw new IllegalArgumentException(
				"EventListener does not implement a supported type."); //$NON-NLS-1$
		}

		for (ListenerRegistration listenerRegistration : listenerRegistrations) {
			if (listenerRegistration.getT().equals(eventListener)) {
				throw new ServletException(
					"EventListener has already been registered."); //$NON-NLS-1$
			}
		}

		ListenerDTO listenerDTO = new ListenerDTO();

		listenerDTO.serviceId = listenerHolder.getServiceId();
		listenerDTO.servletContextId = contextServiceId;
		listenerDTO.types = asStringArray(classes);

		ServletContextHelper curServletContextHelper = getServletContextHelper(
			listenerHolder.getBundle());

		ServletContext servletContext = createServletContext(
			listenerHolder.getBundle(), curServletContextHelper);
		ListenerRegistration listenerRegistration = new ListenerRegistration(
			listenerHolder, classes, listenerDTO, servletContext, this);

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

	public ResourceRegistration addResourceRegistration(ServiceReference<?> resourceRef) {
		ClassLoader legacyTCCL = (ClassLoader)resourceRef.getProperty(Const.EQUINOX_LEGACY_TCCL_PROP);
		Integer rankProp = (Integer) resourceRef.getProperty(Constants.SERVICE_RANKING);
		int serviceRanking = rankProp == null ? 0 : rankProp.intValue();
		Object patternObj = resourceRef.getProperty(HTTP_WHITEBOARD_RESOURCE_PATTERN);
		if (!(patternObj instanceof String) &&
			!(patternObj instanceof String[]) &&
			!(patternObj instanceof Collection)) {
			throw new HttpWhiteboardFailureException("Expect pattern to be String+ (String | String[] | Collection<String>)", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$
		}
		List<String> patternList = StringPlus.from(patternObj);
		String[] patterns = patternList.toArray(new String[0]);
		Long serviceId = (Long)resourceRef.getProperty(
			Constants.SERVICE_ID);
		if (legacyTCCL != null) {
			// this is a legacy registration; use a negative id for the DTO
			serviceId = -serviceId;
		}
		Object prefixObj = resourceRef.getProperty(
			HTTP_WHITEBOARD_RESOURCE_PREFIX);

		checkPrefix(prefixObj);
		String prefix = (String)prefixObj;

		if ((patterns == null) || (patterns.length < 1)) {
			throw new IllegalArgumentException(
				"Patterns must contain a value."); //$NON-NLS-1$
		}

		for (String pattern : patterns) {
			checkPattern(pattern);
		}

		Bundle bundle = resourceRef.getBundle();
		ServletContextHelper curServletContextHelper = getServletContextHelper(
			bundle);
		Servlet servlet = new ResourceServlet(
			prefix, curServletContextHelper, AccessController.getContext());

		ResourceDTO resourceDTO = new ResourceDTO();

		resourceDTO.patterns = sort(patterns);
		resourceDTO.prefix = prefix;
		resourceDTO.serviceId = serviceId;
		resourceDTO.servletContextId = contextServiceId;

		ServletContext servletContext = createServletContext(
			bundle, curServletContextHelper);
		ResourceRegistration resourceRegistration = new ResourceRegistration(
			resourceRef, new ServiceHolder<>(servlet, bundle, serviceId, serviceRanking, legacyTCCL),
			resourceDTO, curServletContextHelper, this);
		ServletConfig servletConfig = new ServletConfigImpl(
			resourceRegistration.getName(), new HashMap<String, String>(),
			servletContext);

		try {
			resourceRegistration.init(servletConfig);
		}
		catch (Throwable t) {
			resourceRegistration.destroy();

			return Throw.unchecked(t);
		}

		recordEndpointShadowing(resourceRegistration);

		endpointRegistrations.add(resourceRegistration);

		return resourceRegistration;
	}

	public ServletRegistration addServletRegistration(ServiceReference<Servlet> servletRef) {
		ServiceHolder<Servlet> servletHolder = new ServiceHolder<>(consumingContext.getServiceObjects(servletRef));
		Servlet servlet = servletHolder.get();
		ServletRegistration registration = null;
		boolean addedRegisteredObject = false;
		try {
			if (servlet == null) {
				throw new IllegalArgumentException("Servlet cannot be null"); //$NON-NLS-1$
			}
			addedRegisteredObject = httpServiceRuntime.getRegisteredObjects().add(servlet);
			if (!addedRegisteredObject) {
				throw new HttpWhiteboardFailureException("Multiple registration of instance detected. Prototype scope is recommended: " + servletRef, DTOConstants.FAILURE_REASON_SERVICE_IN_USE); //$NON-NLS-1$
			}
			registration = doAddServletRegistration(servletHolder, servletRef);
		} finally {
			if (registration == null) {
				// Always attempt to release here; even though destroy() may have been called
				// on the registration while failing to add.  There are cases where no
				// ServletRegistration may have even been created at all to call destory() on.
				// Also, addedRegisteredObject may be false which means we never call doAddServletRegistration
				servletHolder.release();
				if (addedRegisteredObject) {
					httpServiceRuntime.getRegisteredObjects().remove(servlet);
				}
			}
		}
		return registration;
	}

	private ServletRegistration doAddServletRegistration(ServiceHolder<Servlet> servletHolder, ServiceReference<Servlet> servletRef) {
		ServletDTO servletDTO = DTOUtil.assembleServletDTO(servletRef, getServiceId(), true);
		servletDTO.servletInfo = servletHolder.get().getServletInfo();
		servletDTO.serviceId = servletHolder.getServiceId();

		if (((servletDTO.patterns == null) || (servletDTO.patterns.length == 0)) &&
			(servletDTO.name == null)) {

			StringBuilder sb = new StringBuilder();
			sb.append("One of the service properties "); //$NON-NLS-1$
			sb.append(HTTP_WHITEBOARD_SERVLET_NAME);
			sb.append(", "); //$NON-NLS-1$
			sb.append(HTTP_WHITEBOARD_SERVLET_PATTERN);
			sb.append(" must contain a value."); //$NON-NLS-1$

			throw new HttpWhiteboardFailureException(sb.toString(), DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		if (servletDTO.name == null) {
			servletDTO.name = servletHolder.get().getClass().getName();
		}

		ServletContextHelper curServletContextHelper = getServletContextHelper(
			servletHolder.getBundle());

		ServletContext servletContext = createServletContext(
			servletHolder.getBundle(), curServletContextHelper);
		ServletRegistration servletRegistration = new ServletRegistration(
			servletHolder, servletDTO, curServletContextHelper, this,
			servletContext);
		ServletConfig servletConfig = new ServletConfigImpl(
			servletDTO.name, servletDTO.initParams, servletContext);

		try {
			servletRegistration.init(servletConfig);
		}
		catch (Throwable t) {
			servletRegistration.destroy();

			return Throw.unchecked(t);
		}

		recordEndpointShadowing(servletRegistration);

		endpointRegistrations.add(servletRegistration);

		return servletRegistration;
	}

	private void recordEndpointShadowing(EndpointRegistration<?> newRegistration) {
		Set<EndpointRegistration<?>> shadowedRegs = new HashSet<>();
		for (EndpointRegistration<?> existingRegistration : endpointRegistrations) {
			for (String newPattern : newRegistration.getPatterns()) {
				for (String existingPattern : existingRegistration.getPatterns()) {
					if (newPattern.equals(existingPattern)) {
						// the new reg is shadowing an existing reg
						if (newRegistration.compareTo(existingRegistration) < 0) {
							shadowedRegs.add(existingRegistration);
						}
						// the new reg is shadowed by existing reg
						else {
							shadowedRegs.add(newRegistration);
						}
						// notice that we keep checking all the existing regs. more than one could be shadowed because reg's multi patterns
					}
				}
			}
		}
		for (EndpointRegistration<?> shadowedReg : shadowedRegs) {
			if (shadowedReg instanceof ServletRegistration) {
				recordFailedServletDTO(shadowedReg.getServiceReference(), (ServletDTO)shadowedReg.getD(), DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
			}
			else {
				recordFailedResourceDTO(shadowedReg.getServiceReference(), (ResourceDTO)shadowedReg.getD(), DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
			}
		}
	}

	private void recordErrorPageShadowing(ErrorPageRegistration newRegistration) {
		Set<ErrorPageRegistration> shadowedEPs = new HashSet<>();
		for (EndpointRegistration<?> existingRegistration : endpointRegistrations) {
			if (!(existingRegistration instanceof ErrorPageRegistration)) {
				continue;
			}
			ErrorPageRegistration existingSRegistration = (ErrorPageRegistration)existingRegistration;
			ExtendedErrorPageDTO existingErrorPageDTO = existingSRegistration.getD();
			if ((existingErrorPageDTO == null) ||
				(((existingErrorPageDTO.errorCodeType == ErrorCodeType.RANGE_4XX) || (existingErrorPageDTO.errorCodeType == ErrorCodeType.RANGE_5XX)) && (newRegistration.getD().errorCodeType == ErrorCodeType.SPECIFIC))) {
				continue;
			}
			if (((existingErrorPageDTO.errorCodeType == ErrorCodeType.RANGE_4XX) && (newRegistration.getD().errorCodeType == ErrorCodeType.RANGE_4XX)) ||
				((existingErrorPageDTO.errorCodeType == ErrorCodeType.RANGE_5XX) && (newRegistration.getD().errorCodeType == ErrorCodeType.RANGE_5XX))) {
				if (newRegistration.compareTo(existingSRegistration) < 0) {
					shadowedEPs.add(existingSRegistration);
				}
				// the new reg is shadowed by existing reg
				else {
					shadowedEPs.add(newRegistration);
				}
				continue;
			}
			for (long newErrorCode : newRegistration.getD().errorCodes) {
				for (long existingCode : existingErrorPageDTO.errorCodes) {
					if (newErrorCode == existingCode) {
						// the new reg is shadowing an existing reg
						if (newRegistration.compareTo(existingSRegistration) < 0) {
							shadowedEPs.add(existingSRegistration);
						}
						// the new reg is shadowed by existing reg
						else {
							shadowedEPs.add(newRegistration);
						}
						// notice that we keep checking all the existing regs. more than one could be shadowed because reg's multi patterns
					}
				}
			}
			for (String newException : newRegistration.getD().exceptions) {
				for (String existingException : existingErrorPageDTO.exceptions) {
					if (newException.equals(existingException)) {
						// the new reg is shadowing an existing reg
						if (newRegistration.compareTo(existingSRegistration) < 0) {
							shadowedEPs.add(existingSRegistration);
						}
						// the new reg is shadowed by existing reg
						else {
							shadowedEPs.add(newRegistration);
						}
						// notice that we keep checking all the existing regs. more than one could be shadowed because reg's multi patterns
					}
				}
			}
		}
		for (ErrorPageRegistration shadowedReg : shadowedEPs) {
			recordFailedErrorPageDTO(shadowedReg.getServiceReference(), shadowedReg.getD(), DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
		}
	}

	public synchronized void destroy() {
		flushActiveSessions();
		resourceServiceTracker.close();
		servletServiceTracker.close();
		errorPageServiceTracker.close();
		filterServiceTracker.close();
		listenerServiceTracker.close();

		endpointRegistrations.clear();
		filterRegistrations.clear();
		listenerRegistrations.clear();
		eventListeners.clear();
		proxyContext.destroy();

		shutdown = true;
	}

	public void createContextAttributes() {
		getProxyContext().createContextAttributes(this);
	}

	public void destroyContextAttributes() {
		if (shutdown) {
			return;
		}

		proxyContext.destroyContextAttributes(this);
	}

	public boolean isLegacyContext() {
		return serviceReference.getProperty(HTTP_SERVICE_CONTEXT_PROPERTY) != null;
	}

	public String getContextName() {
		return contextName;
	}

	public String getContextPath() {
		return contextPath;
	}

	public DispatchTargets getDispatchTargets(
		String pathString, RequestInfoDTO requestInfoDTO) {

		Path path = new Path(pathString);

		String queryString = path.getQueryString();
		String requestURI = path.getRequestURI();

		// perfect match
		DispatchTargets dispatchTargets = getDispatchTargets(
			requestURI, null, queryString, Match.EXACT, requestInfoDTO);

		if (dispatchTargets == null) {
			// extension match

			dispatchTargets = getDispatchTargets(
				requestURI, path.getExtension(), queryString, Match.EXTENSION,
				requestInfoDTO);
		}

		if (dispatchTargets == null) {
			// regex match
			dispatchTargets = getDispatchTargets(
				requestURI, null, queryString, Match.REGEX, requestInfoDTO);
		}

		if (dispatchTargets == null) {
			// handle '/' aliases
			dispatchTargets = getDispatchTargets(
				requestURI, null, queryString, Match.DEFAULT_SERVLET,
				requestInfoDTO);
		}

		return dispatchTargets;
	}

	private DispatchTargets getDispatchTargets(
		String requestURI, String extension, String queryString, Match match,
		RequestInfoDTO requestInfoDTO) {

		int pos = requestURI.lastIndexOf('/');

		String servletPath = requestURI;
		String pathInfo = null;

		if (match == Match.DEFAULT_SERVLET) {
			pathInfo = servletPath;
			servletPath = Const.SLASH;
		}

		do {
			DispatchTargets dispatchTargets = getDispatchTargets(
				null, requestURI, servletPath, pathInfo,
				extension, queryString, match, requestInfoDTO);

			if (dispatchTargets != null) {
				return dispatchTargets;
			}

			if (match == Match.EXACT) {
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

	public DispatchTargets getDispatchTargets(
		String servletName, String requestURI, String servletPath,
		String pathInfo, String extension, String queryString, Match match,
		RequestInfoDTO requestInfoDTO) {

		checkShutdown();

		EndpointRegistration<?> endpointRegistration = null;
		for (EndpointRegistration<?> curEndpointRegistration : endpointRegistrations) {
			if (curEndpointRegistration.match(servletName, servletPath, pathInfo, extension, match) != null) {
				endpointRegistration = curEndpointRegistration;

				break;
			}
		}

		if (endpointRegistration == null) {
			return null;
		}

		if (match == Match.EXTENSION) {
			servletPath = servletPath + pathInfo;
			pathInfo = null;
		}

		addEnpointRegistrationsToRequestInfo(
			endpointRegistration, requestInfoDTO);

		if (filterRegistrations.isEmpty()) {
			return new DispatchTargets(
				this, endpointRegistration, servletName, requestURI, servletPath,
				pathInfo, queryString);
		}

		if (requestURI != null) {
			int x = requestURI.lastIndexOf('.');

			if (x != -1) {
				extension = requestURI.substring(x + 1);
			}
		}

		List<FilterRegistration> matchingFilterRegistrations =
			new ArrayList<>();

		collectFilters(
			matchingFilterRegistrations, endpointRegistration.getName(), requestURI,
			servletPath, pathInfo, extension);

		addFilterRegistrationsToRequestInfo(
			matchingFilterRegistrations, requestInfoDTO);

		return new DispatchTargets(
			this, endpointRegistration, matchingFilterRegistrations, servletName,
			requestURI, servletPath, pathInfo, queryString);
	}

	private void collectFilters(
		List<FilterRegistration> matchingFilterRegistrations,
		String servletName, String requestURI, String servletPath, String pathInfo, String extension) {

		for (FilterRegistration filterRegistration : filterRegistrations) {
			if ((filterRegistration.match(
					servletName, requestURI, extension, null) != null) &&
				!matchingFilterRegistrations.contains(filterRegistration)) {

				matchingFilterRegistrations.add(filterRegistration);
			}
		}
	}

	public Map<String, HttpSessionAdaptor> getActiveSessions() {
		return activeSessions;
	}

	public Set<EndpointRegistration<?>> getEndpointRegistrations() {
		return endpointRegistrations;
	}

	public EventListeners getEventListeners() {
		return eventListeners;
	}

	public Set<FilterRegistration> getFilterRegistrations() {
		return filterRegistrations;
	}

	public String getFullContextPath() {
		if (fullContextPath != null) {
			return fullContextPath;
		}

		return fullContextPath = getFullContextPath0();
	}

	private  String getFullContextPath0() {
		List<String> endpoints = httpServiceRuntime.getHttpServiceEndpoints();

		if (endpoints.isEmpty()) {
			return proxyContext.getServletPath().concat(contextPath);
		}

		String defaultEndpoint = endpoints.get(0);

		if (defaultEndpoint.length() > 0) {
			int protocol = defaultEndpoint.indexOf(Const.PROTOCOL);
			if (protocol > -1) {
				defaultEndpoint = defaultEndpoint.substring(protocol + 3);
			}
			int slash = defaultEndpoint.indexOf(Const.SLASH);
			if (defaultEndpoint.endsWith(Const.SLASH)) {
				defaultEndpoint = defaultEndpoint.substring(slash, defaultEndpoint.length() - 1);
			}
		}

		return defaultEndpoint + contextPath;
	}

	public HttpServiceRuntimeImpl getHttpServiceRuntime() {
		return httpServiceRuntime;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public Set<ListenerRegistration> getListenerRegistrations() {
		return listenerRegistrations;
	}

	public ProxyContext getProxyContext() {
		return proxyContext;
	}

	public long getServiceId() {
		return contextServiceId;
	}

	public synchronized ServletContextDTO getServletContextDTO(){
		ServletContextDTO servletContextDTO = new ServletContextDTO();

		ServletContext servletContext = proxyContext.getServletContext();

		servletContextDTO.attributes = getDTOAttributes(servletContext);
		servletContextDTO.contextPath = getContextPath();
		servletContextDTO.initParams = new HashMap<>(initParams);
		servletContextDTO.name = getContextName();
		servletContextDTO.serviceId = getServiceId();

		collectEndpointDTOs(servletContextDTO);
		collectFilterDTOs(servletContextDTO);
		collectListenerDTOs(servletContextDTO);

		return servletContextDTO;
	}

	public boolean matches(ServiceReference<?> whiteBoardService) {
		// make sure the context helper is either one of the built-in ones registered by this http whiteboard implementation;
		// or is visible to the whiteboard registering bundle.

		if (!visibleContextHelper(whiteBoardService)) {
			return false;
		}

		String contextSelector = (String) whiteBoardService.getProperty(
			HTTP_WHITEBOARD_CONTEXT_SELECT);

		// custom equinox behaviour
		if (contextName.equals(contextSelector)) {
			return true;
		}

		if (contextSelector == null) {
			contextSelector = httpServiceRuntime.getDefaultContextSelectFilter(whiteBoardService);
			if (contextSelector == null) {
				contextSelector = "(" + //$NON-NLS-1$
					HTTP_WHITEBOARD_CONTEXT_NAME + "=" //$NON-NLS-1$
					+ HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")"; //$NON-NLS-1$
			}
		}

		if (contextSelector.startsWith(Const.OPEN_PAREN)) {
			org.osgi.framework.Filter targetFilter;

			try {
				targetFilter = FrameworkUtil.createFilter(contextSelector);
			}
			catch (InvalidSyntaxException ise) {
				throw new IllegalArgumentException(ise);
			}

			if (matches(targetFilter)) {
				return true;
			}
		}

		return false;
	}

	private boolean visibleContextHelper(ServiceReference<?> whiteBoardService) {
		if (consumingContext.getBundle().equals(serviceReference.getBundle())) {
			Boolean defaultContextHelper = (Boolean) serviceReference.getProperty(Const.EQUINOX_HTTP_WHITEBOARD_CONTEXT_HELPER_DEFAULT);
			if (defaultContextHelper == null) {
				return true;
			}
		}
		try {
			if (whiteBoardService.getBundle().getBundleContext().getAllServiceReferences(ServletContextHelper.class.getName(), servletContextHelperRefFilter) != null) {
				return true;
			}
		}
		catch (InvalidSyntaxException e) {
			// ignore
		}
		return false;
	}

	public boolean matches(org.osgi.framework.Filter targetFilter) {
		return targetFilter.match(serviceReference);
	}

	@Override
	public String toString() {
		String value = string;

		if (value == null) {
			value = SIMPLE_NAME + '[' + contextName + ", " + trackingContext.getBundle() + ']'; //$NON-NLS-1$

			string = value;
		}

		return value;
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

		List<FilterDTO> filterDTOs = new ArrayList<>();

		for (FilterRegistration filterRegistration : matchedFilterRegistrations) {
			if (Arrays.binarySearch(filterRegistration.getD().dispatcher, DispatcherType.REQUEST.toString()) > -1) {
				filterDTOs.add(filterRegistration.getD());
			}
		}

		requestInfoDTO.filterDTOs = filterDTOs.toArray(new FilterDTO[0]);
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
				DispatcherType.valueOf(type);
			}
			catch (IllegalArgumentException iae) {
				throw new HttpWhiteboardFailureException(
					"Invalid dispatcher '" + type + "'", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		Arrays.sort(dispatcher);

		return dispatcher;
	}

	public static void checkPattern(String pattern) {
		if (pattern == null) {
			throw new HttpWhiteboardFailureException("Pattern cannot be null", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$
		}

		if (pattern.indexOf("*.") == 0) { //$NON-NLS-1$
			return;
		}

		if (Const.BLANK.equals(pattern)) {
			return;
		}

		if (Const.SLASH.equals(pattern)) {
			return;
		}

		if (!pattern.startsWith(Const.SLASH) ||
			(pattern.endsWith(Const.SLASH) && !pattern.equals(Const.SLASH)) ||
			pattern.contains("**")) { //$NON-NLS-1$

			throw new HttpWhiteboardFailureException(
				"Invalid pattern '" + pattern + "'", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static void checkPrefix(Object prefixObj) {
		if (prefixObj == null) {
			throw new HttpWhiteboardFailureException("Prefix cannot be null.", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$
		}

		if (!(prefixObj instanceof String)) {
			throw new HttpWhiteboardFailureException("Prefix must be String.", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$
		}

		String prefix = (String)prefixObj;

		if (prefix.endsWith(Const.SLASH) && !prefix.equals(Const.SLASH)) {
			throw new HttpWhiteboardFailureException("Invalid prefix '" + prefix + "'", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static void checkRegex(String regex) {
		try {
			Pattern.compile(regex);
		}
		catch (PatternSyntaxException pse) {
			throw new HttpWhiteboardFailureException(
				"Invalid regex '" + regex + "'", DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public synchronized void checkShutdown() {
		if (shutdown) {
			throw new IllegalStateException(
				"Context is already shutdown"); //$NON-NLS-1$
		}
	}

	private static String createFilter(long contextServiceId) {
		StringBuilder filterBuilder = new StringBuilder();
		filterBuilder.append('(');
		filterBuilder.append(Constants.SERVICE_ID);
		filterBuilder.append('=');
		filterBuilder.append(contextServiceId);
		filterBuilder.append(')');
		return filterBuilder.toString();
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

		List<ErrorPageDTO> errorPageDTOs = new ArrayList<>();
		List<ResourceDTO> resourceDTOs = new ArrayList<>();
		List<ServletDTO> servletDTOs = new ArrayList<>();

		for (EndpointRegistration<?> endpointRegistration : endpointRegistrations) {
			if (endpointRegistration instanceof ResourceRegistration) {
				if (!httpServiceRuntime.isFailedResourceDTO(endpointRegistration.getServiceReference())) {
					resourceDTOs.add(DTOUtil.clone((ResourceDTO)endpointRegistration.getD()));
				}
			}
			else if (endpointRegistration instanceof ErrorPageRegistration) {
				if (!httpServiceRuntime.isFailedErrorPageDTO(endpointRegistration.getServiceReference())) {
					errorPageDTOs.add(DTOUtil.clone((ExtendedErrorPageDTO)endpointRegistration.getD()));
				}
			}
			else {
				if (!httpServiceRuntime.isFailedServletDTO(endpointRegistration.getServiceReference())) {
					servletDTOs.add(DTOUtil.clone((ServletDTO)endpointRegistration.getD()));
				}
			}
		}

		servletContextDTO.errorPageDTOs = errorPageDTOs.toArray(new ErrorPageDTO[0]);
		servletContextDTO.resourceDTOs = resourceDTOs.toArray(new ResourceDTO[0]);
		servletContextDTO.servletDTOs = servletDTOs.toArray(new ServletDTO[0]);
	}

	private void collectFilterDTOs(
		ServletContextDTO servletContextDTO) {

		List<FilterDTO> filterDTOs = new ArrayList<>();

		for (FilterRegistration filterRegistration : filterRegistrations) {
			filterDTOs.add(DTOUtil.clone(filterRegistration.getD()));
		}

		servletContextDTO.filterDTOs = filterDTOs.toArray(new FilterDTO[0]);
	}

	private void collectListenerDTOs(
		ServletContextDTO servletContextDTO) {

		List<ListenerDTO> listenerDTOs = new ArrayList<>();

		for (ListenerRegistration listenerRegistration : listenerRegistrations) {
			listenerDTOs.add(DTOUtil.clone(listenerRegistration.getD()));
		}

		servletContextDTO.listenerDTOs = listenerDTOs.toArray(new ListenerDTO[0]);
	}

	private Map<String, Object> getDTOAttributes(ServletContext servletContext) {
		Map<String, Object> map = new HashMap<>();

		for (Enumeration<String> names = servletContext.getAttributeNames();
				names.hasMoreElements();) {

			String name = names.nextElement();

			map.put(name, DTOUtil.mapValue(servletContext.getAttribute(name)));
		}

		return Collections.unmodifiableMap(map);
	}

	private List<Class<? extends EventListener>> getListenerClasses(
		ServiceReference<EventListener> listenerReference) {

		List<String> objectClassList = StringPlus.from(listenerReference.getProperty(Constants.OBJECTCLASS));

		List<Class<? extends EventListener>> classes =
			new ArrayList<>();

		if (objectClassList.contains(ServletContextListener.class.getName())) {
			classes.add(ServletContextListener.class);
		}
		if (objectClassList.contains(ServletContextAttributeListener.class.getName())) {
			classes.add(ServletContextAttributeListener.class);
		}
		if (objectClassList.contains(ServletRequestListener.class.getName())) {
			classes.add(ServletRequestListener.class);
		}
		if (objectClassList.contains(ServletRequestAttributeListener.class.getName())) {
			classes.add(ServletRequestAttributeListener.class);
		}
		if (objectClassList.contains(HttpSessionListener.class.getName())) {
			classes.add(HttpSessionListener.class);
		}
		if (objectClassList.contains(HttpSessionAttributeListener.class.getName())) {
			classes.add(HttpSessionAttributeListener.class);
		}

		if (objectClassList.contains(javax.servlet.http.HttpSessionIdListener.class.getName())) {
			classes.add(javax.servlet.http.HttpSessionIdListener.class);
		}

		return classes;
	}

	public ServiceReference<ServletContextHelper> getServiceReference() {
		return serviceReference;
	}

	private ServletContextHelper getServletContextHelper(Bundle curBundle) {
		BundleContext context = curBundle.getBundleContext();
		return context.getService(serviceReference);
	}

	public void ungetServletContextHelper(Bundle curBundle) {
		BundleContext context = curBundle.getBundleContext();
		try {
			context.ungetService(serviceReference);
		} catch (IllegalStateException e) {
			// this can happen if the whiteboard bundle is in the process of stopping
			// and the framework is in the middle of auto-unregistering any services
			// the bundle forgot to unregister on stop
		}
	}

	private String[] sort(String[] values) {
		if (values == null) {
			return null;
		}

		Arrays.sort(values);

		return values;
	}

	private void flushActiveSessions() {
		Collection<HttpSessionAdaptor> httpSessionAdaptors =
			activeSessions.values();

		Iterator<HttpSessionAdaptor> iterator = httpSessionAdaptors.iterator();

		while (iterator.hasNext()) {
			HttpSessionAdaptor httpSessionAdaptor = iterator.next();

			httpSessionAdaptor.invalidate();

			iterator.remove();
		}
	}

	public void removeActiveSession(HttpSession session) {
		removeActiveSession(session.getId());
	}

	public void removeActiveSession(String sessionId) {
		HttpSessionAdaptor httpSessionAdaptor = activeSessions.remove(sessionId);

		if (httpSessionAdaptor != null) {
			httpServiceRuntime.getHttpSessionTracker().removeHttpSessionAdaptor(sessionId, httpSessionAdaptor);
		}
	}

	public void fireSessionIdChanged(String oldSessionId) {
		if (shutdown) {
			return;
		}

		List<javax.servlet.http.HttpSessionIdListener> listeners = eventListeners.get(javax.servlet.http.HttpSessionIdListener.class);

		if (listeners.isEmpty()) {
			return;
		}

		for (HttpSessionAdaptor httpSessionAdaptor : activeSessions.values()) {
			HttpSessionEvent httpSessionEvent = new HttpSessionEvent(httpSessionAdaptor);
			for (javax.servlet.http.HttpSessionIdListener listener : listeners) {
				listener.sessionIdChanged(httpSessionEvent, oldSessionId);
			}
		}
	}

	public HttpSessionAdaptor getSessionAdaptor(
		HttpSession session, ServletContext servletContext) {

		String sessionId = session.getId();

		HttpSessionAdaptor httpSessionAdaptor = activeSessions.get(sessionId);

		if (httpSessionAdaptor != null) {
			return httpSessionAdaptor;
		}

		httpSessionAdaptor = HttpSessionAdaptor.createHttpSessionAdaptor(
			session, servletContext, this);

		HttpSessionAdaptor previousHttpSessionAdaptor =
			addSessionAdaptor(sessionId, httpSessionAdaptor);

		if (previousHttpSessionAdaptor != null) {
			return previousHttpSessionAdaptor;
		}

		List<HttpSessionListener> listeners = eventListeners.get(HttpSessionListener.class);

		if (listeners.isEmpty()) {
			return httpSessionAdaptor;
		}

		HttpSessionEvent httpSessionEvent = new HttpSessionEvent(
			httpSessionAdaptor);

		for (HttpSessionListener listener : listeners) {
			listener.sessionCreated(httpSessionEvent);
		}

		return httpSessionAdaptor;
	}

	public HttpSessionAdaptor addSessionAdaptor(
		String sessionId, HttpSessionAdaptor httpSessionAdaptor) {

		HttpSessionAdaptor previousHttpSessionAdaptor =
			activeSessions.putIfAbsent(sessionId, httpSessionAdaptor);

		if (previousHttpSessionAdaptor != null) {
			return previousHttpSessionAdaptor;
		}

		httpServiceRuntime.getHttpSessionTracker().addHttpSessionAdaptor(sessionId, httpSessionAdaptor);

		return null;
	}

	public void recordFailedErrorPageDTO(
		ServiceReference<?> servletReference, ExtendedErrorPageDTO errorPageDTO, int failureReason) {

		FailedErrorPageDTO failedErrorPageDTO = new FailedErrorPageDTO();
		failedErrorPageDTO.asyncSupported = errorPageDTO.asyncSupported;
		failedErrorPageDTO.errorCodes = errorPageDTO.errorCodes;
		failedErrorPageDTO.exceptions = errorPageDTO.exceptions;
		failedErrorPageDTO.failureReason = failureReason;
		failedErrorPageDTO.initParams = errorPageDTO.initParams;
		failedErrorPageDTO.name = errorPageDTO.name;
		failedErrorPageDTO.serviceId = errorPageDTO.serviceId;
		failedErrorPageDTO.servletContextId = errorPageDTO.servletContextId;
		failedErrorPageDTO.servletInfo = errorPageDTO.servletInfo;

		getHttpServiceRuntime().recordFailedErrorPageDTO(servletReference, failedErrorPageDTO);
	}

	public void recordFailedResourceDTO(
		ServiceReference<?> resourceReference, ResourceDTO resourceDTO, int failureReason) {

		FailedResourceDTO failedResourceDTO = new FailedResourceDTO();
		failedResourceDTO.failureReason = failureReason;
		failedResourceDTO.patterns = resourceDTO.patterns;
		failedResourceDTO.prefix = resourceDTO.prefix;
		failedResourceDTO.serviceId = resourceDTO.serviceId;
		failedResourceDTO.servletContextId = resourceDTO.servletContextId;

		getHttpServiceRuntime().recordFailedResourceDTO(resourceReference, failedResourceDTO);
	}

	public void recordFailedServletDTO(ServiceReference<?> servletReference, ServletDTO servletDTO, int failureReason) {
		FailedServletDTO failedServletDTO = new FailedServletDTO();

		failedServletDTO.asyncSupported = servletDTO.asyncSupported;
		failedServletDTO.failureReason = failureReason;
		failedServletDTO.initParams = servletDTO.initParams;
		failedServletDTO.multipartEnabled = servletDTO.multipartEnabled;
		failedServletDTO.multipartFileSizeThreshold = servletDTO.multipartFileSizeThreshold;
		failedServletDTO.multipartLocation = servletDTO.multipartLocation;
		failedServletDTO.multipartMaxFileSize = servletDTO.multipartMaxFileSize;
		failedServletDTO.multipartMaxRequestSize = servletDTO.multipartMaxRequestSize;
		failedServletDTO.name = servletDTO.name;
		failedServletDTO.patterns = servletDTO.patterns;
		failedServletDTO.serviceId = servletDTO.serviceId;
		failedServletDTO.servletContextId = servletDTO.servletContextId;
		failedServletDTO.servletInfo = servletDTO.servletInfo;

		getHttpServiceRuntime().recordFailedServletDTO(servletReference, failedServletDTO);
	}

	private String validateName() {
		Object contextNameObj = serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_NAME);

		if (contextNameObj == null) {
			throw new IllegalContextNameException(
				HTTP_WHITEBOARD_CONTEXT_NAME + " is null. Ignoring!", //$NON-NLS-1$
				DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
		else if (!(contextNameObj instanceof String)) {
			throw new IllegalContextNameException(
				HTTP_WHITEBOARD_CONTEXT_NAME + " is not String. Ignoring!", //$NON-NLS-1$
				DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		String name = (String)contextNameObj;

		if (!contextNamePattern.matcher(name).matches()) {
			throw new IllegalContextNameException(
				"The context name '" + name + "' does not follow Bundle-SymbolicName syntax.", //$NON-NLS-1$ //$NON-NLS-2$
				DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		// Now check for naming conflicts
		for (ContextController existingContext : httpServiceRuntime.getContextControllers()) {
			if (name.equals(existingContext.getContextName())) {
				if (serviceReference.compareTo(existingContext.serviceReference) < 0) {
					throw new HttpWhiteboardFailureException("Context with same name exists. " + serviceReference, DTOConstants.FAILURE_REASON_VALIDATION_FAILED); //$NON-NLS-1$
				}

				httpServiceRuntime.recordFailedServletContextDTO(
					existingContext.serviceReference, (Long)serviceReference.getProperty(Constants.SERVICE_ID), DTOConstants.FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE);
			}
		}

		return name;
	}

	private String validatePath() {
		Object contextPathObj = serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_PATH);

		if (contextPathObj == null) {
			throw new IllegalContextPathException(
				HTTP_WHITEBOARD_CONTEXT_PATH + " is null. Ignoring!", //$NON-NLS-1$
				DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
		else if (!(contextPathObj instanceof String)) {
			throw new IllegalContextPathException(
				HTTP_WHITEBOARD_CONTEXT_PATH + " is not String. Ignoring!", //$NON-NLS-1$
				DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		String path = (String)contextPathObj;

		try {
			@SuppressWarnings("unused")
			URI uri = new URI(Const.HTTP, Const.LOCALHOST, path, null);
		}
		catch (URISyntaxException use) {
			throw new IllegalContextPathException(
				"The context path '" + path + "' is not valid URI path syntax.", //$NON-NLS-1$ //$NON-NLS-2$
				DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		path = httpServiceRuntime.adaptContextPath(path, serviceReference);
		if (path.equals(Const.SLASH)) {
			path = Const.BLANK;
		}

		return path;
	}

	private static final String[] DISPATCHER =
		new String[] {DispatcherType.REQUEST.toString()};

	private static final String SIMPLE_NAME = ContextController.class.getSimpleName();

	private static final Pattern contextNamePattern = Pattern.compile("^([a-zA-Z_0-9\\-]+\\.)*[a-zA-Z_0-9\\-]+$"); //$NON-NLS-1$

	private final Map<String, String> initParams;
	private final BundleContext trackingContext;
	private final BundleContext consumingContext;
	private final String contextName;
	private final String contextPath;
	private volatile String fullContextPath;
	private final long contextServiceId;
	private final Set<EndpointRegistration<?>> endpointRegistrations = new ConcurrentSkipListSet<>();
	private final EventListeners eventListeners = new EventListeners();
	private final Set<FilterRegistration> filterRegistrations = new ConcurrentSkipListSet<>();
	private final ConcurrentMap<String, HttpSessionAdaptor> activeSessions = new ConcurrentHashMap<>();

	private final HttpServiceRuntimeImpl httpServiceRuntime;
	private final Set<ListenerRegistration> listenerRegistrations = new HashSet<>();
	private final ProxyContext proxyContext;
	private final ServiceReference<ServletContextHelper> serviceReference;
	private final String servletContextHelperRefFilter;
	private boolean shutdown;
	private String string;

	private final ServiceTracker<Servlet, AtomicReference<ErrorPageRegistration>> errorPageServiceTracker;
	private final ServiceTracker<Filter, AtomicReference<FilterRegistration>> filterServiceTracker;
	private final ServiceTracker<EventListener, AtomicReference<ListenerRegistration>> listenerServiceTracker;
	private final ServiceTracker<Object, AtomicReference<ResourceRegistration>> resourceServiceTracker;
	private final ServiceTracker<Servlet, AtomicReference<ServletRegistration>> servletServiceTracker;
}
