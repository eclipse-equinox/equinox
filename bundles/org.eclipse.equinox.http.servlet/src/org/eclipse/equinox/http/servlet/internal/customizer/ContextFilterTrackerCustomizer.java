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

package org.eclipse.equinox.http.servlet.internal.customizer;

import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ContextFilterTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Filter, FilterRegistration> {

	public ContextFilterTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime);

		this.contextController = contextController;
	}

	@Override
	public FilterRegistration addingService(
		ServiceReference<Filter> serviceReference) {

		if (!httpServiceRuntime.matches(serviceReference)) {
			// TODO no match runtime

			return null;
		}

		String contextSelector = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);

		if (!contextController.matches(contextSelector)) {
			return null;
		}

		boolean asyncSupported = parseBoolean(
			serviceReference,
			HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);
		List<String> dispatcherList = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER));
		String[] dispatchers = dispatcherList.toArray(
			new String[dispatcherList.size()]);
		Long serviceId = (Long)serviceReference.getProperty(
			Constants.SERVICE_ID);
		Integer filterPriority = (Integer)serviceReference.getProperty(
			Constants.SERVICE_RANKING);
		if (filterPriority == null) {
			filterPriority = Integer.valueOf(0);
		}
		Map<String, String> initParams = parseInitParams(
			serviceReference, "filter.init."); //$NON-NLS-1$
		List<String> patternList = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN));
		String[] patterns = patternList.toArray(new String[patternList.size()]);
		List<String> servletList = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET));
		String[] servlets = servletList.toArray(new String[servletList.size()]);
		List<String> regexList = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX));

		// TODO add this
		String[] regex = regexList.toArray(new String[regexList.size()]);

		Filter filter = bundleContext.getService(serviceReference);
		String name = parseName(serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME), filter);

		try {
			return contextController.addFilterRegistration(
				filter, asyncSupported, dispatchers, filterPriority.intValue(),
				initParams, name, patterns, serviceId.longValue(), servlets);
		}
		catch (ServletException se) {
			httpServiceRuntime.log(se.getMessage(), se);
		}

		// TODO error?

		return null;
	}

	@Override
	public void modifiedService(
		ServiceReference<Filter> serviceReference,
		FilterRegistration filterRegistration) {
	}

	@Override
	public void removedService(
		ServiceReference<Filter> serviceReference,
		FilterRegistration filterRegistration) {

		bundleContext.ungetService(serviceReference);

		filterRegistration.destroy();
	}

	private ContextController contextController;

}
