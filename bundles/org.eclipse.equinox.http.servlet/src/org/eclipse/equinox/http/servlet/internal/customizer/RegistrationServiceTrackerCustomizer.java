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

import org.eclipse.equinox.http.servlet.internal.HttpServiceRuntimeImpl;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Raymond Augé
 */
public abstract class RegistrationServiceTrackerCustomizer<S, T>
	implements ServiceTrackerCustomizer<S, T> {

	public RegistrationServiceTrackerCustomizer(
		BundleContext bundleContext, HttpServiceRuntimeImpl httpServiceRuntime) {

		this.bundleContext = bundleContext;
		this.httpServiceRuntime = httpServiceRuntime;
	}

	protected BundleContext bundleContext;
	protected HttpServiceRuntimeImpl httpServiceRuntime;

}