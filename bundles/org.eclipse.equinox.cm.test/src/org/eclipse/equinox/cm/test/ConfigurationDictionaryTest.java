/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others
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
package org.eclipse.equinox.cm.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;

public class ConfigurationDictionaryTest extends AbstractCMTest {

	private Configuration getConfiguration(String pid) throws IOException {
		return saveAndUpdate(cm.getConfiguration(pid));
	}

	@Test
	public void testGoodConfigProperties() throws Exception {
		Configuration config = getConfiguration("test");
		Dictionary<String, Object> dict = config.getProperties();
		try {
			dict.put("1", new String("x"));
			dict.put("2", Integer.valueOf(1));
			dict.put("3", Long.valueOf(1));
			dict.put("4", Float.valueOf(1));
			dict.put("5", Double.valueOf(1));
			dict.put("6", Byte.valueOf((byte) 1));
			dict.put("7", Short.valueOf((short) 1));
			dict.put("8", Character.valueOf('a'));
			dict.put("9", Boolean.TRUE);
			dict.put("10", new String[] { "x" });
			dict.put("11", new Integer[] { Integer.valueOf(1) });
			dict.put("12", new Long[] { Long.valueOf(1) });
			dict.put("13", new Float[] { Float.valueOf(1) });
			dict.put("14", new Double[] { Double.valueOf(1) });
			dict.put("15", new Byte[] { Byte.valueOf((byte) 1) });
			dict.put("16", new Short[] { Short.valueOf((short) 1) });
			dict.put("17", new Character[] { Character.valueOf('a') });
			dict.put("18", new Boolean[] { Boolean.TRUE });
			dict.put("19", new int[] { 1 });
			dict.put("20", new long[] { 1 });
			dict.put("21", new float[] { 1 });
			dict.put("22", new double[] { 1 });
			dict.put("23", new byte[] { 1 });
			dict.put("24", new short[] { 1 });
			dict.put("25", new char[] { 'a' });
			dict.put("26", new boolean[] { true });
			dict.put("27", new Vector<>());
			Vector<Object> v = new Vector<>();
			v.add(new String("x"));
			v.add(Integer.valueOf(1));
			v.add(Long.valueOf(1));
			v.add(Float.valueOf(1));
			v.add(Double.valueOf(1));
			v.add(Byte.valueOf((byte) 1));
			v.add(Short.valueOf((short) 1));
			v.add(Character.valueOf('a'));
			v.add(Boolean.TRUE);
			dict.put("28", v);
			Collection<Object> c = new ArrayList<>();
			c.add(new String("x"));
			c.add(Integer.valueOf(1));
			c.add(Long.valueOf(1));
			c.add(Float.valueOf(1));
			c.add(Double.valueOf(1));
			c.add(Byte.valueOf((byte) 1));
			c.add(Short.valueOf((short) 1));
			c.add(Character.valueOf('a'));
			c.add(Boolean.TRUE);
			dict.put("29", c);
		} catch (IllegalArgumentException e) {
			fail(e.getMessage());
		}

		config.update(dict);
		Dictionary<String, Object> dict2 = config.getProperties();

		assertEquals(dict.size(), dict2.size());
		Enumeration<String> keys = dict.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			Object value1 = dict.get(key);
			Class<?> class1 = value1.getClass();
			Object value2 = dict2.get(key);
			Class<?> class2 = value2.getClass();
			if (class1.isArray()) {
				assertSame(class1, class2);
				assertSame(class1.getComponentType(), class2.getComponentType());
				int arrayLength1 = Array.getLength(value1);
				int arrayLength2 = Array.getLength(value1);
				assertEquals(arrayLength1, arrayLength2);
				if (value1 instanceof Object[]) {
					assertTrue(Arrays.asList((Object[]) value1).containsAll(Arrays.asList((Object[]) value2)));
				}
			} else if (value1 instanceof Collection) {
				Collection<?> c1 = (Collection<?>) value1;
				Collection<?> c2 = (Collection<?>) value2;
				assertEquals(c1.size(), c2.size());
				assertTrue(c1.containsAll(c2));
			} else {
				assertEquals(value1, value2);
			}
		}
	}

	@Test
	public void testNullKey() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		assertThrows(NullPointerException.class, () -> dict.put(null, "x"));
	}

	@Test
	public void testNullValue() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		assertThrows(NullPointerException.class, () -> dict.put("x", null));
	}

	@Test
	public void testObjectValue() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		Object value = new Object();
		assertThrows(IllegalArgumentException.class, () -> dict.put("x", value));
	}

	@Test
	public void testObjectArray() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		Object[] value = new Object[] { new Object() };
		assertThrows(IllegalArgumentException.class, () -> dict.put("x", value));
	}

	@Test
	public void testObjectVector() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		Vector<Object> v = new Vector<>();
		v.add(new Object());
		assertThrows(IllegalArgumentException.class, () -> dict.put("x", v));
	}

	@Test
	public void testObjectCollection() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		Collection<Object> c = new ArrayList<>();
		c.add(new Object());
		assertThrows(IllegalArgumentException.class, () -> dict.put("x", c));
	}

	@Test
	public void testPutGetCustomCollection() throws Exception {
		Configuration config = getConfiguration("test2");
		Dictionary<String, Object> dict = config.getProperties();
		Collection<Object> c = new ArrayList<Object>() {
			private static final long serialVersionUID = 1L;
		};
		dict.put("x", c);
		config.update(dict);
	}

	@Test
	public void testGet() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		assertNotNull(dict.get(Constants.SERVICE_PID));
	}

	@Test
	public void testGetNull() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		assertThrows(NullPointerException.class, () -> dict.get(null));
	}

	@Test
	public void testRemove() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		assertFalse(dict.isEmpty());
		assertNotNull(dict.remove(Constants.SERVICE_PID));
		assertTrue(dict.isEmpty());
	}

	@Test
	public void testRemoveNull() throws Exception {
		Dictionary<String, Object> dict = getConfiguration("test2").getProperties();
		assertThrows(NullPointerException.class, () -> dict.remove(null));
	}
}
