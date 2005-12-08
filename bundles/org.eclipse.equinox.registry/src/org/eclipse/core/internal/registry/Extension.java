/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.lang.ref.SoftReference;

/**
 * An object which represents the user-defined extension in a plug-in manifest.  
 */
public class Extension extends RegistryObject {
	public static final Extension[] EMPTY_ARRAY = new Extension[0];

	//Extension simple identifier
	private String simpleId;
	//The namespace for the extension. 
	private String namespace;

	//	Place holder for the label and  the extension point. It contains either a String[] or a SoftReference to a String[].
	//The array layout is [label, extension point name]
	private Object extraInformation;
	private static final byte LABEL = 0; //The human readable name of the extension
	private static final byte XPT_NAME = 1; // The fully qualified name of the extension point to which this extension is attached to
	private static final int EXTRA_SIZE = 2;

	protected Extension(ExtensionRegistry registry, boolean isDynamic) {
		super(registry, isDynamic);
	}

	protected Extension(int self, String simpleId, String namespace, int[] children, int extraData, ExtensionRegistry registry, boolean isDynamic) {
		super(registry, isDynamic);

		setObjectId(self);
		this.simpleId = simpleId;
		setRawChildren(children);
		setExtraDataOffset(extraData);
		this.namespace = namespace;
	}

	protected String getExtensionPointIdentifier() {
		return getExtraData()[XPT_NAME];
	}

	protected String getSimpleIdentifier() {
		return simpleId;
	}

	protected String getUniqueIdentifier() {
		return simpleId == null ? null : this.getNamespace() + '.' + simpleId;
	}

	void setExtensionPointIdentifier(String value) {
		ensureExtraInformationType();
		((String[]) extraInformation)[XPT_NAME] = value;
	}

	void setSimpleIdentifier(String value) {
		simpleId = value;
	}

	private String[] getExtraData() {
		//The extension has been created by parsing, or does not have any extra data 
		if (noExtraData()) {
			if (extraInformation != null)
				return (String[]) extraInformation;
			return null;
		}

		//The extension has been loaded from the cache. 
		String[] result = null;
		if (extraInformation == null || (result = ((extraInformation instanceof SoftReference) ? (String[]) ((SoftReference) extraInformation).get() : (String[]) extraInformation)) == null) {
			result = registry.getCleanTableReader().loadExtensionExtraData(getExtraDataOffset());
			extraInformation = new SoftReference(result);
		}
		return result;
	}

	String getLabel() {
		String s = getExtraData()[LABEL];
		if (s == null)
			return ""; //$NON-NLS-1$
		return s;
	}

	void setLabel(String value) {
		ensureExtraInformationType();
		((String[]) extraInformation)[LABEL] = value;
	}

	public String getNamespace() {
		return namespace;
	}

	void setNamespace(String value) {
		namespace = value;
	}

	public String toString() {
		return getUniqueIdentifier() + " -> " + getExtensionPointIdentifier(); //$NON-NLS-1$
	}

	/**
	 * At the end of this method, extra information will be a string[]
	 */
	private void ensureExtraInformationType() {
		if (extraInformation instanceof SoftReference) {
			extraInformation = ((SoftReference) extraInformation).get();
		}
		if (extraInformation == null) {
			extraInformation = new String[EXTRA_SIZE];
		}
	}
}
