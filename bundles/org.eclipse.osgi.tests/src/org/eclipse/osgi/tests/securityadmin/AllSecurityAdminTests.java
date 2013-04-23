/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
