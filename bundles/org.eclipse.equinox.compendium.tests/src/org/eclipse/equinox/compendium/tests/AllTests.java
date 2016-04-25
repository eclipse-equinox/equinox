/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
