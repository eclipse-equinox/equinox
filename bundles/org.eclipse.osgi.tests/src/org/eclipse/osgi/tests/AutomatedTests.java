/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests;

import junit.framework.*;
import org.eclipse.osgi.tests.bundles.BundleTests;
import org.eclipse.osgi.tests.composites.AllCompositeTests;
import org.eclipse.osgi.tests.eventmgr.EventManagerTests;
import org.eclipse.osgi.tests.filter.FilterTests;
import org.eclipse.osgi.tests.internal.plugins.InstallTests;
import org.eclipse.osgi.tests.listeners.ExceptionHandlerTests;
import org.eclipse.osgi.tests.misc.MiscTests;
import org.eclipse.osgi.tests.permissions.PermissionTests;
import org.eclipse.osgi.tests.serviceregistry.ServiceRegistryTests;

public class AutomatedTests extends TestCase {
	public final static String PI_OSGI_TESTS = "org.eclipse.osgi.tests"; //$NON-NLS-1$

	/**
	 * AllTests constructor.
	 */
	public AutomatedTests() {
		super(null);
	}

	/**
	 * AllTests constructor comment.
	 * @param name java.lang.String
	 */
	public AutomatedTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(AutomatedTests.class.getName());
		//		suite.addTest(new TestSuite(SimpleTests.class));
		suite.addTest(new TestSuite(InstallTests.class));
		suite.addTest(org.eclipse.osgi.tests.eclipseadaptor.AllTests.suite());
		suite.addTest(org.eclipse.osgi.tests.services.resolver.AllTests.suite());
		suite.addTest(org.eclipse.osgi.tests.security.SecurityTestSuite.suite());
		suite.addTest(org.eclipse.osgi.tests.appadmin.AllTests.suite());
		suite.addTest(new TestSuite(ExceptionHandlerTests.class));
		suite.addTest(org.eclipse.osgi.tests.configuration.AllTests.suite());
		suite.addTest(org.eclipse.osgi.tests.services.datalocation.AllTests.suite());
		suite.addTest(org.eclipse.osgi.tests.util.AllTests.suite());
		suite.addTest(MiscTests.suite());
		suite.addTest(BundleTests.suite());
		suite.addTest(ServiceRegistryTests.suite());
		suite.addTest(EventManagerTests.suite());
		suite.addTest(FilterTests.suite());
		suite.addTest(PermissionTests.suite());
		suite.addTest(AllCompositeTests.suite());
		suite.addTest(org.eclipse.osgi.tests.securityadmin.AllSecurityAdminTests.suite());
		return suite;
	}
}
