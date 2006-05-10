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

import org.eclipse.equinox.ds.model.ProvideDescription;
import org.eclipse.equinox.ds.model.ServiceDescription;
import org.xml.sax.Attributes;

class ProvideElement extends ElementHandler {
	private ServiceElement parent;
	private ProvideDescription provide;

	ProvideElement(ParserHandler root, ServiceElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		provide = new ProvideDescription();

		processAttributes(attributes);

		if (provide.getInterfacename() == null) {
			root.logError("provide interface not specified");
		}
	}

	protected void handleAttribute(String name, String value) {
		if (name.equals(ParserConstants.INTERFACE_ATTRIBUTE)) {
			provide.setInterfacename(value);
			return;
		}
		root.logError("unrecognized provide element attribute: " + name);
	}

	public void endElement(String uri, String localName, String qName) {
		ServiceDescription service = parent.getServiceDescription();

		service.addProvide(provide);
		root.setHandler(parent);
	}

	protected String getElementName() {
		return "provide";
	}
}
