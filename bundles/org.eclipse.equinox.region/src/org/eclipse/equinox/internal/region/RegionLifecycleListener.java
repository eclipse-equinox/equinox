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

package org.eclipse.equinox.internal.region;

import org.eclipse.equinox.region.Region;

/**
 * {@link RegionLifecycleListener} is a service interface to listen for regions being added to and deleted from the
 * region digraph.
 * <p />
 * Note that this is an internal interface and is not intended for external use.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Implementations of this interface must be thread safe.
 */
public interface RegionLifecycleListener {

	/**
	 * Called after the given region is added to the digraph.
	 * 
	 * @param region the region which has been added
	 */
	void regionAdded(Region region);

	/**
	 * Called before the given region is removed from the digraph.
	 * 
	 * @param region the region which is about to be removed
	 */
	void regionRemoving(Region region);

}
