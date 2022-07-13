/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.equinox.region.tests;

import org.eclipse.equinox.region.internal.tests.*;
import org.eclipse.equinox.region.internal.tests.hook.*;
import org.eclipse.equinox.region.tests.system.Bug346127Test;
import org.eclipse.equinox.region.tests.system.RegionSystemTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({RegionBundleCollisionHookTests.class, RegionBundleEventHookTests.class, RegionBundleFindHookTests.class, RegionResolverHookTests.class, RegionServiceEventHookTests.class, RegionServiceFindHookTests.class, BundleIdBasedRegionTests.class, StandardRegionDigraphPeristenceTests.class, StandardRegionDigraphTests.class, StandardRegionFilterTests.class, RegionSystemTests.class, Bug346127Test.class

})
public class AllTests {
	// see @SuiteClasses
}
