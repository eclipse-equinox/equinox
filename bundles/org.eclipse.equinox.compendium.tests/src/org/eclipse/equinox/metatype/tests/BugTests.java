/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
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
import static org.junit.Assert.assertTrue;

import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.*;

public class BugTests extends AbstractTest {
	private Bundle bundle;

	/*
	 * A cardinality of zero should not require a default value to be specified.
	 */
	@Test
	public void test334642() throws Exception {
		doTest334642();
		restartMetatype();
		doTest334642();
	}

	private void doTest334642() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		Assert.assertNotNull("Metatype information not found", mti); //$NON-NLS-1$
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb3", null); //$NON-NLS-1$
		Assert.assertNotNull("Object class definition not found", ocd); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		Assert.assertNotNull("Attribute definitions not found", ads); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of attribute definitions", 3, ads.length); //$NON-NLS-1$

		AttributeDefinition ad = findAttributeDefinitionById("password1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		Assert.assertEquals("Wrong cardinality", 0, ad.getCardinality()); //$NON-NLS-1$
		assertNull("Default value should be null", ad.getDefaultValue()); //$NON-NLS-1$
		Assert.assertNotNull("Validation should be present", ad.validate(getFirstDefaultValue(ad.getDefaultValue()))); //$NON-NLS-1$
		assertTrue("Validation should fail", ad.validate(getFirstDefaultValue(ad.getDefaultValue())).length() > 0); //$NON-NLS-1$

		ad = findAttributeDefinitionById("password2", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		Assert.assertEquals("Wrong cardinality", 0, ad.getCardinality()); //$NON-NLS-1$
		assertNull("Default value should be null", ad.getDefaultValue()); //$NON-NLS-1$
		Assert.assertNotNull("Validation should be present", ad.validate(getFirstDefaultValue(ad.getDefaultValue()))); //$NON-NLS-1$
		assertTrue("Validation should fail", ad.validate(getFirstDefaultValue(ad.getDefaultValue())).length() > 0); //$NON-NLS-1$

		ad = findAttributeDefinitionById("string1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		Assert.assertEquals("Wrong cardinality", 0, ad.getCardinality()); //$NON-NLS-1$
		Assert.assertEquals("Only one default value should exist", 1, ad.getDefaultValue().length); //$NON-NLS-1$
		Assert.assertEquals("Wrong default value", "Hello, world!", getFirstDefaultValue(ad.getDefaultValue())); //$NON-NLS-1$ //$NON-NLS-2$
		assertValidationPass(escape(getFirstDefaultValue(ad.getDefaultValue())), ad);
	}

	/*
	 * StringIndexOutOfBoundsException when description or name attributes are an empty string
	 */
	@Test
	public void test341963() throws Exception {
		doTest341963();
		restartMetatype();
		doTest341963();
	}

	private void doTest341963() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		Assert.assertNotNull("Metatype information not found", mti); //$NON-NLS-1$
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("ocd2", null); //$NON-NLS-1$
		Assert.assertNotNull("Object class definition not found", ocd); //$NON-NLS-1$
		Assert.assertEquals("Wrong name", "", ocd.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong description", "", ocd.getDescription()); //$NON-NLS-1$ //$NON-NLS-2$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		Assert.assertNotNull("Attribute definitions not found", ads); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of attribute definitions", 1, ads.length); //$NON-NLS-1$

		AttributeDefinition ad = findAttributeDefinitionById("ad1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		Assert.assertEquals("Wrong name", "", ad.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong description", "", ad.getDescription()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb3"); //$NON-NLS-1$
		bundle.start();
	}

}
