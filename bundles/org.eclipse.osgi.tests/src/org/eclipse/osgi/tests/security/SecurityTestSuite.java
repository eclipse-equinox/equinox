/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.osgi.tests.security;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SecurityTestSuite extends TestCase {
	public static Test suite() {
		TestSuite suite = new TestSuite("Unit tests for Equinox security");
		suite.addTest(new JUnit4TestAdapter(BundleToJarInputStreamTest.class));
		// trust engine tests
		suite.addTest(KeyStoreTrustEngineTest.suite());
		// signed bundle tests - *uses* trust engine
		suite.addTest(SignedBundleTest.suite());
		suite.addTest(SignedBundleTest.localSuite());
		suite.addTest(OSGiAPICertificateTest.suite());
		return suite;
	}
}
