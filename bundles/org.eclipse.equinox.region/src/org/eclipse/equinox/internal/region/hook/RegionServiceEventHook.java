/*******************************************************************************
 * Copyright (c) 2011, 2015 VMware Inc.
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

package org.eclipse.equinox.internal.region.hook;

import java.util.*;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.EventHook;

/**
 * {@link RegionServiceEventHook} manages the visibility of service events across regions according to the
 * {@link RegionDigraph}.
 * <p>
 * The current implementation avoids traversing the graph multiple times from the same region.
 * This is necessary to optimize the case where many bundles with service listeners
 * are contained in the same region.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
@SuppressWarnings("deprecation")
public final class RegionServiceEventHook implements EventHook {

	private final RegionDigraph regionDigraph;

	public RegionServiceEventHook(RegionDigraph regionDigraph) {
		this.regionDigraph = regionDigraph;
	}

	/**
	 * {@inheritDoc}
	 */
	public void event(ServiceEvent event, Collection<BundleContext> contexts) {
		ServiceReference<?> eventService = event.getServiceReference();
		Map<Region, Boolean> regionAccess = new HashMap<>();
		Iterator<BundleContext> i = contexts.iterator();
		while (i.hasNext()) {
			Bundle bundle = RegionBundleFindHook.getBundle(i.next());
			if (bundle == null) {
				// no bundle for context remove access from it
				i.remove();
				continue;
			}
			if (bundle.getBundleId() == 0L) {
				// let system bundle see all
				continue;
			}
			Region region = regionDigraph.getRegion(bundle);
			if (region == null) {
				// no region for context remove access from it
				i.remove();
			} else {
				Boolean accessible = regionAccess.get(region);
				if (accessible == null) {
					// we have not checked this region's access do it now
					accessible = isAccessible(region, eventService);
					regionAccess.put(region, accessible);
				}
				if (!accessible) {
					i.remove();
				}
			}
		}
	}

	private Boolean isAccessible(Region region, ServiceReference<?> candidateServiceReference) {
		Collection<ServiceReference<?>> candidates = new ArrayList<>(1);
		candidates.add(candidateServiceReference);
		RegionServiceFindHook.find(region, candidates);
		return !candidates.isEmpty();
	}

}
