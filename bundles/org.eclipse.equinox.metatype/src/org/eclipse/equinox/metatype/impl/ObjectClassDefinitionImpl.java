/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;
import org.osgi.framework.Bundle;

/**
 * Implementation of ObjectClassDefinition
 */
public class ObjectClassDefinitionImpl extends LocalizationElement implements EquinoxObjectClassDefinition, Cloneable {
	public static final char LOCALE_SEP = '_';

	private static final Comparator<Icon> iconComparator = new Comparator<Icon>() {
		public int compare(Icon icon1, Icon icon2) {
			return icon1.getIconSize().compareTo(icon2.getIconSize());
		}
	};

	private final String _name;
	private final String _id;
	private final String _description;
	private final int _type;
	private final Vector<AttributeDefinitionImpl> _required = new Vector<AttributeDefinitionImpl>(7);
	private final Vector<AttributeDefinitionImpl> _optional = new Vector<AttributeDefinitionImpl>(7);
	private final ExtendableHelper helper;

	// @GuardedBy("this")
	private List<Icon> icons;

	/*
	 * Constructor of class ObjectClassDefinitionImpl.
	 */
	public ObjectClassDefinitionImpl(String name, String description, String id, String localization, Map<String, Map<String, String>> extensionAttributes) {
		this(name, description, id, 0, localization, new ExtendableHelper(extensionAttributes));
	}

	/*
	 * Constructor of class ObjectClassDefinitionImpl.
	 */
	public ObjectClassDefinitionImpl(String name, String description, String id, int type, String localization, ExtendableHelper helper) {
		this._name = name;
		this._id = id;
		this._description = description;
		this._type = type;
		this._localization = localization;
		this.helper = helper;
	}

	/*
	 * 
	 */
	public synchronized Object clone() {

		ObjectClassDefinitionImpl ocd = new ObjectClassDefinitionImpl(_name, _description, _id, _type, _localization, helper);
		for (int i = 0; i < _required.size(); i++) {
			AttributeDefinitionImpl ad = _required.elementAt(i);
			ocd.addAttributeDefinition((AttributeDefinitionImpl) ad.clone(), true);
		}
		for (int i = 0; i < _optional.size(); i++) {
			AttributeDefinitionImpl ad = _optional.elementAt(i);
			ocd.addAttributeDefinition((AttributeDefinitionImpl) ad.clone(), false);
		}
		if (icons != null)
			ocd.setIcons(new ArrayList<Icon>(icons));
		return ocd;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getName()
	 */
	public String getName() {
		return getLocalized(_name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getID()
	 */
	public String getID() {
		return _id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getDescription()
	 */
	public String getDescription() {
		return getLocalized(_description);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getAttributeDefinitions(int)
	 */
	public EquinoxAttributeDefinition[] getAttributeDefinitions(int filter) {

		EquinoxAttributeDefinition[] atts;
		switch (filter) {
			case REQUIRED :
				atts = new EquinoxAttributeDefinition[_required.size()];
				_required.toArray(atts);
				return atts;
			case OPTIONAL :
				atts = new EquinoxAttributeDefinition[_optional.size()];
				_optional.toArray(atts);
				return atts;
			case ALL :
			default :
				atts = new EquinoxAttributeDefinition[_required.size() + _optional.size()];
				Enumeration<AttributeDefinitionImpl> e = _required.elements();
				int i = 0;
				while (e.hasMoreElements()) {
					atts[i] = e.nextElement();
					i++;
				}
				e = _optional.elements();
				while (e.hasMoreElements()) {
					atts[i] = e.nextElement();
					i++;
				}
				return atts;
		}
	}

	/*
	 * Method to add one new AD to ObjectClassDefinition.
	 */
	void addAttributeDefinition(AttributeDefinitionImpl ad, boolean isRequired) {

		if (isRequired) {
			_required.addElement(ad);
		} else {
			_optional.addElement(ad);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getIcon(int)
	 */
	public synchronized InputStream getIcon(int sizeHint) throws IOException {
		// The parameter simply represents a requested size. This method should never return null if an
		// icon exists.
		// Temporary icon to hold the requested size for use in binary search comparator.
		Icon icon = new Icon(null, sizeHint, null);
		@SuppressWarnings("hiding")
		// Use a local reference to the icon list to be sure we don't suddenly start using a new one.
		List<Icon> icons = this.icons;
		int index = Collections.binarySearch(icons, icon, iconComparator);
		if (index < 0) {
			// If the index is less than zero, there wasn't an exact match.
			// Compute the insertion point. This will be the index of the first icon whose 
			// size was greater than the requested size, or the list's length if there were none.
			int insertionPoint = -(index + 1);
			Icon lessThan = insertionPoint == 0 ? null : icons.get(insertionPoint - 1);
			Icon greaterThan = insertionPoint == icons.size() ? null : icons.get(insertionPoint);
			if (lessThan == null)
				// There were no icons whose size was smaller than the requested size.
				icon = greaterThan;
			else if (greaterThan == null)
				// There were no icons whose size was greater than the requested size.
				icon = lessThan;
			else {
				// There was at least one icon with a smaller size and at least one with
				// a greater size than the requested size. Compute the average to see which one to choose.
				int average = (greaterThan.getIconSize() + lessThan.getIconSize()) / 2;
				if (sizeHint < average)
					// The smaller icon is closer to the requested size.
					icon = lessThan;
				else
					// The larger icon is closer to the requested size.
					icon = greaterThan;
			}
		} else
			// The index was greater than or equal to zero, indicating the index of an exact match.
			icon = icons.get(index);
		Bundle b = icon.getIconBundle();
		URL[] urls = FragmentUtils.findEntries(b, getLocalized(icon.getIconName()));
		if (urls != null && urls.length > 0) {
			return urls[0].openStream();
		}
		return null;
	}

	synchronized void setIcons(List<Icon> icons) {
		// Prepare the list of icons for binary searches as in getIcon(int).
		Collections.sort(icons, iconComparator);
		// Make the list unmodifiable for safe binary searches without copying.
		// We assume the caller makes no modifications to the list.
		this.icons = Collections.unmodifiableList(icons);
	}

	/**
	 * Method to set the resource bundle for this OCD and all its ADs.
	 */
	void setResourceBundle(String assignedLocale, Bundle bundle) {

		_rb = getResourceBundle(assignedLocale, bundle);

		Enumeration<AttributeDefinitionImpl> allADReqs = _required.elements();
		while (allADReqs.hasMoreElements()) {
			AttributeDefinitionImpl ad = allADReqs.nextElement();
			ad.setResourceBundle(_rb);
		}

		Enumeration<AttributeDefinitionImpl> allADOpts = _optional.elements();
		while (allADOpts.hasMoreElements()) {
			AttributeDefinitionImpl ad = allADOpts.nextElement();
			ad.setResourceBundle(_rb);
		}
	}

	/*
	 * Internal Method - to get resource bundle.
	 */
	private ResourceBundle getResourceBundle(String locale, final Bundle bundle) {
		// Determine the base name of the bundle localization property files.
		// If the <MetaData> 'localization' attribute was not specified,
		// use the Bundle-Localization manifest header value instead if it exists.
		String resourceBase = _localization != null ? _localization : MetaTypeProviderImpl.getBundleLocalization(bundle);

		// There are seven searching candidates possible:
		// baseName + 
		//		"_" + language1 + "_" + country1 + "_" + variation1	+ ".properties"
		// or	"_" + language1 + "_" + country1					+ ".properties"
		// or	"_" + language1										+ ".properties"
		// or	"_" + language2 + "_" + country2 + "_" + variation2	+ ".properties"
		// or	"_" + language2 + "_" + country2					+ ".properties"
		// or	"_" + language2										+ ".properties"
		// or	""													+ ".properties"
		//
		// Where language1[_country1[_variation1]] is the requested locale,
		// and language2[_country2[_variation2]] is the default locale.

		String[] searchCandidates = new String[7];

		// Candidates from passed locale:
		if (locale != null && locale.length() > 0) {
			int idx1_first = locale.indexOf(LOCALE_SEP);
			if (idx1_first == -1) {
				// locale has only language.
				searchCandidates[2] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale;
			} else {
				// locale has at least language and country.
				searchCandidates[2] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale.substring(0, idx1_first);
				int idx1_second = locale.indexOf(LOCALE_SEP, idx1_first + 1);
				if (idx1_second == -1) {
					// locale just has both language and country.
					searchCandidates[1] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale;
				} else {
					// locale has language, country, and variation all.
					searchCandidates[1] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale.substring(0, idx1_second);
					searchCandidates[0] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + locale;
				}
			}
		}

		// Candidates from Locale.getDefault():
		String defaultLocale = Locale.getDefault().toString();
		int idx2_first = defaultLocale.indexOf(LOCALE_SEP);
		int idx2_second = defaultLocale.indexOf(LOCALE_SEP, idx2_first + 1);
		if (idx2_second != -1) {
			// default-locale is format of [language]_[country]_variation.
			searchCandidates[3] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + defaultLocale;
			if (searchCandidates[3].equalsIgnoreCase(searchCandidates[0])) {
				searchCandidates[3] = null;
			}
		}
		if ((idx2_first != -1) && (idx2_second != idx2_first + 1)) {
			// default-locale is format of [language]_country[_variation].
			searchCandidates[4] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + ((idx2_second == -1) ? defaultLocale : defaultLocale.substring(0, idx2_second));
			if (searchCandidates[4].equalsIgnoreCase(searchCandidates[1])) {
				searchCandidates[4] = null;
			}
		}
		if ((idx2_first == -1) && (defaultLocale.length() > 0)) {
			// default-locale has only language.
			searchCandidates[5] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + defaultLocale;
		} else if (idx2_first > 0) {
			// default-locale is format of language_[...].
			searchCandidates[5] = MetaTypeProviderImpl.RESOURCE_FILE_CONN + defaultLocale.substring(0, idx2_first);
		}
		if (searchCandidates[5] != null && searchCandidates[5].equalsIgnoreCase(searchCandidates[2])) {
			searchCandidates[5] = null;
		}

		// The final candidate.
		searchCandidates[6] = ""; //$NON-NLS-1$

		URL resourceUrl = null;
		URL[] urls = null;

		for (int idx = 0; (idx < searchCandidates.length) && (resourceUrl == null); idx++) {
			urls = (searchCandidates[idx] == null ? null : FragmentUtils.findEntries(bundle, resourceBase + searchCandidates[idx] + MetaTypeProviderImpl.RESOURCE_FILE_EXT));
			if (urls != null && urls.length > 0)
				resourceUrl = urls[0];
		}

		if (resourceUrl != null) {
			try {
				return new PropertyResourceBundle(resourceUrl.openStream());
			} catch (IOException ioe) {
				// Exception when creating PropertyResourceBundle object.
			}
		}
		return null;
	}

	public Map<String, String> getExtensionAttributes(String schema) {
		return helper.getExtensionAttributes(schema);
	}

	public Set<String> getExtensionUris() {
		return helper.getExtensionUris();
	}
}
