/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 */

public class Activator implements BundleActivator {
	/*
	 * ----------------------------------------------------------------------
	 *      BundleActivator Interface implementation
	 * ----------------------------------------------------------------------
	 */

	private Http http = null;

	/**
	 * Required by BundleActivator Interface.
	 */
	public void start(BundleContext context) throws Exception {
		http = new Http(context);
		http.start();
	}

	/**
	 * Required by BundleActivator Interface.
	 */
	public void stop(BundleContext context) throws Exception {
		http.stop();
		http.close();
		http = null;
	}
}
