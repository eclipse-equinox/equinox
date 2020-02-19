/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
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
package org.eclipse.osgi.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class OSGiTestsActivator implements BundleActivator {
	public static final String TEST_FILES_ROOT = "test_files/";

	private static OSGiTestsActivator instance;
	private BundleContext context;

	public OSGiTestsActivator() {
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
