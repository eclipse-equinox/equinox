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
import org.eclipse.equinox.ds.model.ImplementationDescription;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class ImplementationElement extends DefaultHandler {
	private ParserHandler root;
	private ComponentElement parent;
	private ImplementationDescription implementation;

	ImplementationElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		implementation = new ImplementationDescription();

		int size = attributes.getLength();
		for (int i = 0; i < size; i++) {
			String key = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (key.equals(ParserConstants.CLASS_ATTRIBUTE)) {
				implementation.setClassname(value);
				continue;
			}
			root.logError("unrecognized implementation element attribute: " + key);
		}

		if (implementation.getClassname() == null) {
			root.logError("implementation class not specified");
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		root.logError("implementation does not support nested elements");
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
		if (component.getImplementation() != null) {
			root.logError("more than one implementation element");
		}

		component.setImplementation(implementation);
		root.setHandler(parent);
	}
}