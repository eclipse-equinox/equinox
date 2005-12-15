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
import org.eclipse.equinox.ds.model.PropertyValueDescription;
import org.osgi.service.component.ComponentConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class ComponentElement extends DefaultHandler {
	private ParserHandler root;
	private ParserHandler parent;
	private ComponentDescription component;
	private boolean immediateSet;

	ComponentElement(ParserHandler root, Attributes attributes) {
		this.root = root;
		this.parent = root;
		component = new ComponentDescription(root.bundleContext);
		immediateSet = false;

		int size = attributes.getLength();
		for (int i = 0; i < size; i++) {
			String key = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (key.equals(ParserConstants.NAME_ATTRIBUTE)) {
				component.setName(value);
				PropertyValueDescription nameProperty = new PropertyValueDescription();
				nameProperty.setName(ComponentConstants.COMPONENT_NAME);
				nameProperty.setValue(value);
				component.addPropertyDescription(nameProperty);
				continue;
			}
			if (key.equals(ParserConstants.ENABLED_ATTRIBUTE)) {
				component.setAutoenable(value.equalsIgnoreCase("true"));
				continue;
			}
			if (key.equals(ParserConstants.FACTORY_ATTRIBUTE)) {
				component.setFactory(value);
				continue;
			}
			if (key.equals(ParserConstants.IMMEDIATE_ATTRIBUTE)) {
				component.setImmediate(value.equalsIgnoreCase("true"));
				immediateSet = true;
				continue;
			}
			root.logError("unrecognized component element attribute: " + key);
		}

		if (component.getName() == null) {
			root.logError("component name not specified");
		}
	}

	ComponentDescription getComponentDescription() {
		return component;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (localName.equals(ParserConstants.IMPLEMENTATION_ELEMENT)) {
			root.setHandler(new ImplementationElement(root, this, attributes));
			return;
		}

		if (localName.equals(ParserConstants.PROPERTY_ELEMENT)) {
			root.setHandler(new PropertyElement(root, this, attributes));
			return;
		}

		if (localName.equals(ParserConstants.PROPERTIES_ELEMENT)) {
			root.setHandler(new PropertiesElement(root, this, attributes));
			return;
		}

		if (localName.equals(ParserConstants.SERVICE_ELEMENT)) {
			root.setHandler(new ServiceElement(root, this, attributes));
			return;
		}

		if (localName.equals(ParserConstants.REFERENCE_ELEMENT)) {
			root.setHandler(new ReferenceElement(root, this, attributes));
			return;
		}
		root.logError("unrecognized element of component: " + localName);
	}

	public void characters(char[] ch, int start, int length) {
		int end = start + length;
		for (int i = start; i < end; i++) {
			if (!Character.isWhitespace(ch[i])) {
				root.logError("element body must be empty");
			}
		}
	}

	public void endElement(String uri, String localName, String qName) {

		// if unset, immediate attribute is false if service element is
		// specified or true otherwise
		// if component factory then immediate by default is false
		if (!immediateSet && (component.getFactory() == null)) {
			component.setImmediate(component.getService() == null);
		}

		if (component.getImplementation() == null) {
			root.logError("no implementation element");
		}

		if (root.isError()) {
			root.setError(false);
		} else {
			parent.addComponentDescription(component);
		}

		root.setHandler(parent);
	}
}