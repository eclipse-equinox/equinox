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

import java.io.IOException;
import java.io.InputStream;
import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.*;

/*
 * Ensure the metadata XML information from an unresolved bundle is provided.
 * 
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=363374.
 */
public class UnresolvedBundleTest extends AbstractTest {
	private Bundle bundle;

	@Test
	public void testUnresolvedBundle() throws Exception {
		assertBundleUnresolved();
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		Assert.assertNotNull("Metatype information was null", mti); //$NON-NLS-1$
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb8", null); //$NON-NLS-1$
		Assert.assertNotNull("Object class definition was null", ocd); //$NON-NLS-1$
		Assert.assertEquals("Wrong object class definition ID", "1", ocd.getID()); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong object class definition name", "OCD1 Name", ocd.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		Assert.assertEquals("Wrong number of attribute definitions", 1, ads.length); //$NON-NLS-1$
		AttributeDefinition ad = ads[0];
		Assert.assertNotNull("Attribute definition was null", ad); //$NON-NLS-1$
		Assert.assertEquals("Wrong attribute definition ID", "1", ad.getID()); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong attribute definition name", "AD1 Name", ad.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong attribute definition type", AttributeDefinition.STRING, ad.getType()); //$NON-NLS-1$
		InputStream icon = ocd.getIcon(10000);
		Assert.assertNotNull("Icon was null", icon); //$NON-NLS-1$
		try {
			icon.close();
		} catch (IOException e) {
			// noop
		}
		String[] locales = mti.getLocales();
		Assert.assertNotNull("Locales was null", locales); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of locales", 1, locales.length); //$NON-NLS-1$
		Assert.assertEquals("Wrong locale", "en", locales[0]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb8"); //$NON-NLS-1$
		assertBundleUnresolved();
	}

	private void assertBundleUnresolved() {
		Assert.assertEquals("Bundle should not be resolved", Bundle.INSTALLED, bundle.getState()); //$NON-NLS-1$
	}
}
