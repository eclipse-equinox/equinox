/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.io;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

/**
 * This class implements a hashtable, which serves the needs of
 * ConfigurationManagement of capsulation of configuration properties with
 * String keys, which are case insentive at lookup (at get, remove, put
 * operations) but preserve the last case of keys.
 * 
 * The implementaion of the Externalizable interface allows remote transfer of
 * those properties.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ExternalizableDictionary extends Dictionary implements Externalizable, Cloneable {

	private static Class[] CLASSES = new Class[] {ExternalizableDictionary.class};
	private HashtableEntry table[];
	private int count;
	private int threshold;
	private float loadFactor;
	private static int MIN_CAPACITY = 5;

	ClassLoader loader;

	/**
	 * Constructs a new, empty dictionary with the specified initial capacity
	 * and the specified load factor.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the hashtable. when 0 is passed for
	 *            capacity, minimum possible capacity is used; i.e. when the
	 *            object is read as externalizable, then the table is created
	 *            with the written size.
	 * @param loadFactor
	 *            a number between 0.0 and 1.0.
	 * @exception IllegalArgumentException
	 *                if the initial capacity is less than zero, or if the load
	 *                factor is less than or equal to zero.
	 */
	public ExternalizableDictionary(int initialCapacity, float loadFactor) {
		if ((initialCapacity < 0) || (loadFactor <= 0.0)) {
			throw new IllegalArgumentException();
		}
		this.loadFactor = loadFactor;
		if (initialCapacity > 0)
			initTable(initialCapacity);
	}

	private void initTable(int initialCapacity) {
		table = new HashtableEntry[initialCapacity];
		threshold = (int) (initialCapacity * loadFactor);
	}

	/**
	 * Constructs a new, empty dictionary with the wanted capacity and default
	 * load factor.
	 */
	public ExternalizableDictionary(int initialCapacity) {
		this(initialCapacity, 0.75f);
	}

	/**
	 * Constructs a new, empty dictionary with a default capacity and load
	 * factor.
	 */
	public ExternalizableDictionary() {
		this(101, 0.75f);
	}

	public ClassLoader setClassLaoder(ClassLoader loader) {
		ClassLoader tmp = this.loader;
		this.loader = loader;
		return tmp;
	}

	public ClassLoader setClassLoader(ClassLoader loader) {
		ClassLoader tmp = this.loader;
		this.loader = loader;
		return tmp;
	}

	public Class[] remoteInterfaces() {
		return CLASSES;
	}

	/**
	 * Gets the size of the elements in the dictionary.
	 * 
	 * @return size of hashtable
	 */
	public int size() {
		return count;
	}

	/**
	 * Checks if there is any element in the dictionary.
	 * 
	 * @return true if empty.
	 */
	public boolean isEmpty() {
		return count == 0;
	}

	/**
	 * Gets an enumeration with dictionary's keys.
	 * 
	 * @return an enumeration with dictionary's keys.
	 */
	public synchronized Enumeration keys() {
		return new HashtableEnumerator(table, true);
	}

	/**
	 * Gets an enumeration with dictionary's values.
	 * 
	 * @return an enumeration with dictionary's values.
	 */
	public synchronized Enumeration elements() {
		return new HashtableEnumerator(table, false);
	}

	private int hashCode(String key) {
		return key.toLowerCase().hashCode();
	}

	/**
	 * Gets the value corresponding to this key or any of its case
	 * representations.
	 * 
	 * @param key
	 *            String key
	 * @return object value or null if none
	 */
	public synchronized Object get(Object key) {
		if (table != null) {
			HashtableEntry tab[] = table;
			int hash = hashCode((String) key);
			int index = (hash & 0x7FFFFFFF) % tab.length;
			for (HashtableEntry e = tab[index]; e != null; e = e.next) {
				if ((e.hash == hash) && e.key.equalsIgnoreCase((String) key)) {
					return e.value;
				}
			}
		}
		return null;
	}

	protected void rehash() {
		int oldCapacity = table.length;
		HashtableEntry oldTable[] = table;

		int newCapacity = oldCapacity * 2 + 1;
		HashtableEntry newTable[] = new HashtableEntry[newCapacity];

		threshold = (int) (newCapacity * loadFactor);
		table = newTable;

		for (int i = oldCapacity; i-- > 0;) {
			for (HashtableEntry old = oldTable[i]; old != null;) {
				HashtableEntry e = old;
				old = old.next;
				int index = (e.hash & 0x7FFFFFFF) % newCapacity;
				e.next = newTable[index];
				newTable[index] = e;
			}
		}
	}

	/**
	 * Puts the key and the value in the table. If there already is a key equal
	 * ignore case to the one passed the new value exchhanes the old one.
	 * 
	 * @param key
	 *            String key
	 * @param value
	 *            object to put
	 * @return old value if any, or null if none
	 * @exception IllegalArgumentException
	 *                if key is not a string
	 */
	public synchronized Object put(Object key, Object value) throws IllegalArgumentException {

		if (value == null) {
			throw new NullPointerException();
		}
		if (table == null)
			initTable(MIN_CAPACITY);
		try {
			// Makes sure the key is not already in the hashtable.
			int hash = hashCode((String) key);
			int index;
			HashtableEntry[] tab = null;
			do {
				tab = table;
				index = (hash & 0x7FFFFFFF) % tab.length;
				for (HashtableEntry e = tab[index]; e != null; e = e.next) {
					if ((e.hash == hash) && e.key.equalsIgnoreCase((String) key)) {
						Object old = e.value;
						e.value = value;
						return old;
					}
				}
				if (count >= threshold) {
					// Rehash the table if the threshold is exceeded
					rehash();
					continue;
				}
				break;
			} while (true);

			// Creates the new entry.
			HashtableEntry e = new HashtableEntry();
			e.hash = hash;
			e.key = (String) key;
			e.value = value;
			e.next = tab[index];
			tab[index] = e;
			count++;
			return null;
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException("Non string keys are not accepted!");
		}
	}

	/**
	 * Removes the key and its corresponding value. Key is searched case
	 * insensitively.
	 * 
	 * @param key
	 *            string key
	 * @return object removed or null if none
	 * @exception IllegalArgumentException
	 *                if key is not s string
	 */
	public synchronized Object remove(Object key) throws IllegalArgumentException {

		if (table == null)
			return null;
		try {
			HashtableEntry tab[] = table;
			int hash = hashCode((String) key);
			int index = (hash & 0x7FFFFFFF) % tab.length;
			for (HashtableEntry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
				if ((e.hash == hash) && e.key.equalsIgnoreCase((String) key)) {
					if (prev != null) {
						prev.next = e.next;
					} else {
						tab[index] = e.next;
					}
					count--;
					return e.value;
				}
			}
			return null;
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException("Non string keys are not accepted!");
		}
	}

	/**
	 * Clears all elements from the distionary.
	 */
	public synchronized void clear() {
		if (table == null)
			return;
		for (int index = table.length; --index >= 0;) {
			table[index] = null;
		}
		count = 0;
	}

	/**
	 * Creates a shallow copy of this hashtable. The keys and values themselves
	 * are not cloned.
	 * 
	 * @return a new CMDictioanry with the same fields
	 */
	public synchronized Object clone() {
		try {
			ExternalizableDictionary cmDict = (ExternalizableDictionary) super.clone();
			if (table != null) {
				cmDict.table = new HashtableEntry[table.length];
				for (int i = table.length; i-- > 0;) {
					cmDict.table[i] = (table[i] != null) ? (HashtableEntry) table[i].clone() : null;
				}
			}
			return cmDict;
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

	/**
	 * Compares the specified Object with this Dictionary for equality,
	 * 
	 * @return true if the specified Object is equal to this Dictionary.
	 */

	public synchronized boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Dictionary))
			return false;
		Dictionary dict = (Dictionary) o;
		int max = size();
		if (dict.size() != max)
			return false;
		Enumeration k = keys();
		Enumeration e = elements();
		for (int i = 0; i < max; i++) {
			Object key = k.nextElement();
			Object value = e.nextElement();
			if (!value.equals(dict.get(key))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Makes a string represntation of this object.
	 * 
	 * @return string represntation of this object
	 */
	public synchronized String toString() {
		int max = size() - 1;
		StringBuffer buf = new StringBuffer();
		Enumeration k = keys();
		Enumeration e = elements();
		buf.append('{');
		for (int i = 0; i <= max; i++) {
			String s1 = (String) k.nextElement();
			String s2 = e.nextElement().toString();
			buf.append(s1);
			buf.append('=');
			buf.append(s2);
			if (i < max) {
				buf.append(',');
				buf.append(' ');
			}
		}
		buf.append('}');
		return buf.toString();
	}

	/**
	 * Writes this object to the stream passed. The object can be loaded again
	 * via its readObject method.
	 * 
	 * @param os
	 *            strream to write data to.
	 * @exception Exception
	 *                if error of any kind occurs
	 */
	public synchronized void writeObject(OutputStream os) throws Exception {
		Enumeration keys = keys();
		Enumeration values = elements();
		PDataStream.writeInt(size(), os);
		// dictionary is never empty:
		// it either has elements or is null
		while (keys.hasMoreElements()) {
			PDataStream.writeUTF((String) keys.nextElement(), os);
			writeValue(values.nextElement(), os);
		}
	}

	private static void writeValue(Object value, OutputStream os) throws IOException {

		if (value == null) {
			os.write(-1);
		} else {
			Class valueClass = value.getClass();
			// write 0 for a single value, 1 for array, and 2 for Vector
			if (valueClass.isArray()) {
				os.write(1);
				int length = Array.getLength(value);
				PDataStream.writeInt(length, os);
				Class componentType = valueClass.getComponentType();
				if (componentType.isPrimitive()) {
					// primitive
					PDataStream.writeBoolean(true, os);
					// 1: int;2: long; 3: byte; 4: boolean; 5: character; 6:
					// short; 7: float; 8: double
					writePrimitiveArray(componentType, value, length, os);
				} else {
					PDataStream.writeBoolean(false, os);
					PDataStream.writeUTF(componentType.getName(), os);
					Object[] oArr = (Object[]) value;
					for (int i = 0; i < length; i++) {
						writeValue(oArr[i], os);
					}
				}
			} else if (valueClass.equals(Vector.class)) {
				os.write(2);
				int size = ((Vector) value).size();
				PDataStream.writeInt(size, os);
				for (int i = 0; i < size; i++) {
					writeValue(((Vector) value).elementAt(i), os);
				}
			} else {
				os.write(0);
				writeRealObject(value, valueClass, os);
			}
		}
	}

	private static Object readValue(InputStream is, ClassLoader loader) throws Exception {
		byte type = (byte) is.read();
		if (type == -1) {
			return null;
		}
		Class vClass = null;
		if (type == 2) {
			int length = PDataStream.readInt(is);
			Vector v = new Vector(length);
			for (int i = 0; i < length; i++) {
				v.insertElementAt(readValue(is, loader), i);
			}
			return v;
		} else if (type == 0) {
			return readRealObject((byte) is.read(), is, loader);
		} else {
			int length = PDataStream.readInt(is);
			boolean primitive = PDataStream.readBoolean(is);
			if (primitive) {
				return readPrimitiveArray(length, is);
			}
			vClass = loader == null ? Class.forName(PDataStream.readUTF(is)) : loader.loadClass(PDataStream.readUTF(is));
			Object array = Array.newInstance(vClass, length);
			for (int i = 0; i < length; i++) {
				Array.set(array, i, readValue(is, loader));
			}
			return array;
		}
	}

	/**
	 * Reads the data from the InputStream and loads the data in the table.
	 * 
	 * @param is
	 *            stream to read dictionary's data from
	 * @exception Exception
	 *                if an error of any kind occurs while reading
	 */
	public synchronized void readObject(InputStream is) throws Exception {
		try {
			int size = PDataStream.readInt(is);
			if (table == null) {
				if (size > 0) {
					initTable(size);
				} else
					initTable(MIN_CAPACITY);
			}
			for (int i = 0; i < size; i++) {
				put(PDataStream.readUTF(is), readValue(is, loader));
			}
		} catch (Exception e) {
			throw e;
		}
	}

	// 1: int;2: long; 3: byte; 4: boolean; 5: character;
	// 6: short; 7: float; 8: double
	private static void writePrimitiveArray(Class componentType, Object array, int length, OutputStream os) throws IOException {

		if (componentType.equals(Integer.TYPE)) {
			int[] ints = (int[]) array;
			os.write(1);
			for (int i = 0; i < length; i++) {
				PDataStream.writeInt(ints[i], os);
			}
		} else if (componentType.equals(Long.TYPE)) {
			os.write(2);
			long[] longs = (long[]) array;
			for (int i = 0; i < length; i++) {
				PDataStream.writeLong(longs[i], os);
			}
		} else if (componentType.equals(Byte.TYPE)) {
			os.write(3);
			os.write((byte[]) array);
		} else if (componentType.equals(Boolean.TYPE)) {
			os.write(4);
			boolean[] booleans = (boolean[]) array;
			for (int i = 0; i < length; i++) {
				PDataStream.writeBoolean(booleans[i], os);
			}
		} else if (componentType.equals(Character.TYPE)) {
			os.write(5);
			char[] chars = (char[]) array;
			for (int i = 0; i < length; i++) {
				PDataStream.writeChar(chars[i], os);
			}
		} else if (componentType.equals(Short.TYPE)) {
			os.write(6);
			short[] shorts = (short[]) array;
			for (int i = 0; i < length; i++) {
				PDataStream.writeShort(shorts[i], os);
			}
		} else if (componentType.equals(Float.TYPE)) {
			os.write(7);
			float[] floats = (float[]) array;
			for (int i = 0; i < length; i++) {
				PDataStream.writeFloat(floats[i], os);
			}
		} else if (componentType.equals(Double.TYPE)) {
			os.write(8);
			double[] doubles = (double[]) array;
			for (int i = 0; i < length; i++) {
				PDataStream.writeDouble(doubles[i], os);
			}
		} else {
			throw new IllegalArgumentException("Unsupported Primitive Type: " + componentType);
		}
	}

	private static Object readPrimitiveArray(int length, InputStream is) throws IOException {
		byte type = (byte) is.read();
		if (type == 1) {
			int[] ints = new int[length];
			for (int i = 0; i < length; i++) {
				ints[i] = PDataStream.readInt(is);
			}
			return ints;
		} else if (type == 2) {
			long[] longs = new long[length];
			for (int i = 0; i < length; i++) {
				longs[i] = PDataStream.readLong(is);
			}
			return longs;
		} else if (type == 3) {
			byte[] bytes = new byte[length];
			is.read(bytes);
			return bytes;
		} else if (type == 4) {
			boolean[] booleans = new boolean[length];
			for (int i = 0; i < length; i++) {
				booleans[i] = PDataStream.readBoolean(is);
			}
			return booleans;
		} else if (type == 5) {
			char[] chars = new char[length];
			for (int i = 0; i < length; i++) {
				chars[i] = PDataStream.readChar(is);
			}
			return chars;
		} else if (type == 6) {
			short[] shorts = new short[length];
			for (int i = 0; i < length; i++) {
				shorts[i] = PDataStream.readShort(is);
			}
			return shorts;
		} else if (type == 7) {
			float[] floats = new float[length];
			for (int i = 0; i < length; i++) {
				floats[i] = PDataStream.readFloat(is);
			}
			return floats;
		} else if (type == 8) {
			double[] doubles = new double[length];
			for (int i = 0; i < length; i++) {
				doubles[i] = PDataStream.readDouble(is);
			}
			return doubles;
		} else {
			throw new IllegalArgumentException("Trying to read unsupported primitive type: " + type);
		}
	}

	// only if this is an object (not primitive) and non null!
	private static void writeRealObject(Object value, Class vClass, OutputStream os) throws IOException {
		try {
			if (vClass.equals(String.class)) {
				os.write(0);
				PDataStream.writeUTF((String) value, os);
			} else if (vClass.equals(Integer.class)) {
				os.write(1);
				PDataStream.writeInt(((Integer) value).intValue(), os);
			} else if (vClass.equals(Long.class)) {
				os.write(2);
				PDataStream.writeLong(((Long) value).longValue(), os);
			} else if (vClass.equals(Byte.class)) {
				os.write(3);
				os.write(((Byte) value).byteValue());
			} else if (vClass.equals(Boolean.class)) {
				os.write(4);
				PDataStream.writeBoolean(((Boolean) value).booleanValue(), os);
			} else if (vClass.equals(Character.class)) {
				os.write(5);
				PDataStream.writeChar(((Character) value).charValue(), os);
			} else if (vClass.equals(Short.class)) {
				os.write(6);
				PDataStream.writeShort(((Short) value).shortValue(), os);
			} else if (vClass.equals(Float.class)) {
				os.write(7);
				PDataStream.writeFloat(((Float) value).floatValue(), os);
			} else if (vClass.equals(Double.class)) {
				os.write(8);
				PDataStream.writeDouble(((Double) value).doubleValue(), os);
			} else if (Externalizable.class.isAssignableFrom(vClass)) {
				os.write(11);
				String name = vClass.getName();
				PDataStream.writeUTF(name, os);
				Externalizable tmp = (Externalizable) value;
				tmp.writeObject(os);
			} else {
				os.write(12);
				ObjectOutputStream out = new ObjectOutputStream(os);
				out.writeObject(value);
			}
		} catch (Exception exc) {
			throw new IOException(exc.toString());
		}
	}

	private static Object readRealObject(byte type, InputStream is, ClassLoader loader) throws IOException {
		try {
			if (type == 0) {
				return PDataStream.readUTF(is);
			} else if (type == 1) {
				return new Integer(PDataStream.readInt(is));
			} else if (type == 2) {
				return new Long(PDataStream.readLong(is));
			} else if (type == 3) {
				return new Byte((byte) is.read());
			} else if (type == 4) {
				return PDataStream.readBoolean(is) ? Boolean.TRUE : Boolean.FALSE;
			} else if (type == 5) {
				return new Character(PDataStream.readChar(is));
			} else if (type == 6) {
				return new Short(PDataStream.readShort(is));
			} else if (type == 7) {
				return new Float(PDataStream.readFloat(is));
			} else if (type == 8) {
				return new Double(PDataStream.readDouble(is));
			} else if (type == 11) {
				String name = PDataStream.readUTF(is);
				Class c = loader == null ? Class.forName(name) : loader.loadClass(name);
				if (Externalizable.class.isAssignableFrom(c)) {
					Externalizable obj = (Externalizable) c.newInstance();
					obj.readObject(is);
					return obj;
				}
				throw new IOException("Could not read object " + name);
			} else if (type == 12) {
				ObjectInputStream in = loader == null ? new ObjectInputStream(is) : (ObjectInputStream) new XObjectInputStream(loader, is);
				return in.readObject();
			}
		} catch (ClassNotFoundException cnfe) {
			throw new IOException("Could not find class " + cnfe.toString());
		} catch (Exception exc) {
			throw exc instanceof IOException ? (IOException) exc : new IOException("Could not read object " + exc.toString());
		}
		throw new IllegalArgumentException("Unsupported Typed Object: " + type);
	}

	/**
	 * 
	 * 
	 * @param props
	 * @exception IllegalArgumentException
	 */
	public synchronized void copyFrom(Dictionary props) throws IllegalArgumentException {
		Enumeration keys = props.keys();
		Enumeration values = props.elements();
		while (keys.hasMoreElements()) {
			put(keys.nextElement(), values.nextElement());
		}
	}

}

class HashtableEntry {
	int hash;
	String key;
	Object value;
	HashtableEntry next;

	protected Object clone() {
		HashtableEntry entry = new HashtableEntry();
		entry.hash = hash;
		entry.key = key;
		entry.value = value;
		entry.next = (next != null) ? (HashtableEntry) next.clone() : null;
		return entry;
	}
}

class HashtableEnumerator implements Enumeration {
	boolean keys;
	int index;
	HashtableEntry table[];
	HashtableEntry entry;

	HashtableEnumerator(HashtableEntry table[], boolean keys) {
		this.table = table;
		this.keys = keys;
		this.index = table.length;
	}

	public boolean hasMoreElements() {
		if (table == null)
			return false;
		if (entry != null) {
			return true;
		}
		while (index-- > 0) {
			if ((entry = table[index]) != null) {
				return true;
			}
		}
		return false;
	}

	public Object nextElement() {
		if (table != null) {
			if (entry == null) {
				while ((index-- > 0) && ((entry = table[index]) == null));
			}
			if (entry != null) {
				HashtableEntry e = entry;
				entry = e.next;
				return keys ? e.key : e.value;
			}
		}
		throw new NoSuchElementException("HashtableEnumerator");
	}
}

class XObjectInputStream extends ObjectInputStream {

	ClassLoader loader;

	public XObjectInputStream(ClassLoader loader, InputStream is) throws IOException {
		super(is);
		this.loader = loader;
	}

	protected Class resolveClass(ObjectStreamClass v) throws ClassNotFoundException {
		return loader.loadClass(v.getName());
	}
}
