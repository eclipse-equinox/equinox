/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.cm.test;

import java.util.*;
import junit.framework.TestCase;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigurationDictionaryTest extends TestCase {

	private ConfigurationAdmin cm;
	private ServiceReference reference;

	public ConfigurationDictionaryTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		Activator.getBundle("org.eclipse.equinox.cm").start();
		reference = Activator.getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
		cm = (ConfigurationAdmin) Activator.getBundleContext().getService(reference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(reference);
		Activator.getBundle("org.eclipse.equinox.cm").stop();
	}

	public void testGoodConfigProperties() throws Exception {
		Configuration config = cm.getConfiguration("test");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			dict.put("1", new String("x"));
			dict.put("2", new Integer(1));
			dict.put("3", new Long(1));
			dict.put("4", new Float(1));
			dict.put("5", new Double(1));
			dict.put("6", new Byte((byte) 1));
			dict.put("7", new Short((short) 1));
			dict.put("8", new Character('a'));
			dict.put("9", new Boolean(true));
			dict.put("10", new String[] {"x"});
			dict.put("11", new Integer[] {new Integer(1)});
			dict.put("12", new Long[] {new Long(1)});
			dict.put("13", new Float[] {new Float(1)});
			dict.put("14", new Double[] {new Double(1)});
			dict.put("15", new Byte[] {new Byte((byte) 1)});
			dict.put("16", new Short[] {new Short((short) 1)});
			dict.put("17", new Character[] {new Character('a')});
			dict.put("18", new Boolean[] {new Boolean(true)});
			dict.put("19", new int[] {1});
			dict.put("20", new long[] {1});
			dict.put("21", new float[] {1});
			dict.put("22", new double[] {1});
			dict.put("23", new byte[] {1});
			dict.put("24", new short[] {1});
			dict.put("25", new char[] {'a'});
			dict.put("26", new boolean[] {true});
			dict.put("27", new Vector());
			Vector v = new Vector();
			v.add(new String("x"));
			v.add(new Integer(1));
			v.add(new Long(1));
			v.add(new Float(1));
			v.add(new Double(1));
			v.add(new Byte((byte) 1));
			v.add(new Short((short) 1));
			v.add(new Character('a'));
			v.add(new Boolean(true));
			dict.put("28", v);

		} catch (IllegalArgumentException e) {
			fail(e.getMessage());
		}

		config.update(dict);
		Dictionary dict2 = config.getProperties();

		Enumeration enum1 = dict.elements();
		Enumeration enum2 = dict.elements();
		while (enum1.hasMoreElements())
			assertEquals(enum1.nextElement(), enum2.nextElement());

		assertFalse(enum2.hasMoreElements());
		config.delete();
	}

	public void testNullKey() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			dict.put(null, "x");
		} catch (NullPointerException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	public void testNullValue() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			dict.put("x", null);
		} catch (NullPointerException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	public void testObjectValue() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			dict.put("x", new Object());
		} catch (IllegalArgumentException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	public void testObjectArray() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			dict.put("x", new Object[] {new Object()});
		} catch (IllegalArgumentException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	public void testObjectVector() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			Vector v = new Vector();
			v.add(new Object());
			dict.put("x", v);
		} catch (IllegalArgumentException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	public void testGet() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			assertTrue(null != dict.get(Constants.SERVICE_PID));
		} finally {
			config.delete();
		}
	}

	public void testGetNull() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			dict.get(null);
		} catch (NullPointerException e) {
			return;
		} finally {
			config.delete();
		}
		fail();
	}

	public void testRemove() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
		try {
			assertFalse(dict.isEmpty());
			assertTrue(null != dict.remove(Constants.SERVICE_PID));
			assertTrue(dict.isEmpty());
		} finally {
			config.delete();
		}
	}

	public void testRemoveNull() throws Exception {
		Configuration config = cm.getConfiguration("test2");
		config.update();
		Dictionary dict = config.getProperties();
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
