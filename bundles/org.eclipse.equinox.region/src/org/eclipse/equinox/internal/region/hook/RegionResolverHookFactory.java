/*******************************************************************************
 * Copyright (c) 2010, 2011 VMware Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SpringSource, a division of VMware - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.equinox.internal.region.hook;

import java.util.Collection;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

/**
 * {@link RegionResolverHook} constructs an instance of
 * {@link RegionResolverHook} for a particular resolution operation.
 * <p>
 * <strong>Concurrent Semantics</strong>
 * </p>
 * 
 * Thread safe.
 */
public final class RegionResolverHookFactory implements ResolverHookFactory {

	private final RegionDigraph regionDigraph;

	public RegionResolverHookFactory(RegionDigraph regionDigraph) {
		this.regionDigraph = regionDigraph;
	}

	@Override
	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return new RegionResolverHook(this.regionDigraph);
	}

}
