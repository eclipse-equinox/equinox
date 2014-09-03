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

import java.util.EventListener;
import javax.servlet.ServletException;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.registration.ListenerRegistration;
import org.osgi.framework.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ContextListenerTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<EventListener, ListenerRegistration> {

	public ContextListenerTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime);

		this.contextController = contextController;
	}

	@Override
	public ListenerRegistration addingService(
		ServiceReference<EventListener> serviceReference) {

		if (!httpServiceRuntime.matches(serviceReference)) {
			// TODO no match runtime

			return null;
		}

		String contextSelector = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);

		if (!contextController.matches(contextSelector)) {
			return null;
		}

		Long serviceId = (Long)serviceReference.getProperty(
			Constants.SERVICE_ID);
		EventListener eventListener = bundleContext.getService(
			serviceReference);

		try {
			return contextController.addListenerRegistration(
				eventListener, serviceId.longValue());
		}
		catch (ServletException se) {
			httpServiceRuntime.log(se.getMessage(), se);
		}

		return null;
	}

	@Override
	public void
		modifiedService(
			ServiceReference<EventListener> serviceReference,
			ListenerRegistration listenerRegistration) {
	}

	@Override
	public void
		removedService(
			ServiceReference<EventListener> serviceReference,
			ListenerRegistration listenerRegistration) {

		bundleContext.ungetService(serviceReference);

		listenerRegistration.destroy();
	}

	private ContextController contextController;

}
