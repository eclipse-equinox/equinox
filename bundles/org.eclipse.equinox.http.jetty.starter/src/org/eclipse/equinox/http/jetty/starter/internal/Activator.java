/*******************************************************************************
 * Copyright (c) 2015, 2017 Raymond Auge and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Raymond Auge - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.http.jetty.starter.internal;

import org.osgi.framework.*;
import org.osgi.framework.startlevel.BundleStartLevel;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(org.eclipse.equinox.http.jetty.JettyConstants.class);

		BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);

		if (bundleStartLevel.isActivationPolicyUsed()) {
			bundle.stop();
			bundle.start();
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// ignore
	}

}
