/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.equinox.region.tests.system;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.tests.BundleInstaller;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.*;

/*
 * Here are the dependencies between the bundles:
 * PP1 --> NONE
 * CP1 --> NONE
 * SP1 -- package pkg1.* --> PP1
 * PP2 -- capability CP1 --> CP1
 * SP2 -- package pkg2.* --> PP2
 * CP2 -- package pkg1.* --> PP1
 * CP2 -- service pkg1.* --> SP1
 * PC1 -- package pkg2.* --> PP2
 * BC1 -- bundle PP2     --> PP2
 * SC1 -- service pkg2.* --> SP2
 * SC1 -- package pkg2.* --> PP2
 * CC1 -- capability CP2 --> CP2
 */
public class AbstractRegionSystemTest {
	public static final String PP1 = "PackageProvider1";
	public static final String SP1 = "ServiceProvider1";
	public static final String CP1 = "CapabilityProvider1";
	public static final String PP2 = "PackageProvider2";
	public static final String SP2 = "ServiceProvider2";
	public static final String CP2 = "CapabilityProvider2";
	public static final String PC1 = "PackageClient1";
	public static final String BC1 = "BundleClient1";
	public static final String SC1 = "ServiceClient1";
	public static final String CC1 = "CapabilityClient1";
	public static List<String> ALL = Arrays.asList(PP1, SP1, CP1, PP2, SP2, CP2, PC1, BC1, SC1, CC1);

	public static final String SINGLETON1 = "Singleton1";
	public static final String SINGLETON2 = "Singleton2";

	protected BundleInstaller bundleInstaller;
	protected RegionDigraph digraph;
	protected Bundle testsBundle;
	ServiceReference<RegionDigraph> digraphReference;

	@Before
	public void setUp() throws Exception {
		testsBundle = FrameworkUtil.getBundle(this.getClass());
		BundleContext context = getContext();

		digraphReference = context.getServiceReference(RegionDigraph.class);
		assertNotNull("No digraph reference found", digraphReference);
		digraph = context.getService(digraphReference);
		assertNotNull("No digraph found", digraph);

		bundleInstaller = new BundleInstaller("bundle_tests", testsBundle); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		for (Region region : digraph) {
			if (!region.contains(0)) {
				digraph.removeRegion(region);
			}
		}
		bundleInstaller.shutdown();
		if (digraphReference != null)
			getContext().ungetService(digraphReference);
	}

	protected BundleContext getContext() {
		BundleContext context = testsBundle.getBundleContext();
		assertNotNull("No context available", context);
		return context;
	}
}
