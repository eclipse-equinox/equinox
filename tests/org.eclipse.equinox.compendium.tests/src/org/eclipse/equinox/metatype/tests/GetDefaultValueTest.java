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

import static org.junit.Assert.fail;

import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.*;

/*
 * Ensure default values on attribute definitions are correct when the default
 * value is either unspecified or specified as an empty string.
 * 
 * See https://www.osgi.org/members/bugzilla/show_bug.cgi?id=2182.
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=349189.
 */
public class GetDefaultValueTest extends AbstractTest {
	private Bundle bundle;

	@Test
	public void testGetDefaultValue() throws Exception {
		doTestGetDefaultValue();
		restartMetatype();
		doTestGetDefaultValue();
	}

	private void doTestGetDefaultValue() {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		Assert.assertNotNull("Metatype information was null", mti); //$NON-NLS-1$
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1.getDefaultValues", null); //$NON-NLS-1$
		Assert.assertNotNull("Object class definition was null", ocd); //$NON-NLS-1$
		Assert.assertEquals("Wrong object class definition ID", "ocd2", ocd.getID()); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong object class definition name", "getDefaultValues", ocd.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		Assert.assertEquals("Wrong number of attribute definitions", 24, ads.length); //$NON-NLS-1$
		for (AttributeDefinition ad : ads) {
			switch (Integer.parseInt(ad.getID())) {
				case 1:
					assertAttributeDefinition(ad, 0, null, null, "1", null, null, null, AttributeDefinition.INTEGER); //$NON-NLS-1$
					break;
				case 2:
					assertAttributeDefinition(ad, 1, null, null, "2", null, null, null, AttributeDefinition.DOUBLE); //$NON-NLS-1$
					break;
				case 3:
					assertAttributeDefinition(ad, -1, null, null, "3", null, null, null, AttributeDefinition.SHORT); //$NON-NLS-1$
					break;
				case 4:
					assertAttributeDefinition(ad, 5, null, null, "4", null, null, null, AttributeDefinition.BYTE); //$NON-NLS-1$
					break;
				case 5:
					assertAttributeDefinition(ad, -10, null, null, "5", null, null, null, AttributeDefinition.INTEGER); //$NON-NLS-1$
					break;
				case 6:
					assertAttributeDefinition(ad, 0, null, null, "6", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$
					break;
				case 7:
					assertAttributeDefinition(ad, 1, null, null, "7", null, null, null, AttributeDefinition.PASSWORD); //$NON-NLS-1$
					break;
				case 8:
					assertAttributeDefinition(ad, -1, null, null, "8", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$
					break;
				case 9:
					assertAttributeDefinition(ad, 20, null, null, "9", null, null, null, AttributeDefinition.PASSWORD); //$NON-NLS-1$
					break;
				case 10:
					assertAttributeDefinition(ad, -9, null, null, "10", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$
					break;
				case 11:
					assertAttributeDefinition(ad, 0, null, null, "11", null, null, null, AttributeDefinition.BYTE); //$NON-NLS-1$
					break;
				case 12:
					assertAttributeDefinition(ad, 1, null, null, "12", null, null, null, AttributeDefinition.SHORT); //$NON-NLS-1$
					break;
				case 13:
					assertAttributeDefinition(ad, -1, null, null, "13", null, null, null, AttributeDefinition.INTEGER); //$NON-NLS-1$
					break;
				case 14:
					assertAttributeDefinition(ad, 7, null, null, "14", null, null, null, AttributeDefinition.DOUBLE); //$NON-NLS-1$
					break;
				case 15:
					assertAttributeDefinition(ad, -3, null, null, "15", null, null, null, AttributeDefinition.DOUBLE); //$NON-NLS-1$
					break;
				case 16:
					assertAttributeDefinition(ad, 0, new String[] {""}, null, "16", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case 17:
					assertAttributeDefinition(ad, 1, new String[0], null, "17", null, null, null, AttributeDefinition.PASSWORD); //$NON-NLS-1$
					break;
				case 18:
					assertAttributeDefinition(ad, -1, new String[0], null, "18", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$
					break;
				case 19:
					assertAttributeDefinition(ad, 42, new String[0], null, "19", null, null, null, AttributeDefinition.PASSWORD); //$NON-NLS-1$
					break;
				case 20:
					assertAttributeDefinition(ad, -210, new String[0], null, "20", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$
					break;
				case 21:
					assertAttributeDefinition(ad, 2, new String[] {"", ""}, null, "21", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					break;
				case 22:
					assertAttributeDefinition(ad, -2, new String[] {"", ""}, null, "22", null, null, null, AttributeDefinition.PASSWORD); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					break;
				case 23:
					assertAttributeDefinition(ad, 4, new String[] {"", "", ""}, null, "23", null, null, null, AttributeDefinition.STRING); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					break;
				case 24:
					assertAttributeDefinition(ad, -8, new String[] {"", "", "", "", ""}, null, "24", null, null, null, AttributeDefinition.PASSWORD); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					break;
				default:
					fail("Unexpected attribute definition ID: " + ad.getID()); //$NON-NLS-1$
			}
		}
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb1"); //$NON-NLS-1$
	}
}
