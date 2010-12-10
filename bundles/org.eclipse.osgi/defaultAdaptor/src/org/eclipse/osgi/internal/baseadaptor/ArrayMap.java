/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.baseadaptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple map when dealing with small amounts of entries.
 *
 * @param <K> The key type
 * @param <V> the value type
 */
public class ArrayMap<K, V> {
	private final List<K> keys;
	private final List<V> values;

	public ArrayMap(int initialCapacity) {
		keys = new ArrayList<K>(initialCapacity);
		values = new ArrayList<V>(0);
	}

	public V get(K key) {
		int index = keys.indexOf(key);
		if (index < 0)
			return null;
		return values.get(index);
	}

	public void put(K key, V value) {
		int index = keys.indexOf(key);
		if (index > 0) {
			values.add(index, value);
		} else {
			keys.add(key);
			values.add(value);
		}
	}

	public V remove(K key) {
		int index = keys.indexOf(key);
		if (index < 0)
			return null;
		keys.remove(index);
		return values.remove(index);
	}

	public void clear() {
		keys.clear();
		values.clear();
	}

	public List<K> getKeys() {
		return new ArrayList<K>(keys);
	}

	public List<V> getValues() {
		return new ArrayList<V>(values);
	}
}
