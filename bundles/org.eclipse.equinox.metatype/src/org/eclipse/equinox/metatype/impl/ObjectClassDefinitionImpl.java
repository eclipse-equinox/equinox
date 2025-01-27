/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.EquinoxObjectClassDefinition;
import org.eclipse.equinox.metatype.impl.Persistence.Writer;
import org.osgi.framework.Bundle;

/**
 * Implementation of ObjectClassDefinition
 */
public class ObjectClassDefinitionImpl extends LocalizationElement implements EquinoxObjectClassDefinition, Cloneable {
	private static final Comparator<Icon> iconComparator = new Comparator<Icon>() {
		@Override
		public int compare(Icon icon1, Icon icon2) {
			return icon1.getIconSize().compareTo(icon2.getIconSize());
		}
	};

	private final String _name;
	private final String _id;
	private final String _description;
	private final int _type;
	private final List<AttributeDefinitionImpl> _required = new ArrayList<>(7);
	private final List<AttributeDefinitionImpl> _optional = new ArrayList<>(7);
	private final ExtendableHelper helper;

	private volatile List<Icon> icons = null;

	/*
	 * Constructor of class ObjectClassDefinitionImpl.
	 */
	public ObjectClassDefinitionImpl(String name, String description, String id, String localization,
			Map<String, Map<String, String>> extensionAttributes) {
		this(name, description, id, 0, localization, new ExtendableHelper(extensionAttributes));
	}

	/*
	 * Constructor of class ObjectClassDefinitionImpl.
	 */
	public ObjectClassDefinitionImpl(String name, String description, String id, int type, String localization,
			ExtendableHelper helper) {
		super(localization);
		this._name = name;
		this._id = id;
		this._description = description;
		this._type = type;
		this.helper = helper;
	}

	/*
	 */
	@Override
	public Object clone() {

		ObjectClassDefinitionImpl ocd = new ObjectClassDefinitionImpl(_name, _description, _id, _type,
				getLocalization(), helper);
		for (AttributeDefinitionImpl ad : _required) {
			ocd.addAttributeDefinition((AttributeDefinitionImpl) ad.clone(), true);
		}
		for (AttributeDefinitionImpl ad : _optional) {
			ocd.addAttributeDefinition((AttributeDefinitionImpl) ad.clone(), false);
		}
		if (icons != null) {
			ocd.setIcons(new ArrayList<>(icons));
		}
		return ocd;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getName()
	 */
	@Override
	public String getName() {
		return getLocalized(_name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getID()
	 */
	@Override
	public String getID() {
		return _id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getDescription()
	 */
	@Override
	public String getDescription() {
		return getLocalized(_description);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.service.metatype.ObjectClassDefinition#getAttributeDefinitions(int)
	 */
	@Override
	public EquinoxAttributeDefinition[] getAttributeDefinitions(int filter) {

		EquinoxAttributeDefinition[] atts;
		switch (filter) {
		case REQUIRED:
			atts = new EquinoxAttributeDefinition[_required.size()];
			_required.toArray(atts);
			return atts;
		case OPTIONAL:
			atts = new EquinoxAttributeDefinition[_optional.size()];
			_optional.toArray(atts);
			return atts;
		case ALL:
		default:
			atts = new EquinoxAttributeDefinition[_required.size() + _optional.size()];
			int i = 0;
			for (AttributeDefinitionImpl attr : _required) {
				atts[i] = attr;
				i++;
			}
			for (AttributeDefinitionImpl attr : _optional) {
				atts[i] = attr;
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
			_required.add(ad);
		} else {
			_optional.add(ad);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.ObjectClassDefinition#getIcon(int)
	 */
	@Override
	public InputStream getIcon(int sizeHint) throws IOException {
		// The parameter simply represents a requested size. This method should never
		// return null if an
		// icon exists.
		// Temporary icon to hold the requested size for use in binary search
		// comparator.
		Icon icon = new Icon(null, sizeHint, null);
		@SuppressWarnings("hiding")
		// Use a local reference to the icon list to be sure we don't suddenly start
		// using a new one.
		List<Icon> icons = this.icons;
		// Icons will be null if none were specified.
		if (icons == null)
			return null;
		int index = Collections.binarySearch(icons, icon, iconComparator);
		if (index < 0) {
			// If the index is less than zero, there wasn't an exact match.
			// Compute the insertion point. This will be the index of the first icon whose
			// size was greater than the requested size, or the list's length if there were
			// none.
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
				// a greater size than the requested size. Compute the average to see which one
				// to choose.
				int average = (greaterThan.getIconSize() + lessThan.getIconSize()) / 2;
				if (sizeHint < average)
					// The smaller icon is closer to the requested size.
					icon = lessThan;
				else
					// The larger icon is closer to the requested size.
					icon = greaterThan;
			}
		} else
			// The index was greater than or equal to zero, indicating the index of an exact
			// match.
			icon = icons.get(index);
		Bundle b = icon.getIconBundle();
		URL[] urls = FragmentUtils.findEntries(b, getLocalized(icon.getIconName()));
		if (urls != null && urls.length > 0) {
			return urls[0].openStream();
		}
		return null;
	}

	void setIcons(List<Icon> icons) {
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
	void setResourceBundle(String assignedLocale, Bundle bundle, Map<String, ResourceBundle> resourceBundleCache) {
		setLocaleAndBundle(assignedLocale, bundle, resourceBundleCache);
		for (AttributeDefinitionImpl ad : _required) {
			ad.setLocaleAndBundle(assignedLocale, bundle, resourceBundleCache);
		}
		for (AttributeDefinitionImpl ad : _optional) {
			ad.setLocaleAndBundle(assignedLocale, bundle, resourceBundleCache);
		}
	}

	@Override
	public Map<String, String> getExtensionAttributes(String schema) {
		return helper.getExtensionAttributes(schema);
	}

	@Override
	public Set<String> getExtensionUris() {
		return helper.getExtensionUris();
	}

	void getStrings(Set<String> strings) {
		helper.getStrings(strings);
		strings.add(_description);
		strings.add(_id);
		strings.add(_name);
		strings.add(getLocalization());
		@SuppressWarnings("hiding")
		List<Icon> icons = this.icons;
		if (icons != null) {
			for (Icon icon : icons) {
				icon.getStrings(strings);
			}
		}
	}

	static ObjectClassDefinitionImpl load(Bundle b, LogTracker logger, Persistence.Reader reader) throws IOException {
		String description = reader.readString();
		String id = reader.readString();
		String name = reader.readString();
		int type = reader.readInt();
		String localization = reader.readString();
		ExtendableHelper helper = ExtendableHelper.load(reader);
		ObjectClassDefinitionImpl result = new ObjectClassDefinitionImpl(name, description, id, type, localization,
				helper);

		int numIcons = reader.readInt();
		List<Icon> icons = null;
		if (numIcons > 0) {
			icons = new ArrayList<>(numIcons);
			for (int i = 0; i < numIcons; i++) {
				icons.add(Icon.load(reader, b));
			}
		}
		result.setIcons(icons);

		int numRequired = reader.readInt();
		for (int i = 0; i < numRequired; i++) {
			result.addAttributeDefinition(AttributeDefinitionImpl.load(reader, logger), true);
		}
		int numOptional = reader.readInt();
		for (int i = 0; i < numOptional; i++) {
			result.addAttributeDefinition(AttributeDefinitionImpl.load(reader, logger), false);
		}

		return result;
	}

	void write(Writer writer) throws IOException {
		writer.writeString(_description);
		writer.writeString(_id);
		writer.writeString(_name);
		writer.writeInt(_type);
		writer.writeString(getLocalization());
		helper.write(writer);
		List<Icon> curIcons = this.icons;
		writer.writeInt(curIcons == null ? 0 : curIcons.size());
		if (curIcons != null) {
			for (Icon icon : curIcons) {
				icon.write(writer);
			}
		}
		writer.writeInt(_required.size());
		for (AttributeDefinitionImpl ad : _required) {
			ad.write(writer);
		}
		writer.writeInt(_optional.size());
		for (AttributeDefinitionImpl ad : _optional) {
			ad.write(writer);
		}
	}
}
