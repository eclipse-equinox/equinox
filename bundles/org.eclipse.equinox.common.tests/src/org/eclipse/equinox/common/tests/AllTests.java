/*******************************************************************************
 * Copyright (c) 2018 Julian Honnen
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Julian Honnen - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import org.eclipse.core.runtime.tests.FileLocatorTest;
import org.eclipse.equinox.common.tests.adaptable.AdaptableTests;
import org.eclipse.equinox.common.tests.registry.RegistryTests;
import org.eclipse.equinox.common.tests.registry.simple.SimpleRegistryTests;
import org.eclipse.equinox.common.tests.text.StringMatcherTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ StringMatcherTests.class, RuntimeTests.class, AdaptableTests.class, RegistryTests.class,
		SimpleRegistryTests.class, FileLocatorTest.class })
public class AllTests {
	// intentionally left blank
}
