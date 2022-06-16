/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 578599 - Make org.eclipse.osgi.tests' activator obsolete
 *******************************************************************************/
package org.eclipse.osgi.tests;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class OSGiTestsActivator {
	private OSGiTestsActivator() {
	}

	public static final String TEST_FILES_ROOT = "test_files/";

	public static Bundle getBundle() {
		return FrameworkUtil.getBundle(OSGiTestsActivator.class);
	}

	public static BundleContext getContext() {
		return getBundle().getBundleContext();
	}
}
