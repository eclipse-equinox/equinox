/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ //
		ConnectTests.class, //
		ExceptionMessageTest.class, //
		ImportJavaSEPackagesTests.class, //
		MultiReleaseJarTests.class, //
		URLHandlerTests.class, //
		PersistedBundleTests.class, //
		CascadeConfigTests.class, //
		DiscardBundleTests.class, //
		EquinoxBundleAdaptTests.class, //
		LoggingTests.class, //
		BundleResourceTests.class, //
		BundleInstallUpdateTests.class, //
		SystemBundleTests.class, //
		BundleExceptionTests.class, //
		SubstituteExportsBundleTests.class, //
		PackageAdminBundleTests.class, //
		ExtensionBundleTests.class, //
		ClassLoadingBundleTests.class, //
		NativeCodeBundleTests.class, //
		PlatformAdminBundleTests.class, //
		ListenerTests.class, //
		AddDynamicImportTests.class //
})
public class BundleTests {
}
