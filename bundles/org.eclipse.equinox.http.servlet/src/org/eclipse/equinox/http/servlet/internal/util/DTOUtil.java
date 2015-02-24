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

import org.osgi.service.http.runtime.dto.FailedServletContextDTO;

/**
 * @author Raymond Augé
 */
public class DTOUtil {

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

}