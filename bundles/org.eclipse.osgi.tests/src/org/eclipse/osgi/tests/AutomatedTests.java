/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
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
import org.eclipse.osgi.tests.eclipseadaptor.EnvironmentInfoTest;
import org.eclipse.osgi.tests.internal.plugins.InstallTests;
import org.eclipse.osgi.tests.listeners.ExceptionHandlerTests;

public class AutomatedTests extends TestCase {
	public final static String PI_OSGI_TESTS = "org.eclipse.osgi.tests";

	/**
	 * AllTests constructor comment.
	 * @param name java.lang.String
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
		suite.addTest(new TestSuite(EnvironmentInfoTest.class));
		suite.addTest(org.eclipse.osgi.tests.services.resolver.AllTests.suite());
		suite.addTest(new TestSuite(ExceptionHandlerTests.class));
		suite.addTest(org.eclipse.osgi.tests.configuration.AllTests.suite());
		return suite;
	}
}
