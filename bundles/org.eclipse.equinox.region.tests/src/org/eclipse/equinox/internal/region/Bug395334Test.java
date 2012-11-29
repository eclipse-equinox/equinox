/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.region;

import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.eclipse.equinox.region.tests.system.AbstractRegionSystemTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;

/**
 * The StandardRegionFilter.isAllowed(BundleRevision) method must match against 
 * the bundle-symbolic-name attribute as per the RegionFilter API.
 */
public class Bug395334Test extends AbstractRegionSystemTest {
	public void testBug395334() throws Exception {
		RegionFilterBuilder filterBuilder = digraph.createRegionFilterBuilder();
		filterBuilder.allow(RegionFilter.VISIBLE_BUNDLE_NAMESPACE, "(bundle-symbolic-name=org.eclipse.osgi)");
		RegionFilter filter = filterBuilder.build();
		Bundle systemBundle = getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		assertTrue("The bundle revision should be allowed by the filter", filter.isAllowed(systemBundle.adapt(BundleRevision.class)));
	}
}
