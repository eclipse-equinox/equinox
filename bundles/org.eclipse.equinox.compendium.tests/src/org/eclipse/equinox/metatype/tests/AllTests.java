/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for Equinox MetaType"); //$NON-NLS-1$
		suite.addTestSuite(AttributeTypePasswordTest.class);
		suite.addTestSuite(Bug332161Test.class);
		suite.addTestSuite(Bug340899Test.class);
		suite.addTestSuite(BugTests.class);
		suite.addTestSuite(SameOcdPidFactoryPidTest.class);
		suite.addTestSuite(ExtendableTest.class);
		suite.addTestSuite(Bug358969Test.class);
		suite.addTestSuite(UnresolvedBundleTest.class);
		suite.addTestSuite(GetDefaultValueTest.class);
		return suite;
	}
}
