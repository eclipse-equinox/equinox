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
import org.osgi.service.metatype.*;

public class NoADTest extends AbstractTest {
	private Bundle bundle;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb10"); //$NON-NLS-1$
	}

	/*
	 * Ensures an OCD can exist with no ADs
	 */
	@Test
	public void testNoADs() {
		doTestNoADs();
		restartMetatype();
		doTestNoADs();
	}

	private void doTestNoADs() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		String[] pids = mti.getPids();
		Assert.assertNotNull("The pid was not present.", pids); //$NON-NLS-1$
		Assert.assertEquals("Not the expected number of pids.", 1, pids.length); //$NON-NLS-1$
		Assert.assertEquals("Expected pid was not present.", "no.ad.designate", pids[0]); //$NON-NLS-1$ //$NON-NLS-2$
		ObjectClassDefinition ocd = mti.getObjectClassDefinition(pids[0], null);
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		// seems we are not really spec compliant here
		if (ads != null) {
			// should really be null, but if not then make sure it is an empty array
			Assert.assertEquals("Found some ads: " + ads, 0, ads.length); //$NON-NLS-1$
		}
	}
}
