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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class OSGiTests implements BundleActivator {
	public static final String TEST_FILES_ROOT = "test_files/";

	private static OSGiTests instance;
	private BundleContext context;

	public OSGiTests() {
		instance = this;
	}

	public void start(BundleContext context) throws Exception {
		this.context = context;
	}

	public void stop(BundleContext context) throws Exception {
		this.context = null;
	}

	public static BundleContext getContext() {
		return instance != null ? instance.context : null;
	}
}