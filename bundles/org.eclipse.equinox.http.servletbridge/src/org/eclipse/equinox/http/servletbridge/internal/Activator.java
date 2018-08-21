/*******************************************************************************
 * Copyright (c) 2005, 2018 Cognos Incorporated, IBM Corporation and others.
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
package org.eclipse.equinox.http.servletbridge.internal;

import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.eclipse.equinox.servletbridge.BridgeServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private HttpServiceServlet httpServiceServlet;

	@Override
	public void start(BundleContext context) throws Exception {
		httpServiceServlet = new HttpServiceServlet();
		BridgeServlet.registerServletDelegate(httpServiceServlet);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		BridgeServlet.unregisterServletDelegate(httpServiceServlet);
		httpServiceServlet = null;
	}
}
