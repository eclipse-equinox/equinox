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
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.customizer;

import java.util.EventListener;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.eclipse.equinox.http.servlet.internal.registration.ListenerRegistration;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.*;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
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

		try {
			if (!contextController.matches(serviceReference)) {
				// Only the default context will perform the "does anyone match" checks.
				if (httpServiceRuntime.isDefaultContext(contextController) &&
					!httpServiceRuntime.matchesAnyContext(serviceReference)) {

					throw new HttpWhiteboardFailureException(
						"Doesn't match any contexts. " + serviceReference, DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING); //$NON-NLS-1$
				}

				return result;
			}

			httpServiceRuntime.removeFailedListenerDTO(serviceReference);

			String listener = (String)serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);

			if (Boolean.FALSE.toString().equalsIgnoreCase(listener)) {
				return result;
			}

			if (!Boolean.TRUE.toString().equalsIgnoreCase(listener)) {
				throw new HttpWhiteboardFailureException(
					HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER + "=" + listener + " is not a valid option. Ignoring!", //$NON-NLS-1$ //$NON-NLS-2$
					DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}

			result.set(contextController.addListenerRegistration(serviceReference));
		}
		catch (HttpWhiteboardFailureException hwfe) {
			httpServiceRuntime.log(hwfe.getMessage(), hwfe);

			recordFailedListenerDTO(serviceReference, hwfe.getFailureReason());
		}
		catch (Exception e) {
			httpServiceRuntime.log(e.getMessage(), e);

			recordFailedListenerDTO(serviceReference, DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		}

		return result;
	}

	@Override
	public void modifiedService(
		ServiceReference<EventListener> serviceReference,
		AtomicReference<ListenerRegistration> listenerRegistration) {

		removedService(serviceReference, listenerRegistration);
		addingService(serviceReference);
	}

	@Override
	public void removedService(
		ServiceReference<EventListener> serviceReference,
		AtomicReference<ListenerRegistration> listenerReference) {

		ListenerRegistration listenerRegistration = listenerReference.get();
		if (listenerRegistration != null) {
			// Destroy now ungets the object we are using
			listenerRegistration.destroy();
		}

		contextController.getHttpServiceRuntime().removeFailedListenerDTO(serviceReference);
	}

	private void recordFailedListenerDTO(
		ServiceReference<EventListener> serviceReference, int failureReason) {

		FailedListenerDTO failedListenerDTO = new FailedListenerDTO();

		failedListenerDTO.failureReason = failureReason;
		failedListenerDTO.serviceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
		failedListenerDTO.servletContextId = contextController.getServiceId();
		failedListenerDTO.types = StringPlus.from(
			serviceReference.getProperty(Constants.OBJECTCLASS)).toArray(new String[0]);

		contextController.getHttpServiceRuntime().recordFailedListenerDTO(serviceReference, failedListenerDTO);
	}

	private ContextController contextController;

}
