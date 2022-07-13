/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
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

import static org.junit.Assume.assumeTrue;

import java.util.Map;
import org.eclipse.equinox.internal.security.tests.SecurityTestsActivator;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Uses linux module
 */
public class LinuxPreferencesTest extends SecurePreferencesTest {

	final private static String MODULE_ID = "org.eclipse.equinox.security.linux";

	@Before
	public void setUp() {
		assumeTrue(hasBundle(MODULE_ID));
	}

	@Override
	protected String getModuleID() {
		return "org.eclipse.equinox.security.LinuxKeystoreIntegrationJNA";
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
