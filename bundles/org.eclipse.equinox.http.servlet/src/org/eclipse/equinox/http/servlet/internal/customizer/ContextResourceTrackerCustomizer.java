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

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;

import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.eclipse.equinox.http.servlet.internal.registration.ResourceRegistration;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.StringPlus;
import org.osgi.framework.*;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedResourceDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ContextResourceTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Object, ResourceRegistration> {

	public ContextResourceTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime, contextController);
	}

	@Override
	public AtomicReference<ResourceRegistration> addingService(
		ServiceReference<Object> serviceReference) {

		AtomicReference<ResourceRegistration> result = new AtomicReference<ResourceRegistration>();
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
			else if (contextController.isLegacyContext() &&
					(serviceReference.getProperty(Const.EQUINOX_LEGACY_TCCL_PROP) == null) &&  // IS a whiteboard service
					(serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_SELECT) != null) &&
					(((String)serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_SELECT))).contains(HTTP_SERVICE_CONTEXT_PROPERTY.concat(Const.EQUAL))) {

				// don't allow whiteboard Servlets that specifically attempt to bind to a legacy context
				throw new HttpWhiteboardFailureException(
					"Whiteboard resources cannot bind to legacy contexts. " + serviceReference, DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING); //$NON-NLS-1$
			}

			httpServiceRuntime.removeFailedResourceDTO(serviceReference);

			result.set(contextController.addResourceRegistration(serviceReference));
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
	void removeFailed(ServiceReference<Object> serviceReference) {
		contextController.getHttpServiceRuntime().removeFailedResourceDTO(serviceReference);
	}

	private void recordFailed(
		ServiceReference<Object> serviceReference, int failureReason) {

		FailedResourceDTO failedResourceDTO = new FailedResourceDTO();

		failedResourceDTO.failureReason = failureReason;
		failedResourceDTO.patterns = StringPlus.from(
			serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN)).toArray(new String[0]);
		failedResourceDTO.prefix = String.valueOf(serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX));
		failedResourceDTO.serviceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
		failedResourceDTO.servletContextId = contextController.getServiceId();

		contextController.getHttpServiceRuntime().recordFailedResourceDTO(serviceReference, failedResourceDTO);
	}

}
