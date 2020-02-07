/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
package org.eclipse.osgi.tests.eclipseadaptor;

import junit.framework.*;

public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(EnvironmentInfoTest.suite());
		suite.addTest(FilePathTest.suite());
		suite.addTest(new JUnit4TestAdapter(LocaleTransformationTest.class));
		return suite;
	}
}
