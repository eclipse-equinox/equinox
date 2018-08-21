/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
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
package org.eclipse.equinox.cm.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ConfigurationAdminTest.class, ManagedServiceFactoryTest.class, ManagedServiceTest.class, ConfigurationDictionaryTest.class, ConfigurationPluginTest.class, ConfigurationListenerTest.class, ConfigurationEventAdapterTest.class})
public class AllTests {
	// see @SuitClasses
}
