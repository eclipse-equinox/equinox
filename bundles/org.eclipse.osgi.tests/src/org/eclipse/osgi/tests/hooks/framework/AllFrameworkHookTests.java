/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ //
		StorageHookTests.class, //
		ClassLoaderHookTests.class, //
		BundleFileWrapperFactoryHookTests.class, //
		ContextFinderTests.class, //
		DevClassPathWithExtensionTests.class, //
		EmbeddedEquinoxWithURLInClassLoadTests.class, //
		ActivatorOrderTest.class, //
		DevClassPathDuplicateTests.class //
})
public class AllFrameworkHookTests {
}
