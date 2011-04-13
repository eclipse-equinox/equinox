/*******************************************************************************
 * Copyright (c) 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.virgo.kernel.osgi.region.hook;

import java.util.Collection;

import org.eclipse.virgo.kernel.osgi.region.RegionDigraph;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

/**
 * {@link RegionResolverHook} constructs an instance of {@link RegionResolverHook} for a particular resolution
 * operation.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * 
 * Thread safe.
 */
public final class RegionResolverHookFactory implements ResolverHookFactory {

    private final RegionDigraph regionDigraph;

    public RegionResolverHookFactory(RegionDigraph regionDigraph) {
        this.regionDigraph = regionDigraph;
    }

    public ResolverHook begin(Collection<BundleRevision> triggers) {
        return new RegionResolverHook(this.regionDigraph);
    }

}
