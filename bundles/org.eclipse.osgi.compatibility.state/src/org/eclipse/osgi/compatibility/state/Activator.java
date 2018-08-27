/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private final PlatformAdminImpl platformAdmin = new PlatformAdminImpl();

	@Override
	public void start(BundleContext context) throws Exception {
		platformAdmin.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		platformAdmin.stop(context);
	}

}
