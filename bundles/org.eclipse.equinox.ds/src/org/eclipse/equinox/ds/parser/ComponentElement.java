/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

class ComponentElement extends ElementHandler {
	private ParserHandler parent;
	private ComponentDescription component;
	private boolean immediateSet;

	ComponentElement(ParserHandler root, Attributes attributes) {
		this.root = root;
		this.parent = root;
		component = new ComponentDescription(root.bundleContext);
		immediateSet = false;

		processAttributes(attributes);

		if (component.getName() == null) {
			root.logError("component name not specified");
		}
	}
	
	protected void handleAttribute(String name, String value) {
		if (name.equals(ParserConstants.NAME_ATTRIBUTE)) {
			component.setName(value);
			PropertyValueDescription nameProperty = new PropertyValueDescription();
			nameProperty.setName(ComponentConstants.COMPONENT_NAME);
			nameProperty.setValue(value);
			component.addPropertyDescription(nameProperty);
			return;
		}
		if (name.equals(ParserConstants.ENABLED_ATTRIBUTE)) {
			component.setAutoenable(value.equalsIgnoreCase("true"));
			return;
		}
		if (name.equals(ParserConstants.FACTORY_ATTRIBUTE)) {
			component.setFactory(value);
			return;
		}
		if (name.equals(ParserConstants.IMMEDIATE_ATTRIBUTE)) {
			component.setImmediate(value.equalsIgnoreCase("true"));
			immediateSet = true;
			return;
		}
		
		root.logError("unrecognized component element attribute: " + name);
	}



	ComponentDescription getComponentDescription() {
		return component;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (isSCRNamespace(uri)) {
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
		}
		
		super.startElement(uri, localName, qName, attributes);
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

	protected String getElementName() {
		return "component";
	}
}
