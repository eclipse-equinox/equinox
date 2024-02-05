/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *     Jan-Ove Weichel (janove.weichel@vogella.com) - bug 474359
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.util.*;

/**
 * A {@link Properties} class whose entries are sorted by key; can
 * <strong>only</strong> be used in limited scenarios like storing properties to
 * a file.
 * <p>
 * <b>Implementation note</b>: The implementations of the {@link #keys()} and
 * {@link #entrySet()} methods violate the contracts of
 * {@link Properties#keySet()} and {@link Properties#entrySet()}, because the
 * returned sets are <em>not</em> backed by this map. Overriding both methods is
 * necessary to support Oracle and IBM VMS, see
 * <a href="https://bugs.eclipse.org/325000">bug 325000</a>.
 * </p>
 */
public class SortedProperties extends Properties {
	// Warning: This class is referenced by our friend
	// org.eclipse.core.internal.resources.ProjectPreferences

	private static final long serialVersionUID = 1L;

	@Override
	public synchronized Enumeration<Object> keys() {
		return Collections.enumeration(new TreeSet<>(keySet()));
	}

	private static final Comparator<Map.Entry<String, String>> BY_KEY = Comparator.comparing(Map.Entry::getKey);

	@Override
	public Set<Map.Entry<Object, Object>> entrySet() {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Set<Map.Entry<Object, Object>> set = new TreeSet<>((Comparator) BY_KEY);
		set.addAll(super.entrySet());
		return set;
	}
}
