/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		suite.addTest(PlatformAdminTest.suite());
		suite.addTest(StateResolverTest.suite());
		suite.addTest(StateCycleTest.suite());
		suite.addTest(StateComparisonTest.suite());
		suite.addTest(VersionRangeTests.suite());
		suite.addTest(R4ResolverTest.suite());
		return suite;
	}
}
