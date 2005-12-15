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
import org.eclipse.equinox.ds.model.ReferenceDescription;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class ReferenceElement extends DefaultHandler {
	private ParserHandler root;
	private ComponentElement parent;
	private ReferenceDescription reference;

	ReferenceElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		reference = new ReferenceDescription();

		int size = attributes.getLength();
		for (int i = 0; i < size; i++) {
			String key = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (key.equals(ParserConstants.NAME_ATTRIBUTE)) {
				reference.setName(value);
				continue;
			}

			if (key.equals(ParserConstants.INTERFACE_ATTRIBUTE)) {
				reference.setInterfacename(value);
				continue;
			}

			if (key.equals(ParserConstants.CARDINALITY_ATTRIBUTE)) {
				reference.setCardinality(value);
				continue;
			}

			if (key.equals(ParserConstants.POLICY_ATTRIBUTE)) {
				reference.setPolicy(value);
				continue;
			}

			if (key.equals(ParserConstants.TARGET_ATTRIBUTE)) {
				reference.setTarget(value);
				continue;
			}

			if (key.equals(ParserConstants.BIND_ATTRIBUTE)) {
				reference.setBind(value);
				continue;
			}

			if (key.equals(ParserConstants.UNBIND_ATTRIBUTE)) {
				reference.setUnbind(value);
				continue;
			}
			root.logError("unrecognized reference element attribute: " + key);
		}

		if (reference.getName() == null) {
			root.logError("reference name not specified");
		}
		if (reference.getInterfacename() == null) {
			root.logError("reference interface not specified");
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		root.logError("reference does not support nested elements");
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
		component.addReferenceDescription(reference);
		root.setHandler(parent);
	}
}