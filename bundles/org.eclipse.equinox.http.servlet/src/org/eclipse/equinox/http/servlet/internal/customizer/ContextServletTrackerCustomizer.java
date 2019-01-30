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
import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.eclipse.equinox.http.servlet.internal.registration.ServletRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * @author Raymond Augé
 */
public class ContextServletTrackerCustomizer
	extends RegistrationServiceTrackerCustomizer<Servlet, AtomicReference<ServletRegistration>> {

	public ContextServletTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
		ContextController contextController) {

		super(bundleContext, httpServiceRuntime);

		this.contextController = contextController;
	}

	@Override
	public AtomicReference<ServletRegistration> addingService(
		ServiceReference<Servlet> serviceReference) {

		AtomicReference<ServletRegistration> result = new AtomicReference<ServletRegistration>();
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

			httpServiceRuntime.removeFailedServletDTO(serviceReference);

			result.set(contextController.addServletRegistration(serviceReference));
		}
		catch (HttpWhiteboardFailureException hwfe) {
			httpServiceRuntime.log(hwfe.getMessage(), hwfe);

			contextController.recordFailedServletDTO(serviceReference, null, hwfe.getFailureReason());
		}
		catch (Throwable t) {
			httpServiceRuntime.log(t.getMessage(), t);

			contextController.recordFailedServletDTO(serviceReference, null, DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		}

		return result;
	}

	@Override
	public void modifiedService(
		ServiceReference<Servlet> serviceReference,
		AtomicReference<ServletRegistration> servletReference) {

		removedService(serviceReference, servletReference);
		AtomicReference<ServletRegistration> added = addingService(serviceReference);
		servletReference.set(added.get());
	}

	@Override
	public void removedService(
		ServiceReference<Servlet> serviceReference,
		AtomicReference<ServletRegistration> servletReference) {
		ServletRegistration registration = servletReference.get();
		if (registration != null) {
			// destroy will unget the service object we were using
			registration.destroy();
		}

		contextController.getHttpServiceRuntime().removeFailedServletDTO(serviceReference);
		contextController.getHttpServiceRuntime().removeFailedErrorPageDTO(serviceReference);
	}

	private ContextController contextController;

}
