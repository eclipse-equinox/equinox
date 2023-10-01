/*******************************************************************************
 *  Copyright (c) 2018 Julian Honnen
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
package org.eclipse.equinox.common.tests.registry.simple;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ XMLExtensionCreateTest.class, DirectExtensionCreateTest.class, XMLExecutableExtensionTest.class,
		DirectExtensionCreateTwoRegistriesTest.class, TokenAccessTest.class, XMLExtensionCreateEclipseTest.class,
		DirectExtensionRemoveTest.class, MergeContributionTest.class, DuplicatePointsTest.class })
public class SimpleRegistryTests {
	// intentionally left blank
}
