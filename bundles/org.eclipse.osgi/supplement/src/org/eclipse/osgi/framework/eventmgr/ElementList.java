/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.eventmgr;

/**
 * A list of elements.
 * This package private class manages the list of elements. 
 * This class is not synchronized and relies on its callers to perform
 * any necessary synchronization.
 * 
 * The element list implementation must have the following characteristics:
 * <ul>
 * <li>Each element in the list must contain 2 objects. One is the primary object
 * and the other is a companion object than may be used by the EventDispatcher.</li>
 * <li>The returned list MUST be a copy of the list maintained by this object.</li>
 * <li>Processing on the list should not be recursive to avoid stack overflows
 * when managing large lists.</li>
 * <li>The list should prevent duplicate insertions using the identity
 * of the element's primary object. Adding an element with the same primary object
 * as an already added element MUST replace the prior element.</li>
 * </ul>
 * 
 */

class ElementList {
	/**
	 * The empty array singleton instance, returned by getListeners()
	 * when size == 0.
	 */
	private static final ListElement[] EmptyArray = new ListElement[0];

	/**
	 * The initial capacity of the list. Always >= 1.
	 */
	private int capacity;

	/**
	 * The list of elements.  Initially <code>null</code> but initialized
	 * to an array of size capacity the first time an element is added.
	 * Maintains invariant: elements != null IFF size != 0
	 */
	private ListElement[] elements = null;

	/**
	 * The current number of elements.
	 * Maintains invariant: 0 <= size <= elements.length.
	 */
	private int size;

	/**
	 * Creates a element list with an initial capacity of 10.
	 */
	ElementList() {
		this(10);
	}

	/**
	 * Creates a element list with the given initial capacity.
	 *
	 * @param capacity the number of elements which this list can initially 
	 *    accept without growing its internal representation; must be at
	 *    least 1
	 */
	ElementList(int capacity) {
		if (capacity < 1)
			throw new IllegalArgumentException();
		this.capacity = capacity;
	}

	/**
	 * Adds the given element to this list. If an element with an identical 
	 * primary object is already registered, then it is replaced.
	 * (This is necessary to support a feature of the OSGi spec 
	 * where adding the same ServiceListener will result in it using 
	 * the new filter string.)
	 * 
	 * @param element element to add to list
	 */
	void addElement(ListElement element) {
		if (element.primary == null) {
			throw new IllegalArgumentException();
		}

		if (size == 0) {
			elements = new ListElement[capacity];
		} else {
			// check for duplicates using identity
			Object primary = element.primary;
			for (int i = 0; i < size; i++) {
				if (elements[i].primary == primary) {
					elements[i] = element; /* use the most recent companion */
					return;
				}
			}
			// grow array if necessary
			if (size == elements.length) {
				ListElement[] newListeners = new ListElement[size * 2 + 1];
				System.arraycopy(elements, 0, newListeners, 0, size);
				elements = newListeners;
			}
		}

		elements[size] = element;
		size++;
	}

	/**
	 * Removes the element with the specified primary object from this list. 
	 * Has no effect if there is no element in the list with the specified 
	 * primary object.  
	 * 
	 * @param primary Primary object of the element to remove
	 */
	void removeElement(Object primary) {
		if (primary == null) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < size; i++) {
			if (elements[i].primary == primary) {
				if (size == 1) {
					elements = null; /* discard array */
					size = 0;
				} else {
					//TODO need to consider shrinking array to reclaim memory instead of always keeping the largest size
					size--;
					System.arraycopy(elements, i + 1, elements, i, size - i);
					elements[size] = null; /* clean stray reference so it may be GC'd */
				}
				return;
			}
		}
	}

	/**
	 * Returns an array containing all the registered elements.
	 * The resulting array is unaffected by subsequent adds or removes
	 * on this list.
	 * If there are no elements registered, the result is an empty array.
	 * Use this method when processing elements, so that any modifications
	 * to the element list during the processing will have no effect on 
	 * the traversal of the element list.
	 *
	 * @return the list of elements
	 */
	public ListElement[] getElements() {
		if (size == 0) {
			return EmptyArray;
		}
		ListElement[] result = new ListElement[size];
		System.arraycopy(elements, 0, result, 0, size);
		return result;
	}
}