/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jan-Ove Weichel (janove.weichel@vogella.com) - bug 474359
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.util.*;
import java.util.Map.Entry;

public class SortedProperties extends Properties {

	private static final long serialVersionUID = 1L;

	public SortedProperties() {
		super();
	}


	@Override
	public synchronized Enumeration<Object> keys() {
		TreeSet<Object> set = new TreeSet<>();
		for (Enumeration<?> e = super.keys(); e.hasMoreElements();)
			set.add(e.nextElement());
		return Collections.enumeration(set);
	}


	@Override
	public Set<Entry<Object, Object>> entrySet() {
		TreeSet<Entry<Object, Object>> set = new TreeSet<>(new Comparator<Entry<Object, Object>>() {
			@Override
			public int compare(Entry<Object, Object> e1, Entry<Object, Object> e2) {
				String s1 = (String) e1.getKey();
				String s2 = (String) e2.getKey();
				return s1.compareTo(s2);
			}
		});
		for (Iterator<Entry<Object, Object>> i = super.entrySet().iterator(); i.hasNext();)
			set.add(i.next());
		return set;
	}
}