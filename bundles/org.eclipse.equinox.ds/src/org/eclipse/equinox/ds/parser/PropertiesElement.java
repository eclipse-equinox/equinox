/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.parser;

import org.eclipse.equinox.ds.model.ComponentDescription;
import org.eclipse.equinox.ds.model.PropertyResourceDescription;
import org.xml.sax.Attributes;

class PropertiesElement extends ElementHandler {
	private ComponentElement parent;
	private PropertyResourceDescription properties;

	PropertiesElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		properties = new PropertyResourceDescription();

		processAttributes(attributes);
		
		if (properties.getEntry() == null) {
			root.logError("properties entry not specified");
		}
	}

	protected void handleAttribute(String name, String value) {
		if (name.equals(ParserConstants.ENTRY_ATTRIBUTE)) {
			properties.setEntry(value);
			return;
		}
		root.logError("unrecognized properties element attribute: " + name);
	}

	public void endElement(String uri, String localName, String qName) {
		ComponentDescription component = parent.getComponentDescription();
		component.addPropertyDescription(properties);
		root.setHandler(parent);
	}

	protected String getElementName() {
		return "properties";
	}
}