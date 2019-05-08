/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others
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

import static org.junit.Assert.assertNull;

import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.*;

/*
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=395196.
 * 
 * This test enforces the behavior of rejecting a the default value of an
 * attribute definition in an XML file that does not match one of the specified
 * options. The attribute definition behaves as if no default value was
 * declared.
 */
public class Bug395196Test extends AbstractTest {
	private AttributeDefinition[] ads;
	private Bundle bundle;
	private MetaTypeInformation mti;
	private ObjectClassDefinition ocd;

	@Test
	public void testRejectDefaultValueWhenNotAnOption() throws Exception {
		doTestRejectDefaultValueWhenNotAnOption();
		restartMetatype();
		doTestRejectDefaultValueWhenNotAnOption();
	}

	private void doTestRejectDefaultValueWhenNotAnOption() {
		AttributeDefinition ad = findAttributeDefinitionById("ocd3-ad1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertNull("Default value not matching one of the options was not rejected", ad.getDefaultValue()); //$NON-NLS-1$
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb1"); //$NON-NLS-1$
		bundle.start();
		getMetaTypeObjects();
	}

	private void getMetaTypeObjects() {
		mti = metatype.getMetaTypeInformation(bundle);
		Assert.assertNotNull("Metatype information not found", mti); //$NON-NLS-1$
		ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1.testRejectDefaultValueWhenNotAnOption", null); //$NON-NLS-1$
		Assert.assertNotNull("Object class definition not found", ocd); //$NON-NLS-1$
		ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		Assert.assertNotNull("Attribute definitions not found", ads); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of attribute definitions", 1, ads.length); //$NON-NLS-1$
	}

	@Override
	public void restartMetatype() {
		super.restartMetatype();
		getMetaTypeObjects();
	}
}
