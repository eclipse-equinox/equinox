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
import org.xml.sax.helpers.DefaultHandler;

class ServiceElement extends DefaultHandler {
	private ParserHandler root;
	private ComponentElement parent;
	private ServiceDescription service;

	public ServiceElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		service = new ServiceDescription();

		int size = attributes.getLength();
		for (int i = 0; i < size; i++) {
			String key = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (key.equals(ParserConstants.SERVICEFACTORY_ATTRIBUTE)) {
				service.setServicefactory(value.equalsIgnoreCase("true"));
				continue;
			}
			root.logError("unrecognized service element attribute: " + key);
		}
	}

	ServiceDescription getServiceDescription() {
		return service;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (localName.equals(ParserConstants.PROVIDE_ELEMENT)) {
			root.setHandler(new ProvideElement(root, this, attributes));
			return;
		}
		root.logError("unrecognized element of service: " + localName);
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
}