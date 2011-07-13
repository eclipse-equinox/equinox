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

import org.eclipse.equinox.metatype.EquinoxMetaTypeService;

import junit.framework.TestCase;
import org.eclipse.equinox.compendium.tests.Activator;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.AttributeDefinition;

public abstract class AbstractTest extends TestCase {
	protected BundleInstaller bundleInstaller;
	protected EquinoxMetaTypeService metatype;
	protected ServiceReference metaTypeReference;

	protected void assertValidationFail(String value, AttributeDefinition ad) {
		String result = assertValidationPresent(value, ad);
		assertTrue("Validation passed", result.length() > 0); //$NON-NLS-1$
	}

	protected void assertValidationPass(String value, AttributeDefinition ad) {
		String result = assertValidationPresent(value, ad);
		assertEquals("Validation failed", 0, result.length()); //$NON-NLS-1$
	}

	protected String assertValidationPresent(String value, AttributeDefinition ad) {
		String result = ad.validate(value);
		assertNotNull("No validation was present", result); //$NON-NLS-1$
		return result;
	}

	protected String escape(String value) {
		if (value == null || value.length() == 0) {
			return value;
		}
		StringBuffer result = new StringBuffer(value.length() + 20);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == ',' || c == '\\' || Character.isWhitespace(c)) {
				result.append('\\');
			}
			result.append(c);
		}
		return result.toString();
	}

	protected AttributeDefinition findAttributeDefinitionById(String id, AttributeDefinition[] ads) {
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

	protected String getFirstDefaultValue(String[] defaultValue) {
		if (defaultValue == null || defaultValue.length == 0)
			return null;
		return defaultValue[0];
	}

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_METATYPE).start();
		metaTypeReference = Activator.getBundleContext().getServiceReference(EquinoxMetaTypeService.class.getName());
		assertNotNull("Metatype service reference not found", metaTypeReference); //$NON-NLS-1$
		metatype = (EquinoxMetaTypeService) Activator.getBundleContext().getService(metaTypeReference);
		assertNotNull("Metatype service not found", metatype); //$NON-NLS-1$
		bundleInstaller = new BundleInstaller("bundle_tests/metatype", Activator.getBundleContext()); //$NON-NLS-1$
	}

	protected void tearDown() throws Exception {
		bundleInstaller.shutdown();
		Activator.getBundleContext().ungetService(metaTypeReference);
		Activator.getBundle(Activator.BUNDLE_METATYPE).stop();
	}
}
