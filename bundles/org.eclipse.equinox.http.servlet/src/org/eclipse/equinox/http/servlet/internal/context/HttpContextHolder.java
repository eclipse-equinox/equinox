/*******************************************************************************
 * Copyright (c) 2014, 2019 Liferay, Inc.
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

package org.eclipse.equinox.http.servlet.internal.context;

import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.equinox.http.servlet.internal.DefaultServletContextHelper;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;

public class HttpContextHolder {

	final HttpContext httpContext;
	final ServiceRegistration<? extends ServletContextHelper> registration;
	final String filter;
	final AtomicLong useCount = new AtomicLong(0);

	public HttpContextHolder(HttpContext httpContext, ServiceRegistration<DefaultServletContextHelper> registration) {
		this.httpContext = httpContext;
		this.registration = registration;
		StringBuilder filterBuilder = new StringBuilder();
		filterBuilder.append('(');
		filterBuilder.append(Constants.SERVICE_ID);
		filterBuilder.append('=');
		filterBuilder.append(registration.getReference().getProperty(Constants.SERVICE_ID));
		filterBuilder.append(')');
		filter = filterBuilder.toString();
	}

	public ServiceReference<? extends ServletContextHelper> getServiceReference() {
		try {
			return registration.getReference();
		} catch (IllegalStateException e) {
			// do nothing
		}
		return null;
	}

	public String getFilter() {
		return filter;
	}

	public long incrementUseCount() {
		return useCount.incrementAndGet();
	}

	public long decrementUseCount() {
		long result = useCount.decrementAndGet();
		if (result == 0) {
			try {
				registration.unregister();
			} catch (IllegalStateException e) {
				// ignore; already unregistered
			}
		}
		return result;
	}

	public Object getHttpContext() {
		return httpContext;
	}
}
