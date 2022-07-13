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

import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;

public class SameOcdPidFactoryPidTest extends AbstractTest {
	private Bundle bundle;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb2"); //$NON-NLS-1$
		bundle.start();
	}

	/*
	 * Ensures the same OCD referred to by two Designate elements, one using
	 * the factoryPid attribute and the other only the pid attribute, is
	 * accessible via both getPids() and getFactoryPids().
	 */
	@Test
	public void test1() {
		doTest1();
		restartMetatype();
		doTest1();
	}

	private void doTest1() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		String[] pids = mti.getPids();
		Assert.assertNotNull("The pid 'singleton' was not present.", pids); //$NON-NLS-1$
		Assert.assertEquals("Not the expected number of pids.", 1, pids.length); //$NON-NLS-1$
		Assert.assertEquals("Expected pid was not present.", "singleton", pids[0]); //$NON-NLS-1$ //$NON-NLS-2$
		String[] factoryPids = mti.getFactoryPids();
		Assert.assertNotNull("The factory pid 'factory' was not present.", factoryPids); //$NON-NLS-1$
		Assert.assertEquals("Not the expected number of factory pids.", 1, factoryPids.length); //$NON-NLS-1$
		Assert.assertEquals("Expected factory pid was not present.", "factory", factoryPids[0]); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
