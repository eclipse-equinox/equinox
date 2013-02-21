/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.security;

import junit.framework.*;

public class SecurityTestSuite extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite("Unit tests for Equinox security");
		//trust engine tests
		suite.addTest(KeyStoreTrustEngineTest.suite());
		//signed bundle tests - *uses* trust engine
		suite.addTest(SignedBundleTest.suite());
		suite.addTest(SignedBundleTest.localSuite());
		suite.addTest(OSGiAPICertificateTest.suite());
		return suite;
	}
}
