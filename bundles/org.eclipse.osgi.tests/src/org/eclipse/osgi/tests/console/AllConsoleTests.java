/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.console;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllConsoleTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllConsoleTests.class.getName());
		suite.addTest(TestCommandExecution.suite());
		suite.addTest(TestEquinoxStartWithConsole.suite());
		suite.addTest(TestFrameworkCommandInterpreter.suite());
		suite.addTest(TestEquinoxStartWithoutConsole.suite());
		suite.addTest(TestRestrictedTelnetHost.suite());
		return suite;
	}
}
