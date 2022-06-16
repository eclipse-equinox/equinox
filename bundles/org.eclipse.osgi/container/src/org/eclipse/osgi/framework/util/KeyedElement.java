/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

/**
 * NOTE: This interface defines an element that could be inserted into an internal class called
 * <code>KeyedHashSet</code>.  This internal class <code>KeyedHashSet</code> has been deleted.
 * The KeyedElement interface has remained because of the use of it in <code>ClasspathEntry</code>.
 * A keyed element can easily be put into a standard Map implementation by using the keyed element
 * key for the mapping.
 * <p>
 * An element of an <code>KeyedHashSet</code>.  A KeyedElement privides the key which is used to hash
 * the elements in an <code>KeyedHashSet</code>.
 * @since 3.2
 */
// This class was moved from  /org.eclipse.osgi/core/framework/org/eclipse/osgi/framework/internal/core/KeyedElement.java
public interface KeyedElement {
	/**
	 * Returns the hash code of the key
	 * @return the hash code of the key
	 */
	public int getKeyHashCode();

	/**
	 * Compares this element with a specified element
	 * @param other the element to compare with
	 * @return returns true if the specified element equals this element
	 */
	public boolean compare(KeyedElement other);

	/**
	 * Returns the key for this element
	 * @return the key for this element
	 */
	public Object getKey();
}
