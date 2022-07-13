/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.serviceregistry;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ //
		ServiceRegistryTests.class, //
		ServiceExceptionTests.class, //
		ServiceHookTests.class, //
		ServiceTrackerTests.class //
})
public class AllTests {
}
