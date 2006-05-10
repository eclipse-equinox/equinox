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
import org.eclipse.equinox.ds.model.ImplementationDescription;
import org.xml.sax.Attributes;

class ImplementationElement extends ElementHandler {
	private ComponentElement parent;
	private ImplementationDescription implementation;

	ImplementationElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		implementation = new ImplementationDescription();

		processAttributes(attributes);
		
		if (implementation.getClassname() == null) {
			root.logError("implementation class not specified");
		}
	}

	protected void handleAttribute(String name, String value) {
		if (name.equals(ParserConstants.CLASS_ATTRIBUTE)) {
			implementation.setClassname(value);
			return;
		}
		root.logError("unrecognized implementation element attribute: " + name);
	}

	public void endElement(String uri, String localName, String qName) {
		ComponentDescription component = parent.getComponentDescription();
		if (component.getImplementation() != null) {
			root.logError("more than one implementation element");
		}

		component.setImplementation(implementation);
		root.setHandler(parent);
	}

	protected String getElementName() {
		return "implementation";
	}
}
