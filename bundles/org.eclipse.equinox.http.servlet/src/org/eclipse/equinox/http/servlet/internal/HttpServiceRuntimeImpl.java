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
 *     Raymond Augé - bug fixes and enhancements
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal;

import static org.osgi.service.http.runtime.HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.context.ContextPathCustomizer;
import org.eclipse.equinox.http.servlet.internal.context.*;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedErrorPageDTO;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedFailedServletContextDTO;
import org.eclipse.equinox.http.servlet.internal.error.*;
import org.eclipse.equinox.http.servlet.internal.registration.PreprocessorRegistration;
import org.eclipse.equinox.http.servlet.internal.servlet.HttpSessionTracker;
import org.eclipse.equinox.http.servlet.internal.servlet.Match;
import org.eclipse.equinox.http.servlet.internal.util.*;
import org.eclipse.equinox.http.servlet.session.HttpSessionInvalidator;
import org.osgi.framework.*;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.*;
import org.osgi.service.http.whiteboard.Preprocessor;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;
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
		ServletContext parentServletContext, Dictionary<String, Object> attributes) {

		this.trackingContext = trackingContext;
		this.consumingContext = consumingContext;

		this.errorPageServiceFilter = createErrorPageFilter(consumingContext);
		this.servletServiceFilter = createServletFilter(consumingContext);
		this.resourceServiceFilter = createResourceFilter(consumingContext);
		this.filterServiceFilter = createFilterFilter(consumingContext);
		this.listenerServiceFilter = createListenerFilter(consumingContext, parentServletContext);

		this.parentServletContext = parentServletContext;
		this.attributes = new UMDictionaryMap<>(attributes);
		this.targetFilter = "(" + Activator.UNIQUE_SERVICE_ID + "=" + this.attributes.get(Activator.UNIQUE_SERVICE_ID) + ")";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.httpSessionTracker = new HttpSessionTracker(this);
		this.invalidatorReg = trackingContext.registerService(HttpSessionInvalidator.class, this.httpSessionTracker, attributes);

		loggerFactoryTracker = new ServiceTracker<>(consumingContext, LoggerFactory.class, new ServiceTrackerCustomizer<LoggerFactory, Logger>() {
			@Override
			public Logger addingService(ServiceReference<LoggerFactory> reference) {
				return getConsumingContext().getService(reference).getLogger(HttpServiceRuntimeImpl.class);
			}
			@Override
			public void modifiedService(ServiceReference<LoggerFactory> reference, Logger service) {
				// ignore
			}
			@Override
			public void removedService(ServiceReference<LoggerFactory> reference, Logger service) {
				// ignore
			}
		});
		loggerFactoryTracker.open();

		contextServiceTracker =
			new ServiceTracker<>(
				trackingContext, ServletContextHelper.class, this);

		preprocessorServiceTracker =
			new ServiceTracker<>(
				trackingContext, Preprocessor.class, new PreprocessorCustomizer(this));

		contextPathCustomizerHolder = new ContextPathCustomizerHolder(consumingContext, contextServiceTracker);
		contextPathAdaptorTracker = new ServiceTracker<>(
			consumingContext, ContextPathCustomizer.class, contextPathCustomizerHolder);

		Hashtable<String, Object> defaultContextProps = new Hashtable<>();
		defaultContextProps.put(HTTP_WHITEBOARD_CONTEXT_NAME, HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
		defaultContextProps.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
		defaultContextProps.put(HTTP_WHITEBOARD_CONTEXT_PATH, Const.SLASH);
		defaultContextProps.put(HTTP_WHITEBOARD_TARGET, this.targetFilter);
		defaultContextProps.put(Const.EQUINOX_HTTP_WHITEBOARD_CONTEXT_HELPER_DEFAULT, Boolean.TRUE);
		defaultContextReg = consumingContext.registerService(
			ServletContextHelper.class, new DefaultServletContextHelperFactory(), defaultContextProps);
	}

	public synchronized void open() {
		contextPathAdaptorTracker.open();
		contextServiceTracker.open();
		preprocessorServiceTracker.open();
	}

	@Override
	public synchronized AtomicReference<ContextController> addingService(
		ServiceReference<ServletContextHelper> serviceReference) {

		AtomicReference<ContextController> result = new AtomicReference<>();
		if (!matches(serviceReference)) {
			return result;
		}

		try {
			ContextController contextController = new ContextController(
				trackingContext, consumingContext, serviceReference, parentServletContext, this);

			controllerMap.put(serviceReference, contextController);

			result.set(contextController);
		}
		catch (HttpWhiteboardFailureException hwfe) {
			debug(hwfe.getMessage(), hwfe);

			recordFailedServletContextDTO(serviceReference, 0, hwfe.getFailureReason());
		}
		catch (Throwable t) {
			error(t.getMessage(), t);

			recordFailedServletContextDTO(serviceReference, 0, DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		}
		finally {
			incrementServiceChangecount();
		}

		return result;
	}

	public String adaptContextPath(String contextPath, ServiceReference<ServletContextHelper> helper) {
		ContextPathCustomizer pathAdaptor = contextPathCustomizerHolder.getHighestRanked();
		if (pathAdaptor != null) {
			String contextPrefix = pathAdaptor.getContextPathPrefix(helper);
			if (contextPrefix != null && !contextPrefix.isEmpty() && !contextPrefix.equals(Const.SLASH)) {
				if (!contextPrefix.startsWith(Const.SLASH)) {
					contextPrefix = Const.SLASH + contextPrefix;
				}
				// make sure we do not append SLASH context path here
				if (contextPath == null || contextPath.equals(Const.SLASH)) {
					contextPath = Const.BLANK;
				}
				return contextPrefix + contextPath;
			}
		}
		return contextPath;
	}

	public BundleContext getConsumingContext() {
		return consumingContext;
	}

	public String getDefaultContextSelectFilter(ServiceReference<?> httpWhiteBoardService) {
		ContextPathCustomizer pathAdaptor = contextPathCustomizerHolder.getHighestRanked();
		if (pathAdaptor != null) {
			return pathAdaptor.getDefaultContextSelectFilter(httpWhiteBoardService);
		}
		return null;
	}

	public boolean isDefaultContext(ContextController contextController) {
		ServiceReference<?> thisReference = defaultContextReg.getReference();
		ServiceReference<ServletContextHelper> contextReference = contextController.getServiceReference();
		if (thisReference == null) throw new NullPointerException("Default Context Service reference is null. " + this); //$NON-NLS-1$
		if (contextReference == null) throw new NullPointerException("Context Service reference is null. " + contextController); //$NON-NLS-1$
		return thisReference.equals(contextReference);
	}

	public boolean isFailedResourceDTO(ServiceReference<?> serviceReference) {
		return failedResourceDTOs.containsKey(serviceReference);
	}

	public boolean isFailedServletDTO(ServiceReference<?> serviceReference) {
		return failedServletDTOs.containsKey(serviceReference);
	}

	public boolean isFailedErrorPageDTO(ServiceReference<?> serviceReference) {
		return failedErrorPageDTOs.containsKey(serviceReference);
	}

	@Override
	public synchronized RequestInfoDTO calculateRequestInfoDTO(String path) {
		RequestInfoDTO requestInfoDTO = new RequestInfoDTO();

		requestInfoDTO.path = path;

		try {
			getDispatchTargets(path, requestInfoDTO);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		return requestInfoDTO;
	}

	public synchronized void destroy() {
		invalidatorReg.unregister();

		try {
			defaultContextReg.unregister();
		}
		catch (IllegalStateException ise) {
			// ignore
		}

		contextServiceTracker.close();
		contextPathAdaptorTracker.close();
		preprocessorServiceTracker.close();

		controllerMap.clear();
		preprocessorMap.clear();
		registeredObjects.clear();
		legacyContextMap.clear();

		failedErrorPageDTOs.clear();
		failedFilterDTOs.clear();
		failedListenerDTOs.clear();
		failedPreprocessorDTOs.clear();
		failedResourceDTOs.clear();
		failedServletContextDTOs.clear();
		failedServletDTOs.clear();

		httpSessionTracker.clear();
		registeredObjects.clear();
		scheduledExecutor.shutdown();
		loggerFactoryTracker.close();
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
			// handle with servlet mapped to '/'
			// the servletpath is the requestURI minus the contextpath and the pathinfo is null
			dispatchTargets = getDispatchTargets(
				requestURI, null, queryString, Match.DEFAULT_SERVLET,
				requestInfoDTO);
		}

		if (dispatchTargets == null && Const.SLASH.equals(pathString)) {
			// handle with servlet mapped to '' (empty string)
			// the pathinfo is '/' and the servletpath and contextpath are the empty string ("")
			dispatchTargets = getDispatchTargets(
				requestURI, null, queryString, Match.CONTEXT_ROOT,
				requestInfoDTO);
		}

		return dispatchTargets;
	}

	public HttpSessionTracker getHttpSessionTracker() {
		return httpSessionTracker;
	}

	public Set<Object> getRegisteredObjects() {
		return registeredObjects;
	}

	public String getTargetFilter() {
		return targetFilter;
	}

	public ServletContext getParentServletContext() {
		return parentServletContext;
	}

	public List<String> getHttpServiceEndpoints() {
		return StringPlus.from(
			attributes.get(HTTP_SERVICE_ENDPOINT));
	}

	@Override
	public synchronized RuntimeDTO getRuntimeDTO() {
		RuntimeDTO runtimeDTO = new RuntimeDTO();

		runtimeDTO.failedErrorPageDTOs = getFailedErrorPageDTOs();
		runtimeDTO.failedFilterDTOs = getFailedFilterDTOs();
		runtimeDTO.failedListenerDTOs = getFailedListenerDTOs();
		runtimeDTO.failedPreprocessorDTOs = getFailedPreprocessorDTOs();
		runtimeDTO.failedResourceDTOs = getFailedResourceDTOs();
		runtimeDTO.failedServletContextDTOs = getFailedServletContextDTO();
		runtimeDTO.failedServletDTOs = getFailedServletDTOs();
		runtimeDTO.preprocessorDTOs = getPreprocessorDTOs();
		runtimeDTO.serviceDTO = getServiceDTO();
		runtimeDTO.servletContextDTOs = getServletContextDTOs();

		return runtimeDTO;
	}

	private FailedErrorPageDTO[] getFailedErrorPageDTOs() {
		Collection<FailedErrorPageDTO> fepDTOs = failedErrorPageDTOs.values();

		List<FailedErrorPageDTO> copies = new ArrayList<>();

		for (FailedErrorPageDTO failedErrorPageDTO : fepDTOs) {
			copies.add(DTOUtil.clone(failedErrorPageDTO));
		}

		return copies.toArray(new FailedErrorPageDTO[0]);
	}

	private ServiceReferenceDTO getServiceDTO() {
		ServiceReferenceDTO[] services = consumingContext.getBundle().adapt(ServiceReferenceDTO[].class);
		for (ServiceReferenceDTO serviceDTO : services) {
			String[] serviceTypes = (String[]) serviceDTO.properties.get(Constants.OBJECTCLASS);
			for (String type : serviceTypes) {
				if (HttpServiceRuntime.class.getName().equals(type)) {
					return serviceDTO;
				}
			}
		}
		return null;
	}

	public void debug(String message) {
		Logger logger = loggerFactoryTracker.getService();
		if (logger == null) {
			parentServletContext.log(String.valueOf(message));
		}
		else {
			logger.debug(String.valueOf(message));
		}
	}

	public void debug(String message, Throwable t) {
		Logger logger = loggerFactoryTracker.getService();
		if (logger == null) {
			parentServletContext.log(String.valueOf(message), t);
		}
		else {
			logger.debug(String.valueOf(message), t);
		}
	}

	public void error(String message, Throwable t) {
		Logger logger = loggerFactoryTracker.getService();
		if (logger == null) {
			parentServletContext.log(String.valueOf(message), t);
		}
		else {
			logger.error(String.valueOf(message), t);
		}
	}

	public boolean matches(ServiceReference<?> serviceReference) {
		String target = (String)serviceReference.getProperty(HTTP_WHITEBOARD_TARGET);

		if (target == null) {
			return true;
		}

		org.osgi.framework.Filter whiteboardTargetFilter;

		try {
			whiteboardTargetFilter = FrameworkUtil.createFilter(target);
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}

		if (whiteboardTargetFilter.matches(attributes)) {
			return true;
		}

		return false;
	}

	public boolean matchesAnyContext(ServiceReference<?> serviceReference) {
		for (ContextController contextController : controllerMap.values()) {
			if (contextController.matches(serviceReference)) {
				return true;
			}
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

		try {
			ContextController contextController = contextControllerRef.get();
			if (contextController != null) {
				Iterator<Entry<ServiceReference<ServletContextHelper>, ExtendedFailedServletContextDTO>> iterator = failedServletContextDTOs.entrySet().iterator();
				while (iterator.hasNext()) {
					if (iterator.next().getValue().shadowingServiceId == contextController.getServiceId()) {
						iterator.remove();
					}
				}
				contextController.destroy();
			}
			failedServletContextDTOs.remove(serviceReference);
			controllerMap.remove(serviceReference);
			trackingContext.ungetService(serviceReference);
		}
		finally {
			incrementServiceChangecount();
		}
	}

	Collection<ContextController> getContextControllers(String requestURI) {
		int pos = requestURI.lastIndexOf('/');

		do {
			List<ContextController> contextControllers = new ArrayList<>();

			for (ContextController contextController : controllerMap.values()) {
				if (contextController.getContextPath().equals(requestURI)) {
					contextControllers.add(contextController);
				}
			}

			if (!contextControllers.isEmpty()) {
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

	public Collection<ContextController> getContextControllers() {
		return controllerMap.values();
	}

	public DispatchTargets getDispatchTargets(
		String requestURI, String extension, String queryString, Match match,
		RequestInfoDTO requestInfoDTO) {

		Collection<ContextController> contextControllers = getContextControllers(
			requestURI);

		if ((contextControllers == null) || contextControllers.isEmpty()) {
			return null;
		}

		String contextPath =
			contextControllers.iterator().next().getContextPath();

		requestURI = requestURI.substring(contextPath.length());

		int pos = requestURI.lastIndexOf('/');

		String servletPath = decode(requestURI);
		String pathInfo = null;

		if (match == Match.CONTEXT_ROOT) {
			pathInfo = Const.SLASH;
			servletPath = Const.BLANK;
		}

		do {
			for (ContextController contextController : contextControllers) {
				DispatchTargets dispatchTargets =
					contextController.getDispatchTargets(
						null, requestURI, servletPath, pathInfo,
						extension, queryString, match, requestInfoDTO);

				if (dispatchTargets != null) {
					return dispatchTargets;
				}
			}

			if ((match == Match.EXACT) || (match == Match.CONTEXT_ROOT) || (match == Match.DEFAULT_SERVLET)) {
				break;
			}

			if (pos > -1) {
				String newServletPath = requestURI.substring(0, pos);
				pathInfo = decode(requestURI.substring(pos));
				servletPath = decode(newServletPath);
				pos = newServletPath.lastIndexOf('/');

				continue;
			}

			break;
		}
		while (true);

		return null;
	}

	private FailedFilterDTO[] getFailedFilterDTOs() {
		Collection<FailedFilterDTO> ffDTOs = failedFilterDTOs.values();

		List<FailedFilterDTO> copies = new ArrayList<>();

		for (FailedFilterDTO failedFilterDTO : ffDTOs) {
			copies.add(DTOUtil.clone(failedFilterDTO));
		}

		return copies.toArray(new FailedFilterDTO[0]);
	}

	private FailedListenerDTO[] getFailedListenerDTOs() {
		Collection<FailedListenerDTO> flDTOs = failedListenerDTOs.values();

		List<FailedListenerDTO> copies = new ArrayList<>();

		for (FailedListenerDTO failedListenerDTO : flDTOs) {
			copies.add(DTOUtil.clone(failedListenerDTO));
		}

		return copies.toArray(new FailedListenerDTO[0]);
	}

	private FailedResourceDTO[] getFailedResourceDTOs() {
		Collection<FailedResourceDTO> frDTOs = failedResourceDTOs.values();

		List<FailedResourceDTO> copies = new ArrayList<>();

		for (FailedResourceDTO failedResourceDTO : frDTOs) {
			copies.add(DTOUtil.clone(failedResourceDTO));
		}

		return copies.toArray(new FailedResourceDTO[0]);
	}

	private FailedServletContextDTO[] getFailedServletContextDTO() {
		Collection<ExtendedFailedServletContextDTO> fscDTOs = failedServletContextDTOs.values();

		List<FailedServletContextDTO> copies = new ArrayList<>();

		for (FailedServletContextDTO failedServletContextDTO : fscDTOs) {
			copies.add(DTOUtil.clone(failedServletContextDTO));
		}

		return copies.toArray(new FailedServletContextDTO[0]);
	}

	private FailedServletDTO[] getFailedServletDTOs() {
		Collection<FailedServletDTO> fsDTOs = failedServletDTOs.values();

		List<FailedServletDTO> copies = new ArrayList<>();

		for (FailedServletDTO failedServletDTO : fsDTOs) {
			copies.add(DTOUtil.clone(failedServletDTO));
		}

		return copies.toArray(new FailedServletDTO[0]);
	}

	private FailedPreprocessorDTO[] getFailedPreprocessorDTOs() {
		Collection<FailedPreprocessorDTO> fpDTOs = failedPreprocessorDTOs.values();

		List<FailedPreprocessorDTO> copies = new ArrayList<>();

		for (FailedPreprocessorDTO failedPreprocessorDTO : fpDTOs) {
			copies.add(DTOUtil.clone(failedPreprocessorDTO));
		}

		return copies.toArray(new FailedPreprocessorDTO[0]);
	}

	public ServletContextDTO[] getServletContextDTOs() {
		List<ServletContextDTO> servletContextDTOs = new ArrayList<>();

		for (ContextController contextController : controllerMap.values()) {
			servletContextDTOs.add(contextController.getServletContextDTO());
		}

		return servletContextDTOs.toArray(new ServletContextDTO[0]);
	}

	public PreprocessorDTO[] getPreprocessorDTOs() {
		List<PreprocessorDTO> pDTOs = new ArrayList<>();

		for (PreprocessorRegistration registration : preprocessorMap.values()) {
			pDTOs.add(registration.getD());
		}

		return pDTOs.toArray(new PreprocessorDTO[0]);
	}

	public Map<ServiceReference<Preprocessor>, PreprocessorRegistration> getPreprocessorRegistrations() {
		return preprocessorMap;
	}

	public void registerHttpServiceFilter(
		Bundle bundle, String alias, Filter filter, Dictionary<String, String> initparams, HttpContextHolder httpContextHolder) {

		if (alias == null) {
			throw new IllegalArgumentException("Alias cannot be null"); //$NON-NLS-1$
		}
		if (filter == null) {
			throw new IllegalArgumentException("Filter cannot be null"); //$NON-NLS-1$
		}

		ContextController.checkPattern(alias);

		// need to make sure exact matching aliases are converted to wildcard pattern matches
		if (!alias.endsWith(Const.SLASH_STAR) && !alias.startsWith(Const.STAR_DOT) && !alias.contains(Const.SLASH_STAR_DOT)) {
			if (alias.endsWith(Const.SLASH)) {
				alias = alias + '*';
			} else {
				alias = alias + Const.SLASH_STAR;
			}
		}

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

			HttpServiceObjectRegistration objectRegistration = null;
			ServiceRegistration<Filter> registration = null;
			try {
				Dictionary<String, Object> props = new Hashtable<>();
				props.put(HTTP_WHITEBOARD_TARGET, targetFilter);
				props.put(HTTP_WHITEBOARD_FILTER_PATTERN, alias);
				props.put(HTTP_WHITEBOARD_FILTER_NAME, filterName);
				props.put(HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + Const.EQUINOX_LEGACY_CONTEXT_HELPER + "=true)"); //$NON-NLS-1$ //$NON-NLS-2$
				props.put(Const.EQUINOX_LEGACY_CONTEXT_SELECT, getFilter(httpContextHolder.getServiceReference()));
				props.put(Const.EQUINOX_LEGACY_TCCL_PROP, Thread.currentThread().getContextClassLoader());
				props.put(Constants.SERVICE_RANKING, findFilterPriority(initparams));
				fillInitParams(props, initparams, HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX);

				LegacyFilterFactory filterFactory = new LegacyFilterFactory(filter);

				registration = bundle.getBundleContext().registerService(Filter.class, filterFactory, props);

				// check that init got called and did not throw an exception
				filterFactory.checkForError();

				objectRegistration = new HttpServiceObjectRegistration(filter, registration, httpContextHolder, bundle);
				Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
				if (objectRegistrations == null) {
					objectRegistrations = new HashSet<>();
					bundleRegistrations.put(bundle, objectRegistrations);
				}
				objectRegistrations.add(objectRegistration);
				legacyMappings.put(objectRegistration.serviceKey, objectRegistration);
			} finally {
				if (objectRegistration == null || !legacyMappings.containsKey(objectRegistration.serviceKey)) {
					// something bad happened above (likely going to throw a runtime exception)
					decrementFactoryUseCount(httpContextHolder);
					if (registration != null) {
						registration.unregister();
					}
				}
			}
		}
	}

	private void fillInitParams(
		Dictionary<String, Object> props,
		Dictionary<?, ?> initparams, String prefix) {
		if (initparams != null) {
			for (Enumeration<?> eKeys = initparams.keys(); eKeys.hasMoreElements();) {
				String key = String.valueOf(eKeys.nextElement());
				String value = String.valueOf(initparams.get(key));
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
			"filter-priority must be an integer between -1000 and 1000 but " + //$NON-NLS-1$
				"was: " + filterPriority); //$NON-NLS-1$
	}

	public void registerHttpServiceResources(
		Bundle bundle, String alias, String name, HttpContextHolder httpContextHolder) throws NamespaceException {
		if (alias == null) {
			throw new IllegalArgumentException("Alias cannot be null"); //$NON-NLS-1$
		}
		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null"); //$NON-NLS-1$
		}
		String pattern = alias;
		if (pattern.startsWith(Const.SLASH_STAR_DOT)) {
			pattern = pattern.substring(1);
		}
		// need to make sure exact matching aliases are converted to wildcard pattern matches
		if (!pattern.endsWith(Const.SLASH_STAR) && !pattern.startsWith(Const.STAR_DOT) && !pattern.contains(Const.SLASH_STAR_DOT)) {
			if (pattern.endsWith(Const.SLASH)) {
				pattern = pattern + '*';
			} else {
				pattern = pattern + Const.SLASH_STAR;
			}
		}
		// check the pattern against the original input
		ContextController.checkPattern(alias);

		synchronized (legacyMappings) {
			HttpServiceObjectRegistration objectRegistration = null;
			ServiceRegistration<?> registration = null;
			try {
				String fullAlias = getFullAlias(alias, httpContextHolder);
				HttpServiceObjectRegistration existing = legacyMappings.get(fullAlias);
				if (existing != null) {
					throw new PatternInUseException(alias);
				}
				Dictionary<String, Object> props = new Hashtable<>();
				props.put(HTTP_WHITEBOARD_TARGET, targetFilter);
				props.put(HTTP_WHITEBOARD_RESOURCE_PATTERN, pattern);
				props.put(HTTP_WHITEBOARD_RESOURCE_PREFIX, name);
				props.put(HTTP_WHITEBOARD_CONTEXT_SELECT, getFilter(httpContextHolder.getServiceReference()));
				props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
				props.put(Const.EQUINOX_LEGACY_TCCL_PROP, Thread.currentThread().getContextClassLoader());
				registration = bundle.getBundleContext().registerService(String.class, "resource", props); //$NON-NLS-1$

				objectRegistration = new HttpServiceObjectRegistration(fullAlias, registration, httpContextHolder, bundle);

				Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
				if (objectRegistrations == null) {
					objectRegistrations = new HashSet<>();
					bundleRegistrations.put(bundle, objectRegistrations);
				}
				objectRegistrations.add(objectRegistration);

				Map<String, String> aliasCustomizations = bundleAliasCustomizations.get(bundle);
				if (aliasCustomizations == null) {
					aliasCustomizations = new HashMap<>();
					bundleAliasCustomizations.put(bundle, aliasCustomizations);
				}
				aliasCustomizations.put(alias, fullAlias);
				legacyMappings.put(objectRegistration.serviceKey, objectRegistration);
			} finally {
				if (objectRegistration == null || !legacyMappings.containsKey(objectRegistration.serviceKey)) {
					// something bad happened above (likely going to throw a runtime exception)
					// need to clean up the factory reference
					decrementFactoryUseCount(httpContextHolder);
					if (registration != null) {
						registration.unregister();
					}
				}
			}
		}
	}

	private Object getFilter(ServiceReference<? extends ServletContextHelper> serviceReference) {
		String ctxName = (String)serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_NAME);
		return String.format("(&(%s=%s)(%s=%s))", HTTP_SERVICE_CONTEXT_PROPERTY, ctxName, HTTP_WHITEBOARD_CONTEXT_NAME, ctxName); //$NON-NLS-1$
	}

	public void registerHttpServiceServlet(
		Bundle bundle, String alias, Servlet servlet, Dictionary<?, ?> initparams, HttpContextHolder httpContextHolder) throws NamespaceException, ServletException{
		if (alias == null) {
			throw new IllegalArgumentException("Alias cannot be null"); //$NON-NLS-1$
		}
		if (servlet == null) {
			throw new IllegalArgumentException("Servlet cannot be null"); //$NON-NLS-1$
		}

		// check the pattern against the original input
		ContextController.checkPattern(alias);

		Object pattern = alias;
		// need to make sure exact matching aliases are converted to exact matching + wildcard pattern matching
		if (!alias.endsWith(Const.SLASH_STAR) && !alias.startsWith(Const.STAR_DOT) && !alias.contains(Const.SLASH_STAR_DOT)) {
			if (alias.endsWith(Const.SLASH)) {
				pattern = new String[] {alias, alias + '*'};
			} else {
				pattern = new String[] {alias, alias + Const.SLASH_STAR};
			}
		}

		synchronized (legacyMappings) {
			LegacyServlet legacyServlet = new LegacyServlet(servlet);
			if (getRegisteredObjects().contains(legacyServlet)) {
				throw new ServletAlreadyRegisteredException(servlet);
			}
			HttpServiceObjectRegistration objectRegistration = null;
			ServiceRegistration<Servlet> registration = null;
			try {
				String fullAlias = getFullAlias(alias, httpContextHolder);
				HttpServiceObjectRegistration existing = legacyMappings.get(fullAlias);
				if (existing != null) {
					throw new PatternInUseException(alias);
				}
				String servletName = servlet.getClass().getName();
				if ((initparams != null) && (initparams.get(Const.SERVLET_NAME) != null)) {
					servletName = String.valueOf(initparams.get(Const.SERVLET_NAME));
				}

				Dictionary<String, Object> props = new Hashtable<>();
				props.put(HTTP_WHITEBOARD_TARGET, targetFilter);
				props.put(HTTP_WHITEBOARD_SERVLET_PATTERN, pattern);
				props.put(HTTP_WHITEBOARD_SERVLET_NAME, servletName);
				props.put(HTTP_WHITEBOARD_CONTEXT_SELECT, getFilter(httpContextHolder.getServiceReference()));
				props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
				props.put(Const.EQUINOX_LEGACY_TCCL_PROP, Thread.currentThread().getContextClassLoader());
				fillInitParams(props, initparams, HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX);

				registration = bundle.getBundleContext().registerService(Servlet.class, legacyServlet, props);

				// check that init got called and did not throw an exception
				legacyServlet.checkForError();

				objectRegistration = new HttpServiceObjectRegistration(fullAlias, registration, httpContextHolder, bundle);

				Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.get(bundle);
				if (objectRegistrations == null) {
					objectRegistrations = new HashSet<>();
					bundleRegistrations.put(bundle, objectRegistrations);
				}
				objectRegistrations.add(objectRegistration);

				Map<String, String> aliasCustomizations = bundleAliasCustomizations.get(bundle);
				if (aliasCustomizations == null) {
					aliasCustomizations = new HashMap<>();
					bundleAliasCustomizations.put(bundle, aliasCustomizations);
				}
				aliasCustomizations.put(alias, fullAlias);

				legacyMappings.put(objectRegistration.serviceKey, objectRegistration);
			} finally {
				if (objectRegistration == null || !legacyMappings.containsKey(objectRegistration.serviceKey)) {
					// something bad happened above (likely going to throw a runtime exception)
					// need to clean up the factory reference
					decrementFactoryUseCount(httpContextHolder);
					if (registration != null) {
						registration.unregister();
					}
				}
			}
		}
	}

	private String getFullAlias(String alias, HttpContextHolder httpContextHolder) {
		@SuppressWarnings("unchecked")
		AtomicReference<ContextController> controllerRef = contextServiceTracker.getService((ServiceReference<ServletContextHelper>)httpContextHolder.getServiceReference());
		if (controllerRef != null) {
			ContextController controller = controllerRef.get();
			if (controller != null) {
				return controller.getContextPath() + alias;
			}
		}
		return alias;
	}

	public void unregisterHttpServiceAlias(Bundle bundle, String alias) {
		synchronized (legacyMappings) {
			Map<String, String> aliasCustomizations = bundleAliasCustomizations.get(bundle);
			String aliasCustomization = aliasCustomizations == null ? null : aliasCustomizations.remove(alias);
			if (aliasCustomization == null) {
				throw new IllegalArgumentException("The bundle did not register the alias: " + alias); //$NON-NLS-1$
			}
			HttpServiceObjectRegistration objectRegistration = legacyMappings.get(aliasCustomization);
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
			decrementFactoryUseCount(objectRegistration.httpContextHolder);
			legacyMappings.remove(aliasCustomization);

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
			decrementFactoryUseCount(objectRegistration.httpContextHolder);
			legacyMappings.remove(filter);
		}
	}

	public void unregisterHttpServiceObjects(Bundle bundle) {
		synchronized (legacyMappings) {
			bundleAliasCustomizations.remove(bundle);
			Set<HttpServiceObjectRegistration> objectRegistrations = bundleRegistrations.remove(bundle);
			if (objectRegistrations != null) {
				for (HttpServiceObjectRegistration objectRegistration : objectRegistrations) {
					try {
						objectRegistration.registration.unregister();
					} catch (IllegalStateException e) {
						// ignore; already unregistered
					}
					decrementFactoryUseCount(objectRegistration.httpContextHolder);
					legacyMappings.remove(objectRegistration.serviceKey);
				}
			}
		}
	}

	private void decrementFactoryUseCount(HttpContextHolder holder) {
		synchronized (legacyContextMap) {
			if (holder.decrementUseCount() == 0) {
				legacyContextMap.remove(holder.getHttpContext());
			}
		}
	}

	private static org.osgi.framework.Filter createErrorPageFilter(BundleContext context) {
		StringBuilder sb = new StringBuilder();

		sb.append("("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_SERVLET_ERROR_PAGE);
		sb.append("=*)"); //$NON-NLS-1$

		try {
			return context.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	private static org.osgi.framework.Filter createResourceFilter(BundleContext context) {
		StringBuilder sb = new StringBuilder();

		sb.append("(&("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_RESOURCE_PREFIX);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_RESOURCE_PATTERN);
		sb.append("=*))"); //$NON-NLS-1$

		try {
			return context.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	private static org.osgi.framework.Filter createServletFilter(BundleContext context) {
		StringBuilder sb = new StringBuilder();

		sb.append("(&(objectClass="); //$NON-NLS-1$
		sb.append(Servlet.class.getName());
		sb.append(")(|("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_SERVLET_NAME);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_SERVLET_PATTERN);
		sb.append("=*)))"); //$NON-NLS-1$

		try {
			return context.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	private static org.osgi.framework.Filter createFilterFilter(BundleContext context) {
		StringBuilder sb = new StringBuilder();

		sb.append("(&(objectClass="); //$NON-NLS-1$
		sb.append(Filter.class.getName());
		sb.append(")(|("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_FILTER_PATTERN);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_FILTER_REGEX);
		sb.append("=*)("); //$NON-NLS-1$
		sb.append(HTTP_WHITEBOARD_FILTER_SERVLET);
		sb.append("=*)))"); //$NON-NLS-1$

		try {
			return context.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	private static org.osgi.framework.Filter createListenerFilter(BundleContext context, ServletContext servletContext) {
		StringBuilder sb = new StringBuilder();

		sb.append("(&"); //$NON-NLS-1$
		sb.append("(").append(HTTP_WHITEBOARD_LISTENER).append("=*)"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("(|"); //$NON-NLS-1$
		sb.append("(objectClass=").append(ServletContextListener.class.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("(objectClass=").append(ServletContextAttributeListener.class.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("(objectClass=").append(ServletRequestListener.class.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("(objectClass=").append(ServletRequestAttributeListener.class.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("(objectClass=").append(HttpSessionListener.class.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("(objectClass=").append(HttpSessionAttributeListener.class.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("(objectClass=").append(HttpSessionIdListener.class.getName()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(")"); //$NON-NLS-1$
		sb.append(")"); //$NON-NLS-1$

		try {
			return context.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			throw new IllegalArgumentException(ise);
		}
	}

	public org.osgi.framework.Filter getListenerFilter() {
		return listenerServiceFilter;
	}

	public org.osgi.framework.Filter getErrorPageFilter() {
		return errorPageServiceFilter;
	}

	public org.osgi.framework.Filter getFilterFilter() {
		return filterServiceFilter;
	}

	public org.osgi.framework.Filter getServletFilter() {
		return servletServiceFilter;
	}

	public org.osgi.framework.Filter getResourceFilter() {
		return resourceServiceFilter;
	}

	public void recordFailedErrorPageDTO(
		ServiceReference<?> serviceReference,
		FailedErrorPageDTO failedErrorPageDTO) {

		if (failedErrorPageDTOs.containsKey(serviceReference)) {
			return;
		}

		failedErrorPageDTOs.put(serviceReference, failedErrorPageDTO);
	}

	public void recordFailedFilterDTO(
		ServiceReference<Filter> serviceReference,
		FailedFilterDTO failedFilterDTO) {

		if (failedFilterDTOs.containsKey(serviceReference)) {
			return;
		}

		failedFilterDTOs.put(serviceReference, failedFilterDTO);
	}

	public void recordFailedListenerDTO(
		ServiceReference<EventListener> serviceReference,
		FailedListenerDTO failedListenerDTO) {

		if (failedListenerDTOs.containsKey(serviceReference)) {
			return;
		}

		failedListenerDTOs.put(serviceReference, failedListenerDTO);
	}

	public void recordFailedResourceDTO(
		ServiceReference<?> serviceReference, FailedResourceDTO failedResourceDTO) {

		if (failedResourceDTOs.containsKey(serviceReference)) {
			return;
		}

		failedResourceDTOs.put(serviceReference, failedResourceDTO);
	}

	public void recordFailedServletContextDTO(
		ServiceReference<ServletContextHelper> serviceReference, long shadowingServiceId, int failureReason) {

		ExtendedFailedServletContextDTO failedServletContextDTO = new ExtendedFailedServletContextDTO();

		failedServletContextDTO.attributes = Collections.emptyMap();
		failedServletContextDTO.contextPath = String.valueOf(serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_PATH));
		failedServletContextDTO.errorPageDTOs = new ExtendedErrorPageDTO[0];
		failedServletContextDTO.failureReason = failureReason;
		failedServletContextDTO.filterDTOs = new FilterDTO[0];
		failedServletContextDTO.initParams = ServiceProperties.parseInitParams(
			serviceReference, HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX);
		failedServletContextDTO.listenerDTOs = new ListenerDTO[0];
		failedServletContextDTO.name = String.valueOf(serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_NAME));
		failedServletContextDTO.resourceDTOs = new ResourceDTO[0];
		failedServletContextDTO.serviceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
		failedServletContextDTO.servletDTOs = new ServletDTO[0];
		failedServletContextDTO.shadowingServiceId = shadowingServiceId;

		failedServletContextDTOs.put(serviceReference, failedServletContextDTO);
	}

	public void recordFailedServletDTO(
		ServiceReference<?> serviceReference,
		FailedServletDTO failedServletDTO) {

		if (failedServletDTOs.containsKey(serviceReference)) {
			return;
		}

		failedServletDTOs.put(serviceReference, failedServletDTO);
	}

	public void recordFailedPreprocessorDTO(
		ServiceReference<Preprocessor> serviceReference,
		FailedPreprocessorDTO failedPreprocessorDTO) {

		if (failedPreprocessorDTOs.containsKey(serviceReference)) {
			return;
		}

		failedPreprocessorDTOs.put(serviceReference, failedPreprocessorDTO);
	}

	public void removeFailedErrorPageDTO(
		ServiceReference<Servlet> serviceReference) {

		failedErrorPageDTOs.remove(serviceReference);
	}

	public void removeFailedFilterDTO(
		ServiceReference<Filter> serviceReference) {

		failedFilterDTOs.remove(serviceReference);
	}

	public void removeFailedListenerDTO(
		ServiceReference<EventListener> serviceReference) {

		failedListenerDTOs.remove(serviceReference);
	}

	public void removeFailedResourceDTO(
		ServiceReference<Object> serviceReference) {

		failedResourceDTOs.remove(serviceReference);
	}

	public void removeFailedServletDTO(
		ServiceReference<Servlet> serviceReference) {

		failedServletDTOs.remove(serviceReference);
	}

	public void removeFailedPreprocessorDTO(
		ServiceReference<Preprocessor> serviceReference) {

		failedPreprocessorDTOs.remove(serviceReference);
	}

	public synchronized void fireSessionIdChanged(String oldSessionId) {
		for (ContextController contextController : controllerMap.values()) {
			contextController.fireSessionIdChanged(oldSessionId);
		}
	}

	public void sessionDestroyed(String sessionId) {
		httpSessionTracker.invalidate(sessionId, false);
	}

	public void setHsrRegistration(ServiceRegistration<HttpServiceRuntime> hsrRegistration) {
		this.hsrRegistration.set(hsrRegistration);
	}

	ServiceRegistration<HttpServiceRuntime> getHsrRegistration() {
		return hsrRegistration.get();
	}

	long getServiceChangecount() {
		return serviceChangecount.get();
	}

	public void incrementServiceChangecount() {
		serviceChangecount.incrementAndGet();
		if (hsrRegistration.get() != null && !scheduledExecutor.isShutdown() && semaphore.tryAcquire()) {
			scheduledExecutor.schedule(new ChangeCountTimer(), 100, TimeUnit.MILLISECONDS);
		}
	}

	Semaphore getSemaphore() {
		return semaphore;
	}

	private String decode(String urlEncoded) {
		try {
			return URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8.name());
		}
		catch (UnsupportedEncodingException e) {
			return urlEncoded;
		}
	}

	private final Map<String, Object> attributes;
	private final String targetFilter;
	final ServiceRegistration<ServletContextHelper> defaultContextReg;
	private final ServletContext parentServletContext;
	private final BundleContext trackingContext;
	private final BundleContext consumingContext;
	private final org.osgi.framework.Filter errorPageServiceFilter;
	private final org.osgi.framework.Filter servletServiceFilter;
	private final org.osgi.framework.Filter resourceServiceFilter;
	private final org.osgi.framework.Filter filterServiceFilter;
	private final org.osgi.framework.Filter listenerServiceFilter;

	// BEGIN of old HttpService support
	final ConcurrentMap<HttpContext, HttpContextHolder> legacyContextMap =
		new ConcurrentHashMap<>();
	private final Map<Object, HttpServiceObjectRegistration> legacyMappings =
		Collections.synchronizedMap(new HashMap<Object, HttpServiceObjectRegistration>());
	private final Map<Bundle, Set<HttpServiceObjectRegistration>> bundleRegistrations =
		new HashMap<>();
	private final Map<Bundle, Map<String, String>> bundleAliasCustomizations = new HashMap<>();
	// END of old HttpService support

	private final ConcurrentMap<ServiceReference<ServletContextHelper>, ContextController> controllerMap =
		new ConcurrentSkipListMap<>(Collections.reverseOrder());
	private final ConcurrentMap<ServiceReference<Preprocessor>, PreprocessorRegistration> preprocessorMap =
		new ConcurrentSkipListMap<>(Collections.reverseOrder());

	final ConcurrentMap<ServiceReference<?>, FailedErrorPageDTO> failedErrorPageDTOs =
		new ConcurrentHashMap<>();
	final ConcurrentMap<ServiceReference<Filter>, FailedFilterDTO> failedFilterDTOs =
		new ConcurrentHashMap<>();
	final ConcurrentMap<ServiceReference<EventListener>, FailedListenerDTO> failedListenerDTOs =
		new ConcurrentHashMap<>();
	final ConcurrentMap<ServiceReference<?>, FailedResourceDTO> failedResourceDTOs =
		new ConcurrentHashMap<>();
	final ConcurrentMap<ServiceReference<ServletContextHelper>, ExtendedFailedServletContextDTO> failedServletContextDTOs =
		new ConcurrentHashMap<>();
	final ConcurrentMap<ServiceReference<?>, FailedServletDTO> failedServletDTOs =
		new ConcurrentHashMap<>();
	final ConcurrentMap<ServiceReference<?>, FailedPreprocessorDTO> failedPreprocessorDTOs =
		new ConcurrentHashMap<>();

	private final Set<Object> registeredObjects = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
	private final ServiceTracker<LoggerFactory, Logger> loggerFactoryTracker;
	private final ServiceTracker<ServletContextHelper, AtomicReference<ContextController>> contextServiceTracker;
	private final ServiceTracker<Preprocessor, AtomicReference<PreprocessorRegistration>> preprocessorServiceTracker;
	private final ServiceTracker<ContextPathCustomizer, ContextPathCustomizer> contextPathAdaptorTracker;
	private final ContextPathCustomizerHolder contextPathCustomizerHolder;
	private final HttpSessionTracker httpSessionTracker;
	private final ServiceRegistration<HttpSessionInvalidator> invalidatorReg;
	private final AtomicReference<ServiceRegistration<HttpServiceRuntime>> hsrRegistration = new AtomicReference<>();

	private final AtomicLong serviceChangecount = new AtomicLong();
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	private final Semaphore semaphore = new Semaphore(1);

	class ChangeCountTimer implements Callable<Void> {
		@Override
		public Void call() {
			try {
				Dictionary<String,Object> properties = getHsrRegistration().getReference().getProperties();
				properties.put(Constants.SERVICE_CHANGECOUNT, getServiceChangecount());
				getHsrRegistration().setProperties(properties);
				return null;
			}
			finally {
				getSemaphore().release();
			}
		}
	}

	static class LegacyServiceObject {
		final AtomicReference<Exception> error = new AtomicReference<>(new ServletException("The init() method was never called.")); //$NON-NLS-1$
		public void checkForError() {
			Exception result = error.get();
			if (result != null) {
				Throw.unchecked(result);
			}
		}
	}

	public static class LegacyFilterFactory extends LegacyServiceObject implements PrototypeServiceFactory<Filter> {
		final Filter filter;

		public LegacyFilterFactory(Filter filter) {
			this.filter = filter;
		}

		@Override
		public Filter getService(Bundle bundle, ServiceRegistration<Filter> registration) {
			return new LegacyFilter();
		}

		@Override
		public void ungetService(
			Bundle bundle, ServiceRegistration<Filter> registration, Filter service) {
			// do nothing
		}

		// NOTE we do not do the same equals check here for filter that we do for servlet
		// this is because we must allow filter to be applied to all context helpers
		// TODO this means it is still possible that init() will get called if the same filter
		// is registered multiple times.  This is unfortunate but is an error case on the client anyway.
		class LegacyFilter implements Filter {
			/**
			 * @throws ServletException
			 */
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				try {
					filter.init(filterConfig);
					error.set(null);
				} catch (Exception e){
					error.set(e);
					Throw.unchecked(e);
				}
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
		}
	}

	static class LegacyServlet extends LegacyServiceObject implements Servlet {
		final Servlet servlet;

		public LegacyServlet(Servlet servlet) {
			this.servlet = servlet;
		}

		/**
		 * @throws ServletException
		 */
		@Override
		public void init(ServletConfig config)
			throws ServletException {
			try {
				servlet.init(config);
				error.set(null);
			} catch (Exception e){
				error.set(e);
				Throw.unchecked(e);
			}
		}

		@Override
		public ServletConfig getServletConfig() {
			return servlet.getServletConfig();
		}

		@Override
		public void
			service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
			servlet.service(req, res);
		}

		@Override
		public String getServletInfo() {
			return servlet.getServletInfo();
		}

		@Override
		public void destroy() {
			servlet.destroy();
		}

		@Override
		public int hashCode() {
			return servlet.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof LegacyServlet) {
				other = ((LegacyServlet) other).servlet;
			}
			return servlet.equals(other);
		}
	}

	static class ContextPathCustomizerHolder implements ServiceTrackerCustomizer<ContextPathCustomizer, ContextPathCustomizer> {
		private final BundleContext context;
		private final ServiceTracker<ServletContextHelper, AtomicReference<ContextController>> contextServiceTracker;
		private final NavigableMap<ServiceReference<ContextPathCustomizer>, ContextPathCustomizer> pathCustomizers =
			new TreeMap<>(Collections.reverseOrder());

		public ContextPathCustomizerHolder(
			BundleContext context,
			ServiceTracker<ServletContextHelper, AtomicReference<ContextController>> contextServiceTracker) {
			super();
			this.context = context;
			this.contextServiceTracker = contextServiceTracker;
		}

		@Override
		public ContextPathCustomizer addingService(
			ServiceReference<ContextPathCustomizer> reference) {
			ContextPathCustomizer service = context.getService(reference);
			boolean reset = false;
			synchronized (pathCustomizers) {
				pathCustomizers.put(reference, service);
				reset = pathCustomizers.firstKey().equals(reference);
			}
			if (reset) {
				contextServiceTracker.close();
				contextServiceTracker.open();
			}
			return service;
		}

		@Override
		public void modifiedService(
			ServiceReference<ContextPathCustomizer> reference,
			ContextPathCustomizer service) {
			removedService(reference, service);
			addingService(reference);
		}
		@Override
		public void removedService(
			ServiceReference<ContextPathCustomizer> reference,
			ContextPathCustomizer service) {
			boolean reset = false;
			synchronized (pathCustomizers) {
				ServiceReference<ContextPathCustomizer> currentFirst = pathCustomizers.firstKey();
				pathCustomizers.remove(reference);
				reset = currentFirst.equals(reference);
			}

			// only reset if the tracker is still open
			if (reset && contextServiceTracker.getTrackingCount() >= 0) {

				contextServiceTracker.close();
				contextServiceTracker.open();
			}
			context.ungetService(reference);
		}

		ContextPathCustomizer getHighestRanked() {
			synchronized (pathCustomizers) {
				Map.Entry<ServiceReference<ContextPathCustomizer>, ContextPathCustomizer> firstEntry = pathCustomizers.firstEntry();
				return firstEntry == null ? null : firstEntry.getValue();
			}
		}
	}

}
