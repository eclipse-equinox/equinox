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

package org.eclipse.equinox.region.management;

import org.eclipse.equinox.region.Region;

import javax.management.MXBean;

/**
 * A {@link ManageableRegion} is a JMX representation of a {@link Region}.
 * <p />
 * 
 * <strong>Concurrent Semantics</strong><br />
 * Thread safe.
 */
@MXBean
public interface ManageableRegion {

	/**
	 * Returns the region name.
	 * 
	 * @return the region name
	 */
	String getName();

	/**
	 * Returns the {@ManageableRegion}s that this region depends upon.
	 * 
	 * @return an array of {@link ManageableRegion}s
	 */
	ManageableRegion[] getDependencies();

	/**
	 * Returns the bundle ids belonging to this region.
	 * 
	 * @return an array of bundle ids
	 */
	long[] getBundleIds();

}
