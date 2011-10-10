/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.tests;

import org.eclipse.equinox.metatype.*;

import java.util.Map;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.ObjectClassDefinition;

public class ExtendableTest extends AbstractTest {
	private Bundle bundle;

	protected void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("extendable.tb1"); //$NON-NLS-1$
		bundle.start();
	}

	public void testExtensions() {
		EquinoxMetaTypeInformation mti = metatype.getMetaTypeInformation(bundle);
		EquinoxObjectClassDefinition ocd = mti.getObjectClassDefinition("metatype.extendable.tb1.1", null); //$NON-NLS-1$
		Set schemas = ocd.getExtensionUris();
		assertNotNull("Null extension schemas", schemas); //$NON-NLS-1$
		assertEquals("Wrong schemas size", 2, schemas.size()); //$NON-NLS-1$
		assertTrue("Missing schema", schemas.contains("urn:xmlns:foo")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing schema", schemas.contains("urn:xmlns:validation")); //$NON-NLS-1$ //$NON-NLS-2$
		Map attributes = ocd.getExtensionAttributes("urn:xmlns:foo"); //$NON-NLS-1$
		assertNotNull("Null attributes", attributes); //$NON-NLS-1$
		assertEquals("Wrong attributes size", 1, attributes.size()); //$NON-NLS-1$
		assertEquals("Wrong value", "bar", attributes.get("foo")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		attributes = ocd.getExtensionAttributes("urn:xmlns:validation"); //$NON-NLS-1$
		assertNotNull("Null attributes", attributes); //$NON-NLS-1$
		assertEquals("Wrong attributes size", 1, attributes.size()); //$NON-NLS-1$
		assertEquals("Wrong value", "true", attributes.get("enabled")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		EquinoxAttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (int i = 0; i < ads.length; i++) {
			if (ads[i].getID().equals("ad1")) { //$NON-NLS-1$
				schemas = ads[i].getExtensionUris();
				assertNotNull("Null extension schemas", schemas); //$NON-NLS-1$
				assertEquals("Wrong schemas size", 2, schemas.size()); //$NON-NLS-1$
				assertTrue("Missing schema", schemas.contains("urn:xmlns:foo")); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Missing schema", schemas.contains("urn:xmlns:validation")); //$NON-NLS-1$ //$NON-NLS-2$
				attributes = ads[i].getExtensionAttributes("urn:xmlns:foo"); //$NON-NLS-1$
				assertNotNull("Null attributes", attributes); //$NON-NLS-1$
				assertEquals("Wrong attributes size", 1, attributes.size()); //$NON-NLS-1$
				assertEquals("Wrong value", "foo", attributes.get("bar")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				attributes = ads[i].getExtensionAttributes("urn:xmlns:validation"); //$NON-NLS-1$
				assertNotNull("Null attributes", attributes); //$NON-NLS-1$
				assertEquals("Wrong attributes size", 2, attributes.size()); //$NON-NLS-1$
				assertEquals("Wrong value", "[a-zA-Z0-9]", attributes.get("regexp")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				assertEquals("Wrong value", "validation", attributes.get("validation")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}
}
