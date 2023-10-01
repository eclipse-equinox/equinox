/*******************************************************************************
 * Copyright (c) 2019, 2020 Liferay, Inc.
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

import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedErrorPageDTO;
import org.eclipse.equinox.http.servlet.internal.servlet.Match;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * @author Raymond Augé
 */
public class ErrorPageRegistration extends EndpointRegistration<ExtendedErrorPageDTO> {

	public ErrorPageRegistration(ServiceHolder<Servlet> servletHolder, ExtendedErrorPageDTO errorPageDTO,
			ServletContextHelper servletContextHelper, ContextController contextController) {

		super(servletHolder, errorPageDTO, servletContextHelper, contextController);
	}

	@Override
	public String getName() {
		return getD().name;
	}

	@Override
	public String[] getPatterns() {
		return EMPTY;
	}

	@Override
	public long getServiceId() {
		return getD().serviceId;
	}

	@Override
	public ServiceReference<?> getServiceReference() {
		return servletHolder.getServiceReference();
	}

	@Override
	public String match(String name, String servletPath, String pathInfo, String extension, Match match) {

		if (match != Match.ERROR) {
			return null;
		}

		if (name != null) {
			for (long errorCode : getD().errorCodes) {
				if (String.valueOf(errorCode).equals(name)) {
					return name;
				}
			}

			for (String exception : getD().exceptions) {
				if (exception.equals(name)) {
					return name;
				}
			}
		}

		return null;
	}

	private static final String[] EMPTY = new String[0];

}
