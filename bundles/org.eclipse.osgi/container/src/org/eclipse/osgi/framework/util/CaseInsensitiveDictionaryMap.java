/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

package org.eclipse.osgi.framework.util;

import static java.util.Objects.requireNonNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Constants;

/**
 * CaseInsensitiveDictionaryMap classes. This class implements Dictionary and
 * Map with the following behavior:
 * <ul>
 * <li>String keys are case-preserved, but the lookup operations are
 * case-insensitive.</li>
 * <li>Keys and values must not be null.</li>
 * </ul>
 * 
 * @since 3.13
 */
public class CaseInsensitiveDictionaryMap<K, V> extends Dictionary<K, V> implements Map<K, V> {
	// common core service property keys
	private static final CaseInsensitiveKey KEY_SERVICE_OBJECTCLASS = new CaseInsensitiveKey(Constants.OBJECTCLASS);
	private static final CaseInsensitiveKey KEY_SERVICE_BUNDLE_ID = new CaseInsensitiveKey(Constants.SERVICE_BUNDLEID);
	private static final CaseInsensitiveKey KEY_SERVICE_CHANGECOUNT = new CaseInsensitiveKey(
			Constants.SERVICE_CHANGECOUNT);
	private static final CaseInsensitiveKey KEY_SERVICE_DESCRIPTION = new CaseInsensitiveKey(
			Constants.SERVICE_DESCRIPTION);
	private static final CaseInsensitiveKey KEY_SERVICE_ID = new CaseInsensitiveKey(Constants.SERVICE_ID);
	private static final CaseInsensitiveKey KEY_SERVICE_PID = new CaseInsensitiveKey(Constants.SERVICE_PID);
	private static final CaseInsensitiveKey KEY_SERVICE_RANKING = new CaseInsensitiveKey(Constants.SERVICE_RANKING);
	private static final CaseInsensitiveKey KEY_SERVICE_SCOPE = new CaseInsensitiveKey(Constants.SERVICE_SCOPE);
	private static final CaseInsensitiveKey KEY_SERVICE_VENDER = new CaseInsensitiveKey(Constants.SERVICE_VENDOR);

	// common SCR service property keys
	private static final CaseInsensitiveKey KEY_COMPONENT_NAME = new CaseInsensitiveKey("component.name"); //$NON-NLS-1$
	private static final CaseInsensitiveKey KEY_COMPONENT_ID = new CaseInsensitiveKey("component.id"); //$NON-NLS-1$

	// common meta-type property keys
	private static final CaseInsensitiveKey KEY_METATYPE_PID = new CaseInsensitiveKey("metatype.pid"); //$NON-NLS-1$
	private static final CaseInsensitiveKey KEY_METATYPE_FACTORY_PID = new CaseInsensitiveKey("metatype.factory.pid"); //$NON-NLS-1$

	// common event admin keys
	private static final CaseInsensitiveKey KEY_EVENT_TOPICS = new CaseInsensitiveKey("event.topics"); //$NON-NLS-1$
	private static final CaseInsensitiveKey KEY_EVENT_FILTER = new CaseInsensitiveKey("event.filter"); //$NON-NLS-1$

	// jmx keys
	private static final CaseInsensitiveKey KEY_JMX_OBJECTNAME = new CaseInsensitiveKey("jmx.objectname"); //$NON-NLS-1$

	// common bundle manifest headers
	private static final CaseInsensitiveKey KEY_JAR_MANIFESTVERSION = new CaseInsensitiveKey("Manifest-Version"); //$NON-NLS-1$
	private static final CaseInsensitiveKey KEY_BUNDLE_ACTIVATIONPOLICY = new CaseInsensitiveKey(
			Constants.BUNDLE_ACTIVATIONPOLICY);
	private static final CaseInsensitiveKey KEY_BUNDLE_ACTIVATOR = new CaseInsensitiveKey(Constants.BUNDLE_ACTIVATOR);
	private static final CaseInsensitiveKey KEY_BUNDLE_CLASSPATH = new CaseInsensitiveKey(Constants.BUNDLE_CLASSPATH);
	private static final CaseInsensitiveKey KEY_BUNDLE_DESCRIPTION = new CaseInsensitiveKey(
			Constants.BUNDLE_DESCRIPTION);
	private static final CaseInsensitiveKey KEY_BUNDLE_LICENSE = new CaseInsensitiveKey(Constants.BUNDLE_LICENSE);
	private static final CaseInsensitiveKey KEY_BUNDLE_LOCALIZATION = new CaseInsensitiveKey(
			Constants.BUNDLE_LOCALIZATION);
	private static final CaseInsensitiveKey KEY_BUNDLE_MANIFESTVERSION = new CaseInsensitiveKey(
			Constants.BUNDLE_MANIFESTVERSION);
	private static final CaseInsensitiveKey KEY_BUNDLE_NAME = new CaseInsensitiveKey(Constants.BUNDLE_NAME);
	private static final CaseInsensitiveKey KEY_BUNDLE_NATIVECODE = new CaseInsensitiveKey(Constants.BUNDLE_NATIVECODE);
	@SuppressWarnings("deprecation")
	private static final CaseInsensitiveKey KEY_BUNDLE_REQUIREDEXECUTIONENVIRONMENT = new CaseInsensitiveKey(
			Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	private static final CaseInsensitiveKey KEY_BUNDLE_SCM = new CaseInsensitiveKey(Constants.BUNDLE_SCM);
	private static final CaseInsensitiveKey KEY_BUNDLE_SYMBOLICNAME = new CaseInsensitiveKey(
			Constants.BUNDLE_SYMBOLICNAME);
	private static final CaseInsensitiveKey KEY_BUNDLE_VENDOR = new CaseInsensitiveKey(Constants.BUNDLE_VENDOR);
	private static final CaseInsensitiveKey KEY_BUNDLE_VERSION = new CaseInsensitiveKey(Constants.BUNDLE_VERSION);
	private static final CaseInsensitiveKey KEY_BUNDLE_DYNAMICIMPORT_PACKAGE = new CaseInsensitiveKey(
			Constants.DYNAMICIMPORT_PACKAGE);
	private static final CaseInsensitiveKey KEY_BUNDLE_EXPORT_PACKAGE = new CaseInsensitiveKey(
			Constants.EXPORT_PACKAGE);
	private static final CaseInsensitiveKey KEY_BUNDLE_FRAGMENT_HOST = new CaseInsensitiveKey(Constants.FRAGMENT_HOST);
	private static final CaseInsensitiveKey KEY_BUNDLE_IMPORT_PACKAGE = new CaseInsensitiveKey(
			Constants.IMPORT_PACKAGE);
	private static final CaseInsensitiveKey KEY_BUNDLE_REQUIRE_BUNDLE = new CaseInsensitiveKey(
			Constants.REQUIRE_BUNDLE);
	private static final CaseInsensitiveKey KEY_BUNDLE_REQUIRE_CAPABILITY = new CaseInsensitiveKey(
			Constants.REQUIRE_CAPABILITY);
	private static final CaseInsensitiveKey KEY_BUNDLE_PROVIDE_CAPABILITY = new CaseInsensitiveKey(
			Constants.PROVIDE_CAPABILITY);

	@SuppressWarnings("deprecation")
	public static CaseInsensitiveKey findCommonKeyIndex(String key) {
		switch (key) {
		// common core service property keys
		case Constants.OBJECTCLASS:
			return KEY_SERVICE_OBJECTCLASS;
		case Constants.SERVICE_BUNDLEID:
			return KEY_SERVICE_BUNDLE_ID;
		case Constants.SERVICE_CHANGECOUNT:
			return KEY_SERVICE_CHANGECOUNT;
		case Constants.SERVICE_DESCRIPTION:
			return KEY_SERVICE_DESCRIPTION;
		case Constants.SERVICE_ID:
			return KEY_SERVICE_ID;
		case Constants.SERVICE_PID:
			return KEY_SERVICE_PID;
		case Constants.SERVICE_RANKING:
			return KEY_SERVICE_RANKING;
		case Constants.SERVICE_SCOPE:
			return KEY_SERVICE_SCOPE;
		case Constants.SERVICE_VENDOR:
			return KEY_SERVICE_VENDER;

		// common SCR service property keys
		case "component.name": //$NON-NLS-1$
			return KEY_COMPONENT_NAME;
		case "component.id": //$NON-NLS-1$
			return KEY_COMPONENT_ID;

		// common meta-type property keys
		case "metatype.pid": //$NON-NLS-1$
			return KEY_METATYPE_PID;
		case "metatype.factory.pid": //$NON-NLS-1$
			return KEY_METATYPE_FACTORY_PID;

		// common event admin keys
		case "event.topics": //$NON-NLS-1$
			return KEY_EVENT_TOPICS;
		case "event.filter": //$NON-NLS-1$
			return KEY_EVENT_FILTER;

		// jmx keys
		case "jmx.objectname": //$NON-NLS-1$
			return KEY_JMX_OBJECTNAME;

		// common bundle manifest headers
		case "Manifest-Version": //$NON-NLS-1$
			return KEY_JAR_MANIFESTVERSION;
		case Constants.BUNDLE_ACTIVATIONPOLICY:
			return KEY_BUNDLE_ACTIVATIONPOLICY;
		case Constants.BUNDLE_ACTIVATOR:
			return KEY_BUNDLE_ACTIVATOR;
		case Constants.BUNDLE_CLASSPATH:
			return KEY_BUNDLE_CLASSPATH;
		case Constants.BUNDLE_DESCRIPTION:
			return KEY_BUNDLE_DESCRIPTION;
		case Constants.BUNDLE_LICENSE:
			return KEY_BUNDLE_LICENSE;
		case Constants.BUNDLE_LOCALIZATION:
			return KEY_BUNDLE_LOCALIZATION;
		case Constants.BUNDLE_MANIFESTVERSION:
			return KEY_BUNDLE_MANIFESTVERSION;
		case Constants.BUNDLE_NAME:
			return KEY_BUNDLE_NAME;
		case Constants.BUNDLE_NATIVECODE:
			return KEY_BUNDLE_NATIVECODE;
		case Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT:
			return KEY_BUNDLE_REQUIREDEXECUTIONENVIRONMENT;
		case Constants.BUNDLE_SCM:
			return KEY_BUNDLE_SCM;
		case Constants.BUNDLE_SYMBOLICNAME:
			return KEY_BUNDLE_SYMBOLICNAME;
		case Constants.BUNDLE_VENDOR:
			return KEY_BUNDLE_VENDOR;
		case Constants.BUNDLE_VERSION:
			return KEY_BUNDLE_VERSION;
		case Constants.DYNAMICIMPORT_PACKAGE:
			return KEY_BUNDLE_DYNAMICIMPORT_PACKAGE;
		case Constants.EXPORT_PACKAGE:
			return KEY_BUNDLE_EXPORT_PACKAGE;
		case Constants.FRAGMENT_HOST:
			return KEY_BUNDLE_FRAGMENT_HOST;
		case Constants.IMPORT_PACKAGE:
			return KEY_BUNDLE_IMPORT_PACKAGE;
		case Constants.REQUIRE_BUNDLE:
			return KEY_BUNDLE_REQUIRE_BUNDLE;
		case Constants.REQUIRE_CAPABILITY:
			return KEY_BUNDLE_REQUIRE_CAPABILITY;
		case Constants.PROVIDE_CAPABILITY:
			return KEY_BUNDLE_PROVIDE_CAPABILITY;
		}
		return null;
	}

	final Map<Object, V> map;

	/**
	 * Create an empty CaseInsensitiveDictionaryMap.
	 */
	public CaseInsensitiveDictionaryMap() {
		map = new HashMap<>();
	}

	/**
	 * Create an empty CaseInsensitiveDictionaryMap.
	 *
	 * @param initialCapacity The initial capacity.
	 */
	public CaseInsensitiveDictionaryMap(int initialCapacity) {
		map = new HashMap<>(initialCapacity);
	}

	/**
	 * Create a CaseInsensitiveDictionaryMap dictionary from a Dictionary.
	 *
	 * @param dictionary The initial dictionary for this
	 *                   CaseInsensitiveDictionaryMap object.
	 * @throws IllegalArgumentException If a case-variants of a key are in the
	 *                                  dictionary parameter.
	 */
	public CaseInsensitiveDictionaryMap(Dictionary<? extends K, ? extends V> dictionary) {
		this(initialCapacity(dictionary.size()));
		/* initialize the keys and values */
		Enumeration<? extends K> keys = dictionary.keys();
		while (keys.hasMoreElements()) {
			K key = keys.nextElement();
			// ignore null keys
			if (key != null) {
				V value = dictionary.get(key);
				// ignore null values
				if (value != null) {
					if (put(key, value) != null) {
						throw new IllegalArgumentException(NLS.bind(Msg.HEADER_DUPLICATE_KEY_EXCEPTION, key));
					}
				}
			}
		}
	}

	/**
	 * Create a CaseInsensitiveDictionaryMap dictionary from a Map.
	 *
	 * @param map The initial map for this CaseInsensitiveDictionaryMap object.
	 * @throws IllegalArgumentException If a case-variants of a key are in the map
	 *                                  parameter.
	 */
	public CaseInsensitiveDictionaryMap(Map<? extends K, ? extends V> map) {
		this(initialCapacity(map.size()));
		/* initialize the keys and values */
		for (Entry<? extends K, ? extends V> e : map.entrySet()) {
			K key = e.getKey();
			// ignore null keys
			if (key != null) {
				V value = e.getValue();
				// ignore null values
				if (value != null) {
					if (put(key, value) != null) {
						throw new IllegalArgumentException(NLS.bind(Msg.HEADER_DUPLICATE_KEY_EXCEPTION, key));
					}
				}
			}
		}
	}

	/**
	 * Compute the initial capacity of a map for the specified number of entries
	 * based upon the load factor of 0.75f.
	 *
	 * @param size The desired number of entries.
	 * @return The initial capacity of a map.
	 */
	protected static int initialCapacity(int size) {
		return Math.max((int) (size / 0.75f) + 1, 16);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<K> keys() {
		return Collections.enumeration(keySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Enumeration<V> elements() {
		return Collections.enumeration(values());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the key is a String, the key is located in a case-insensitive manner.
	 */
	@Override
	public V get(Object key) {
		return map.get(keyWrap(key));
	}

	/**
	 * Returns the specified key or, if the key is a String, returns a
	 * case-insensitive wrapping of the key.
	 *
	 * @return The specified key or a case-insensitive wrapping of the key.
	 */
	private Object keyWrap(Object key) {
		if (key instanceof String) {
			CaseInsensitiveKey commonKey = findCommonKeyIndex((String) key);
			if (commonKey != null) {
				return commonKey;
			}
			return new CaseInsensitiveKey((String) key);
		}
		return key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return map.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The key and value must be non-null.
	 * <p>
	 * If the key is a String, any case-variant will be replaced.
	 */
	@Override
	public V put(K key, V value) {
		requireNonNull(value);
		if (key instanceof String) {
			Object wrappedKey = keyWrap(key);
			V existing = map.put(wrappedKey, value);
			if (existing != null) {
				// must remove to replace key if case has changed
				map.remove(wrappedKey);
				map.put(wrappedKey, value);
			}
			return existing;
		}
		return map.put(requireNonNull(key), value);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the key is a String, the key is removed in a case-insensitive manner.
	 */
	@Override
	public V remove(Object key) {
		return map.remove(keyWrap(key));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return map.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		map.clear();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the key is a String, the key is located in a case-insensitive manner.
	 */
	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(keyWrap(key));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	private transient Set<Entry<K, V>> entrySet = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> es = entrySet;
		if (es == null) {
			return entrySet = new EntrySet();
		}
		return es;
	}

	private transient Set<K> keySet = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<K> keySet() {
		Set<K> ks = keySet;
		if (ks == null) {
			return keySet = new KeySet();
		}
		return ks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<V> values() {
		return map.values();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the specified map has case-variants of a String key, only the last
	 * case-variant found while iterating over the entrySet will be present in this
	 * object.
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return map.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return map.equals(obj);
	}

	/**
	 * Return an unmodifiable map wrapping this CaseInsensitiveDictionaryMap.
	 *
	 * @return An unmodifiable map wrapping this CaseInsensitiveDictionaryMap.
	 */
	public Map<K, V> asUnmodifiableMap() {
		return Collections.unmodifiableMap(this);
	}

	/**
	 * Return an unmodifiable dictionary wrapping this CaseInsensitiveDictionaryMap.
	 *
	 * @return An unmodifiable dictionary wrapping this
	 *         CaseInsensitiveDictionaryMap.
	 */
	public Dictionary<K, V> asUnmodifiableDictionary() {
		return unmodifiableDictionary(this);
	}

	/**
	 * Return an unmodifiable dictionary wrapping the specified dictionary.
	 *
	 * @return An unmodifiable dictionary wrapping the specified dictionary.
	 */
	public static <K, V> Dictionary<K, V> unmodifiableDictionary(Dictionary<? extends K, ? extends V> d) {
		return new UnmodifiableDictionary<>(d);
	}

	private static final class UnmodifiableDictionary<K, V> extends Dictionary<K, V> {
		private final Dictionary<? extends K, ? extends V> d;

		UnmodifiableDictionary(Dictionary<? extends K, ? extends V> d) {
			this.d = requireNonNull(d);
		}

		@Override
		public int size() {
			return d.size();
		}

		@Override
		public boolean isEmpty() {
			return d.isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Enumeration<K> keys() {
			return (Enumeration<K>) d.keys();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Enumeration<V> elements() {
			return (Enumeration<V>) d.elements();
		}

		@Override
		public V get(Object key) {
			return d.get(key);
		}

		@Override
		public V put(K key, V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return d.toString();
		}

		@Override
		public int hashCode() {
			return d.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			return d.equals(obj);
		}
	}

	static int computeHashCode(String key) {
		int h = 1;
		for (char c : key.toCharArray()) {
			if (c < 0x80) { // ASCII
				if (c >= 'A' && c <= 'Z') {
					c += 'a' - 'A'; // convert to ASCII lowercase
				}
			} else {
				c = Character.toLowerCase(Character.toUpperCase(c));
			}
			h = 31 * h + c;
		}
		return h;
	}

	private static final class CaseInsensitiveKey {
		final String key;
		final private int hashCode;

		CaseInsensitiveKey(String key) {
			this.key = key;
			this.hashCode = computeHashCode(key);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof CaseInsensitiveKey) {
				return key.equalsIgnoreCase(((CaseInsensitiveKey) obj).key);
			}
			return false;
		}

		@Override
		public String toString() {
			return key;
		}
	}

	private final class KeySet extends AbstractSet<K> {
		KeySet() {
		}

		@Override
		public int size() {
			return CaseInsensitiveDictionaryMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return CaseInsensitiveDictionaryMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return CaseInsensitiveDictionaryMap.this.containsKey(o);
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyIterator<>(map.keySet());
		}

		@Override
		public boolean remove(Object o) {
			return CaseInsensitiveDictionaryMap.this.remove(o) != null;
		}

		@Override
		public void clear() {
			CaseInsensitiveDictionaryMap.this.clear();
		}
	}

	private static final class KeyIterator<K> implements Iterator<K> {
		private final Iterator<Object> i;

		KeyIterator(Collection<Object> c) {
			this.i = c.iterator();
		}

		@Override
		public boolean hasNext() {
			return i.hasNext();
		}

		@SuppressWarnings("unchecked")
		@Override
		public K next() {
			Object k = i.next();
			if (k instanceof CaseInsensitiveKey) {
				k = ((CaseInsensitiveKey) k).key;
			}
			return (K) k;
		}

		@Override
		public void remove() {
			i.remove();
		}
	}

	private final class EntrySet extends AbstractSet<Entry<K, V>> {
		EntrySet() {
		}

		@Override
		public int size() {
			return CaseInsensitiveDictionaryMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return CaseInsensitiveDictionaryMap.this.isEmpty();
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIterator<>(map.entrySet());
		}

		@Override
		public void clear() {
			CaseInsensitiveDictionaryMap.this.clear();
		}
	}

	private static final class EntryIterator<K, V> implements Iterator<Entry<K, V>> {
		private final Iterator<Entry<Object, V>> i;

		EntryIterator(Collection<Entry<Object, V>> c) {
			this.i = c.iterator();
		}

		@Override
		public boolean hasNext() {
			return i.hasNext();
		}

		@Override
		public Entry<K, V> next() {
			return new CaseInsentiveEntry<>(i.next());
		}

		@Override
		public void remove() {
			i.remove();
		}
	}

	private static final class CaseInsentiveEntry<K, V> implements Entry<K, V> {
		private final Entry<Object, V> entry;

		CaseInsentiveEntry(Entry<Object, V> entry) {
			this.entry = entry;
		}

		@SuppressWarnings("unchecked")
		@Override
		public K getKey() {
			Object k = entry.getKey();
			if (k instanceof CaseInsensitiveKey) {
				k = ((CaseInsensitiveKey) k).key;
			}
			return (K) k;
		}

		@Override
		public V getValue() {
			return entry.getValue();
		}

		@Override
		public V setValue(V value) {
			return entry.setValue(requireNonNull(value));
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(entry.getKey()) ^ Objects.hashCode(entry.getValue());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Entry) {
				Entry<?, ?> other = (Entry<?, ?>) obj;
				Object k1 = entry.getKey();
				@SuppressWarnings("unchecked")
				Object k2 = (other instanceof CaseInsentiveEntry) ? ((CaseInsentiveEntry<K, V>) other).entry.getKey()
						: other.getKey();
				return Objects.equals(k1, k2) && Objects.equals(entry.getValue(), other.getValue());
			}
			return false;
		}

		@Override
		public String toString() {
			return entry.toString();
		}
	}
}
