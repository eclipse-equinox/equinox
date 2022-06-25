/*******************************************************************************
 * Copyright (c) Feb. 1, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - initial API and implementation and/or initial
 *                    documentation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.customizer;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.*;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedErrorPageDTO;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.eclipse.equinox.http.servlet.internal.registration.ErrorPageRegistration;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.DTOUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * @author Raymond Augé
 */
public class ContextErrorPageTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Servlet, ErrorPageRegistration>{

	public ContextErrorPageTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime, contextController);
	}

	@Override
	public AtomicReference<ErrorPageRegistration>
		addingService(ServiceReference<Servlet> serviceReference) {

		AtomicReference<ErrorPageRegistration> result = new AtomicReference<>();
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
					(((String)serviceReference.getProperty(HTTP_WHITEBOARD_CONTEXT_SELECT))).contains(HTTP_SERVICE_CONTEXT_PROPERTY.concat(Const.EQUAL)) &&
					(serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_PATTERN) != null)) {

				// don't allow whiteboard Servlets that specifically attempt to bind to a legacy context
				throw new HttpWhiteboardFailureException(
					"Whiteboard ErrorPages with pattern cannot bind to legacy contexts. " + serviceReference, DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING); //$NON-NLS-1$
			}

			httpServiceRuntime.removeFailedErrorPageDTO(serviceReference);

			result.set(contextController.addErrorPageRegistration(serviceReference));
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
	void removeFailed(ServiceReference<Servlet> serviceReference) {
		contextController.getHttpServiceRuntime().removeFailedErrorPageDTO(serviceReference);
	}

	void recordFailed(ServiceReference<?> servletReference, int failureReason) {
		ExtendedErrorPageDTO errorPageDTO = DTOUtil.assembleErrorPageDTO(servletReference, contextController.getServiceId(), false);

		contextController.recordFailedErrorPageDTO(servletReference, errorPageDTO, failureReason);
	}

}
