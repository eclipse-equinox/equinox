/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.common.tests.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.ContributorFactoryOSGi;
import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.IDynamicExtensionRegistry;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests contributor resolution for Bundle-based contributors.
 *
 * @since 3.3
 */
public class ContributorsTest {

	@Test
	public void testResolution() throws IOException, BundleException {
		Bundle bundle = null;
		Bundle fragment = null;
		try {
			BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
			bundle = BundleTestingHelper.installBundle("0.1", bundleContext, "Plugin_Testing/registry/contributors/A");
			fragment = BundleTestingHelper.installBundle("0.2", bundleContext,
					"Plugin_Testing/registry/contributors/B");
			BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundle, fragment });

			IExtensionRegistry registry = RegistryFactory.getRegistry();
			IExtensionPoint bundleExtPoint = registry.getExtensionPoint("testContributors.xptContibutorsA");
			IContributor bundleContributor = bundleExtPoint.getContributor();
			Bundle contributingBundle = ContributorFactoryOSGi.resolve(bundleContributor);
			assertNotNull(contributingBundle);
			assertTrue(contributingBundle.equals(bundle));

			IExtensionPoint fragmentExtPoint = registry.getExtensionPoint("testContributors.contrFragment");
			IContributor fragmentContributor = fragmentExtPoint.getContributor();
			Bundle contributingFragment = ContributorFactoryOSGi.resolve(fragmentContributor);
			assertNotNull(contributingFragment);
			assertTrue(contributingFragment.equals(fragment));
		} finally {
			if (bundle != null) {
				bundle.uninstall();
			}
			if (fragment != null) {
				fragment.uninstall();
			}
		}
	}

	/**
	 * bundleA, bundleB, and fragment on bundleA all use the same namespace. Verify
	 * that getting elements by contributor returns all elements from the
	 * contributor and only from that contributor.
	 */
	@Test
	public void testByContributor() throws IOException, BundleException {
		Bundle bundleA = null;
		Bundle bundleB = null;
		Bundle fragment = null;
		try {
			BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
			bundleA = BundleTestingHelper.installBundle("0.1", bundleContext,
					"Plugin_Testing/registry/elementsByContributor/A");
			bundleB = BundleTestingHelper.installBundle("0.2", bundleContext,
					"Plugin_Testing/registry/elementsByContributor/B");
			fragment = BundleTestingHelper.installBundle("0.2", bundleContext,
					"Plugin_Testing/registry/elementsByContributor/Afragment");
			BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] { bundleA, bundleB, fragment });

			IExtensionRegistry registry = RegistryFactory.getRegistry();

			// verify bundleA (bundle B is the same - will work if this works)
			IContributor contributorA = ContributorFactoryOSGi.createContributor(bundleA);

			IExtensionPoint[] extPointsA = registry.getExtensionPoints(contributorA);
			assertNotNull(extPointsA);
			assertEquals(1, extPointsA.length);
			assertTrue(extPointsA[0].getUniqueIdentifier().equals("org.eclipse.test.registryByContrib.PointA"));

			IExtension[] extsA = registry.getExtensions(contributorA);
			assertNotNull(extsA);
			assertEquals(1, extsA.length);
			assertTrue(extsA[0].getUniqueIdentifier().equals("org.eclipse.test.registryByContrib.ExtensionA"));

			// verify fragment
			IContributor contributorAF = ContributorFactoryOSGi.createContributor(fragment);
			IExtensionPoint[] extPointsFragmentA = registry.getExtensionPoints(contributorAF);
			assertNotNull(extPointsFragmentA);
			assertEquals(1, extPointsFragmentA.length);
			assertTrue(
					extPointsFragmentA[0].getUniqueIdentifier().equals("org.eclipse.test.registryByContrib.PointFA"));

			IExtension[] extsFragmentA = registry.getExtensions(contributorAF);
			assertNotNull(extsFragmentA);
			assertEquals(1, extsFragmentA.length);
			assertTrue(extsFragmentA[0].getUniqueIdentifier().equals("org.eclipse.test.registryByContrib.ExtensionFA"));

		} finally {
			if (bundleA != null) {
				bundleA.uninstall();
			}
			if (bundleB != null) {
				bundleB.uninstall();
			}
			if (fragment != null) {
				fragment.uninstall();
			}
		}
	}

	/**
	 * Checks
	 * {@link IDynamicExtensionRegistry#removeContributor(IContributor, Object)}. A
	 * separate registry is created as removal functionality is not allowed by the
	 * default Eclipse registry.
	 */
	@Test
	public void testContributorRemoval() throws IOException {
		Object masterKey = new Object();
		IExtensionRegistry registry = RegistryFactory.createRegistry(null, masterKey, null);

		assertTrue(addContribution(registry, "A"));
		assertTrue(addContribution(registry, "B"));

		assertNotNull(registry.getExtensionPoint("org.eclipse.test.registryByContrib.PointA"));
		assertNotNull(registry.getExtensionPoint("org.eclipse.test.registryByContrib.PointB"));

		IContributor[] contributors = ((IDynamicExtensionRegistry) registry).getAllContributors();
		assertNotNull(contributors);
		assertEquals(2, contributors.length);
		IContributor contributorB = null;
		for (IContributor contributor : contributors) {
			if ("B".equals(contributor.getName())) {
				contributorB = contributor;
				break;
			}
		}
		assertNotNull(contributorB);

		((IDynamicExtensionRegistry) registry).removeContributor(contributorB, masterKey);

		assertNotNull(registry.getExtensionPoint("org.eclipse.test.registryByContrib.PointA"));
		assertNull(registry.getExtensionPoint("org.eclipse.test.registryByContrib.PointB"));
	}

	private boolean addContribution(IExtensionRegistry registry, String fileName) throws IOException {
		String fullPath = "Plugin_Testing/registry/elementsByContributor/" + fileName + "/plugin.xml";
		URL urlA = FrameworkUtil.getBundle(getClass()).getEntry(fullPath);
		if (urlA == null) {
			throw new IOException("No entry to '" + fullPath //$NON-NLS-1$
					+ "' could be found or caller does not have the appropriate permissions.");//$NON-NLS-1$
		}
		InputStream is = urlA.openStream();
		IContributor nonBundleContributor = ContributorFactorySimple.createContributor(fileName);
		return registry.addContribution(is, nonBundleContributor, false, urlA.getFile(), null, null);
	}
}
