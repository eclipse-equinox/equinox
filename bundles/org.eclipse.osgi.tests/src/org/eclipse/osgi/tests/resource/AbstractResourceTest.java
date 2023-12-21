/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.osgi.tests.resource;

import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractResourceTest {
	protected BundleInstaller installer;

	@Before
	public void setUp() throws Exception {
		installer = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "wiringTests/bundles", OSGiTestsActivator.getContext()); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		installer.shutdown();
	}
}
