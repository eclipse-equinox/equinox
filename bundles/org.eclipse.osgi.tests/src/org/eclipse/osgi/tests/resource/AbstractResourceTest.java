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
package org.eclipse.osgi.tests.resource;

import org.eclipse.osgi.tests.OSGiTest;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.osgi.framework.InvalidSyntaxException;

public abstract class AbstractResourceTest extends OSGiTest {
	protected BundleInstaller installer;

	public AbstractResourceTest() {
		super();
	}

	public AbstractResourceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		try {
			installer = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "wiringTests/bundles", OSGiTestsActivator.getContext()); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Failed to create bundle installer", e); //$NON-NLS-1$
		}
	}

	protected void tearDown() throws Exception {
		installer.shutdown();
		super.tearDown();
	}
}
