/*******************************************************************************
 * Copyright (c) 2008, 2013 VMware Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.internal.region.management.StandardManageableRegionDigraph;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.*;
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
	private static final String REGION_REGISTER_MBEANS = "org.eclipse.equinox.region.register.mbeans"; //$NON-NLS-1$
	private static final Dictionary<String, Object> MAX_RANKING = new Hashtable<String, Object>(Collections.singletonMap(Constants.SERVICE_RANKING, Integer.MAX_VALUE));

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
		// after registering the region hooks we need to verify no ophans exist
		// if they do then we assume the need to be in the kernel region
		checkForOrphans(digraph);
		digraphMBean = registerDigraphMbean(digraph);
		registerService(RegionDigraph.class, digraph);
	}

	private void checkForOrphans(StandardRegionDigraph regionDigraph) {
		// we assume the system bundle is in the root region.
		Region rootRegion = regionDigraph.getRegion(0);
		if (rootRegion != null) {
			Bundle[] bundles = bundleContext.getBundles();
			for (Bundle bundle : bundles) {
				if (regionDigraph.getRegion(bundle) == null) {
					// we have an orphan; add it to the root region
					try {
						rootRegion.addBundle(bundle.getBundleId());
					} catch (BundleException e) {
						// ignore, someone added the bundle to another region since we checked
					}
				}
			}
		}
	}

	public void stop(BundleContext bc) throws IOException {
		if (digraphMBean != null) {
			digraphMBean.unregisterMbean();
			digraphMBean = null;
		}
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
		try (InputStream in = new BufferedInputStream(new FileInputStream(digraphFile))) {
			// TODO need to validate bundle IDs to make sure they are consistent with current bundles
			return StandardRegionDigraphPersistence.readRegionDigraph(new DataInputStream(in), this.bundleContext, this.threadLocal);
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
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(bundleContext.getDataFile(DIGRAPH_FILE)))) {
			digraph.getRegionDigraphPersistence().save(digraph, out);
		}
	}

	private StandardManageableRegionDigraph registerDigraphMbean(RegionDigraph regionDigraph) {
		if ("false".equals(this.bundleContext.getProperty(REGION_REGISTER_MBEANS))) { //$NON-NLS-1$
			return null;
		}
		StandardManageableRegionDigraph standardManageableRegionDigraph = new StandardManageableRegionDigraph(regionDigraph, this.domain, this.bundleContext);
		standardManageableRegionDigraph.registerMBean();
		return standardManageableRegionDigraph;
	}

	@SuppressWarnings("deprecation")
	private void registerRegionHooks(StandardRegionDigraph regionDigraph) {
		registerService(ResolverHookFactory.class, regionDigraph.getResolverHookFactory());

		registerService(CollisionHook.class, regionDigraph.getBundleCollisionHook());
		registerService(FindHook.class, regionDigraph.getBundleFindHook());
		registerService(EventHook.class, regionDigraph.getBundleEventHook());

		registerService(org.osgi.framework.hooks.service.FindHook.class, regionDigraph.getServiceFindHook());
		registerService(org.osgi.framework.hooks.service.EventHook.class, regionDigraph.getServiceEventHook());
	}

	private <S> void registerService(Class<S> clazz, S service) {
		this.registrations.add(this.bundleContext.registerService(clazz, service, MAX_RANKING));
	}
}
