/*******************************************************************************
 * Copyright (c) 2006 Cognos Incorporated.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http.registry;

import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * The NamedHttpContextService provides access to an HttpContext instance whose resources are added
 * via the "httpcontexts" extension point.
 */
public interface NamedHttpContextService {
	/**
	 * returns the HttpContext associated with the HttpService reference and http context name
	 *
	 * @param httpServiceReference The ServiceReference of the http service to which this HttpContext applies
	 * @param httpContextName The name of the HttpContext. Must be provided
	 * 
	 * @return The HttpContext associated with the "name" and Http Service Reference; <code>null</code>
	 *         if the ServiceReference is invalid, no longer being tracked.
	 */
	public HttpContext getNamedHttpContext(ServiceReference httpServiceReference, String httpContextName);
}