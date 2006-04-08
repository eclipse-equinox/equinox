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
import org.eclipse.equinox.ds.model.ServiceDescription;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ServiceElement extends ElementHandler {
	private ComponentElement parent;
	private ServiceDescription service;

	public ServiceElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		service = new ServiceDescription();

		processAttributes(attributes);
	}

	protected void handleAttribute(String name, String value) {
		if (name.equals(ParserConstants.SERVICEFACTORY_ATTRIBUTE)) {
			service.setServicefactory(value.equalsIgnoreCase("true"));
			return;
		}
		root.logError("unrecognized service element attribute: " + name);
	}

	ServiceDescription getServiceDescription() {
		return service;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (isSCRNamespace(uri)) {
			if (localName.equals(ParserConstants.PROVIDE_ELEMENT)) {
				root.setHandler(new ProvideElement(root, this, attributes));
				return;
			}
		}
		
		super.startElement(uri, localName, qName, attributes);
	}

	public void endElement(String uri, String localName, String qName) {
		ComponentDescription component = parent.getComponentDescription();
		if (component.getService() != null) {
			root.logError("more than one service element");
		}

		if (service.getProvides().length == 0) {
			root.logError("no provide elements specified");
		}

		if ((component.getFactory() != null) && service.isServicefactory()) {
			root.logError("component factory is incompatible with Service factory ");
		}

		component.setService(service);
		root.setHandler(parent);
	}

	protected String getElementName() {
		return "service";
	}
}