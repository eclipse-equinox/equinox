/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
package org.eclipse.osgi.tests.securityadmin;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllSecurityAdminTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllSecurityAdminTests.class.getName());
		suite.addTest(SecurityAdminUnitTests.suite());
		suite.addTest(SecurityManagerTests.suite());
		return suite;
	}
}
