/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A set data structure which only accepts {@link KeyedElement} objects as elements of the set. 
 * Unlike typical set implementations this set requires each element to provide its own key.  This helps
 * reduce the overhead of storing the keys of each individual element<p>
 * This class in not thread safe, clients must ensure synchronization when modifying an object of this type.
 * @since 3.2 
 */
// This class was moved from  /org.eclipse.osgi/core/framework/org/eclipse/osgi/framework/internal/core/KeyedHashSet.java
public class KeyedHashSet {
	public static final int MINIMUM_SIZE = 7;
	int elementCount = 0;
	KeyedElement[] elements;
	private boolean replace;
	private int capacity;

	/**
	 * Constructs an KeyedHashSet which allows elements to be replaced and with the minimum initial capacity.
	 */
	public KeyedHashSet() {
		this(MINIMUM_SIZE, true);
	}

	/**
	 * Constructs an KeyedHashSet with the minimum initial capacity.
	 * @param replace true if this set allows elements to be replaced
	 */
	public KeyedHashSet(boolean replace) {
		this(MINIMUM_SIZE, replace);
	}

	/**
	 * Constructs an KeyedHashSet which allows elements to be replaced.
	 * @param capacity the initial capacity of this set
	 */
	public KeyedHashSet(int capacity) {
		this(capacity, true);
	}

	/**
	 * Constructs an KeyedHashSet
	 * @param capacity the initial capacity of this set
	 * @param replace true if this set allows elements to be replaced
	 */
	public KeyedHashSet(int capacity, boolean replace) {
		elements = new KeyedElement[Math.max(MINIMUM_SIZE, capacity * 2)];
		this.replace = replace;
		this.capacity = capacity;
	}

	/**
	 * Constructs a new KeyedHashSet and copies to specified KeyedHashSet's contents to the new KeyedHashSet.
	 * @param original the KeyedHashSet to copy
	 */
	public KeyedHashSet(KeyedHashSet original) {
		elements = new KeyedElement[original.elements.length];
		System.arraycopy(original.elements, 0, elements, 0, original.elements.length);
		elementCount = original.elementCount;
		replace = original.replace;
		capacity = original.capacity;
	}

	/**
	 * Adds an element to this set. If an element with the same key already exists,
	 * replaces it depending on the replace flag.
	 * @return true if the element was added/stored, false otherwise
	 */
	public boolean add(KeyedElement element) {
		int hash = hash(element);

		// search for an empty slot at the end of the array
		for (int i = hash; i < elements.length; i++) {
			if (elements[i] == null) {
				elements[i] = element;
				elementCount++;
				// grow if necessary
				if (shouldGrow())
					expand();
				return true;
			}
			if (elements[i].compare(element)) {
				if (replace)
					elements[i] = element;
				return replace;
			}
		}

		// search for an empty slot at the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			if (elements[i] == null) {
				elements[i] = element;
				elementCount++;
				// grow if necessary
				if (shouldGrow())
					expand();
				return true;
			}
			if (elements[i].compare(element)) {
				if (replace)
					elements[i] = element;
				return replace;
			}
		}

		// if we didn't find a free slot, then try again with the expanded set
		expand();
		return add(element);
	}

	/**
	 * Adds the specified list of elements to this set.  Some elements may not
	 * get added if the replace flag is set.
	 * @param toAdd the list of elements to add to this set.
	 */
	public void addAll(KeyedElement[] toAdd) {
		for (int i = 0; i < toAdd.length; i++)
			add(toAdd[i]);
	}

	/**
	 * Returns true if the specified element exists in this set.
	 * @param element the requested element
	 * @return true if the specified element exists in this set; false otherwise.
	 */
	public boolean contains(KeyedElement element) {
		return get(element) != null;
	}

	/**
	 * Returns true if an element with the specified key exists in this set.
	 * @param key the key of the requested element
	 * @return true if an element with the specified key exists in this set; false otherwise
	 */
	public boolean containsKey(Object key) {
		return getByKey(key) != null;
	}

	/**
	 * Returns all elements that exist in this set
	 * @return all elements that exist in this set
	 */
	public KeyedElement[] elements() {
		return (KeyedElement[]) elements(new KeyedElement[elementCount]);
	}

	/**
	 * Copies all elements that exist in this set into the specified array.  No size 
	 * checking is done.  If the specified array is to small an ArrayIndexOutOfBoundsException
	 * will be thrown.
	 * @param result the array to copy the existing elements into.
	 * @return the specified array.
	 * @throws ArrayIndexOutOfBoundsException if the specified array is to small.
	 */
	public Object[] elements(Object[] result) {
		int j = 0;
		for (int i = 0; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element != null)
				result[j++] = element;
		}
		return result;
	}

	/**
	 * The array isn't large enough so double its size and rehash
	 * all its current values.
	 */
	protected void expand() {
		KeyedElement[] oldElements = elements;
		elements = new KeyedElement[elements.length * 2];

		int maxArrayIndex = elements.length - 1;
		for (int i = 0; i < oldElements.length; i++) {
			KeyedElement element = oldElements[i];
			if (element != null) {
				int hash = hash(element);
				while (elements[hash] != null) {
					hash++;
					if (hash > maxArrayIndex)
						hash = 0;
				}
				elements[hash] = element;
			}
		}
	}

	/**
	 * Returns the element with the specified key, or null if not found.
	 * @param key the requested element's key
	 * @return the element with the specified key, or null if not found.
	 */
	public KeyedElement getByKey(Object key) {
		if (elementCount == 0)
			return null;
		int hash = keyHash(key);

		// search the last half of the array
		for (int i = hash; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return null;
			if (element.getKey().equals(key))
				return element;
		}

		// search the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return null;
			if (element.getKey().equals(key))
				return element;
		}

		// nothing found so return null
		return null;
	}

	/**
	 * Returns the element which compares to the specified element, or null if not found.
	 * @see KeyedElement#compare(KeyedElement)
	 * @param otherElement the requested element 
	 * @return the element which compares to the specified element, or null if not found.
	 */
	public KeyedElement get(KeyedElement otherElement) {
		if (elementCount == 0)
			return null;
		int hash = hash(otherElement);

		// search the last half of the array
		for (int i = hash; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return null;
			if (element.compare(otherElement))
				return element;
		}

		// search the beginning of the array
		for (int i = 0; i < hash - 1; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return null;
			if (element.compare(otherElement))
				return element;
		}

		// nothing found so return null
		return null;
	}

	/**
	 * Returns true if this set is empty
	 * @return true if this set is empty
	 */
	public boolean isEmpty() {
		return elementCount == 0;
	}

	/**
	 * The element at the given index has been removed so move
	 * elements to keep the set properly hashed.
	 * @param anIndex the index that has been removed
	 */
	protected void rehashTo(int anIndex) {

		int target = anIndex;
		int index = anIndex + 1;
		if (index >= elements.length)
			index = 0;
		KeyedElement element = elements[index];
		while (element != null) {
			int hashIndex = hash(element);
			boolean match;
			if (index < target)
				match = !(hashIndex > target || hashIndex <= index);
			else
				match = !(hashIndex > target && hashIndex <= index);
			if (match) {
				elements[target] = element;
				target = index;
			}
			index++;
			if (index >= elements.length)
				index = 0;
			element = elements[index];
		}
		elements[target] = null;
	}

	/**
	 * Removes the element with the specified key
	 * @param key the requested element's key
	 * @return true if an element was removed
	 */
	public boolean removeByKey(Object key) {
		if (elementCount == 0)
			return false;
		int hash = keyHash(key);

		for (int i = hash; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return false;
			if (element.getKey().equals(key)) {
				rehashTo(i);
				elementCount--;
				return true;
			}
		}

		for (int i = 0; i < hash - 1; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return false;
			if (element.getKey().equals(key)) {
				rehashTo(i);
				elementCount--;
				return true;
			}
		}

		return true;
	}

	/**
	 * Removes the element which compares to the specified element
	 * @param toRemove the requested element to remove
	 * @return true if an element was removed
	 */
	public boolean remove(KeyedElement toRemove) {
		if (elementCount == 0)
			return false;

		int hash = hash(toRemove);

		for (int i = hash; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return false;
			if (element.compare(toRemove)) {
				rehashTo(i);
				elementCount--;
				return true;
			}
		}

		for (int i = 0; i < hash - 1; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				return false;
			if (element.compare(toRemove)) {
				rehashTo(i);
				elementCount--;
				return true;
			}
		}
		return false;
	}

	private int hash(KeyedElement element) {
		return Math.abs(element.getKeyHashCode()) % elements.length;
	}

	private int keyHash(Object key) {
		return Math.abs(key.hashCode()) % elements.length;
	}

	/**
	 * Removes all of the specified elements from this set
	 * @param toRemove the requested elements to remove
	 */
	public void removeAll(KeyedElement[] toRemove) {
		for (int i = 0; i < toRemove.length; i++)
			remove(toRemove[i]);
	}

	private boolean shouldGrow() {
		return elementCount > elements.length * 0.75;
	}

	/**
	 * Returns the number of elements in this set
	 * @return the number of elements in this set
	 */
	public int size() {
		return elementCount;
	}

	public String toString() {
		StringBuffer result = new StringBuffer(100);
		result.append("{"); //$NON-NLS-1$
		boolean first = true;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null) {
				if (first)
					first = false;
				else
					result.append(", "); //$NON-NLS-1$
				result.append(elements[i]);
			}
		}
		result.append("}"); //$NON-NLS-1$
		return result.toString();
	}

	/**
	 * Returns the number of collisions this set currently has
	 * @return the number of collisions this set currently has
	 */
	public int countCollisions() {
		int result = 0;
		int lastHash = 0;
		boolean found = false;
		for (int i = 0; i < elements.length; i++) {
			KeyedElement element = elements[i];
			if (element == null)
				found = false;
			else {
				int hash = hash(element);
				if (found)
					if (lastHash == hash)
						result++;
					else
						found = false;
				else {
					lastHash = hash;
					found = true;
				}
			}
		}
		return result;
	}

	/**
	 * Returns an iterator of elements in this set
	 * @return an iterator of elements in this set
	 */
	public Iterator<KeyedElement> iterator() {
		return new EquinoxSetIterator();
	}

	class EquinoxSetIterator implements Iterator<KeyedElement> {
		private int currentIndex = -1;
		private int found;

		public boolean hasNext() {
			return found < elementCount;
		}

		public KeyedElement next() {
			if (!hasNext())
				throw new NoSuchElementException();
			while (++currentIndex < elements.length)
				if (elements[currentIndex] != null) {
					found++;
					return elements[currentIndex];
				}
			// this would mean we have less elements than we thought
			throw new NoSuchElementException();
		}

		public void remove() {
			// as allowed by the API
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Clears all elements from this set
	 */
	public void clear() {
		elements = new KeyedElement[Math.max(MINIMUM_SIZE, capacity * 2)];
		elementCount = 0;
	}
}
