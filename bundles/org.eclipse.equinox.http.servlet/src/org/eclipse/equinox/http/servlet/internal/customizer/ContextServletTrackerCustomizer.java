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
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ContextServletTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Servlet, ServletRegistration> {

	public ContextServletTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime);

		this.contextController = contextController;
	}

	@Override
	public ServletRegistration addingService(
		ServiceReference<Servlet> serviceReference) {

		if (!httpServiceRuntime.matches(serviceReference)) {
			// TODO no match runtime

			return null;
		}

		String contextSelector = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);

		if (!contextController.matches(contextSelector)) {
			// TODO no match context

			return null;
		}

		Servlet servlet = bundleContext.getService(serviceReference);

		if (httpServiceRuntime.getRegisteredServlets().contains(servlet)) {
			return null;
		}

		boolean asyncSupported = parseBoolean(
			serviceReference,
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);
		List<String> errorPageList = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE));
		String[] errorPages = errorPageList.toArray(
			new String[errorPageList.size()]);
		Map<String, String> initParams = parseInitParams(
			serviceReference, "servlet.init.");
		List<String> patternList = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN));
		String[] patterns = patternList.toArray(new String[patternList.size()]);
		Long serviceId = (Long)serviceReference.getProperty(
			Constants.SERVICE_ID);

		String servletName = parseName(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME), servlet);

		try {
			return contextController.addServletRegistration(
				servlet, asyncSupported, errorPages, initParams, patterns,
				serviceId.longValue(), servletName, false);
		}
		catch (ServletException se) {
			httpServiceRuntime.log(se.getMessage(), se);
		}

		// TODO error?

		return null;
	}

	@Override
	public void modifiedService(
		ServiceReference<Servlet> serviceReference,
		ServletRegistration servletRegistration) {
	}

	@Override
	public void removedService(
		ServiceReference<Servlet> serviceReference,
		ServletRegistration servletRegistration) {

		bundleContext.ungetService(serviceReference);

		servletRegistration.destroy();
	}

	private ContextController contextController;

}
