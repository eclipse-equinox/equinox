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

public class AttributeTypePasswordTest extends AbstractTest {
	private Bundle bundle;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb1"); //$NON-NLS-1$
		bundle.start();
	}

	/*
	 * Ensures the PASSWORD type is recognized.
	 */
	@Test
	public void testAttributeTypePassword1() {
		doTestAttributeTypePassword1();
		restartMetatype();
		doTestAttributeTypePassword1();
	}

	private void doTestAttributeTypePassword1() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (AttributeDefinition ad : ads) {
			if (ad.getID().equals("password1")) { //$NON-NLS-1$
				Assert.assertEquals("Attribute type is not PASSWORD", AttributeDefinition.PASSWORD, ad.getType()); //$NON-NLS-1$
			}
		}
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type.
	 */
	@Test
	public void testAttributeTypePassword2() {
		doTestAttributeTypePassword2();
		restartMetatype();
		doTestAttributeTypePassword2();
	}

	private void doTestAttributeTypePassword2() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		AttributeDefinition ad = findAttributeDefinitionById("password1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertValidationPass("1234abcd", ad); //$NON-NLS-1$
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type.
	 */
	@Test
	public void testAttributeTypePassword3() {
		doTestAttributeTypePassword3();
		restartMetatype();
		doTestAttributeTypePassword3();
	}

	private void doTestAttributeTypePassword3() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		AttributeDefinition ad = findAttributeDefinitionById("password2", ads); //$NON-NLS-1$
		assertValidationPass("password", ad); //$NON-NLS-1$
		assertValidationFail("1234abcd", ad); //$NON-NLS-1$
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type. PASSWORD
	 * length should be no less than min.
	 */
	@Test
	public void testAttributeTypePassword4() {
		doTestAttributeTypePassword4();
		restartMetatype();
		doTestAttributeTypePassword4();
	}

	private void doTestAttributeTypePassword4() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		AttributeDefinition ad = findAttributeDefinitionById("password3", ads); //$NON-NLS-1$
		assertValidationPass("12345678", ad); //$NON-NLS-1$
		assertValidationPass("123456789", ad); //$NON-NLS-1$
		assertValidationFail("1234567", ad); //$NON-NLS-1$
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type. PASSWORD
	 * length should be no greater than max.
	 */
	@Test
	public void testAttributeTypePassword5() {
		doTestAttributeTypePassword5();
		restartMetatype();
		doTestAttributeTypePassword5();
	}

	private void doTestAttributeTypePassword5() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		AttributeDefinition ad = findAttributeDefinitionById("password4", ads); //$NON-NLS-1$
		assertValidationPass("12345", ad); //$NON-NLS-1$
		assertValidationPass("1234", ad); //$NON-NLS-1$
		assertValidationFail("123456", ad); //$NON-NLS-1$
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type. PASSWORD
	 * length should be no less than min and no greater than max.
	 */
	@Test
	public void testAttributeTypePassword6() {
		doTestAttributeTypePassword6();
		restartMetatype();
		doTestAttributeTypePassword6();
	}

	private void doTestAttributeTypePassword6() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		AttributeDefinition ad = findAttributeDefinitionById("password5", ads); //$NON-NLS-1$
		assertValidationPass("123", ad); //$NON-NLS-1$
		assertValidationFail("12", ad); //$NON-NLS-1$
		assertValidationPass("123456", ad); //$NON-NLS-1$
		assertValidationFail("1234567", ad); //$NON-NLS-1$
	}
}
