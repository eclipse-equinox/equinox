/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.osgi.tests.services.resolver;

import junit.framework.*;

public class AllTests extends TestCase {

	public AllTests() {
		super(null);
	}

	public AllTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(SubstitutableExportsTest.suite());
		suite.addTest(DisabledInfoTest.suite());
		suite.addTest(PlatformAdminTest.suite());
		suite.addTest(StateResolverTest.suite());
		suite.addTest(StateCycleTest.suite());
		suite.addTest(StateComparisonTest.suite());
		suite.addTest(VersionRangeTests.suite());
		suite.addTest(R4ResolverTest.suite());
		suite.addTest(XFriendsInternalResolverTest.suite());
		suite.addTest(GenericCapabilityTest.suite());
		suite.addTest(OSGiCapabilityTest.suite());
		suite.addTest(DevModeTest.suite());
		return suite;
	}
}
