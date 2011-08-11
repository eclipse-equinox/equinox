/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

/**
 * An object which represents the user-defined contents of an extension
 * in a plug-in manifest.
 */
public class ConfigurationElementMulti extends ConfigurationElement {

	/**
	 * Translated values for the locale
	 */
	private DirectMap translatedProperties = new DirectMap(10, 0.5f);

	protected ConfigurationElementMulti(ExtensionRegistry registry, boolean persist) {
		super(registry, persist);
	}

	protected ConfigurationElementMulti(int self, String contributorId, String name, String[] propertiesAndValue, int[] children, int extraDataOffset, int parent, byte parentType, ExtensionRegistry registry, boolean persist) {
		super(self, contributorId, name, propertiesAndValue, children, extraDataOffset, parent, parentType, registry, persist);
	}

	String getAttribute(String attrName, String locale) {
		if (propertiesAndValue.length <= 1)
			return null;
		//round down to an even size
		int size = propertiesAndValue.length - (propertiesAndValue.length % 2);
		int index = -1;
		for (int i = 0, j = 0; i < size; i += 2, j++) {
			if (!(propertiesAndValue[i].equals(attrName)))
				continue;
			index = j;
			break;
		}
		if (index == -1)
			return null;

		String result = getTranslatedAtIndex(index, locale);
		if (result != null)
			return result;
		return propertiesAndValue[index * 2 + 1]; // return non-translated value
	}

	String getValue(String locale) {
		if (propertiesAndValue.length == 0 || propertiesAndValue.length % 2 == 0)
			return null;
		int index = propertiesAndValue.length - 1;
		return getTranslatedAtIndex(index, locale);
	}

	synchronized private String getTranslatedAtIndex(int index, String locale) {
		String[] translated = null;
		if (!translatedProperties.containsKey(locale)) {
			String[] propertiesNonTranslated = getNonTranslated();
			translated = registry.translate(propertiesNonTranslated, getContributor(), locale);
			translatedProperties.put(locale, translated);
			registry.getObjectManager().markDirty();
		} else
			translated = translatedProperties.get(locale);

		if (translated != null)
			return translated[index];
		return null;
	}

	private String[] getNonTranslated() {
		int size = propertiesAndValue.length / 2;
		boolean hasValue = ((propertiesAndValue.length % 2) == 1);
		if (hasValue)
			size++;
		String[] propertiesNonTranslated = new String[size];
		int pos = 0;
		for (int i = 1; i < propertiesAndValue.length; i += 2) {
			propertiesNonTranslated[pos] = propertiesAndValue[i];
			pos++;
		}
		if (hasValue)
			propertiesNonTranslated[pos] = propertiesAndValue[propertiesAndValue.length - 1];
		return propertiesNonTranslated;
	}

	synchronized int getNumCachedLocales() {
		return translatedProperties.getSzie();
	}

	synchronized String[] getCachedLocales() {
		return translatedProperties.getKeys();
	}

	synchronized String[][] getCachedTranslations() {
		return translatedProperties.getValues();
	}

	synchronized void setTranslatedProperties(DirectMap translated) {
		translatedProperties = translated;
	}

	///////////////////////////////////////////////////////////////////////////////////
	// "Default" locale

	public String getAttribute(String attrName) {
		return getAttribute(attrName, getLocale());
	}

	public String getValue() {
		return getValue(getLocale());
	}

}
