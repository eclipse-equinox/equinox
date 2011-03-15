/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.tests;

import junit.framework.TestCase;
import org.eclipse.equinox.compendium.tests.Activator;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.*;

public class BugTests extends TestCase {
	private Bundle bundle;
	private BundleInstaller bundleInstaller;
	private MetaTypeService metaType;
	private ServiceReference metaTypeReference;

	/*
	 * A cardinality of zero should not require a default value to be specified.
	 */
	public void test334642() {
		MetaTypeInformation mti = metaType.getMetaTypeInformation(bundle);
		assertNotNull("Metatype information not found", mti); //$NON-NLS-1$
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb3", null); //$NON-NLS-1$
		assertNotNull("Object class definition not found", ocd); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		assertNotNull("Attribute definitions not found", ads); //$NON-NLS-1$
		assertEquals("Wrong numbder of attribute definitions", 3, ads.length); //$NON-NLS-1$

		AttributeDefinition ad = findAttributeDefinition("password1", ads); //$NON-NLS-1$
		assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertEquals("Wrong cardinality", 0, ad.getCardinality()); //$NON-NLS-1$
		assertNull("Default value should be null", ad.getDefaultValue()); //$NON-NLS-1$
		assertNotNull("Validation should be present", ad.validate(getFirstDefaultValue(ad.getDefaultValue()))); //$NON-NLS-1$
		assertTrue("Validation should fail", ad.validate(getFirstDefaultValue(ad.getDefaultValue())).length() > 0); //$NON-NLS-1$

		ad = findAttributeDefinition("password2", ads); //$NON-NLS-1$
		assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertEquals("Wrong cardinality", 0, ad.getCardinality()); //$NON-NLS-1$
		assertNull("Default value should be null", ad.getDefaultValue()); //$NON-NLS-1$
		assertNotNull("Validation should be present", ad.validate(getFirstDefaultValue(ad.getDefaultValue()))); //$NON-NLS-1$
		assertTrue("Validation should fail", ad.validate(getFirstDefaultValue(ad.getDefaultValue())).length() > 0); //$NON-NLS-1$

		ad = findAttributeDefinition("string1", ads); //$NON-NLS-1$
		assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertEquals("Wrong cardinality", 0, ad.getCardinality()); //$NON-NLS-1$
		assertEquals("Only one default value should exist", 1, ad.getDefaultValue().length); //$NON-NLS-1$
		assertEquals("Wrong default value", "Hello, world!", getFirstDefaultValue(ad.getDefaultValue())); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("Validation should not be present", ad.validate(getFirstDefaultValue(ad.getDefaultValue()))); //$NON-NLS-1$
	}

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_METATYPE).start();
		metaTypeReference = Activator.getBundleContext().getServiceReference(MetaTypeService.class.getName());
		metaType = (MetaTypeService) Activator.getBundleContext().getService(metaTypeReference);
		bundleInstaller = new BundleInstaller("bundle_tests/metatype", Activator.getBundleContext()); //$NON-NLS-1$
		bundle = bundleInstaller.installBundle("tb3"); //$NON-NLS-1$
		bundle.start();
	}

	protected void tearDown() throws Exception {
		bundleInstaller.shutdown();
		Activator.getBundleContext().ungetService(metaTypeReference);
		Activator.getBundle(Activator.BUNDLE_METATYPE).stop();
	}

	private String getFirstDefaultValue(String[] defaultValues) {
		if (defaultValues == null || defaultValues.length == 0)
			return null;
		return defaultValues[0];
	}

	private AttributeDefinition findAttributeDefinition(String id, AttributeDefinition[] ads) {
		if (id == null || ads == null)
			return null;
		for (int i = 0; i < ads.length; i++) {
			if (ads[i] == null)
				continue;
			if (id.equals(ads[i].getID())) {
				return ads[i];
			}
		}
		return null;
	}
}
