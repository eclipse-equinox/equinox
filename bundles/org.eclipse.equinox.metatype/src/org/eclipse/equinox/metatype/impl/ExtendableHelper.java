/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others
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
package org.eclipse.equinox.metatype.impl;

import java.util.*;
import org.eclipse.equinox.metatype.Extendable;

public class ExtendableHelper implements Extendable {
	private final Map<String, Map<String, String>> attributes;

	public ExtendableHelper() {
		this(Collections.<String, Map<String, String>> emptyMap());
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
