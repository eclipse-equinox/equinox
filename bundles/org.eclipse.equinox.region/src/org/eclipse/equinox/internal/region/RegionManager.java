/*******************************************************************************
 * Copyright (c) 2008, 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.internal.region.management.StandardManageableRegionDigraph;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

/**
 * Creates and manages the {@link RegionDigraph} associated
 * with the running framework.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Threadsafe.
 * 
 */
public final class RegionManager implements BundleActivator {

	private static final String REGION_KERNEL = "org.eclipse.equinox.region.kernel"; //$NON-NLS-1$
	private static final String REGION_DOMAIN_PROP = "org.eclipse.equinox.region.domain"; //$NON-NLS-1$
	private static final String DIGRAPH_FILE = "digraph"; //$NON-NLS-1$

	Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

	private BundleContext bundleContext;

	private final ThreadLocal<Region> threadLocal = new ThreadLocal<Region>();

	private String domain;

	private StandardRegionDigraph digraph;

	private StandardManageableRegionDigraph digraphMBean;

	public void start(BundleContext bc) throws BundleException, IOException, InvalidSyntaxException {
		this.bundleContext = bc;
		this.domain = bc.getProperty(REGION_DOMAIN_PROP);
		if (this.domain == null)
			this.domain = REGION_DOMAIN_PROP;
		digraph = loadRegionDigraph();
		registerRegionHooks(digraph);
		digraphMBean = registerDigraphMbean(digraph);
		registerRegionDigraph(digraph);
	}

	public void stop(BundleContext bc) throws IOException {
		digraphMBean.unregisterMbean();
		for (ServiceRegistration<?> registration : registrations)
			registration.unregister();
		saveDigraph();
	}

	private StandardRegionDigraph loadRegionDigraph() throws BundleException, IOException, InvalidSyntaxException {
		File digraphFile = bundleContext.getDataFile(DIGRAPH_FILE);
		if (digraphFile == null || !digraphFile.exists()) {
			// no persistent digraph available, create a new one
			return createRegionDigraph();
		}
		FileInputStream in = new FileInputStream(digraphFile);
		try {
			// TODO need to validate bundle IDs to make sure they are consistent with current bundles
			return StandardRegionDigraphPersistence.readRegionDigraph(new DataInputStream(in), this.bundleContext, this.threadLocal);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// We tried our best to clean up
			}
		}
	}

	private StandardRegionDigraph createRegionDigraph() throws BundleException {
		StandardRegionDigraph regionDigraph = new StandardRegionDigraph(this.bundleContext, this.threadLocal);
		Region kernelRegion = regionDigraph.createRegion(REGION_KERNEL);
		for (Bundle bundle : this.bundleContext.getBundles()) {
			kernelRegion.addBundle(bundle);
		}
		return regionDigraph;
	}

	private void saveDigraph() throws IOException {
		FileOutputStream digraphFile = new FileOutputStream(bundleContext.getDataFile(DIGRAPH_FILE));
		try {
			digraph.getRegionDigraphPersistence().save(digraph, digraphFile);
		} finally {
			try {
				digraphFile.close();
			} catch (IOException e) {
				// ignore;
			}
		}

	}

	private StandardManageableRegionDigraph registerDigraphMbean(RegionDigraph regionDigraph) {
		StandardManageableRegionDigraph standardManageableRegionDigraph = new StandardManageableRegionDigraph(regionDigraph, this.domain, this.bundleContext);
		standardManageableRegionDigraph.registerMBean();
		return standardManageableRegionDigraph;
	}

	private void registerRegionHooks(StandardRegionDigraph regionDigraph) {
		registerResolverHookFactory(regionDigraph.getResolverHookFactory());

		registerBundleCollisionHook(regionDigraph.getBundleCollisionHook());
		registerBundleFindHook(regionDigraph.getBundleFindHook());
		registerBundleEventHook(regionDigraph.getBundleEventHook());

		registerServiceFindHook(regionDigraph.getServiceFindHook());
		registerServiceEventHook(regionDigraph.getServiceEventHook());
	}

	private void registerRegionDigraph(RegionDigraph regionDigraph) {
		this.registrations.add(this.bundleContext.registerService(RegionDigraph.class, regionDigraph, null));
	}

	private void registerServiceFindHook(org.osgi.framework.hooks.service.FindHook serviceFindHook) {
		this.registrations.add(this.bundleContext.registerService(org.osgi.framework.hooks.service.FindHook.class, serviceFindHook, null));
	}

	@SuppressWarnings("deprecation")
	private void registerServiceEventHook(org.osgi.framework.hooks.service.EventHook serviceEventHook) {
		this.registrations.add(this.bundleContext.registerService(org.osgi.framework.hooks.service.EventHook.class, serviceEventHook, null));
	}

	private void registerBundleFindHook(FindHook findHook) {
		this.registrations.add(this.bundleContext.registerService(FindHook.class, findHook, null));
	}

	private void registerBundleEventHook(EventHook eventHook) {
		this.registrations.add(this.bundleContext.registerService(EventHook.class, eventHook, null));

	}

	private void registerBundleCollisionHook(Object collisionHook) {
		if (collisionHook != null) {
			this.registrations.add(this.bundleContext.registerService("org.osgi.framework.hooks.bundle.CollisionHook", collisionHook, null)); //$NON-NLS-1$
		}
	}

	private void registerResolverHookFactory(ResolverHookFactory resolverHookFactory) {
		this.registrations.add(this.bundleContext.registerService(ResolverHookFactory.class, resolverHookFactory, null));
	}

}
