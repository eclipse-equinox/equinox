/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
import org.eclipse.osgi.tests.resolver.*;

public class R4ResolverTest extends TestCase {

	public R4ResolverTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(R4ResolverTest.class.getName());
		suite.addTest(new TestSuite(TestAttributes_001.class));
		suite.addTest(new TestSuite(TestBSN_001.class));
		suite.addTest(new TestSuite(TestCycle_001.class));
		suite.addTest(new TestSuite(TestCycle_002.class));
		suite.addTest(new TestSuite(TestCycle_003.class));
		suite.addTest(new TestSuite(TestCycle_004.class));
		suite.addTest(new TestSuite(TestCycle_005.class));
		suite.addTest(new TestSuite(TestCycle_006.class));
		suite.addTest(new TestSuite(TestDynamic_001.class));
		suite.addTest(new TestSuite(TestDynamic_002.class));
		suite.addTest(new TestSuite(TestDynamic_003.class));
		suite.addTest(new TestSuite(TestDynamic_004.class));
		suite.addTest(new TestSuite(TestDynamic_005.class));
		suite.addTest(new TestSuite(TestDynamic_006.class));
		suite.addTest(new TestSuite(TestGenerated_001.class));
		suite.addTest(new TestSuite(TestGrouping_001.class));
		suite.addTest(new TestSuite(TestGrouping_002.class));
		suite.addTest(new TestSuite(TestGrouping_003.class));
		suite.addTest(new TestSuite(TestGrouping_006.class));
		suite.addTest(new TestSuite(TestGrouping_008.class));
		suite.addTest(new TestSuite(TestOptional_001.class));
		suite.addTest(new TestSuite(TestOptional_002.class));
		suite.addTest(new TestSuite(TestPropagation_001.class));
		suite.addTest(new TestSuite(TestPropagation_003.class));
		suite.addTest(new TestSuite(TestRFC79_001.class));
		suite.addTest(new TestSuite(TestRFC79_002.class));
		suite.addTest(new TestSuite(TestRFC79_003.class));
		suite.addTest(new TestSuite(TestRFC79_004.class));
		suite.addTest(new TestSuite(TestRFC79_005.class));
		suite.addTest(new TestSuite(TestRFC79_006.class));
		suite.addTest(new TestSuite(TestRFC79_007.class));
		suite.addTest(new TestSuite(TestVersion_001.class));
		suite.addTest(new TestSuite(TestVersion_002.class));
		suite.addTest(new TestSuite(TestVersion_003.class));
		return suite;
	}

}
