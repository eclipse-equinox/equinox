/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import org.eclipse.equinox.metatype.Extendable;

import java.util.*;

public class ExtendableHelper implements Extendable {
	private final Map<String, Map<String, String>> attributes;

	@SuppressWarnings("unchecked")
	public ExtendableHelper() {
		this(Collections.EMPTY_MAP);
	}

	public ExtendableHelper(Map<String, Map<String, String>> attributes) {
		if (attributes == null)
			throw new NullPointerException();
		this.attributes = attributes;
	}

	public Map<String, String> getExtensionAttributes(String schema) {
		return Collections.unmodifiableMap(attributes.get(schema));
	}

	public Set<String> getExtensionUris() {
		return Collections.unmodifiableSet(attributes.keySet());
	}

}
