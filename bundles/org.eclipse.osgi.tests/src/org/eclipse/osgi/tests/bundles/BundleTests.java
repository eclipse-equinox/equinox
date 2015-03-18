/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BundleTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(BundleTests.class.getName());
		suite.addTest(URLHandlerTests.suite());
		suite.addTest(PersistedBundleTests.suite());
		suite.addTest(CascadeConfigTests.suite());
		suite.addTest(DiscardBundleTests.suite());
		suite.addTest(LoggingTests.suite());
		suite.addTest(BundleResourceTests.suite());
		suite.addTest(BundleInstallUpdateTests.suite());
		suite.addTest(SystemBundleTests.suite());
		suite.addTest(BundleExceptionTests.suite());
		suite.addTest(SubstituteExportsBundleTests.suite());
		suite.addTest(PackageAdminBundleTests.suite());
		suite.addTest(ExtensionBundleTests.suite());
		suite.addTest(ClassLoadingBundleTests.suite());
		suite.addTest(NativeCodeBundleTests.suite());
		suite.addTest(PlatformAdminBundleTests.suite());
		return suite;
	}
}
