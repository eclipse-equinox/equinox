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

import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.osgi.framework.Bundle;

/**
 * Implementation of ObjectClassDefinition
 */
public class ObjectClassDefinitionImpl extends LocalizationElement implements EquinoxObjectClassDefinition, Cloneable {

	public static final char LOCALE_SEP = '_';

	String _name;
	String _id;
	String _description;

	int _type;
	Vector<AttributeDefinitionImpl> _required = new Vector<AttributeDefinitionImpl>(7);
	Vector<AttributeDefinitionImpl> _optional = new Vector<AttributeDefinitionImpl>(7);
	Icon _icon;

	private final ExtendableHelper helper;

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
		if (_icon != null) {
			ocd.setIcon((Icon) _icon.clone());
		}
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

	/**
	 * Method to set the name of ObjectClassDefinition.
	 */
	void setName(String name) {
		this._name = name;
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
	 * Method to set the description of ObjectClassDefinition.
	 */
	void setDescription(String description) {
		this._description = description;
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
	public InputStream getIcon(int sizeHint) throws IOException {
		// The parameter simply represents a requested size. This method should never return null if an
		// icon exists.
		// TODO This method may change further depending on the outcome of certain ongoing CPEG discussions.
		// It is thought that users should be able to specify the same icon multiple times but of different
		// sizes. This would require a change to the XML schema. This method would then return the icon with
		// a size closest to the requested size.
		if ((_icon == null)) {
			return null;
		}
		Bundle b = _icon.getIconBundle();
		URL[] urls = FragmentUtils.findEntries(b, getLocalized(_icon.getIconName()));
		if (urls != null && urls.length > 0) {
			return urls[0].openStream();
		}
		return null;
	}

	/**
	 * Method to set the icon of ObjectClassDefinition.
	 */
	void setIcon(Icon icon) {
		this._icon = icon;
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
