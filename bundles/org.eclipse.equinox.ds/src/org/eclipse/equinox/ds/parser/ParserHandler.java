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

import java.util.List;
import org.eclipse.equinox.ds.Log;
import org.eclipse.equinox.ds.model.ComponentDescription;
import org.osgi.framework.BundleContext;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ParserHandler implements the methods for the DefaultHandler of the SAX
 * parser. Each Service Component bundle contains a set of xml files which are
 * parsed.
 * 
 * @version $Revision: 1.2 $
 */

class ParserHandler extends DefaultHandler {

	/* set this to true to compile in debug messages */
	private static final boolean DEBUG = false;

	private DefaultHandler handler;
	private List components;
	protected BundleContext bundleContext;
	private int depth;
	private boolean error;

	ParserHandler(BundleContext bundleContext, List components) {
		this.bundleContext = bundleContext;
		this.components = components;
	}

	public void setHandler(DefaultHandler handler) {
		this.handler = handler;
	}

	public void addComponentDescription(ComponentDescription component) {
		components.add(component);
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public boolean isError() {
		return error;
	}

	public void logError(String msg) {
		error = true;
		Log.log(1, "[SCR] Parser Error. ", new SAXException(msg));
	}

	public void startDocument() throws SAXException {
		handler = this;
		depth = 0;
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {

		if (DEBUG) {
			System.out.println("[startPrefixMapping:prefix]" + prefix);
			System.out.println("[startPrefixMapping:uri]" + uri);
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		depth++;

		if (DEBUG) {
			System.out.println("[startElement:begin]");
			System.out.println(" [uri]" + uri);
			System.out.println(" [localName]" + localName);
			System.out.println(" [qName]" + qName);

			int size = attributes.getLength();
			for (int i = 0; i < size; i++) {
				String key = attributes.getQName(i);
				String value = attributes.getValue(i);
				System.out.println(" [attr:" + i + ":localName]" + attributes.getLocalName(i));
				System.out.println(" [attr:" + i + ":qName]" + attributes.getQName(i));
				System.out.println(" [attr:" + i + ":type]" + attributes.getType(i));
				System.out.println(" [attr:" + i + ":URI]" + attributes.getURI(i));
				System.out.println(" [attr:" + i + ":value]" + attributes.getValue(i));
			}
			System.out.println("[startElement:end]");
		}

		if (handler != this) {
			handler.startElement(uri, localName, qName, attributes);
			return;
		}

		if (localName.equals(ParserConstants.COMPONENT_ELEMENT)) {
			if (((depth == 1) && (uri.length() == 0)) || uri.equals(ParserConstants.SCR_NAMESPACE)) {
				setHandler(new ComponentElement(this, attributes));
			}
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {

		if (DEBUG) {
			System.out.print("[characters:begin]");
			System.out.print(new String(ch, start, length));
			System.out.println("[characters:end]");
		}

		if (handler != this) {
			handler.characters(ch, start, length);
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {

		if (DEBUG) {
			System.out.println("[endElement:uri]" + uri);
			System.out.println("[endElement:localName]" + localName);
			System.out.println("[endElement:qName]" + qName);
		}

		if (handler != this) {
			handler.endElement(uri, localName, qName);
		}

		depth--;
	}

	public void endPrefixMapping(String prefix) throws SAXException {
		if (DEBUG) {
			System.out.println("[endPrefixMapping:prefix]" + prefix);
		}
	}

	public void endDocument() throws SAXException {
		if (DEBUG) {
			System.out.println("[endDocument]");
		}
	}

	public void warning(SAXParseException e) throws SAXException {
		if (DEBUG) {
			System.out.println("[warning]");
			e.printStackTrace();
		}
	}

	public void error(SAXParseException e) throws SAXException {
		if (DEBUG) {
			System.out.println("[error]");
			e.printStackTrace();
		}
	}

	public void fatalError(SAXParseException e) throws SAXException {
		if (DEBUG) {
			System.out.println("[fatalError]");
			e.printStackTrace();
		}
		throw e;
	}
}
