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

import java.util.*;

/**
 * A Shrinkable Collection. This class provides a wrapper for a list of collections
 * that allows items to be removed from the wrapped collections (shrinking) but
 * does not allow items to be added to the wrapped collections. 
 * 
 * <p>
 * The collections must act as sets in that each collection in the list
 * must not have two entries which are equal.
 * 
 * <p>
 * All the optional <code>Collection</code> operations except
 * <code>add</code> and <code>addAll</code> are supported. Attempting to add to the
 * collection will result in an <code>UnsupportedOperationException</code>.
 *
 */

public class ShrinkableCollection<E> implements Collection<E> {
	private final Collection<? extends E> collection;
	private final List<Collection<? extends E>> list;

	public ShrinkableCollection(Collection<? extends E> c) {
		if (c == null) {
			throw new NullPointerException();
		}
		@SuppressWarnings("unchecked")
		List<Collection<? extends E>> empty = Collections.EMPTY_LIST;
		list = empty;
		collection = c;
	}

	public ShrinkableCollection(Collection<? extends E> c1, Collection<? extends E> c2) {
		list = new ArrayList<Collection<? extends E>>(2);
		list.add(c1);
		list.add(c2);
		collection = initComposite(list);
	}

	public ShrinkableCollection(List<Collection<? extends E>> l) {
		list = new ArrayList<Collection<? extends E>>(l);
		collection = initComposite(list);
	}

	private static <E> Collection<? extends E> initComposite(List<Collection<? extends E>> collections) {
		int size = 0;
		for (Collection<? extends E> c : collections) {
			assert verifyNoDuplicates(c);
			size += c.size();
		}
		Collection<E> result = new ArrayList<E>(size);
		for (Collection<? extends E> c : collections) {
			for (E e : c) {
				if (!result.contains(e)) {
					result.add(e);
				}
			}
		}
		return result;
	}

	private static <E> boolean verifyNoDuplicates(Collection<? extends E> c) {
		for (E e : c) {
			int count = 0;
			for (E f : c) {
				if (e == null) {
					if (f == null) {
						count++;
					}
				} else {
					if (e.equals(f)) {
						count++;
					}
				}
			}
			if (count != 1) {
				return false;
			}
		}
		return true;
	}

	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		collection.clear();
		for (Collection<? extends E> c : list) {
			c.clear();
		}
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

	public Iterator<E> iterator() {
		@SuppressWarnings("unchecked")
		final Iterator<E> iter = (Iterator<E>) collection.iterator();
		final List<Collection<? extends E>> collections = list;
		if (collections.isEmpty()) {
			return iter;
		}
		return new Iterator<E>() {
			private E last;

			public boolean hasNext() {
				return iter.hasNext();
			}

			public E next() {
				last = iter.next();
				return last;
			}

			public void remove() {
				iter.remove();
				for (Collection<? extends E> c : collections) {
					c.remove(last);
				}
			}
		};
	}

	public boolean remove(Object o) {
		final boolean result = collection.remove(o);
		if (result) {
			for (Collection<? extends E> c : list) {
				c.remove(o);
			}
		}
		return result;
	}

	public boolean removeAll(Collection<?> c) {
		final boolean result = collection.removeAll(c);
		if (result) {
			for (Collection<? extends E> cc : list) {
				cc.removeAll(c);
			}
		}
		return result;
	}

	public boolean retainAll(Collection<?> c) {
		final boolean result = collection.retainAll(c);
		if (result) {
			for (Collection<? extends E> cc : list) {
				cc.retainAll(c);
			}
		}
		return result;
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

	public String toString() {
		return collection.toString();
	}
}
