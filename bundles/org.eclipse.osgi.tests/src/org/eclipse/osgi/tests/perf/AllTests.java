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
package org.eclipse.osgi.tests.perf;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ StatePerformanceTest.class, StateUsesPerformanceTest.class })
public class AllTests {
	public static final String DEGRADATION_RESOLUTION = "Performance decrease caused by additional fuctionality required for ResovlerHooks in OSGi R4.3 specification. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=324753 for details.";
}
