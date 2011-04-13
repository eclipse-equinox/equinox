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
package org.eclipse.equinox.region.tests.system;

import java.util.Arrays;

import java.util.List;

import org.eclipse.virgo.kernel.osgi.region.Region;

import org.osgi.framework.BundleContext;

import org.osgi.framework.ServiceReference;

import org.eclipse.virgo.kernel.osgi.region.RegionDigraph;

import org.osgi.framework.wiring.BundleRevision;

import org.osgi.framework.wiring.BundleWiring;

import org.osgi.framework.Bundle;

import org.osgi.framework.FrameworkUtil;

import org.eclipse.equinox.region.tests.BundleInstaller;

import junit.framework.TestCase;

public class AbstractRegionSystemTest extends TestCase{
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
	public static List<String> ALL = Arrays.asList(
			PP1, 
			SP1, 
			CP1, 
			PP2, 
			SP2,
			CP2,
			PC1,
			BC1,
			SC1,
			CC1);

	protected BundleInstaller bundleInstaller;
	protected RegionDigraph digraph;
	protected BundleContext context;
	ServiceReference<RegionDigraph> digraphReference;

	@Override
	protected void setUp() throws Exception {
		// this is a fragment of the region impl bundle
		// this line makes sure the region impl bundle is started
		Bundle regionBundle = FrameworkUtil.getBundle(this.getClass());
		regionBundle.start();
		context = regionBundle.getBundleContext();
		assertNotNull("No context found", context);

		digraphReference = context.getServiceReference(RegionDigraph.class);
		assertNotNull("No digraph found", digraphReference);
		digraph = context.getService(digraphReference);
		assertNotNull("No digraph found");

		// fun code to get our fragment bundle
		Bundle testBundle = regionBundle.adapt(BundleWiring.class).getProvidedWires(BundleRevision.HOST_NAMESPACE).get(0).getRequirerWiring().getBundle();
		bundleInstaller = new BundleInstaller("bundle_tests", regionBundle.getBundleContext(), testBundle); //$NON-NLS-1$
	}

	@Override
	protected void tearDown() throws Exception {
		for (Region region : digraph) {
			if (!region.contains(0)) {
				digraph.removeRegion(region);
				for (Long bundleID : region.getBundleIds()) {
					Bundle b = context.getBundle(bundleID);
					b.uninstall();
				}
			}
		}
		bundleInstaller.shutdown();
		if (digraphReference != null)
			context.ungetService(digraphReference);
	}

	
}
