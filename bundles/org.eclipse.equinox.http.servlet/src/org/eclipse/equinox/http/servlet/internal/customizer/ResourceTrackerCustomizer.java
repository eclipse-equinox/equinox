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

import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ResourceTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Servlet, Servlet> {

	public ResourceTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime) {

		super(bundleContext, httpServiceRuntime);
	}

	@Override
	public Servlet addingService(ServiceReference<Servlet> serviceReference) {
		if (!httpServiceRuntime.matches(serviceReference)) {
			// TODO no match runtime

			return null;
		}

		String contextSelector = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);

		ContextController contextController =
			httpServiceRuntime.getOrAddContextController(
				contextSelector,
				serviceReference.getBundle().getBundleContext());

		if (contextController == null) {
			// TODO no match context

			return null;
		}

		return bundleContext.getService(serviceReference);
	}

	@Override
	public void modifiedService(
		ServiceReference<Servlet> serviceReference, Servlet servlet) {
	}

	@Override
	public void removedService(
		ServiceReference<Servlet> serviceReference, Servlet servlet) {

		bundleContext.ungetService(serviceReference);
	}

}
