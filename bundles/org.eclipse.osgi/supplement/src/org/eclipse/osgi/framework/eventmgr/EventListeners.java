/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.eventmgr;

import java.util.*;

/**
 * This class manages a list of listeners. The class implements the Map interface mapping
 * listener objects onto their companion objects.
 * 
 * The object is copy-on-write to allow for safe, unsynchronized traversal.
 * 
 * Listeners may be added or removed as necessary.
 * 
 * This class uses identity for comparison, not equals.
 * 
 * @since 3.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class EventListeners implements Map {
	/**
	 * The empty array singleton instance.
	 */
	private static final ListElement[] emptyArray = new ListElement[0];

	/**
	 * The list of elements.
	 */
	private volatile ListElement[] list;

	/**
	 * Creates an empty listener list.
	 *
	 */
	public EventListeners() {
		list = emptyArray;
	}

	/**
	 * Creates an empty listener list.
	 *
	 * @param capacity This argument is ignored.
	 * @deprecated As of 3.5
	 */
	public EventListeners(int capacity) {
		this();
	}

	/**
	 * Copy constructor.
	 *
	 * @param source The list to copy.
	 * @since 3.5
	 */
	public EventListeners(EventListeners source) {
		this.list = source.list();
	}

	private ListElement[] list() {
		return list;
	}

	/**
	 * Add a listener and its companion object to the list.
	 * If a listener object is already in the list, then it is replaced.
	 * Listeners are compared using identity.
	 *
	 * @param listener This is the listener object to be added to the list.
	 * @param companion This is an optional listener companion object.
	 * This object will be passed to the EventDispatcher along with the listener
	 * when the listener is to be called. This may be null.
	 * @return <code>null</code> if this listener was newly added to the list.
	 * Otherwise the previous value of the companion object.
	 * @throws IllegalArgumentException If listener is null.
	 * @since 3.5
	 */
	public synchronized Object put(Object listener, Object companion) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}

		int size = list.length;
		for (int i = 0; i < size; i++) {
			if (list[i].primary == listener) {
				Object c = list[i].companion;
				if (c == companion) {
					return c;
				}
				ListElement[] newList = new ListElement[size];
				System.arraycopy(list, 0, newList, 0, size);
				newList[i] = new ListElement(listener, companion);
				list = newList;
				return c;
			}
		}

		ListElement[] newList = new ListElement[size + 1];
		if (size > 0) {
			System.arraycopy(list, 0, newList, 0, size);
		}
		newList[size] = new ListElement(listener, companion);
		list = newList;
		return null;
	}

	/**
	 * Remove a listener from the list and indicate if the listener was removed.
	 * Listeners are compared using identity.
	 *
	 * @param listener This is the listener object to be removed from the list.
	 * @return <code>null</code> if the listener was not in the list. 
	 * Otherwise, the companion with which the listener was registered.
	 * @throws IllegalArgumentException If listener is null.
	 * @since 3.5
	 */
	public synchronized Object remove(Object listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}

		int size = list.length;
		for (int i = 0; i < size; i++) {
			if (list[i].primary == listener) {
				Object c = list[i].companion;
				if (size == 1) {
					list = emptyArray;
					return c;
				}
				ListElement[] newList = new ListElement[size - 1];
				if (i > 0) {
					System.arraycopy(list, 0, newList, 0, i);
				}
				int next = size - 1 - i;
				if (next > 0) {
					System.arraycopy(list, i + 1, newList, i, next);
				}
				list = newList;
				return c;
			}
		}
		return null;
	}

	/**
	 * Remove all listeners from the list.
	 * 
	 * @since 3.5
	 */
	public synchronized void clear() {
		list = emptyArray;
	}

	/**
	 * Is the list empty?
	 * 
	 * @return <code>true</code> if the list is empty.
	 * @since 3.5
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Return the size of the list.
	 * 
	 * @return The size of the list.
	 * @since 3.5
	 */
	public int size() {
		return list().length;
	}

	/**
	 * Return the companion object for the listener.
	 * Listeners are compared using identity.
	 * 
	 * @param listener The listener object.
	 * @return The companion object for the listener.
	 * @since 3.5
	 */
	public Object get(Object listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}

		ListElement[] l = list();
		for (int i = 0; i < l.length; i++) {
			if (l[i].primary == listener) {
				return l[i].companion;
			}
		}
		return null;
	}

	/**
	 * Check if the list contains the listener.
	 * Listeners are compared using identity.
	 * 
	 * @param listener The listener object.
	 * @return <code>true</code> if the listener is in the list.
	 * @since 3.5
	 */
	public boolean containsKey(Object listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}

		ListElement[] l = list();
		for (int i = 0; i < l.length; i++) {
			if (l[i].primary == listener) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the list contains the companion.
	 * Companions are compared using identity.
	 * 
	 * @param companion The companion object for the listener.
	 * @return <code>true</code> if the companion is in the list.
	 * @since 3.5
	 */
	public boolean containsValue(Object companion) {
		ListElement[] l = list();
		for (int i = 0; i < l.length; i++) {
			if (l[i].companion == companion) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * 
	 * @param var0
	 * @since 3.5
	 */
	public synchronized void putAll(Map source) {
		int sourceSize = source.size();
		if (sourceSize == 0) {
			return;
		}
		ListElement[] toCopy;
		if (source instanceof EventListeners) {
			toCopy = ((EventListeners) source).list();
		} else {
			toCopy = new ListElement[sourceSize];
			Iterator iter = source.entrySet().iterator();
			for (int i = 0; i < sourceSize; i++) {
				Map.Entry entry = (Map.Entry) iter.next();
				toCopy[i] = new ListElement(entry.getKey(), entry.getValue());
			}
		}

		int size = list.length;
		ListElement[] newList = new ListElement[size + sourceSize];
		System.arraycopy(list, 0, newList, 0, size);
		copy: for (int n = 0; n < sourceSize; n++) {
			ListElement copy = toCopy[n];
			for (int i = 0; i < size; i++) {
				if (newList[i].primary == copy.primary) {
					newList[i] = copy;
					continue copy;
				}
			}
			newList[size] = copy;
			size++;
		}
		if (size == newList.length) {
			list = newList;
			return;
		}
		ListElement[] l = new ListElement[size];
		System.arraycopy(newList, 0, l, 0, size);
		list = l;
	}

	/**
	 * Returns a set containing a Map.Entry for each listener in this list.
	 * 
	 * @return A Set of Map.Entry for each listener in this list
	 * @since 3.5
	 */
	public Set entrySet() {
		return new InnerSet(list(), InnerSet.ENTRY);
	}

	/**
	 * Returns a set containing the listeners in this list.
	 * 
	 * @return A Set of the listeners in this list
	 * @since 3.5
	 */
	public Set keySet() {
		return new InnerSet(list(), InnerSet.KEY);
	}

	/**
	 * Returns a collection containing the companions in this list.
	 * 
	 * @return A Collection of the companions in this list
	 * @since 3.5
	 */
	public Collection values() {
		return new InnerSet(list(), InnerSet.VALUE);
	}

	/**
	 * Add a listener to the list.
	 * If a listener object is already in the list, then it is replaced.
	 * This method calls the put method.
	 *
	 * @param listener This is the listener object to be added to the list.
	 * @param listenerObject This is an optional listener-specific object.
	 * This object will be passed to the EventDispatcher along with the listener
	 * when the listener is to be called. This may be null
	 * @throws IllegalArgumentException If listener is null.
	 */
	public void addListener(Object listener, Object listenerObject) {
		put(listener, listenerObject);
	}

	/**
	 * Remove a listener from the list.
	 * This method calls the remove method.
	 *
	 * @param listener This is the listener object to be removed from the list.
	 * @throws IllegalArgumentException If listener is null.
	 */
	public void removeListener(Object listener) {
		remove(listener);
	}

	/**
	 * Remove all listeners from the list.
	 * 
	 * This method calls the clear method.
	 */
	public void removeAllListeners() {
		clear();
	}

	/**
	 * Return the list of (listener, listenerObject) pairs.
	 * Package private method.
	 * The array may be longer than the number of pairs in the array.
	 * The end of the pairs is signalled by a null element or
	 * end of array. 
	 * This array must not be modified by anyone and should not be 
	 * exposed outside of this package.
	 * To reduce memory allocations, the internal array is shared
	 * with the rest of this package. However an array returned by this method
	 * must not be modified in anyway.
	 * 
	 * @return A shared array that must not be modified by anyone. 
	 */
	ListElement[] getListeners() {
		return list();
	}

	/**
	 * ListElement is a package private class. This class
	 * represents a primary object (e.g. listener) and its companion object.
	 * ListElements are stored in EventListeners.
	 * ListElements are immutable.
	 */
	static class ListElement implements Map.Entry {
		/**
		 * Primary object.
		 */
		final Object primary;

		/**
		 * Companion object.
		 */
		final Object companion;

		/**
		 * Constructor for ElementList element
		 * @param primary Primary object in element. Used for uniqueness.
		 * @param companion Companion object stored with primary object.
		 */
		ListElement(final Object primary, final Object companion) {
			this.primary = primary;
			this.companion = companion;
		}

		public Object getKey() {
			return primary;
		}

		public Object getValue() {
			return companion;
		}

		public Object setValue(Object o) {
			throw new UnsupportedOperationException();
		}
	}

	private static class InnerSet extends AbstractSet {
		final ListElement[] list;
		final int returnType;
		final static int ENTRY = 1;
		final static int KEY = 2;
		final static int VALUE = 3;

		InnerSet(ListElement[] list, int returnType) {
			this.list = list;
			this.returnType = returnType;
		}

		public Iterator iterator() {
			return new Iterator() {
				private int cursor = 0;

				public boolean hasNext() {
					return cursor < list.length;
				}

				public Object next() {
					if (cursor == list.length) {
						throw new NoSuchElementException();
					}
					switch (returnType) {
						case ENTRY :
							return list[cursor++];
						case KEY :
							return list[cursor++].primary;
						case VALUE :
							return list[cursor++].companion;
					}
					throw new InternalError();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		public int size() {
			return list.length;
		}
	}
}
