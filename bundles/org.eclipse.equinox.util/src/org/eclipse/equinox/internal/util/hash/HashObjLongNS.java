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
package org.eclipse.equinox.internal.util.hash;

import java.util.NoSuchElementException;

/**
 * Hashtable for mapping Object keys to long values. The methods of this
 * hashtable are not synchronized, and if used concurently must be externally
 * synchronized
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class HashObjLongNS {

	static final float LOAD_FACTOR = 0.75f;

	// count of elements available in table
	private int count = 0;
	// used for computation of next position
	private int step = 499979;

	/**
	 * Used to enumerate the keys in the hash table. The key at index
	 * <code>i</code> is valid only if
	 * <ul>
	 * <code>  keys[i] != null </code>
	 * </ul>
	 */
	public Object[] keys;

	/**
	 * Used to enumerate the values in the hash table. The value at index
	 * <code>i</code> is valid only if
	 * <ul>
	 * <code>  keys[i] != null </code>
	 * </ul>
	 */
	public long[] values;

	/**
	 * Can be used to check if a key or value is valid. The value or key at
	 * index <code>i</code> is valid if the following expression is true
	 * <ul>
	 * <code>  next[i] != -1 && next[i] < next.length </code>
	 * </ul>
	 */
	public int[] next;

	private int limit;
	private double loadFactor;

	/**
	 * Constructs an empty hash table with keys of type int and values af type
	 * Object. Uses default load factor (0.75) and default capacity (89)
	 * 
	 */
	public HashObjLongNS() {
		this(101, LOAD_FACTOR);
	}

	/**
	 * Constructs an empty hash table with keys of type int and values af type
	 * Object. Uses default load factor (0.75).
	 * 
	 * @param capacity
	 *            initial capacity of the table
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>capacity</code> < 1.
	 */
	public HashObjLongNS(int capacity) {
		this(capacity, LOAD_FACTOR);
	}

	/**
	 * Constructs an empty hash table with keys of type int and values of type
	 * Object.
	 * 
	 * @param capacity
	 *            initial capacity of the table
	 * @param lf
	 *            load factor ot the table
	 * 
	 * @exception IllegalArgumentException
	 *                if <code>capacity</code> < 1 or <code>lf</code> < 0.0
	 */
	public HashObjLongNS(int capacity, double lf) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Invalid hashtable capacity: " + capacity + ".");
		}

		if (capacity == 0)
			capacity = 101;

		if (lf < 0) {
			throw new IllegalArgumentException("Invalid load factor: " + lf + ".");
		}

		if (lf > 1.0) {
			lf = 1.0;
		}
		loadFactor = lf;
		limit = (int) (capacity * lf);
		count = 0;

		keys = new Object[capacity];
		values = new long[capacity];
		next = new int[capacity];
		for (int i = 0; i < capacity; i++) {
			next[i] = -1;
		}
	}

	/**
	 * Adds in hashtable an element with <code>key</code> key and
	 * <code>value</code> value. If an element with the specified key is
	 * already in the table only change it's value.
	 * 
	 * @param key
	 *            the key of the inserted element
	 * @param value
	 *            the value of the inserted element
	 */
	public void put(Object key, long value) {
		if (count >= limit) {
			rehash();
		}
		if (_put(key, value)) {
			count++;
		}
	}

	/**
	 * Returns an value which is mapped to the <code>key</code> key. If there
	 * is no such a key, throws <code>NoSuchElementException</code>.
	 * 
	 * @param key
	 *            the key we are searching for
	 * @return the value this key is mapped to in the table.
	 * 
	 * @exception NoSuchElementException
	 *                if there is no element with the specified key.
	 */
	public long get(Object key) {
		int pos = find(key);
		if (pos == -1)
			throw new NoSuchElementException();
		return values[pos];
	}

	/**
	 * Removes an element with the specified key from the table. throws
	 * <code>NoSuchElementException</code> if there is no element with this
	 * key.
	 * 
	 * @param key
	 *            the key of the element we want to remove
	 * @exception NoSuchElementException
	 *                if there is no element with the specified key.
	 */
	public long remove(Object key) {
		int pos = find(key);
		if (pos == -1)
			throw new NoSuchElementException();
		next[pos] += next.length; // mark this field as empty
		keys[pos] = null;
		count--;
		return values[pos];
	}

	/**
	 * Empties the hash table
	 */
	public void removeAll() {
		for (int i = 0; i < values.length; i++) {
			keys[i] = null;
			next[i] = -1;
		}
	}

	/**
	 * Rehashes the contents of the hashtable into a hashtable with a larger
	 * capacity. This method is called automatically when the number of keys in
	 * the hashtable exceeds this hashtable's capacity and load factor.
	 */
	public void rehash() {
		Object[] tmpKeys = keys;
		long[] tmpValues = values;
		int[] tmpNext = next;

		int capacity = keys.length * 2 + 1;

		// polzwame temp array-i za da ne se namaje hashtable-a pri OutOfMemory
		Object[] keys = new Object[capacity];
		long[] values = new long[capacity];
		int[] next = new int[capacity];
		for (int i = 0; i < next.length; i++) {
			next[i] = -1;
		}

		this.keys = keys;
		this.values = values;
		this.next = next;

		for (int i = 0; i < tmpNext.length; i++) {
			if ((tmpNext[i] >= 0) && (tmpNext[i] < tmpNext.length)) {
				_put(tmpKeys[i], tmpValues[i]);
			}
		}

		limit = (int) (capacity * loadFactor);
	}

	/**
	 * Returns the count of elements currently in the table
	 * 
	 * @return the count of elements
	 */
	public int size() {
		return count;
	}

	private int find(Object key) {
		return find(key, (key.hashCode() & 0x7fffffff) % keys.length);
	} // find

	private int find(Object key, int pos) {
		int i = 0;

		while (next[pos] >= 0) {
			if (key.equals(keys[pos])) {
				if (next[pos] < next.length) {
					return pos;
				}
			}
			if ((pos = next[pos]) >= next.length) {
				pos -= next.length;
			}
			if (++i > next.length) {
				return -1;
			}
		}

		return -1;
	}

	private boolean _put(Object key, long value) {
		int pos = (key.hashCode() & 0x7fffffff) % keys.length;
		int index = find(key, pos);
		if (index != -1) {
			values[index] = value;
			return false;
		}

		while ((next[pos] >= 0) && (next[pos] < next.length)) {
			pos = next[pos];
		}

		keys[pos] = key;
		values[pos] = value;
		if (next[pos] < 0) {
			next[pos] = (pos + step) % next.length;
		} else {
			next[pos] -= next.length;
		}
		return true;
	} // _put

}
