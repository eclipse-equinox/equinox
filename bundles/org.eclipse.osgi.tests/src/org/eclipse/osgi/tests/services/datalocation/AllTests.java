/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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
package org.eclipse.osgi.tests.services.datalocation;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests extends TestSuite {

	public AllTests() {
		super();
	}

	public AllTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(LocationAreaSessionTest.suite());
		suite.addTest(BasicLocationTests.suite());
		suite.addTest(SimpleTests.suite());
		suite.addTest(FileManagerTests.suite());
		suite.addTest(StreamManagerTests.suite());
		return suite;
	}
}
