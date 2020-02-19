/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.osgi.tests.services.resolver;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ SubstitutableExportsTest.class, DisabledInfoTest.class, PlatformAdminTest.class,
		StateResolverTest.class, StateCycleTest.class, StateComparisonTest.class, VersionRangeTests.class,
		R4ResolverTest.class, XFriendsInternalResolverTest.class, GenericCapabilityTest.class, OSGiCapabilityTest.class,
		DevModeTest.class })
public class AllTests {
}
