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

class IgnoredElement extends DefaultHandler {

	private ParserHandler root;
	private DefaultHandler parent;
	
	private int depth;
	
	IgnoredElement(ParserHandler root, DefaultHandler parent) {
		this.root = root;
		this.parent = parent;
		depth = 1;
	}
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		depth++;
	}
	
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		depth--;
		if (depth == 0) {
			root.setHandler(parent);
		}
	}

}
