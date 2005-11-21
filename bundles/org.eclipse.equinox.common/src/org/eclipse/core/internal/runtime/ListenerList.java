/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

/**
 * This class is going to be removed soon.
 * Please use org.eclipse.core.runtime.ListenerList
 * 
 * @deprecated
 */
// TODO remove this class
public class ListenerList {

	/**
	 * The empty array singleton instance.
	 */
	private static final Object[] EmptyArray = new Object[0];

	/**
	 * Mode constant (value 0) indicating that listeners should be considered
	 * the <a href="#same">same</a> if they are equal.
	 */
	public static final int EQUALITY = 0;

	/**
	 * Mode constant (value 1) indicating that listeners should be considered
	 * the <a href="#same">same</a> if they are identical.
	 */
	public static final int IDENTITY = 1;

	/**
	 * Indicates the comparison mode used to determine if two
	 * listeners are equivalent
	 */
	private final boolean identity;

	/**
	 * The list of listeners.  Initially empty but initialized
	 * to an array of size capacity the first time a listener is added.
	 * Maintains invariant: listeners != null
	 */
	private volatile Object[] listeners = EmptyArray;

	/**
	 * Creates a listener list in which listeners are compared using equality.
	 */
	public ListenerList() {
		this(EQUALITY);
	}

	/**
	 * Creates a listener list using the provided comparison mode.
	 * 
	 * @param mode The mode used to determine if listeners are the <a href="#same">same</a>.
	 */
	public ListenerList(int mode) {
		if (mode != EQUALITY && mode != IDENTITY)
			throw new IllegalArgumentException();
		this.identity = mode == IDENTITY;
	}

	/**
	 * Adds a listener to this list. This method has no effect if the <a href="#same">same</a>
	 * listener is already registered.
	 * 
	 * @param listener the listener to add
	 */
	public synchronized void add(Object listener) {
		// This method is synchronized to protect against multiple threads adding 
		// or removing listeners concurrently. This does not block concurrent readers.
		if (listener == null)
			throw new IllegalArgumentException();
		// check for duplicates 
		final int oldSize = listeners.length;
		for (int i = 0; i < oldSize; ++i) {
			Object listener2 = listeners[i];
			if (identity ? listener == listener2 : listener.equals(listener2))
				return;
		}
		// Thread safety: create new array to avoid affecting concurrent readers
		Object[] newListeners = new Object[oldSize + 1];
		System.arraycopy(listeners, 0, newListeners, 0, oldSize);
		newListeners[oldSize] = listener;
		//atomic assignment
		this.listeners = newListeners;
	}

	/**
	 * Returns an array containing all the registered listeners.
	 * The resulting array is unaffected by subsequent adds or removes.
	 * If there are no listeners registered, the result is an empty array.
	 * Use this method when notifying listeners, so that any modifications
	 * to the listener list during the notification will have no effect on 
	 * the notification itself.
	 * <p>
	 * Note: Callers of this method <b>must not</b> modify the returned array. 
	 *
	 * @return the list of registered listeners
	 */
	public Object[] getListeners() {
		return listeners;
	}

	/**
	 * Returns whether this listener list is empty.
	 *
	 * @return <code>true</code> if there are no registered listeners, and
	 *   <code>false</code> otherwise
	 */
	public boolean isEmpty() {
		return listeners.length == 0;
	}

	/**
	 * Removes a listener from this list. Has no effect if the <a href="#same">same</a> 
	 * listener was not already registered.
	 *
	 * @param listener the listener to remove
	 */
	public synchronized void remove(Object listener) {
		// This method is synchronized to protect against multiple threads adding 
		// or removing listeners concurrently. This does not block concurrent readers.
		if (listener == null)
			throw new IllegalArgumentException();
		int oldSize = listeners.length;
		for (int i = 0; i < oldSize; ++i) {
			Object listener2 = listeners[i];
			if (identity ? listener == listener2 : listener.equals(listener2)) {
				if (oldSize == 1) {
					listeners = EmptyArray;
				} else {
					// Thread safety: create new array to avoid affecting concurrent readers
					Object[] newListeners = new Object[oldSize - 1];
					System.arraycopy(listeners, 0, newListeners, 0, i);
					System.arraycopy(listeners, i + 1, newListeners, i, oldSize - i - 1);
					//atomic assignment to field
					this.listeners = newListeners;
				}
				return;
			}
		}
	}

	/**
	 * Returns the number of registered listeners.
	 *
	 * @return the number of registered listeners
	 */
	public int size() {
		return listeners.length;
	}
}
