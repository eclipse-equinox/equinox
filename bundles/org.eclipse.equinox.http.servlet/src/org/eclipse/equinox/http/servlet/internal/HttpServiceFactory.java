/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others.
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
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/

package org.eclipse.equinox.http.servlet.internal;

import org.osgi.framework.*;
import org.osgi.service.http.HttpService;

// Factory to create http services. This is because the service needs to be
// customized for each bundle in order to implement the default resource
// lookups.
public class HttpServiceFactory implements ServiceFactory<HttpService> {

	private final HttpServiceRuntimeImpl httpServiceRuntime;

	public HttpServiceFactory(HttpServiceRuntimeImpl httpServiceRuntime) {
		this.httpServiceRuntime = httpServiceRuntime;
	}

	@Override
	public HttpService getService(Bundle bundle, ServiceRegistration<HttpService> serviceRegistration) {

		return new HttpServiceImpl(bundle, httpServiceRuntime);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<HttpService> serviceRegistration,
			HttpService httpService) {

		HttpServiceImpl httpServiceImpl = (HttpServiceImpl) httpService;

		httpServiceImpl.shutdown();
	}

}
