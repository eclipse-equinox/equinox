/*******************************************************************************
 * Copyright (c) Feb. 2, 2019 Liferay, Inc.
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

package org.eclipse.equinox.http.servlet.internal;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_PREPROCESSOR_INIT_PARAM_PREFIX;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import org.eclipse.equinox.http.servlet.internal.context.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.eclipse.equinox.http.servlet.internal.registration.PreprocessorRegistration;
import org.eclipse.equinox.http.servlet.internal.servlet.FilterConfigImpl;
import org.eclipse.equinox.http.servlet.internal.util.ServiceProperties;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.*;
import org.osgi.service.http.whiteboard.Preprocessor;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Raymond Augé
 */
public class PreprocessorCustomizer
		implements ServiceTrackerCustomizer<Preprocessor, AtomicReference<PreprocessorRegistration>> {

	public PreprocessorCustomizer(HttpServiceRuntimeImpl httpServiceRuntime) {
		this.httpServiceRuntime = httpServiceRuntime;
	}

	@Override
	public AtomicReference<PreprocessorRegistration> addingService(ServiceReference<Preprocessor> serviceReference) {

		AtomicReference<PreprocessorRegistration> result = new AtomicReference<>();
		if (!httpServiceRuntime.matches(serviceReference)) {
			return result;
		}

		try {
			removeFailed(serviceReference);

			result.set(addPreprocessorRegistration(serviceReference));
		} catch (HttpWhiteboardFailureException hwfe) {
			httpServiceRuntime.debug(hwfe.getMessage(), hwfe);

			recordFailed(serviceReference, hwfe.getFailureReason());
		} catch (Exception e) {
			httpServiceRuntime.error(e.getMessage(), e);

			recordFailed(serviceReference, DTOConstants.FAILURE_REASON_EXCEPTION_ON_INIT);
		} finally {
			httpServiceRuntime.incrementServiceChangecount();
		}

		return result;
	}

	@Override
	public void modifiedService(ServiceReference<Preprocessor> serviceReference,
			AtomicReference<PreprocessorRegistration> reference) {

		removedService(serviceReference, reference);
		AtomicReference<PreprocessorRegistration> added = addingService(serviceReference);
		reference.set(added.get());
	}

	@Override
	public void removedService(ServiceReference<Preprocessor> serviceReference,
			AtomicReference<PreprocessorRegistration> reference) {
		try {
			PreprocessorRegistration registration = reference.get();
			if (registration != null) {
				registration.destroy();
			}

			removeFailed(serviceReference);
		} finally {
			httpServiceRuntime.incrementServiceChangecount();
		}
	}

	void removeFailed(ServiceReference<Preprocessor> serviceReference) {
		httpServiceRuntime.removeFailedPreprocessorDTO(serviceReference);
	}

	private void recordFailed(ServiceReference<Preprocessor> serviceReference, int failureReason) {

		FailedPreprocessorDTO failedPreprocessorDTO = new FailedPreprocessorDTO();

		failedPreprocessorDTO.failureReason = failureReason;
		failedPreprocessorDTO.initParams = ServiceProperties.parseInitParams(serviceReference,
				HTTP_WHITEBOARD_PREPROCESSOR_INIT_PARAM_PREFIX);
		failedPreprocessorDTO.serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);

		httpServiceRuntime.recordFailedPreprocessorDTO(serviceReference, failedPreprocessorDTO);
	}

	private PreprocessorRegistration addPreprocessorRegistration(ServiceReference<Preprocessor> preprocessorRef)
			throws ServletException {

		ServiceHolder<Preprocessor> preprocessorHolder = new ServiceHolder<>(
				httpServiceRuntime.getConsumingContext().getServiceObjects(preprocessorRef));
		Preprocessor preprocessor = preprocessorHolder.get();
		PreprocessorRegistration registration = null;
		boolean addedRegisteredObject = false;
		try {
			if (preprocessor == null) {
				throw new IllegalArgumentException("Preprocessor cannot be null"); //$NON-NLS-1$
			}
			addedRegisteredObject = httpServiceRuntime.getRegisteredObjects().add(preprocessor);
			if (!addedRegisteredObject) {
				throw new HttpWhiteboardFailureException(
						"Multiple registration of instance detected. Prototype scope is recommended: " //$NON-NLS-1$
								+ preprocessorRef,
						DTOConstants.FAILURE_REASON_SERVICE_IN_USE);
			}
			registration = doAddPreprocessorRegistration(preprocessorHolder, preprocessorRef);
		} finally {
			if (registration == null) {
				preprocessorHolder.release();
				if (addedRegisteredObject) {
					httpServiceRuntime.getRegisteredObjects().remove(preprocessor);
				}
			}
		}
		return registration;
	}

	private PreprocessorRegistration doAddPreprocessorRegistration(ServiceHolder<Preprocessor> preprocessorHolder,
			ServiceReference<Preprocessor> preprocessorRef) throws ServletException {

		PreprocessorDTO preprocessorDTO = new PreprocessorDTO();

		preprocessorDTO.initParams = ServiceProperties.parseInitParams(preprocessorRef,
				HTTP_WHITEBOARD_PREPROCESSOR_INIT_PARAM_PREFIX);
		preprocessorDTO.serviceId = preprocessorHolder.getServiceId();

		PreprocessorRegistration newRegistration = new PreprocessorRegistration(preprocessorHolder, preprocessorDTO,
				httpServiceRuntime);
		FilterConfig filterConfig = new FilterConfigImpl(preprocessorHolder.get().getClass().getCanonicalName(),
				preprocessorDTO.initParams, httpServiceRuntime.getParentServletContext());

		newRegistration.init(filterConfig);

		httpServiceRuntime.getPreprocessorRegistrations().put(preprocessorHolder.getServiceReference(),
				newRegistration);

		return newRegistration;
	}

	private final HttpServiceRuntimeImpl httpServiceRuntime;

}
