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

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.*;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.eclipse.equinox.http.servlet.internal.util.Const;
import org.eclipse.equinox.http.servlet.internal.util.DTOUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.ServletDTO;

/**
 * @author Raymond Augé
 */
public class ContextServletTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Servlet, ServletRegistration> {

	public ContextServletTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime, contextController);
	}

	@Override
	public AtomicReference<ServletRegistration> addingService(
		ServiceReference<Servlet> serviceReference) {

		AtomicReference<ServletRegistration> result = new AtomicReference<ServletRegistration>();
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
					"Whiteboard Servlets with pattern cannot bind to legacy contexts. " + serviceReference, DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING); //$NON-NLS-1$
			}

			httpServiceRuntime.removeFailedServletDTO(serviceReference);

			result.set(contextController.addServletRegistration(serviceReference));
		}
		catch (HttpWhiteboardFailureException hwfe) {
			httpServiceRuntime.log(hwfe.getMessage(), hwfe);

			recordFailed(serviceReference, hwfe.getFailureReason());
		}
		catch (Throwable t) {
			httpServiceRuntime.log(t.getMessage(), t);

			recordFailed(serviceReference, DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		}
		finally {
			httpServiceRuntime.incrementServiceChangecount();
		}

		return result;
	}

	@Override
	void removeFailed(ServiceReference<Servlet> serviceReference) {
		contextController.getHttpServiceRuntime().removeFailedServletDTO(serviceReference);
	}

	void recordFailed(ServiceReference<?> servletReference, int failureReason) {
		ServletDTO servletDTO = DTOUtil.assembleServletDTO(servletReference, contextController.getServiceId(), false);

		contextController.recordFailedServletDTO(servletReference, servletDTO, failureReason);
	}

}
