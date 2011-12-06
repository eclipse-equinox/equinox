/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/*
 * The Bundle-Activator for the bundle. Ideally this class is kept as small as
 * possible. 
 */
public class Activator extends Object implements BundleActivator {
	private static Activator INSTANCE;

	public static BundleContext getBundleContext() {
		return Activator.INSTANCE != null ? Activator.INSTANCE.bundleContext : null;
	}
	
	private BundleContext bundleContext;
	
	public Activator() {
		super();
		Activator.INSTANCE = this;
	}

	public void start(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
	}
	
	public void stop(BundleContext bundleContext) throws Exception {
		this.bundleContext = null;
	}
}
