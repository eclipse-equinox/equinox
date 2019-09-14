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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.*;
import org.eclipse.equinox.compendium.tests.Activator;
import org.eclipse.equinox.metatype.EquinoxMetaTypeService;
import org.eclipse.osgi.tests.bundles.BundleInstaller;
import org.junit.*;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.*;

public abstract class AbstractTest {
	protected BundleInstaller bundleInstaller;
	protected EquinoxMetaTypeService metatype;
	protected ServiceReference<EquinoxMetaTypeService> metaTypeReference;

	protected void assertAttributeDefinition(AttributeDefinition ad, int cardinality, String[] defaultValue, String description, String id, String name, String[] optionLabels, String[] optionValues, int type) {
		Assert.assertEquals("Wrong cardinality", cardinality, ad.getCardinality()); //$NON-NLS-1$
		assertEquals("Wrong default value", defaultValue, ad.getDefaultValue()); //$NON-NLS-1$
		Assert.assertEquals("Wrong description", description, ad.getDescription()); //$NON-NLS-1$
		Assert.assertEquals("Wrong id", id, ad.getID()); //$NON-NLS-1$
		Assert.assertEquals("Wrong name", name, ad.getName()); //$NON-NLS-1$
		assertEquals("Wrong option labels", optionLabels, ad.getOptionLabels()); //$NON-NLS-1$
		assertEquals("Wrong option values", optionValues, ad.getOptionValues()); //$NON-NLS-1$
		Assert.assertEquals("Wrong type", type, ad.getType()); //$NON-NLS-1$
	}

	protected void assertAttributeDefinitions(AttributeDefinition[] ads, int size) {
		Assert.assertNotNull("Null attribute definitions", ads); //$NON-NLS-1$
		Assert.assertEquals("Wrong attribute definitions size", size, ads.length); //$NON-NLS-1$
	}

	protected void assertEquals(String message, String[] s1, String[] s2) {
		if (s1 == s2)
			return;
		// We know that at least one is not null from the above check.
		if (s1 == null || s2 == null)
			fail(message + " (one of the arrays was null)"); //$NON-NLS-1$
		if (s1.length != s2.length)
			fail(message + " (array lengths weren't equal)"); //$NON-NLS-1$
		for (int i = 0; i < s1.length; i++)
			Assert.assertEquals(message, s1[i], s2[i]);
	}

	protected void assertIcon(InputStream icon, int size) throws IOException {
		Assert.assertNotNull("Icon was null", icon); //$NON-NLS-1$
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			byte[] bytes = new byte[2048];
			int read;
			try {
				while ((read = icon.read(bytes)) != -1) {
					baos.write(bytes, 0, read);
				}
			} finally {
				icon.close();
			}
			Assert.assertEquals("Wrong size.", size, baos.size()); //$NON-NLS-1$
		} finally {
			baos.close();
		}
	}

	protected void assertNotNull(MetaTypeInformation mti) {
		Assert.assertNotNull("Metatype information was null", mti); //$NON-NLS-1$
	}

	protected void assertNotNull(ObjectClassDefinition ocd) {
		Assert.assertNotNull("Object class definition was null", ocd); //$NON-NLS-1$
	}

	protected void assertObjectClassDefinition(ObjectClassDefinition ocd, String id, String name, String description) {
		assertNotNull(ocd);
		Assert.assertEquals("Wrong object class definition ID", id, ocd.getID()); //$NON-NLS-1$
		Assert.assertEquals("Wrong object class definition name", name, ocd.getName()); //$NON-NLS-1$
		Assert.assertEquals("Wrong object class definition description", description, ocd.getDescription()); //$NON-NLS-1$
	}

	protected void assertValidationFail(String value, AttributeDefinition ad) {
		String result = assertValidationPresent(value, ad);
		assertTrue("Validation passed", result.length() > 0); //$NON-NLS-1$
	}

	protected void assertValidationPass(String value, AttributeDefinition ad) {
		String result = assertValidationPresent(value, ad);
		Assert.assertEquals("Validation failed", 0, result.length()); //$NON-NLS-1$
	}

	protected String assertValidationPresent(String value, AttributeDefinition ad) {
		String result = ad.validate(value);
		Assert.assertNotNull("No validation was present", result); //$NON-NLS-1$
		return result;
	}

	protected String escape(String value) {
		if (value == null || value.length() == 0) {
			return value;
		}
		StringBuilder result = new StringBuilder(value.length() + 20);
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
		for (AttributeDefinition ad : ads) {
			if (ad == null) {
				continue;
			}
			if (id.equals(ad.getID())) {
				return ad;
			}
		}
		return null;
	}

	protected String getFirstDefaultValue(String[] defaultValue) {
		if (defaultValue == null || defaultValue.length == 0)
			return null;
		return defaultValue[0];
	}

	@Before
	public void setUp() throws Exception {
		startMetatype();
		bundleInstaller = new BundleInstaller("bundle_tests/metatype", Activator.getBundleContext()); //$NON-NLS-1$
	}

	private void startMetatype() throws Exception {
		Activator.getBundle(Activator.BUNDLE_METATYPE).start();
		metaTypeReference = Activator.getBundleContext().getServiceReference(EquinoxMetaTypeService.class);
		Assert.assertNotNull("Metatype service reference not found", metaTypeReference); //$NON-NLS-1$
		metatype = Activator.getBundleContext().getService(metaTypeReference);
		Assert.assertNotNull("Metatype service not found", metatype); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		bundleInstaller.shutdown();
		stopMetatype();
	}

	private void stopMetatype() throws Exception {
		Activator.getBundleContext().ungetService(metaTypeReference);
		Activator.getBundle(Activator.BUNDLE_METATYPE).stop();
	}

	public void restartMetatype() {
		try {
			stopMetatype();
			startMetatype();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
