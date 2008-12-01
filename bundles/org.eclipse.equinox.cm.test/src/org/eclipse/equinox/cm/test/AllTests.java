/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.cm.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.eclipse.equinox.cm.test");
		suite.addTestSuite(ConfigurationAdminTest.class);
		suite.addTestSuite(ManagedServiceFactoryTest.class);
		suite.addTestSuite(ManagedServiceTest.class);
		suite.addTestSuite(ConfigurationDictionaryTest.class);
		suite.addTestSuite(ConfigurationPluginTest.class);
		suite.addTestSuite(ConfigurationListenerTest.class);
		suite.addTestSuite(ConfigurationEventAdapterTest.class);
		return suite;
	}

}
