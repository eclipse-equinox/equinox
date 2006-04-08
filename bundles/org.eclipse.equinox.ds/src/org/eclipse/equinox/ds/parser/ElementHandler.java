/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Base class for element handlers to provide some common behavior.
 * 
 */
abstract class ElementHandler extends DefaultHandler {
	
	protected ParserHandler	root;

	/**
	 * Return the name of the handled element.
	 * 
	 * @return The name of the handled element.
	 */
	protected abstract String getElementName();
	
	protected boolean isSCRNamespace(String uri) {
		return (uri.length() == 0) || uri.equals(ParserConstants.SCR_NAMESPACE);
	}
	
	protected void processAttributes(Attributes attributes) {
		int size = attributes.getLength();
		for (int i = 0; i < size; i++) {
			if (isSCRNamespace(attributes.getURI(i))) {
				handleAttribute(attributes.getLocalName(i), attributes.getValue(i));
			}
		}
	}
	
	protected abstract void handleAttribute(String name, String value);

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (isSCRNamespace(uri)) {
			root.logError("unrecognized element "+ qName);
		}
		
		root.setHandler(new IgnoredElement(root, this));
	}

	public void characters(char[] ch, int start, int length) {
		int end = start + length;
		for (int i = start; i < end; i++) {
			if (!Character.isWhitespace(ch[i])) {
				root.logError(getElementName()+ " element body must be empty");
			}
		}
	}

}
