/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.tests.storage;

import java.util.Map;
import org.eclipse.equinox.internal.security.tests.SecurityTestsActivator;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Tests Windows module, if available.
 */
public class WinPreferencesTest extends SecurePreferencesTest {

	final private static String WIN_BUNDLE = "org.eclipse.equinox.security.win32";

	@Before
	public void setUp() {
		org.junit.Assume.assumeTrue(hasBundle(WIN_BUNDLE));
	}

	/**
	 * Unique ID of the Windows module.
	 */
	static private final String WIN_64BIT_MODULE_ID = "org.eclipse.equinox.security.WindowsPasswordProvider64bit"; //$NON-NLS-1$

	@Override
	protected String getModuleID() {
		return WIN_64BIT_MODULE_ID;
	}

	@Override
	protected Map<String, Object> getOptions() {
		// Don't specify default password when testing specific password provider
		return getOptions(null);
	}

	static private boolean hasBundle(String symbolicID) {
		BundleContext context = SecurityTestsActivator.getDefault().getBundleContext();
		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles) {
			String bundleName = bundle.getSymbolicName();
			if (!symbolicID.equals(bundleName))
				continue;
			int bundleState = bundle.getState();
			return (bundleState != Bundle.INSTALLED) && (bundleState != Bundle.UNINSTALLED);
		}
		return false;
	}
}
