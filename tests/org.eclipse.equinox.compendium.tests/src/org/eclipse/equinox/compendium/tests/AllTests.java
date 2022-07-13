/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others
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
package org.eclipse.equinox.compendium.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({org.eclipse.equinox.coordinator.tests.AllTests.class, org.eclipse.equinox.event.tests.AllTests.class, org.eclipse.equinox.metatype.tests.AllTests.class, org.eclipse.equinox.useradmin.tests.AllTests.class})
public class AllTests {
	//see @SuiteClasses
}
