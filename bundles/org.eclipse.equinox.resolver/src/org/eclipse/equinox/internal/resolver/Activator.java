/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.resolver;

import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.osgi.framework.*;
import org.osgi.service.resolver.Resolver;

public class Activator implements BundleActivator {

	private ServiceRegistration<Resolver> resolverReg;

	public void start(BundleContext context) throws Exception {
		resolverReg = context.registerService(Resolver.class, new EquinoxResolver(StateObjectFactory.defaultFactory), null);
	}

	public void stop(BundleContext context) throws Exception {
		resolverReg.unregister();
		resolverReg = null;
	}

}
