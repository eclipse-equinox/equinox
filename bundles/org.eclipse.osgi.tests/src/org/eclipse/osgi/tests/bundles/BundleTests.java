/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BundleTests {
	public static Test suite() {
		TestSuite suite = new TestSuite(BundleTests.class.getName());
		suite.addTest(ConnectTests.suite());
		suite.addTest(ExceptionMessageTest.suite());
		suite.addTest(ImportJavaSEPackagesTests.suite());
		suite.addTest(MultiReleaseJarTests.suite());
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
		suite.addTest(ListenerTests.suite());
		return suite;
	}
}
