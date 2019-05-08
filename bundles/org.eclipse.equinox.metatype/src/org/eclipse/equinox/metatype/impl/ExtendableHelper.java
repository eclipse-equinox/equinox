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

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.metatype.Extendable;
import org.eclipse.equinox.metatype.impl.Persistence.Reader;
import org.eclipse.equinox.metatype.impl.Persistence.Writer;

public class ExtendableHelper implements Extendable {
	private final Map<String, Map<String, String>> extensions;

	public ExtendableHelper() {
		this(Collections.<String, Map<String, String>> emptyMap());
	}

	public ExtendableHelper(Map<String, Map<String, String>> extensions) {
		if (extensions == null)
			throw new NullPointerException();
		this.extensions = extensions;
	}

	@Override
	public Map<String, String> getExtensionAttributes(String schema) {
		return Collections.unmodifiableMap(extensions.get(schema));
	}

	@Override
	public Set<String> getExtensionUris() {
		return Collections.unmodifiableSet(extensions.keySet());
	}

	void getStrings(Set<String> strings) {
		for (Entry<String, Map<String, String>> e1 : extensions.entrySet()) {
			strings.add(e1.getKey());
			for (Entry<String, String> e2 : e1.getValue().entrySet()) {
				strings.add(e2.getKey());
				strings.add(e2.getValue());
			}
		}
	}

	public static ExtendableHelper load(Reader reader) throws IOException {
		Map<String, Map<String, String>> extensions = new HashMap<>();
		int numExtensions = reader.readInt();
		for (int i = 0; i < numExtensions; i++) {
			String extKey = reader.readString();
			int numAttrs = reader.readInt();
			Map<String, String> extensionAttrs = new HashMap<>();
			for (int j = 0; j < numAttrs; j++) {
				String attrKey = reader.readString();
				String attrValue = reader.readString();
				extensionAttrs.put(attrKey, attrValue);
			}
			extensions.put(extKey, extensionAttrs);
		}
		return new ExtendableHelper(extensions);
	}

	public void write(Writer writer) throws IOException {
		writer.writeInt(extensions.size());
		for (Entry<String, Map<String, String>> extensionEntry : extensions.entrySet()) {
			writer.writeString(extensionEntry.getKey());
			Map<String, String> extensionAttrs = extensionEntry.getValue();
			writer.writeInt(extensionAttrs.size());
			for (Entry<String, String> attrs : extensionAttrs.entrySet()) {
				writer.writeString(attrs.getKey());
				writer.writeString(attrs.getValue());
			}
		}
	}

}
