/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.cm.test;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

	private static BundleContext bundleContext;

	public void start(BundleContext context) throws Exception {
		setBundleContext(context);
	}

	public void stop(BundleContext context) throws Exception {
		setBundleContext(null);
	}

	private static synchronized void setBundleContext(BundleContext context) {
		bundleContext = context;
	}

	static synchronized BundleContext getBundleContext() {
		return bundleContext;
	}

	static Bundle getBundle(String symbolicName) {
		return org.eclipse.osgi.framework.util.Wirings.getBundle(symbolicName).orElse(null);
	}
}
