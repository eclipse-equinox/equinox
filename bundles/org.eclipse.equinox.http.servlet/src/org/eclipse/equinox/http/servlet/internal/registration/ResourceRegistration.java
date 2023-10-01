/*******************************************************************************
 * Copyright (c) 2014, 2020 Raymond Augé and others.
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

package org.eclipse.equinox.http.servlet.internal.registration;

import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.ServiceHolder;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.ResourceDTO;

/**
 * @author Raymond Augé
 */
public class ResourceRegistration extends EndpointRegistration<ResourceDTO> {

	public ResourceRegistration(ServiceReference<?> serviceReference, ServiceHolder<Servlet> servletHolder,
			ResourceDTO resourceDTO, ServletContextHelper servletContextHelper, ContextController contextController) {

		super(servletHolder, resourceDTO, servletContextHelper, contextController);

		this.serviceReference = serviceReference;
		name = servletHolder.get().getClass().getName().concat("#").concat(getD().prefix); //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String[] getPatterns() {
		return getD().patterns;
	}

	@Override
	public long getServiceId() {
		return getD().serviceId;
	}

	@Override
	public ServiceReference<?> getServiceReference() {
		return serviceReference;
	}

	private final String name;
	private final ServiceReference<?> serviceReference;

}
