/*******************************************************************************
 * Copyright (c) 2014, 2019 Raymond Augé and others.
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

package org.eclipse.equinox.http.servlet.internal.customizer;

import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.registration.Registration;
import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Raymond Augé
 */
public abstract class RegistrationServiceTrackerCustomizer<S, T extends Registration<?, ? extends DTO>>
		implements ServiceTrackerCustomizer<S, AtomicReference<T>> {

	public RegistrationServiceTrackerCustomizer(BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime,
			ContextController contextController) {

		this.bundleContext = bundleContext;
		this.httpServiceRuntime = httpServiceRuntime;
		this.contextController = contextController;
	}

	@Override
	public void modifiedService(ServiceReference<S> serviceReference, AtomicReference<T> filterReference) {

		removedService(serviceReference, filterReference);
		AtomicReference<T> added = addingService(serviceReference);
		filterReference.set(added.get());
	}

	@Override
	public void removedService(ServiceReference<S> serviceReference, AtomicReference<T> filterReference) {

		try {
			T registration = filterReference.get();
			if (registration != null) {
				registration.destroy();
			}

			removeFailed(serviceReference);
		} finally {
			httpServiceRuntime.incrementServiceChangecount();
		}
	}

	abstract void removeFailed(ServiceReference<S> serviceReference);

	protected final BundleContext bundleContext;
	protected final ContextController contextController;
	protected final HttpServiceRuntimeImpl httpServiceRuntime;

}
