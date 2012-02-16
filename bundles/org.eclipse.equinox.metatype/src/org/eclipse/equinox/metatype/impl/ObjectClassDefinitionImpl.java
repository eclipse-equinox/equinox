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
		super(localization);
		this._name = name;
		this._id = id;
		this._description = description;
		this._type = type;
		this.helper = helper;
	}

	/*
	 * 
	 */
	public synchronized Object clone() {

		ObjectClassDefinitionImpl ocd = new ObjectClassDefinitionImpl(_name, _description, _id, _type, getLocalization(), helper);
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
		// Icons will be null if none were specified.
		if (icons == null)
			return null;
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
		// Do nothing if icons is null or empty.
		if (icons == null || icons.isEmpty())
			return;
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
		setLocaleAndBundle(assignedLocale, bundle);
		Enumeration<AttributeDefinitionImpl> allADReqs = _required.elements();
		while (allADReqs.hasMoreElements()) {
			AttributeDefinitionImpl ad = allADReqs.nextElement();
			ad.setLocaleAndBundle(assignedLocale, bundle);
		}

		Enumeration<AttributeDefinitionImpl> allADOpts = _optional.elements();
		while (allADOpts.hasMoreElements()) {
			AttributeDefinitionImpl ad = allADOpts.nextElement();
			ad.setLocaleAndBundle(assignedLocale, bundle);
		}
	}

	public Map<String, String> getExtensionAttributes(String schema) {
		return helper.getExtensionAttributes(schema);
	}

	public Set<String> getExtensionUris() {
		return helper.getExtensionUris();
	}
}
