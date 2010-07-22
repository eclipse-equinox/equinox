/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.net.URL;
import java.util.Enumeration;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;

public class BundleResourceTests extends CoreTest {
	private BundleInstaller installer;

	protected void setUp() throws Exception {
		try {
			installer = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "resourcetests/bundles", OSGiTestsActivator.getContext()); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Failed to create bundle installer", e); //$NON-NLS-1$
		}
	}

	protected void tearDown() throws Exception {
		installer.shutdown();
	}

	public static Test suite() {
		return new TestSuite(BundleResourceTests.class);
	}

	public void testBug320546_01() throws Exception {
		Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
		URL result = bundle.getEntry("../../../../security");
		assertNull("Found resource!", result);
		result = bundle.getEntry("../file.txt");
		assertNull("Found resource!", result);
		result = bundle.getEntry("../../");
		assertNull("Found resource!", result);
		result = bundle.getEntry("folder/../../file.txt");
		assertNull("Found resource!", result);
		result = bundle.getEntry("folder/../plugin.xml");
		assertNotNull("Did not find resource!", result);
		result = bundle.getEntry("/plugin.xml");
		assertNotNull("Did not find resource!", result);
	}

	public void testBug320546_02() throws Exception {
		Bundle bundle = installer.installBundle("test"); //$NON-NLS-1$
		Enumeration paths = bundle.getEntryPaths("../");
		assertNull("found resource!", paths);
		paths = bundle.getEntryPaths("folder");
		assertNotNull("Did not find resource!", paths);
		paths = bundle.getEntryPaths("folder/../../");
		assertNull("found resource!", paths);
		paths = bundle.getEntryPaths("folder/..");
		assertNotNull("Did not find resource!", paths);
	}
}
