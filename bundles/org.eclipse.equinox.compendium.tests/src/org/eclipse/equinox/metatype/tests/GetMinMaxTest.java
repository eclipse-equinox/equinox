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

import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.metatype.*;
import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.ObjectClassDefinition;

public class GetMinMaxTest extends AbstractTest {
	private Bundle bundle;
	private EquinoxObjectClassDefinition ocd;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("getMinMax.tb1"); //$NON-NLS-1$
		bundle.start();
		EquinoxMetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.getMinMax.tb1", null); //$NON-NLS-1$
	}

	@Test
	public void testGetMax() throws Exception {
		assertMaxValue("getMax", "0"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetMaxAsNotANumber() {
		assertMaxValue("getMaxAsNotANumber", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetMaxNull() {
		assertMaxValue("getMaxNull", null); //$NON-NLS-1$
	}

	@Test
	public void testGetMin() {
		assertMinValue("getMin", "5"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetMinAsNotANumber() {
		assertMinValue("getMinAsNotANumber", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testGetMinNull() {
		assertMinValue("getMinNull", null); //$NON-NLS-1$
	}

	@Test
	public void testMinMaxStringValidationUsingInteger() {
		String name = "minMaxValidUsingInt"; //$NON-NLS-1$
		assertValid(name, "12345678"); //$NON-NLS-1$
		assertValid(name, "1234567812345678"); //$NON-NLS-1$
		assertInvalid(name, "1234567"); //$NON-NLS-1$
		assertInvalid(name, "12345678123456789"); //$NON-NLS-1$
	}

	@Test
	public void testMinMaxStringValidationUsingString() {
		String name = "minMaxValidUsingString"; //$NON-NLS-1$
		assertValid(name, "1"); //$NON-NLS-1$
		assertValid(name, "1.2"); //$NON-NLS-1$
		assertInvalid(name, "0"); //$NON-NLS-1$
		assertInvalid(name, "1.3"); //$NON-NLS-1$
	}

	private void assertMaxValue(String name, String value) {
		assertMinMaxValue(true, name, value);
	}

	private void assertMinValue(String name, String value) {
		assertMinMaxValue(false, name, value);
	}

	private void assertMinMaxValue(boolean max, String name, String value) {
		EquinoxAttributeDefinition ad = getAttributeDefinition(name);
		Assert.assertEquals("Wrong " + (max ? "max" : "min") + " value", value, max ? ad.getMax() : ad.getMin()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	private void assertValid(String name, String value) {
		assertValidation(name, value, true);

	}

	private void assertInvalid(String name, String value) {
		assertValidation(name, value, false);
	}

	private void assertValidation(String name, String value, boolean valid) {
		EquinoxAttributeDefinition ad = getAttributeDefinition(name);
		if (valid) {
			Assert.assertEquals("Should be valid", "", ad.validate(value)); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			assertTrue("Should be invalid", ad.validate(value).length() > 0); //$NON-NLS-1$
		}
	}

	private EquinoxAttributeDefinition getAttributeDefinition(String name) {
		EquinoxAttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		return (EquinoxAttributeDefinition) findAttributeDefinitionById(name, ads);
	}
}
