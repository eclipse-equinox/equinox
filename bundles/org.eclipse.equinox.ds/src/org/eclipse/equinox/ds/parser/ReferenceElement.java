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
import org.eclipse.equinox.ds.model.ReferenceDescription;
import org.xml.sax.Attributes;

class ReferenceElement extends ElementHandler {
	private ComponentElement parent;
	private ReferenceDescription reference;

	ReferenceElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		reference = new ReferenceDescription();

		processAttributes(attributes);

		if (reference.getName() == null) {
			root.logError("reference name not specified");
		}
		if (reference.getInterfacename() == null) {
			root.logError("reference interface not specified");
		}
	}

	protected void handleAttribute(String name, String value) {
		if (name.equals(ParserConstants.NAME_ATTRIBUTE)) {
			reference.setName(value);
			return;
		}

		if (name.equals(ParserConstants.INTERFACE_ATTRIBUTE)) {
			reference.setInterfacename(value);
			return;
		}

		if (name.equals(ParserConstants.CARDINALITY_ATTRIBUTE)) {
			reference.setCardinality(value);
			return;
		}

		if (name.equals(ParserConstants.POLICY_ATTRIBUTE)) {
			reference.setPolicy(value);
			return;
		}

		if (name.equals(ParserConstants.TARGET_ATTRIBUTE)) {
			reference.setTarget(value);
			return;
		}

		if (name.equals(ParserConstants.BIND_ATTRIBUTE)) {
			reference.setBind(value);
			return;
		}

		if (name.equals(ParserConstants.UNBIND_ATTRIBUTE)) {
			reference.setUnbind(value);
			return;
		}
		root.logError("unrecognized reference element attribute: " + name);
	}

	public void endElement(String uri, String localName, String qName) {
		ComponentDescription component = parent.getComponentDescription();
		component.addReferenceDescription(reference);
		root.setHandler(parent);
	}

	protected String getElementName() {
		return "reference";
	}
}
