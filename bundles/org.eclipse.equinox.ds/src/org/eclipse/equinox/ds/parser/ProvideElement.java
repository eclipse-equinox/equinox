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

import org.eclipse.equinox.ds.model.ProvideDescription;
import org.eclipse.equinox.ds.model.ServiceDescription;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class ProvideElement extends DefaultHandler {
	private ParserHandler root;
	private ServiceElement parent;
	private ProvideDescription provide;

	ProvideElement(ParserHandler root, ServiceElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		provide = new ProvideDescription();

		int size = attributes.getLength();
		for (int i = 0; i < size; i++) {
			String key = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (key.equals(ParserConstants.INTERFACE_ATTRIBUTE)) {
				provide.setInterfacename(value);
				continue;
			}
			root.logError("unrecognized provide element attribute: " + key);
		}

		if (provide.getInterfacename() == null) {
			root.logError("provide interface not specified");
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		root.logError("provide does not support nested elements");
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
		ServiceDescription service = parent.getServiceDescription();

		service.addProvide(provide);
		root.setHandler(parent);
	}
}