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

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.*;

public class Bug340899Test extends AbstractTest {
	private static final String ad1description = "This is the description of the attribute definition."; //$NON-NLS-1$
	private static final String ad1name = "AD1 Name"; //$NON-NLS-1$
	private static final String ocd1description = "This is the description of the object class definition."; //$NON-NLS-1$
	private static final String ocd1name = "OCD1 Name"; //$NON-NLS-1$

	/*
	 * When not overridden by either the <MetaData> localization attribute or the
	 * Bundle-Localization manifest header, the default property file base name of
	 * 'OSGI-INF/l10n/bundle' should be used.
	 */
	@Test
	public void test1() throws Exception {
		doTest1();
		restartMetatype();
		doTest1();
	}

	private void doTest1() throws Exception {
		execute("tb5", "org.eclipse.equinox.metatype.tests.tb5"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * When not overridden by the <MetaData> localization attribute, the property
	 * file base name declared within the Bundle-Localization manifest header should
	 * override the default base name.
	 */
	@Test
	public void test2() throws Exception {
		doTest2();
		restartMetatype();
		doTest2();
	}

	private void doTest2() throws Exception {
		execute("tb6", "org.eclipse.equinox.metatype.tests.tb6"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * When specified, the property file base name declared within the <MetaData>
	 * localization attribute should override the Bundle-Localization manifest
	 * header and the default base name.
	 */
	@Test
	public void test3() throws Exception {
		doTest3();
		restartMetatype();
		doTest3();
	}

	private void doTest3() throws Exception {
		execute("tb7", "org.eclipse.equinox.metatype.tests.tb7"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void execute(String bundleName, String ocdId) throws Exception {
		Bundle bundle = bundleInstaller.installBundle(bundleName);
		try {
			bundle.start();
			MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
			Assert.assertNotNull("Metatype information not found", mti); //$NON-NLS-1$
			ObjectClassDefinition ocd = mti.getObjectClassDefinition(ocdId, null);
			Assert.assertNotNull("Object class definition not found", ocd); //$NON-NLS-1$
			Assert.assertEquals("Wrong OCD name", ocd1name, ocd.getName()); //$NON-NLS-1$
			Assert.assertEquals("Wrong OCD description", ocd1description, ocd.getDescription()); //$NON-NLS-1$
			AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
			Assert.assertNotNull("Attribute definitions not found", ads); //$NON-NLS-1$
			Assert.assertEquals("Wrong number of attribute definitions", 1, ads.length); //$NON-NLS-1$
			AttributeDefinition ad = findAttributeDefinitionById("ad1", ads); //$NON-NLS-1$
			Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
			Assert.assertEquals("Wrong AD name", ad1name, ad.getName()); //$NON-NLS-1$
			Assert.assertEquals("Wrong AD description", ad1description, ad.getDescription()); //$NON-NLS-1$
		} finally {
			try {
				bundle.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
