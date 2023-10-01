/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class SecurityTestsActivator implements BundleActivator {

	/**
	 * ID of this bundle
	 */
	public static final String PLUGIN_ID = "org.eclipse.equinox.security.tests";

	private static SecurityTestsActivator singleton;

	private BundleContext bundleContext;

	/*
	 * Returns the singleton for this Activator. Callers should be aware that this
	 * will return nulCooll if the bundle is not active.
	 */
	public static SecurityTestsActivator getDefault() {
		return singleton;
	}

	public SecurityTestsActivator() {
		super();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		singleton = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
		singleton = null;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

}
