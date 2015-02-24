/*******************************************************************************
 * Copyright (c) Feb 23, 2015 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Raymond Augé <raymond.auge@liferay.com> - Bug 460639
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.util;

import org.osgi.service.http.runtime.dto.*;

/**
 * @author Raymond Augé
 */
public class DTOUtil {

	public static ErrorPageDTO clone(ErrorPageDTO original) {
		ErrorPageDTO clone = new ErrorPageDTO();

		clone.asyncSupported = original.asyncSupported;
		clone.errorCodes = original.errorCodes;
		clone.exceptions = original.exceptions;
		clone.initParams = original.initParams;
		clone.name = original.name;
		clone.serviceId = original.serviceId;
		clone.servletContextId = original.servletContextId;
		clone.servletInfo = original.servletInfo;

		return clone;
	}

	public static FailedServletContextDTO clone(FailedServletContextDTO original) {
		FailedServletContextDTO clone = new FailedServletContextDTO();

		clone.attributes = original.attributes;
		clone.contextName = original.contextName;
		clone.contextPath = original.contextPath;
		clone.errorPageDTOs = original.errorPageDTOs;
		clone.failureReason = original.failureReason;
		clone.filterDTOs = original.filterDTOs;
		clone.initParams = original.initParams;
		clone.listenerDTOs = original.listenerDTOs;
		clone.name = original.name;
		clone.resourceDTOs = original.resourceDTOs;
		clone.serviceId = original.serviceId;
		clone.servletDTOs = original.servletDTOs;

		return clone;
	}

	public static FilterDTO clone(FilterDTO original) {
		FilterDTO clone = new FilterDTO();

		clone.asyncSupported = original.asyncSupported;
		clone.dispatcher = original.dispatcher;
		clone.initParams = original.initParams;
		clone.name = original.name;
		clone.patterns = original.patterns;
		clone.regexs = original.regexs;
		clone.serviceId = original.serviceId;
		clone.servletContextId = original.servletContextId;
		clone.servletNames = original.servletNames;

		return clone;
	}

	public static ListenerDTO clone(ListenerDTO original) {
		ListenerDTO clone = new ListenerDTO();

		clone.serviceId = original.serviceId;
		clone.servletContextId = original.servletContextId;
		clone.types = original.types;

		return clone;
	}

	public static ResourceDTO clone(ResourceDTO original) {
		ResourceDTO clone = new ResourceDTO();

		clone.patterns = original.patterns;
		clone.prefix = original.prefix;
		clone.serviceId = original.serviceId;
		clone.servletContextId = original.servletContextId;

		return clone;
	}

	public static ServletDTO clone(ServletDTO original) {
		ServletDTO clone = new ServletDTO();

		clone.asyncSupported = original.asyncSupported;
		clone.initParams = original.initParams;
		clone.name = original.name;
		clone.patterns = original.patterns;
		clone.serviceId = original.serviceId;
		clone.servletContextId = original.servletContextId;
		clone.servletInfo = original.servletInfo;

		return clone;
	}

}