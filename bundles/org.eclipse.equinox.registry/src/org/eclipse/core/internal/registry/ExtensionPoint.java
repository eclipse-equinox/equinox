/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.io.File;
import java.lang.ref.SoftReference;
import org.eclipse.core.runtime.IContributor;

/**
 * An object which represents the user-defined extension point in a 
 * plug-in manifest. 
 */
public class ExtensionPoint extends RegistryObject {
	public static final ExtensionPoint[] EMPTY_ARRAY = new ExtensionPoint[0];

	//Place holder for the label and the schema. It contains either a String[] or a SoftReference to a String[].
	//The array layout is [label, schemaReference, fullyQualifiedName, namespace, contributorId]
	private Object extraInformation;
	//Indexes of the various fields
	private static final byte LABEL = 0; //The human readable name for the extension point
	private static final byte SCHEMA = 1; //The schema of the extension point
	private static final byte QUALIFIED_NAME = 2; //The fully qualified name of the extension point
	private static final byte NAMESPACE = 3; //The name of the namespace of the extension point
	private static final byte CONTRIBUTOR_ID = 4; //The ID of the actual contributor of the extension point
	private static final int EXTRA_SIZE = 5;

	protected ExtensionPoint(ExtensionRegistry registry, boolean persist) {
		super(registry, persist);
	}

	protected ExtensionPoint(int self, int[] children, int dataOffset, ExtensionRegistry registry, boolean persist) {
		super(registry, persist);

		setObjectId(self);
		setRawChildren(children);
		setExtraDataOffset(dataOffset);
	}

	protected String getSimpleIdentifier() {
		return getUniqueIdentifier().substring(getUniqueIdentifier().lastIndexOf('.') + 1);
	}

	private String[] getExtraData() {
		//The extension point has been created by parsing, or does not have any extra data 
		if (noExtraData()) { //When this is true, the extraInformation is always a String[]. This happens when the object is created by the parser.  
			if (extraInformation != null)
				return (String[]) extraInformation;
			return new String[EXTRA_SIZE];
		}

		//The extension point has been loaded from the cache. 
		String[] result = null;
		if (extraInformation == null || (result = ((extraInformation instanceof SoftReference) ? (String[]) ((SoftReference) extraInformation).get() : (String[]) extraInformation)) == null) {
			result = registry.getTableReader().loadExtensionPointExtraData(getExtraDataOffset());
			extraInformation = new SoftReference(result);
		}
		return result;
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

	protected String getSchemaReference() {
		String[] result = getExtraData();
		return result[1] == null ? "" : result[SCHEMA].replace(File.separatorChar, '/'); //$NON-NLS-1$		
	}

	protected String getLabel() {
		String[] result = getExtraData();
		return result[0] == null ? "" : result[LABEL]; //$NON-NLS-1$
	}

	protected String getUniqueIdentifier() {
		return getExtraData()[QUALIFIED_NAME];
	}

	public String getNamespace() {
		return getExtraData()[NAMESPACE];
	}

	protected String getContributorId() {
		return getExtraData()[CONTRIBUTOR_ID];
	}

	public IContributor getContributor() {
		return registry.getObjectManager().getContributor(getContributorId());
	}

	void setSchema(String value) {
		ensureExtraInformationType();
		((String[]) extraInformation)[SCHEMA] = value;
	}

	void setLabel(String value) {
		ensureExtraInformationType();
		((String[]) extraInformation)[LABEL] = value;
	}

	void setUniqueIdentifier(String value) {
		ensureExtraInformationType();
		((String[]) extraInformation)[QUALIFIED_NAME] = value;
	}

	void setNamespace(String value) {
		ensureExtraInformationType();
		((String[]) extraInformation)[NAMESPACE] = value;
	}

	void setContributorId(String id) {
		ensureExtraInformationType();
		((String[]) extraInformation)[CONTRIBUTOR_ID] = id;
	}

	public String toString() {
		return getUniqueIdentifier();
	}

	protected String getLabelAsIs() {
		String[] result = getExtraData();
		return result[0] == null ? "" : result[LABEL]; //$NON-NLS-1$
	}

	protected String getLabel(String locale) {
		registry.logMultiLangError();
		return getLabel();
	}

}
