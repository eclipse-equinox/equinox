/*******************************************************************************
 * Copyright (c) Nov 20, 2014 Liferay, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Liferay, Inc. - initial API and implementation and/or initial 
 *                    documentation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.customizer;

import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ContextControllerManager<S> implements	ServiceTrackerCustomizer<S, ServiceReference<S>> {
	private final HttpServiceRuntimeImpl httpServiceRuntime;

	public ContextControllerManager(HttpServiceRuntimeImpl httpServiceRuntime) {
		this.httpServiceRuntime = httpServiceRuntime;
	}
	@Override
	public ServiceReference<S> addingService(ServiceReference<S> reference) {
		if (!httpServiceRuntime.matches(reference)) {
			// Does not match the runtime, 
			// but we still track incase it changes properties to match later
			return reference;
		}

		String contextSelector = (String)reference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);

		httpServiceRuntime.getOrAddContextController(
			contextSelector,
			reference.getBundle().getBundleContext());

		return reference;
	}

	@Override
	public void modifiedService(
		ServiceReference<S> reference, ServiceReference<S> service) {
		removedService(reference, service);
		addingService(reference);
	}

	@Override
	public void removedService(
		ServiceReference<S> reference, ServiceReference<S> service) {
		// TODO consider cleaning up ContextControllers that are no longer used by any services
	}

}
