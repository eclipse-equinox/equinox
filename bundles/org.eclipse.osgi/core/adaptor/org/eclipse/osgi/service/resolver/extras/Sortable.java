/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver.extras;

import java.util.Comparator;

/**
 * Represents a collection of elements which can be sorted.
 * @since 3.8
 */
public interface Sortable<E> {
	/**
	 * Sorts collection of elements represented by this sortable according
	 * to the specified comparator.
	 * @param comparator the comparator used to sort the collection
	 * of elements represented by this sortable.
	 */
	void sort(Comparator<E> comparator);
}