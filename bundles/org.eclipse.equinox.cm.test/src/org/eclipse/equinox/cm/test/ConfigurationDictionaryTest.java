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
import org.junit.*;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigurationDictionaryTest {

	private ConfigurationAdmin cm;
	private ServiceReference<ConfigurationAdmin> reference;

	@Before
	public void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.cm").start();
		reference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class);
		cm = Activator.getBundleContext().getService(reference);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(reference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
	}

	@Test
	public void testGoodConfigProperties() throws Exception {
		Configuration config = cm.getConfiguration("test");
		config.update();
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
			dict.put("10", new String[] {"x"});
			dict.put("11", new Integer[] {Integer.valueOf(1)});
			dict.put("12", new Long[] {Long.valueOf(1)});
			dict.put("13", new Float[] {Float.valueOf(1)});
			dict.put("14", new Double[] {Double.valueOf(1)});
			dict.put("15", new Byte[] {Byte.valueOf((byte) 1)});
			dict.put("16", new Short[] {Short.valueOf((short) 1)});
			dict.put("17", new Character[] {Character.valueOf('a')});
			dict.put("18", new Boolean[] {Boolean.TRUE});
			dict.put("19", new int[] {1});
			dict.put("20", new long[] {1});
			dict.put("21", new float[] {1});
			dict.put("22", new double[] {1});
			dict.put("23", new byte[] {1});
			dict.put("24", new short[] {1});
			dict.put("25", new char[] {'a'});
			dict.put("26", new boolean[] {true});
			dict.put("27", new Vector<Object>());
			Vector<Object> v = new Vector<Object>();
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
			Collection<Object> c = new ArrayList<Object>();
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

		assertTrue(dict.size() == dict2.size());
		Enumeration<String> keys = dict.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			Object value1 = dict.get(key);
			Class<?> class1 = value1.getClass();
			Object value2 = dict2.get(key);
			Class<?> class2 = value2.getClass();
			if (value1.getClass().isArray()) {
				assertTrue(class1 == class2);
				assertTrue(class1.getComponentType() == class2.getComponentType());
				int arrayLength1 = Array.getLength(value1);
				int arrayLength2 = Array.getLength(value1);
				assertTrue(arrayLength1 == arrayLength2);
				if (value1 instanceof Object[])
					assertTrue(Arrays.asList((Object[]) value1).containsAll(Arrays.asList((Object[]) value2)));
			} else if (value1 instanceof Collection) {
				Collection<?> c1 = (Collection<?>) value1;
				Collection<?> c2 = (Collection<?>) value2;
				assertTrue(c1.size() == c2.size());
				assertTrue(c1.containsAll(c2));
			} else
				assertEquals(value1, value2);
		}
		config.delete();
	}

	@Test
	public void testNullKey() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			dict.put(null, "x");
		} catch (NullPointerException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	@Test
	public void testNullValue() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			dict.put("x", null);
		} catch (NullPointerException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	@Test
	public void testObjectValue() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			dict.put("x", new Object());
		} catch (IllegalArgumentException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	@Test
	public void testObjectArray() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			dict.put("x", new Object[] {new Object()});
		} catch (IllegalArgumentException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	@Test
	public void testObjectVector() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			Vector<Object> v = new Vector<Object>();
			v.add(new Object());
			dict.put("x", v);
		} catch (IllegalArgumentException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	@Test
	public void testObjectCollection() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			Collection<Object> c = new ArrayList<Object>();
			c.add(new Object());
			dict.put("x", c);
		} catch (IllegalArgumentException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	@Test
	public void testPutGetCustomCollection() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			Collection<Object> c = new ArrayList<Object>() {
				private static final long serialVersionUID = 1L;
			};
			dict.put("x", c);
			config.update(dict);
		} catch (IOException e) {
			fail();
		} finally {
			config.delete();
		}
	}

	@Test
	public void testGet() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			assertTrue(null != dict.get(Constants.SERVICE_PID));
		} finally {
			config.delete();
		}
	}

	@Test
	public void testGetNull() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			dict.get(null);
		} catch (NullPointerException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	@Test
	public void testRemove() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			assertFalse(dict.isEmpty());
			assertTrue(null != dict.remove(Constants.SERVICE_PID));
			assertTrue(dict.isEmpty());
		} finally {
			config.delete();
		}
	}

	@Test
	public void testRemoveNull() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary<String, Object> dict = config.getProperties();
		try {
			dict.remove(null);
		} catch (NullPointerException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}
}
