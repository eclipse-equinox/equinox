/*******************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Equinox Project - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.region.internal.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * Test helper to create mock OSGi bundles with proper relationships.
 * This replaces the functionality previously provided by Virgo StubBundle.
 */
public class MockBundleBuilder {

	/**
	 * Creates a mock Bundle with the specified properties.
	 * 
	 * @param bundleId      the bundle ID
	 * @param symbolicName  the bundle symbolic name
	 * @param version       the bundle version
	 * @param location      the bundle location
	 * @return a mock Bundle configured with the specified properties
	 */
	public static Bundle createMockBundle(long bundleId, String symbolicName, Version version, String location) {
		Bundle bundle = mock(Bundle.class);
		BundleContext bundleContext = mock(BundleContext.class);

		// Set up basic bundle properties
		when(bundle.getBundleId()).thenReturn(bundleId);
		when(bundle.getSymbolicName()).thenReturn(symbolicName);
		when(bundle.getVersion()).thenReturn(version);
		when(bundle.getLocation()).thenReturn(location);
		when(bundle.getBundleContext()).thenReturn(bundleContext);

		// Set up bundle context to return the bundle
		when(bundleContext.getBundle()).thenReturn(bundle);
		when(bundleContext.getBundle(bundleId)).thenReturn(bundle);

		return bundle;
	}

	/**
	 * Creates a mock BundleContext.
	 * 
	 * @return a mock BundleContext
	 */
	public static BundleContext createMockBundleContext() {
		return mock(BundleContext.class);
	}
}
