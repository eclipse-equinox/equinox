/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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
