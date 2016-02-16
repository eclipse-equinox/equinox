/*******************************************************************************
 * Copyright (c) 2005, 2016 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé - bug fixes and enhancements
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.registration;

import javax.servlet.Servlet;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.ContextController.ServiceHolder;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedServletDTO;
import org.eclipse.equinox.http.servlet.internal.servlet.Match;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;

//This class wraps the servlet object registered in the HttpService.registerServlet call, to manage the context classloader when handleRequests are being asked.
public class ServletRegistration extends EndpointRegistration<ExtendedServletDTO> {

	public ServletRegistration(
		ServiceHolder<Servlet> servletHolder, ExtendedServletDTO servletDTO, ErrorPageDTO errorPageDTO,
		ServletContextHelper servletContextHelper,
		ContextController contextController, ClassLoader legacyTCCL) {

		super(servletHolder, servletDTO, servletContextHelper, contextController, legacyTCCL);

		this.errorPageDTO = errorPageDTO;
	}

	public ErrorPageDTO getErrorPageDTO() {
		return errorPageDTO;
	}

	@Override
	public String getName() {
		return getD().name;
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
	public String match(
		String name, String servletPath, String pathInfo, String extension,
		Match match) {

		if ((errorPageDTO != null) && (name != null)) {
			for (long errorCode : errorPageDTO.errorCodes) {
				if (String.valueOf(errorCode).equals(name)) {
					return name;
				}
			}

			for (String exception : errorPageDTO.exceptions) {
				if (exception.equals(name)) {
					return name;
				}
			}
		}

		return super.match(name, servletPath, pathInfo, extension, match);
	}

	private ErrorPageDTO errorPageDTO;

}
