/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class OSGiTestsPlugin extends Plugin {
	private static OSGiTestsPlugin plugin;
	private BundleContext context;

	public OSGiTestsPlugin() {
		super();
		plugin = this;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
	}

	public void stop(BundleContext context) throws Exception {
		context = null;
	}

	public static BundleContext getContext() {
		return plugin != null ? plugin.context : null;
	}
}