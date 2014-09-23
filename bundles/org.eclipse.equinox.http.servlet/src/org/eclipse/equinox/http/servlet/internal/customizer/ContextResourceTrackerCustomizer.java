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
import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.registration.ResourceRegistration;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ContextResourceTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Servlet, ResourceRegistration> {

	public ContextResourceTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime);

		this.contextController = contextController;
	}

	@Override
	public ResourceRegistration addingService(
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

		List<String> patternList = StringPlus.from(
			serviceReference.getProperty(
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN));
		String[] patterns = patternList.toArray(new String[patternList.size()]);
		Long serviceId = (Long)serviceReference.getProperty(
			Constants.SERVICE_ID);
		String prefix = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);

		return contextController.addResourceRegistration(
			patterns, prefix, serviceId.longValue(), false);
	}

	@Override
	public void modifiedService(
		ServiceReference<Servlet> serviceReference,
		ResourceRegistration resourceRegistration) {
	}

	@Override
	public void removedService(
		ServiceReference<Servlet> serviceReference,
		ResourceRegistration resourceRegistration) {

		bundleContext.ungetService(serviceReference);

		resourceRegistration.destroy();
	}

	private ContextController contextController;

}
