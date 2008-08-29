/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.serviceregistry;

import java.util.Collection;
import java.util.Iterator;

/**
 * A Shrinkable Collection. This class provides a wrapper for a collection
 * that allows items to be removed from the wrapped collection (shrinking) but
 * does not allow items to be added to the wrapped collection. 
 * 
 * <p>
 * All the optional <code>Collection</code> operations except
 * <code>add</code> and <code>addAll</code> are supported. Attempting to add to the
 * collection will result in an <code>UnsupportedOperationException</code>.
 *
 */

public class ShrinkableCollection implements Collection {
	private final Collection collection;

	public ShrinkableCollection(Collection c) {
		if (c == null) {
			throw new NullPointerException();
		}
		collection = c;
	}

	public boolean add(Object var0) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection var0) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		collection.clear();
	}

	public boolean contains(Object var0) {
		return collection.contains(var0);
	}

	public boolean containsAll(Collection var0) {
		return collection.containsAll(var0);
	}

	public boolean isEmpty() {
		return collection.isEmpty();
	}

	public Iterator iterator() {
		return collection.iterator();
	}

	public boolean remove(Object var0) {
		return collection.remove(var0);
	}

	public boolean removeAll(Collection var0) {
		return collection.removeAll(var0);
	}

	public boolean retainAll(Collection var0) {
		return collection.retainAll(var0);
	}

	public int size() {
		return collection.size();
	}

	public Object[] toArray() {
		return collection.toArray();
	}

	public Object[] toArray(Object[] var0) {
		return collection.toArray(var0);
	}

}
