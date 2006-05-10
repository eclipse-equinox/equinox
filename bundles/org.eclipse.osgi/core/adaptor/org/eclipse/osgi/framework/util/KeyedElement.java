/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.util;

/**
 * An element of an <code>KeyedHashSet</code>.  A KeyedElement privides the key which is used to hash 
 * the elements in an <code>KeyedHashSet</code>.
 * @see KeyedHashSet
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
