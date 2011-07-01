/*******************************************************************************
 * Copyright (c) 1997-2011 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.impl;

import java.util.*;

/**
 * This is an utility class to provide a read-only dictionary.
 * 
 * @author Stoyan Boshev
 */
public class ReadOnlyDictionary extends Dictionary implements Map {

	Map delegate;

	/**
	 * Creates a new ReadOnlyDictionary with initial set of properties
	 * 
	 * @param initialProps the initialProperties for this dictionary
	 */
	public ReadOnlyDictionary(Map initialProps) {
		this.delegate = initialProps != null ? initialProps : Collections.EMPTY_MAP;
	}

	public void updateDelegate(Map newDelegate) {
		this.delegate = newDelegate != null ? newDelegate : Collections.EMPTY_MAP;
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#put(K, V)
	 */
	public Object put(Object key, Object value) {
		// do not modify - this is read only
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
		// do not modify - this is read only
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#size()
	 */
	public int size() {
		return delegate.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#isEmpty()
	 */
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#keys()
	 */
	public Enumeration keys() {
		return Collections.enumeration(delegate.keySet());
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#elements()
	 */
	public Enumeration elements() {
		return Collections.enumeration(delegate.values());
	}

	/* (non-Javadoc)
	 * @see java.util.Dictionary#get(java.lang.Object)
	 */
	public Object get(Object key) {
		return delegate.get(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		// do not modify - this is read only
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map arg0) {
		// do not modify - this is read only
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		return Collections.unmodifiableCollection(delegate.values());
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
		return Collections.unmodifiableSet(delegate.keySet());
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
		return Collections.unmodifiableSet(delegate.entrySet());
	}
}
