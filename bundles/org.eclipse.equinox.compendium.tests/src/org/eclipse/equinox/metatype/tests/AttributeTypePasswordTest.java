/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others
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

public class AttributeTypePasswordTest extends TestCase {
	private Bundle bundle;
	private BundleInstaller bundleInstaller;
	private MetaTypeService metaType;
	private ServiceReference metaTypeReference;

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_METATYPE).start();
		metaTypeReference = Activator.getBundleContext().getServiceReference(MetaTypeService.class.getName());
		metaType = (MetaTypeService) Activator.getBundleContext().getService(metaTypeReference);
		bundleInstaller = new BundleInstaller("bundle_tests/metatype", Activator.getBundleContext()); //$NON-NLS-1$
		bundleInstaller.refreshPackages(null);
		bundle = bundleInstaller.installBundle("tb1"); //$NON-NLS-1$
		bundle.start();
	}

	protected void tearDown() throws Exception {
		bundleInstaller.shutdown();
		Activator.getBundleContext().ungetService(metaTypeReference);
		Activator.getBundle(Activator.BUNDLE_METATYPE).stop();
	}

	/*
	 * Ensures the PASSWORD type is recognized.
	 */
	public void testAttributeTypePassword1() {
		MetaTypeInformation mti = metaType.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (int i = 0; i < ads.length; i++) {
			if (ads[i].getID().equals("password1")) { //$NON-NLS-1$
				assertEquals("Attribute type is not PASSWORD", AttributeDefinition.PASSWORD, ads[i].getType()); //$NON-NLS-1$
			}
		}
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type.
	 * Validation should not be present when min and max are not specified and 
	 * their are no enumerated constraints.
	 */
	public void testAttributeTypePassword2() {
		MetaTypeInformation mti = metaType.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (int i = 0; i < ads.length; i++) {
			if (ads[i].getID().equals("password1")) { //$NON-NLS-1$
				assertNull("Validation should not be present when min and max are not specified and their are no enumerated constraints", ads[i].validate("1234abcd")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type.
	 * Validation should be present when min and max are not specified and 
	 * their are enumerated constraints.
	 */
	public void testAttributeTypePassword3() {
		MetaTypeInformation mti = metaType.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (int i = 0; i < ads.length; i++) {
			if (ads[i].getID().equals("password2")) { //$NON-NLS-1$
				assertNotNull("Validation should be present when min and max are not specified and their are enumerated constraints", ads[i].validate("password")); //$NON-NLS-1$ //$NON-NLS-2$
				assertEquals("Value 'password' should have been valid", 0, ads[i].validate("password").length()); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Value '1234abcd' should not have been valid", ads[i].validate("1234abcd").length() > 0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type.
	 * PASSWORD length should be no less than min.
	 */
	public void testAttributeTypePassword4() {
		MetaTypeInformation mti = metaType.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (int i = 0; i < ads.length; i++) {
			if (ads[i].getID().equals("password3")) { //$NON-NLS-1$
				assertEquals("Value '12345678' should have been valid", 0, ads[i].validate("12345678").length()); //$NON-NLS-1$ //$NON-NLS-2$
				assertEquals("Value '123456789' should have been valid", 0, ads[i].validate("123456789").length()); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Value '1234567' should not have been valid", ads[i].validate("1234567").length() > 0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type.
	 * PASSWORD length should be no greater than max.
	 */
	public void testAttributeTypePassword5() {
		MetaTypeInformation mti = metaType.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (int i = 0; i < ads.length; i++) {
			if (ads[i].getID().equals("password4")) { //$NON-NLS-1$
				assertEquals("Value '12345' should have been valid", 0, ads[i].validate("12345").length()); //$NON-NLS-1$ //$NON-NLS-2$
				assertEquals("Value '1234' should have been valid", 0, ads[i].validate("1234").length()); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Value '123456' should not have been valid", ads[i].validate("123456").length() > 0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/*
	 * Ensures the PASSWORD type is treated the same as the STRING type.
	 * PASSWORD length should be no less than min and no greater than max.
	 */
	public void testAttributeTypePassword6() {
		MetaTypeInformation mti = metaType.getMetaTypeInformation(bundle);
		ObjectClassDefinition ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb1", null); //$NON-NLS-1$
		AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		for (int i = 0; i < ads.length; i++) {
			if (ads[i].getID().equals("password5")) { //$NON-NLS-1$
				assertEquals("Value '123' should have been valid", 0, ads[i].validate("123").length()); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Value '12' should not have been valid", ads[i].validate("12").length() > 0); //$NON-NLS-1$ //$NON-NLS-2$
				assertEquals("Value '123456' should have been valid", 0, ads[i].validate("123456").length()); //$NON-NLS-1$ //$NON-NLS-2$
				assertTrue("Value '1234567' should not have been valid", ads[i].validate("1234567").length() > 0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
}
