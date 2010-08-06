/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

public class ShrinkableCollection<E> implements Collection<E> {
	private final Collection<? extends E> collection;

	ShrinkableCollection(Collection<? extends E> c) {
		if (c == null) {
			throw new NullPointerException();
		}
		collection = c;
	}

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		collection.clear();
	}

	public boolean contains(Object o) {
		return collection.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return collection.containsAll(c);
	}

	public boolean isEmpty() {
		return collection.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public Iterator<E> iterator() {
		return (Iterator<E>) collection.iterator();
	}

	public boolean remove(Object o) {
		return collection.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return collection.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return collection.retainAll(c);
	}

	public int size() {
		return collection.size();
	}

	public Object[] toArray() {
		return collection.toArray();
	}

	public <T> T[] toArray(T[] var0) {
		return collection.toArray(var0);
	}
}
