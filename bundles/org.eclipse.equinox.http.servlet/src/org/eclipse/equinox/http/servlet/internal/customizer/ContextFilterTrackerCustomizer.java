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

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.Filter;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;
import org.eclipse.equinox.http.servlet.internal.util.*;
import org.osgi.framework.*;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedFilterDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class ContextFilterTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Filter, FilterRegistration> {

	public ContextFilterTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime, contextController);
	}

	@Override
	public AtomicReference<FilterRegistration> addingService(
		ServiceReference<Filter> serviceReference) {

		AtomicReference<FilterRegistration> result = new AtomicReference<FilterRegistration>();
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

			httpServiceRuntime.removeFailedFilterDTO(serviceReference);

			result.set(contextController.addFilterRegistration(serviceReference));
		}
		catch (HttpWhiteboardFailureException hwfe) {
			httpServiceRuntime.log(hwfe.getMessage(), hwfe);

			recordFailed(serviceReference, hwfe.getFailureReason());
		}
		catch (Exception e) {
			httpServiceRuntime.log(e.getMessage(), e);

			recordFailed(serviceReference, DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		}
		finally {
			httpServiceRuntime.incrementServiceChangecount();
		}

		return result;
	}

	@Override
	void removeFailed(ServiceReference<Filter> serviceReference) {
		contextController.getHttpServiceRuntime().removeFailedFilterDTO(serviceReference);
	}

	private void recordFailed(
		ServiceReference<Filter> serviceReference, int failureReason) {

		FailedFilterDTO failedFilterDTO = new FailedFilterDTO();

		failedFilterDTO.asyncSupported = BooleanPlus.from(
			serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED), false);
		failedFilterDTO.dispatcher = StringPlus.from(
			serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER)).toArray(new String[0]);
		failedFilterDTO.failureReason = failureReason;
		failedFilterDTO.initParams = ServiceProperties.parseInitParams(
			serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX);
		failedFilterDTO.name = (String)serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME);
		failedFilterDTO.patterns = StringPlus.from(
			serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN)).toArray(new String[0]);
		failedFilterDTO.regexs = StringPlus.from(
			serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX)).toArray(new String[0]);
		failedFilterDTO.serviceId = (Long)serviceReference.getProperty(Constants.SERVICE_ID);
		failedFilterDTO.servletContextId = contextController.getServiceId();
		failedFilterDTO.servletNames = StringPlus.from(
			serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET)).toArray(new String[0]);

		contextController.getHttpServiceRuntime().recordFailedFilterDTO(serviceReference, failedFilterDTO);
	}

}
