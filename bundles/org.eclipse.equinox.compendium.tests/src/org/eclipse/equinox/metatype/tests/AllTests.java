/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others
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
package org.eclipse.equinox.metatype.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AttributeTypePasswordTest.class, Bug332161Test.class, Bug340899Test.class, BugTests.class,
		SameOcdPidFactoryPidTest.class, ExtendableTest.class, Bug358969Test.class, UnresolvedBundleTest.class,
		GetDefaultValueTest.class, IconTest.class, Bug395196Test.class, NoADTest.class, GetMinMaxTest.class })
public class AllTests {
	// see @SuiteClasses
}
