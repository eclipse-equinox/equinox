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
package org.eclipse.osgi.tests.internal.plugins;

import java.io.IOException;
import junit.framework.TestCase;
import org.eclipse.osgi.tests.BundleTestingHelper;
import org.osgi.framework.*;

public class InstallTests extends TestCase {

	public InstallTests() {
		super();
	}

	public InstallTests(String name) {
		super(name);
	}

	public void testInstallInvalidManifest() throws BundleException, IOException {
		Bundle installed = null;
		try {
			installed = BundleTestingHelper.installBundle("internal/plugins/installTests/bundle03");
			// should have failed with BundleException
			fail("1.0");
		} catch (BundleException be) {
			// success - the manifest was invalid
		} finally {
			if (installed != null)
				// clean-up - only runs if we end-up accepting an invalid manifest				
				installed.uninstall();
		}
	}

	public void testInstallLocationWithSpaces() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle("internal/plugins/installTests/bundle 01");
		try {
			assertEquals("1.0", "bundle01", installed.getSymbolicName());
			assertEquals("1.1", Bundle.INSTALLED, installed.getState());
		} finally {
			// clean-up
			installed.uninstall();
		}
	}

	public void testInstallLocationWithUnderscores() throws BundleException, IOException {
		Bundle installed = null;
		installed = BundleTestingHelper.installBundle("internal/plugins/installTests/bundle02_1.0.0");
		try {
			assertEquals("1.0", "bundle02", installed.getSymbolicName());
			assertEquals("1.1", Bundle.INSTALLED, installed.getState());
			assertEquals("1.2", "2.0", installed.getHeaders().get(Constants.BUNDLE_VERSION));
		} finally {
			// clean-up
			installed.uninstall();
		}
	}
}