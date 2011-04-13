/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.region.tests;

import junit.framework.TestSuite;

import org.eclipse.equinox.region.tests.system.RegionSystemTests;

import junit.framework.*;
import org.eclipse.virgo.kernel.osgi.region.hook.*;
import org.eclipse.virgo.kernel.osgi.region.internal.*;

public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Equinox Region Tests");
		suite.addTest(new JUnit4TestAdapter(RegionBundleEventHookTests.class));
		suite.addTest(new JUnit4TestAdapter(RegionBundleFindHookTests.class));
		suite.addTest(new JUnit4TestAdapter(RegionResolverHookTests.class));
		suite.addTest(new JUnit4TestAdapter(RegionServiceEventHookTests.class));
		suite.addTest(new JUnit4TestAdapter(RegionServiceFindHookTests.class));
		suite.addTest(new JUnit4TestAdapter(BundleIdBasedRegionTests.class));
		suite.addTest(new JUnit4TestAdapter(StandardRegionDigraphPeristenceTests.class));
		suite.addTest(new JUnit4TestAdapter(StandardRegionDigraphTests.class));
		suite.addTest(new JUnit4TestAdapter(StandardRegionFilterTests.class));
		suite.addTest(new TestSuite(RegionSystemTests.class));
		return suite;
	}
}
