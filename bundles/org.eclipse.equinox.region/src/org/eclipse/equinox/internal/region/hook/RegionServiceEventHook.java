/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.equinox.internal.region.hook;

import java.util.*;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.*;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;

/**
 * {@link RegionServiceEventHook} manages the visibility of service events across regions according to the
 * {@link RegionDigraph}.
 * <p>
 * The current implementation delegates to {@link RegionServiceFindHook}. This is likely to perform adequately because
 * of the relatively low frequency (compared to service lookups) of service events and the typically small number of
 * service listeners.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
@SuppressWarnings("deprecation")
public final class RegionServiceEventHook implements EventHook {

	private final FindHook serviceFindHook;

	public RegionServiceEventHook(FindHook bundleFindBook) {
		this.serviceFindHook = bundleFindBook;
	}

	/**
	 * {@inheritDoc}
	 */
	public void event(ServiceEvent event, Collection<BundleContext> contexts) {
		ServiceReference<?> eventBundle = event.getServiceReference();
		Iterator<BundleContext> i = contexts.iterator();
		while (i.hasNext()) {
			if (!find(i.next(), eventBundle)) {
				i.remove();
			}
		}
	}

	private boolean find(BundleContext finderBundleContext, ServiceReference<?> candidateServiceReference) {
		Collection<ServiceReference<?>> candidates = new ArrayList<ServiceReference<?>>();
		candidates.add(candidateServiceReference);
		this.serviceFindHook.find(finderBundleContext, "", "", false, candidates); //$NON-NLS-1$ //$NON-NLS-2$
		return !candidates.isEmpty();
	}

}
