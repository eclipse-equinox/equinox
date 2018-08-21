/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.region.internal.tests;

import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.eclipse.equinox.region.tests.system.AbstractRegionSystemTest;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;

/**
 * The StandardRegionFilter.isAllowed(BundleRevision) method must match against 
 * the bundle-symbolic-name attribute as per the RegionFilter API.
 */
public class Bug395334Test extends AbstractRegionSystemTest {
	@Test
	public void testBug395334() throws Exception {
		RegionFilterBuilder filterBuilder = digraph.createRegionFilterBuilder();
		filterBuilder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(bundle-symbolic-name=org.eclipse.osgi)");
		RegionFilter filter = filterBuilder.build();
		Bundle systemBundle = getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		assertTrue("The bundle revision should be allowed by the filter", filter.isAllowed(systemBundle.adapt(BundleRevision.class)));
	}
}
