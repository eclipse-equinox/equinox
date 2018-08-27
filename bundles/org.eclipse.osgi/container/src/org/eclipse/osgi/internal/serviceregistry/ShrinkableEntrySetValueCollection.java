/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

package org.eclipse.osgi.internal.serviceregistry;

import java.util.*;

public class ShrinkableEntrySetValueCollection<E> extends AbstractCollection<E> implements Collection<E> {
	private final Set<? extends Map.Entry<?, ? extends E>> entrySet;

	public ShrinkableEntrySetValueCollection(Set<? extends Map.Entry<?, ? extends E>> e) {
		if (e == null) {
			throw new NullPointerException();
		}
		entrySet = e;
	}

	public void clear() {
		entrySet.clear();
	}

	public boolean isEmpty() {
		return entrySet.isEmpty();
	}

	public Iterator<E> iterator() {
		return new ValueIterator<>(entrySet.iterator());
	}

	public int size() {
		return entrySet.size();
	}

	/**
	 * Iterator over the values of the entry set.
	 */
	static private final class ValueIterator<E> implements Iterator<E> {
		private final Iterator<? extends Map.Entry<?, ? extends E>> iter;

		ValueIterator(Iterator<? extends Map.Entry<?, ? extends E>> i) {
			iter = i;
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public E next() {
			final Map.Entry<?, ? extends E> entry = iter.next();
			return entry.getValue();
		}

		public void remove() {
			iter.remove();
		}
	}
}
