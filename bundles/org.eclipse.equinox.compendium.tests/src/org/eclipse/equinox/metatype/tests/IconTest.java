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

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.*;

/*
 * Ensure icons for object class definitions are properly supported.
 * 
 * Although an object class definition conceptually has only one icon, it may
 * come in a variety of sizes. Since a metatype implementation is not expected
 * to scale images itself, users must be able to declare the same icon multiple
 * times with different sizes within the XML. The spec simply says 
 * implementations may return an icon greater or less than the requested size 
 * to provide maximum freedom when no icon of the requested size exists. The
 * Equinox implementation will return the icon that is closest to the requested
 * size in either direction.
 * 
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=349189.
 */
public class IconTest extends AbstractTest {
	private Bundle bundle;

	@Test
	public void testIcon() throws Exception {
		doTestIcon();
		restartMetatype();
		doTestIcon();
	}

	private void doTestIcon() throws Exception {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		assertNotNull(mti);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb9", null); //$NON-NLS-1$
		assertObjectClassDefinition(ocd, "1", "ocd1", null); //$NON-NLS-1$ //$NON-NLS-2$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		assertAttributeDefinitions(ads, 1);
		assertAttributeDefinition(ads[0], 0, null, null, "1", "ad1", null, null, AttributeDefinition.STRING); //$NON-NLS-1$ //$NON-NLS-2$
		assertIcon(ocd.getIcon(4931), 4931);
		assertIcon(ocd.getIcon(7933), 7933);
		assertIcon(ocd.getIcon(10182), 10182);
		assertIcon(ocd.getIcon(5000), 4931);
		assertIcon(ocd.getIcon(50000), 10182);
		assertIcon(ocd.getIcon(6400), 4931);
		assertIcon(ocd.getIcon(6500), 7933);
	}

	@Test
	public void testNullIcon() throws Exception {
		doTestNullIcon();
		restartMetatype();
		doTestNullIcon();
	}

	private void doTestNullIcon() throws Exception {
		MetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		assertNotNull(mti);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb9.2", null); //$NON-NLS-1$
		assertObjectClassDefinition(ocd, "2", "ocd2", null); //$NON-NLS-1$ //$NON-NLS-2$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		assertAttributeDefinitions(ads, 1);
		assertAttributeDefinition(ads[0], 0, null, null, "1", "ad1", null, null, AttributeDefinition.BYTE); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("Icon was not null", ocd.getIcon(10000)); //$NON-NLS-1$
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb9"); //$NON-NLS-1$
	}
}
