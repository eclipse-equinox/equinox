/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.tests;

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

	public void testRejectDefaultValueWhenNotAnOption() {
		AttributeDefinition ad = findAttributeDefinitionById("ocd3-ad1", ads); //$NON-NLS-1$
		assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertNull("Default value not matching one of the options was not rejected", ad.getDefaultValue()); //$NON-NLS-1$
	}

	protected void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb1"); //$NON-NLS-1$
		bundle.start();
		mti = metatype.getMetaTypeInformation(bundle);
		assertNotNull("Metatype information not found", mti); //$NON-NLS-1$
		ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1.testRejectDefaultValueWhenNotAnOption", null); //$NON-NLS-1$
		assertNotNull("Object class definition not found", ocd); //$NON-NLS-1$
		ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		assertNotNull("Attribute definitions not found", ads); //$NON-NLS-1$
		assertEquals("Wrong number of attribute definitions", 1, ads.length); //$NON-NLS-1$
	}

	protected void tearDown() throws Exception {
		bundle.stop();
		super.tearDown();
	}
}
