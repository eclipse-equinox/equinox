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
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletException;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.ContextController.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.registration.ListenerRegistration;
import org.osgi.framework.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ContextListenerTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<EventListener,  AtomicReference<ListenerRegistration>> {

	public ContextListenerTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime);

		this.contextController = contextController;
	}

	@Override
	public AtomicReference<ListenerRegistration> addingService(
		ServiceReference<EventListener> serviceReference) {

		AtomicReference<ListenerRegistration> result = new AtomicReference<ListenerRegistration>();
		if (!httpServiceRuntime.matches(serviceReference)) {
			return result;
		}

		String contextSelector = (String)serviceReference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);

		if (!contextController.matches(contextSelector)) {
			return result;
		}


		ServiceHolder<EventListener> listenerHolder = new ServiceHolder<EventListener>(bundleContext.getServiceObjects(serviceReference));
		Long serviceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
		try {
			result.set(contextController.addListenerRegistration(
				listenerHolder, serviceId.longValue()));
		}
		catch (ServletException se) {
			httpServiceRuntime.log(se.getMessage(), se);
		} finally {
			if (result.get() == null) {
				listenerHolder.release();
			}
		}

		return result;
	}

	@Override
	public void
		modifiedService(
			ServiceReference<EventListener> serviceReference,
			AtomicReference<ListenerRegistration> listenerRegistration) {
		removedService(serviceReference, listenerRegistration);
		addingService(serviceReference);
	}

	@Override
	public void
		removedService(
			ServiceReference<EventListener> serviceReference,
			AtomicReference<ListenerRegistration> listenerReference) {

		ListenerRegistration listenerRegistration = listenerReference.get();
		if (listenerRegistration != null) {
			// Destroy now ungets the object we are using
			listenerRegistration.destroy();
		}
	}

	private ContextController contextController;

}
