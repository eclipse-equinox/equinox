/*******************************************************************************
 * Copyright (c) 2011 VMware Inc.
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

package org.eclipse.equinox.region.management;

import javax.management.MXBean;
import org.eclipse.equinox.region.Region;

/**
 * A {@link ManageableRegion} is a JMX representation of a {@link Region}.
 * <p>
 * 
 * <strong>Concurrent Semantics</strong>
 * </p>
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
	 * Returns the {@link ManageableRegion}s that this region depends upon.
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
