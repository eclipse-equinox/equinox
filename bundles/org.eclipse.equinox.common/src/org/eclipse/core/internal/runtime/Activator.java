/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The Common runtime plugin class.
 * 
 * This class can only be used if OSGi plugin is available.
 */
public class Activator implements BundleActivator {

	/**
	 * The bundle context associated this plug-in
	 */
	private static BundleContext bundleContext;

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		CommonOSGiUtils.getDefault().closeServices();
		bundleContext = null;
	}

	static BundleContext getContext() {
		return bundleContext;
	}
}
