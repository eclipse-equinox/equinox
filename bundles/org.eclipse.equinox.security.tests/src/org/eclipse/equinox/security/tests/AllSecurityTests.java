/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.security.tests;

import junit.framework.*;
import org.eclipse.equinox.internal.security.tests.SecurityTestsActivator;
import org.eclipse.equinox.internal.security.tests.storage.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * As tests use registry listeners, UI listeners might cause tests to time out and fail.
 * As such this tests should be run in a headless mode.
 */
public class AllSecurityTests extends TestCase {

	final private static String WIN_BUNDLE = "org.eclipse.equinox.security.win32.x86";

	public AllSecurityTests() {
		super(null);
	}

	public AllSecurityTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(AllSecurityTests.class.getName());

		// stand-alone test for Base64
		suite.addTest(Base64Test.suite());
		// test node names encoding ("slash eliminator")
		suite.addTest(SlashEncodeTest.suite());
		//  tests secure Preferences functionality using default provider
		suite.addTest(DefaultPreferencesTest.suite());
		// check dynamic additions / removals
		suite.addTest(DynamicPreferencesTest.suite());

		// testing Windows-specific path should only be attempted if bundle is resolved
		if (hasBundle(WIN_BUNDLE))
			suite.addTest(WinPreferencesTest.suite());

		return suite;
	}

	static private boolean hasBundle(String symbolicID) {
		BundleContext context = SecurityTestsActivator.getDefault().getBundleContext();
		Bundle[] bundles = context.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			String bundleName = bundles[i].getSymbolicName();
			if (!symbolicID.equals(bundleName))
				continue;
			int bundleState = bundles[i].getState();
			return (bundleState != Bundle.INSTALLED) && (bundleState != Bundle.UNINSTALLED);
		}
		return false;
	}
}
