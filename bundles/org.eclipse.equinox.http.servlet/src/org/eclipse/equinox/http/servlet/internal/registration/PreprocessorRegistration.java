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

package org.eclipse.equinox.http.servlet.internal.registration;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ServiceHolder;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.runtime.dto.PreprocessorDTO;
import org.osgi.service.http.whiteboard.Preprocessor;

/**
 * @author Raymond Augé
 */
public class PreprocessorRegistration extends Registration<Preprocessor, PreprocessorDTO>
		implements Comparable<PreprocessorRegistration> {

	private final ServiceHolder<Preprocessor> preprocessorHolder;
	private final ClassLoader classLoader;
	private final HttpServiceRuntimeImpl httpServiceRuntime;

	public PreprocessorRegistration(ServiceHolder<Preprocessor> preprocessorHolder, PreprocessorDTO preprocessorDTO,
			HttpServiceRuntimeImpl httpServiceRuntime) {

		super(preprocessorHolder.get(), preprocessorDTO);
		this.preprocessorHolder = preprocessorHolder;
		this.httpServiceRuntime = httpServiceRuntime;
		this.classLoader = preprocessorHolder.getBundle().adapt(BundleWiring.class).getClassLoader();
	}

	@Override
	public int compareTo(PreprocessorRegistration o) {
		ServiceReference<Preprocessor> thisRef = preprocessorHolder.getServiceReference();
		ServiceReference<Preprocessor> otherRef = o.preprocessorHolder.getServiceReference();
		return thisRef.compareTo(otherRef);
	}

	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			getT().doFilter(request, response, chain);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Override
	public void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			httpServiceRuntime.getPreprocessorRegistrations().remove(preprocessorHolder.getServiceReference());
			preprocessorHolder.getBundle().getBundleContext().ungetService(preprocessorHolder.getServiceReference());
			super.destroy();
			getT().destroy();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
			preprocessorHolder.release();
		}
	}

	@Override
	public int hashCode() {
		return Long.valueOf(getD().serviceId).hashCode();
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);

			getT().init(filterConfig);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

}
