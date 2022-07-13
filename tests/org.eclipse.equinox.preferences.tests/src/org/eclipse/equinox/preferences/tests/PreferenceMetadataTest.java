/*******************************************************************************
 * Copyright (c) 2020 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.preferences.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.preferences.PreferenceMetadata;
import org.junit.Test;

public class PreferenceMetadataTest {

	@Test
	public void testPreferenceMetadata() {
		PreferenceMetadata<String> option = new PreferenceMetadata<>(String.class, "identifier", "default", "name",
				"description");
		assertEquals("identifier", option.identifer());
		assertEquals("default", option.defaultValue());
		assertEquals("name", option.name());
		assertEquals("description", option.description());
		assertEquals(String.class, option.valueClass());
	}

	@Test(expected = NullPointerException.class)
	public void testPreferenceMetadataNullValueType() {
		new PreferenceMetadata<>(null, "identifier", "default", "name", "description");
	}

	@Test(expected = NullPointerException.class)
	public void testPreferenceMetadataNullIdentifier() {
		new PreferenceMetadata<>(Object.class, null, "default", "name", "description");
	}

	@Test(expected = NullPointerException.class)
	public void testPreferenceMetadataNullDefaultValue() {
		new PreferenceMetadata<>(Object.class, "identifier", null, "name", "description");
	}

	@Test(expected = NullPointerException.class)
	public void testPreferenceMetadataNullName() {
		new PreferenceMetadata<>(Object.class, "identifier", "default", null, "description");
	}

	@Test(expected = NullPointerException.class)
	public void testPreferenceMetadataNullDescription() {
		new PreferenceMetadata<>(Object.class, "identifier", "default", "name", null);
	}

}
