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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.OsgiPreferenceMetadataStore;
import org.eclipse.core.runtime.preferences.PreferenceMetadata;
import org.junit.Test;

public class OsgiPreferenceMetadataStoreTest {

	private final PreferenceMetadata<Object> unknown = new PreferenceMetadata<>(Object.class, "unknown", "", "Unknown",
			"An option with unclear semantic, i.e. typical one");
	private final PreferenceMetadata<String> string2020 = new PreferenceMetadata<>(String.class, "string2020", "2020",
			"String 2020");
	private final PreferenceMetadata<Boolean> negative = new PreferenceMetadata<>(Boolean.class, "negative", false,
			"Negative");
	private final PreferenceMetadata<Boolean> positive = new PreferenceMetadata<>(Boolean.class, "positive", true,
			"Positive");
	private final PreferenceMetadata<byte[]> bytes2020 = new PreferenceMetadata<>(byte[].class, "bytes2020",
			new byte[] { 20, 20 }, "Bytes 2020");
	private final PreferenceMetadata<Double> double2020 = new PreferenceMetadata<>(Double.class, "double2020", 2020d,
			"Double 2020");
	private final PreferenceMetadata<Float> float2020 = new PreferenceMetadata<>(Float.class, "float2020", 2020f,
			"Float 2020");
	private final PreferenceMetadata<Integer> int2020 = new PreferenceMetadata<>(Integer.class, "int2020", 2020,
			"Int 2020");
	private final PreferenceMetadata<Long> long2020 = new PreferenceMetadata<>(Long.class, "long2020", 2020l,
			"Long 2020");

	@Test(expected = NullPointerException.class)
	public void testNullPreferences() {
		new OsgiPreferenceMetadataStore(null);
	}

	@Test
	public void testConsumable() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		assertFalse(storage.handles(Object.class));
		assertTrue(storage.handles(String.class));
		assertTrue(storage.handles(Boolean.class));
		assertTrue(storage.handles(byte[].class));
		assertFalse(storage.handles(Byte[].class));
		assertTrue(storage.handles(Double.class));
		assertTrue(storage.handles(Float.class));
		assertTrue(storage.handles(Integer.class));
		assertTrue(storage.handles(Long.class));
	}

	@Test
	public void testLoadString() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		preferences.put(string2020.identifer(), "2002");
		assertEquals("2002", storage.load(string2020));
	}

	@Test
	public void testLoadBoolean() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		preferences.putBoolean(negative.identifer(), true);
		assertEquals(true, storage.load(negative));
		preferences.putBoolean(positive.identifer(), false);
		assertEquals(false, storage.load(positive));
	}

	@Test
	public void testLoadByteArray() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		preferences.putByteArray(bytes2020.identifer(), new byte[] { 20, 02 });
		assertArrayEquals(new byte[] { 20, 02 }, storage.load(bytes2020));
	}

	@Test
	public void testLoadDouble() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		preferences.putDouble(double2020.identifer(), 2002d);
		assertEquals(2002d, storage.load(double2020), 0);
	}

	@Test
	public void testLoadFloat() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		preferences.putFloat(float2020.identifer(), 2002f);
		assertEquals(2002f, storage.load(float2020), 0);
	}

	@Test
	public void testLoadInt() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		preferences.putLong(int2020.identifer(), 2002l);
		assertEquals(2002l, (long) storage.load(int2020));
	}

	@Test
	public void testLoadLong() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		preferences.putLong(long2020.identifer(), 2002l);
		assertEquals(2002l, (long) storage.load(long2020));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testLoadUnknown() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		assertEquals(new Object(), storage.load(unknown));
	}

	@Test
	public void testSaveString() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save("2002", string2020);
		assertEquals("2002", preferences.get(string2020.identifer(), string2020.defaultValue()));
	}

	@Test
	public void testSaveBoolean() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save(true, negative);
		assertEquals(true, preferences.getBoolean(negative.identifer(), negative.defaultValue()));
		storage.save(false, positive);
		assertEquals(false, preferences.getBoolean(positive.identifer(), positive.defaultValue()));
	}

	@Test
	public void testSaveByteArray() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save(new byte[] { 20, 02 }, bytes2020);
		assertArrayEquals(new byte[] { 20, 02 },
				preferences.getByteArray(bytes2020.identifer(), bytes2020.defaultValue()));
	}

	@Test
	public void testSaveDouble() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save(2002d, double2020);
		assertEquals(2002d, preferences.getDouble(double2020.identifer(), double2020.defaultValue()), 0);
	}

	@Test
	public void testSaveFloat() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save(2002f, float2020);
		assertEquals(2002f, preferences.getDouble(float2020.identifer(), float2020.defaultValue()), 0);
	}

	@Test
	public void testSaveInt() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save(2002, int2020);
		assertEquals(2002, preferences.getInt(int2020.identifer(), int2020.defaultValue()));
	}

	@Test
	public void testSaveLong() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save(2002l, long2020);
		assertEquals(2002l, preferences.getLong(long2020.identifer(), long2020.defaultValue()));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testSaveUnknown() {
		IEclipsePreferences preferences = anyPreferences();
		OsgiPreferenceMetadataStore storage = new OsgiPreferenceMetadataStore(preferences);
		storage.save(new Object(), unknown);
	}

	@SuppressWarnings("restriction")
	private IEclipsePreferences anyPreferences() {
		return new org.eclipse.core.internal.preferences.EclipsePreferences();
	}

}
