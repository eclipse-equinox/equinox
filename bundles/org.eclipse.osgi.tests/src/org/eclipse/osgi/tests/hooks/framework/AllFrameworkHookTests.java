/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package org.eclipse.osgi.tests.hooks.framework;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllFrameworkHookTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(AllFrameworkHookTests.class.getName());
		suite.addTest(new TestSuite(StorageHookTests.class));
		suite.addTest(new TestSuite(ClassLoaderHookTests.class));
		suite.addTest(new TestSuite(BundleFileWrapperFactoryHookTests.class));
		suite.addTest(new TestSuite(ContextFinderTests.class));
		suite.addTest(new TestSuite(DevClassPathWithExtensionTests.class));
		suite.addTest(new TestSuite(EmbeddedEquinoxWithURLInClassLoadTests.class));
		suite.addTest(new TestSuite(ActivatorOrderTest.class));
		return suite;
	}
}
