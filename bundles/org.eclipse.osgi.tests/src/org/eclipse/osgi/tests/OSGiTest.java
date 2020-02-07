/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

import org.eclipse.core.tests.harness.CoreTest;
import org.osgi.framework.BundleContext;

/**
 * @since 3.1
 */
public class OSGiTest extends CoreTest {

	public static final String PI_OSGI_TESTS = "org.eclipse.osgi.tests";

	public OSGiTest() {
		super();
	}

	public OSGiTest(String name) {
		super(name);
	}

	public BundleContext getContext() {
		return OSGiTestsActivator.getContext();
	}
}
