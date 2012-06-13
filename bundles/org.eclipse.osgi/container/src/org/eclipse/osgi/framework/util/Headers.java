/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;

/**
 * Headers classes. This class implements a Dictionary that has
 * the following behavior:
 * <ul>
 * <li>put and remove clear throw UnsupportedOperationException.
 * The Dictionary is thus read-only to others.
 * <li>The String keys in the Dictionary are case-preserved,
 * but the get operation is case-insensitive.
 * </ul>
 * @since 3.1
 */
public class Headers<K, V> extends Dictionary<K, V> implements Map<K, V> {
	private boolean readOnly = false;
	private K[] headers;
	private V[] values;
	private int size = 0;

	/**
	 * Create an empty Headers dictionary.
	 *
	 * @param initialCapacity The initial capacity of this Headers object.
	 */
	public Headers(int initialCapacity) {
		super();
		@SuppressWarnings("unchecked")
		K[] k = (K[]) new Object[initialCapacity];
		headers = k;
		@SuppressWarnings("unchecked")
		V[] v = (V[]) new Object[initialCapacity];
		values = v;
	}

	/**
	 * Create a Headers dictionary from a Dictionary.
	 *
	 * @param values The initial dictionary for this Headers object.
	 * @exception IllegalArgumentException If a case-variant of the key is
	 * in the dictionary parameter.
	 */
	public Headers(Dictionary<? extends K, ? extends V> values) {
		this(values.size());
		/* initialize the headers and values */
		Enumeration<? extends K> keys = values.keys();
		while (keys.hasMoreElements()) {
			K key = keys.nextElement();
			set(key, values.get(key));
		}
	}

	/**
	 * Case-preserved keys.
	 */
	public synchronized Enumeration<K> keys() {
		return new ArrayEnumeration<K>(headers, size);
	}

	/**
	 * Values.
	 */
	public synchronized Enumeration<V> elements() {
		return new ArrayEnumeration<V>(values, size);
	}

	private int getIndex(Object key) {
		boolean stringKey = key instanceof String;
		for (int i = 0; i < size; i++) {
			if (stringKey && (headers[i] instanceof String)) {
				if (((String) headers[i]).equalsIgnoreCase((String) key))
					return i;
			} else {
				if (headers[i].equals(key))
					return i;
			}
		}
		return -1;
	}

	private V remove(int remove) {
		V removed = values[remove];
		for (int i = remove; i < size; i++) {
			if (i == headers.length - 1) {
				headers[i] = null;
				values[i] = null;
			} else {
				headers[i] = headers[i + 1];
				values[i] = values[i + 1];
			}
		}
		if (remove < size)
			size--;
		return removed;
	}

	private void add(K header, V value) {
		if (size == headers.length) {
			// grow the arrays
			@SuppressWarnings("unchecked")
			K[] nh = (K[]) new Object[headers.length + 10];
			K[] newHeaders = nh;
			@SuppressWarnings("unchecked")
			V[] nv = (V[]) new Object[values.length + 10];
			V[] newValues = nv;
			System.arraycopy(headers, 0, newHeaders, 0, headers.length);
			System.arraycopy(values, 0, newValues, 0, values.length);
			headers = newHeaders;
			values = newValues;
		}
		headers[size] = header;
		values[size] = value;
		size++;
	}

	/**
	 * Support case-insensitivity for keys.
	 *
	 * @param key name.
	 */
	public synchronized V get(Object key) {
		int i = -1;
		if ((i = getIndex(key)) != -1)
			return values[i];
		return null;
	}

	/**
	 * Set a header value or optionally replace it if it already exists.
	 *
	 * @param key Key name.
	 * @param value Value of the key or null to remove key.
	 * @param replace A value of true will allow a previous
	 * value of the key to be replaced.  A value of false 
	 * will cause an IllegalArgumentException to be thrown 
	 * if a previous value of the key exists.
	 * @return the previous value to which the key was mapped,
	 * or null if the key did not have a previous mapping.
	 *
	 * @exception IllegalArgumentException If a case-variant of the key is
	 * already present.
	 * @since 3.2
	 */
	public synchronized V set(K key, V value, boolean replace) {
		if (readOnly)
			throw new UnsupportedOperationException();
		if (key instanceof String) {
			@SuppressWarnings("unchecked")
			K k = (K) ((String) key).intern();
			key = k;
		}
		int i = getIndex(key);
		if (value == null) { /* remove */
			if (i != -1)
				return remove(i);
		} else { /* put */
			if (i != -1) { /* duplicate key */
				if (!replace)
					throw new IllegalArgumentException(NLS.bind(Msg.HEADER_DUPLICATE_KEY_EXCEPTION, key));
				V oldVal = values[i];
				values[i] = value;
				return oldVal;
			}
			add(key, value);
		}
		return null;
	}

	/**
	 * Set a header value.
	 *
	 * @param key Key name.
	 * @param value Value of the key or null to remove key.
	 * @return the previous value to which the key was mapped,
	 * or null if the key did not have a previous mapping.
	 *
	 * @exception IllegalArgumentException If a case-variant of the key is
	 * already present.
	 */
	public synchronized V set(K key, V value) {
		return set(key, value, false);
	}

	public synchronized void setReadOnly() {
		readOnly = true;
	}

	/**
	 * Returns the number of entries (distinct keys) in this dictionary.
	 *
	 * @return  the number of keys in this dictionary.
	 */
	public synchronized int size() {
		return size;
	}

	/**
	 * Tests if this dictionary maps no keys to value. The general contract
	 * for the <tt>isEmpty</tt> method is that the result is true if and only
	 * if this dictionary contains no entries.
	 *
	 * @return  <code>true</code> if this dictionary maps no keys to values;
	 *          <code>false</code> otherwise.
	 */
	public synchronized boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Always throws UnsupportedOperationException.
	 *
	 * @param key header name.
	 * @param value header value.
	 * @throws UnsupportedOperationException
	 */
	public synchronized V put(K key, V value) {
		if (readOnly)
			throw new UnsupportedOperationException();
		return set(key, value, true);
	}

	/**
	 * Always throws UnsupportedOperationException.
	 *
	 * @param key header name.
	 * @throws UnsupportedOperationException
	 */
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return values.toString();
	}

	public static Headers<String, String> parseManifest(InputStream in) throws BundleException {
		Headers<String, String> headers = new Headers<String, String>(10);
		try {
			ManifestElement.parseBundleManifest(in, headers);
		} catch (IOException e) {
			throw new BundleException(Msg.MANIFEST_IOEXCEPTION, BundleException.MANIFEST_ERROR, e);
		}
		headers.setReadOnly();
		return headers;
	}

	private static class ArrayEnumeration<E> implements Enumeration<E> {
		private E[] array;
		int cur = 0;

		public ArrayEnumeration(E[] array, int size) {
			@SuppressWarnings("unchecked")
			E[] a = (E[]) new Object[size];
			this.array = a;
			System.arraycopy(array, 0, this.array, 0, this.array.length);
		}

		public boolean hasMoreElements() {
			return cur < array.length;
		}

		public E nextElement() {
			return array[cur++];
		}
	}

	public synchronized void clear() {
		if (readOnly)
			throw new UnsupportedOperationException();
	}

	public synchronized boolean containsKey(Object key) {
		return getIndex(key) >= 0;
	}

	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	public Set<Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	public void putAll(Map<? extends K, ? extends V> c) {
		throw new UnsupportedOperationException();
	}

	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}
}
