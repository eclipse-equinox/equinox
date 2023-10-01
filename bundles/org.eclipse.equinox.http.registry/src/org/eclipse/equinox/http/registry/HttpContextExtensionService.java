/*******************************************************************************
 * Copyright (c) 2005, 2008 Cognos Incorporated, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/

package org.eclipse.equinox.http.registry;

import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * The HttpContextExtensionService provides access to an HttpContext instance
 * whose resources and implementation are added via the "httpcontexts" extension
 * point.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface HttpContextExtensionService {
	/**
	 * returns the HttpContext associated with the HttpService reference and http
	 * context name
	 *
	 * @param httpServiceReference The ServiceReference of the http service to which
	 *                             this HttpContext applies
	 * @param httpContextId        The name of the HttpContext. Must be provided
	 * 
	 * @return The HttpContext associated with the "id" and Http Service Reference;
	 *         <code>null</code> if the HttpContext is unavailable.
	 */
	public HttpContext getHttpContext(ServiceReference<HttpService> httpServiceReference, String httpContextId);
}
