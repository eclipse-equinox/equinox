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
	extends RegistrationServiceTrackerCustomizer<EventListener,  ListenerRegistration> {

	public ContextListenerTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime, contextController);
	}

	@Override
	public AtomicReference<ListenerRegistration> addingService(
		ServiceReference<EventListener> serviceReference) {

		AtomicReference<ListenerRegistration> result = new AtomicReference<ListenerRegistration>();
		if (!httpServiceRuntime.matches(serviceReference)) {
			return result;
		}

		try {
			contextController.checkShutdown();

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

			Object listenerObj = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);

			if (!(listenerObj instanceof Boolean) && !(listenerObj instanceof String)) {
				throw new HttpWhiteboardFailureException(
					HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER + "=" + listenerObj + " is not a valid option. Ignoring!", //$NON-NLS-1$ //$NON-NLS-2$
					DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}

			if (Boolean.FALSE.equals(listenerObj) || !Boolean.valueOf(String.valueOf(listenerObj)).booleanValue()) {
				// Asks to be ignored.
				return result;
			}

			result.set(contextController.addListenerRegistration(serviceReference));
		}
		catch (HttpWhiteboardFailureException hwfe) {
			httpServiceRuntime.debug(hwfe.getMessage(), hwfe);

			recordFailed(serviceReference, hwfe.getFailureReason());
		}
		catch (Throwable t) {
			httpServiceRuntime.error(t.getMessage(), t);

			recordFailed(serviceReference, DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		}
		finally {
			httpServiceRuntime.incrementServiceChangecount();
		}

		return result;
	}

	@Override
	void removeFailed(ServiceReference<EventListener> serviceReference) {
		contextController.getHttpServiceRuntime().removeFailedListenerDTO(serviceReference);
	}

	private void recordFailed(
		ServiceReference<EventListener> serviceReference, int failureReason) {

		FailedListenerDTO failedListenerDTO = new FailedListenerDTO();

		failedListenerDTO.failureReason = failureReason;
		failedListenerDTO.serviceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
		failedListenerDTO.servletContextId = contextController.getServiceId();
		failedListenerDTO.types = StringPlus.from(
			serviceReference.getProperty(Constants.OBJECTCLASS)).toArray(new String[0]);

		contextController.getHttpServiceRuntime().recordFailedListenerDTO(serviceReference, failedListenerDTO);
	}

}
