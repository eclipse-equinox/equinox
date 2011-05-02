/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.xml;

import java.io.*;
import java.util.Vector;

/**
 * XmlSerializer is an utility class, that simplifies creation of XML files. It
 * is designed to be similar to the XML Pull API serializer.
 * 
 * @author Valentin Valchev
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class XmlSerializer {

	private Writer writer; // underlying writer
	private Vector stack; // of XML entity names
	private StringWriter attrsWriter; // current attribute writer
	private StringBuffer attrs; // current attribute string
	private boolean empty; // is the current node empty
	private boolean closed; // is the current node closed...
	private String indentString;
	private boolean prologWritten;

	/**
	 * Opens and initializes XML output on top of existing Writer.
	 * 
	 * @param writer
	 *            the output writer
	 * @return self
	 */
	public XmlSerializer setOutput(Writer writer) {
		this.writer = writer;
		this.closed = true;
		this.prologWritten = false;
		if (stack == null) {
			stack = new Vector(5);
		} else {
			stack.removeAllElements();
		}
		if (attrs != null) {
			attrs.setLength(0);
		}
		return this;
	}

	/**
	 * Set to use binary output stream with given encoding.
	 * 
	 * @param os
	 *            the output stream
	 * @param encoding
	 *            the output encoding
	 * @throws IOException
	 * @see #setOutput(Writer)
	 * @throws NullPointerException
	 *             is output stream is null
	 */
	public void setOutput(OutputStream os, String encoding) throws IOException {
		if (os == null)
			throw new NullPointerException("output stream can not be null");
		Writer writer = encoding != null ? //
		new OutputStreamWriter(os, encoding)
				: //
				new OutputStreamWriter(os);
		setOutput(writer);
	}

	/**
	 * Writes XML prolog code:
	 * 
	 * <pre>
	 *        &lt;?xml version=&quot;1.0&quot;?&gt;
	 * </pre>
	 * 
	 * @param encoding
	 *            the selected encoding. If <code>null</code> encoding
	 *            attribute is not written.
	 * @param standalone
	 *            if the XML is stand-alone, no DTD or schema
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 * @throws IllegalStateException
	 *             if prolog code is already written
	 */
	public XmlSerializer startDocument(String encoding, Boolean standalone) throws IOException {
		if (prologWritten) {
			throw new IllegalStateException("Prolog already written");
		}
		writer.write("<?xml version=\"1.0\"");
		if (encoding != null) {
			writer.write(" encoding=\"");
			writer.write(encoding);
			writer.write("\"");
		}
		if (standalone != null) {
			writer.write(" standalone=\"");
			writer.write(standalone.booleanValue() ? "yes" : "no");
			writer.write("\"");
		}
		writer.write("?>");
		prologWritten = true;
		return this;
	}

	/**
	 * Close this writer. It does not close the underlying writer, but does
	 * throw an exception if there are as yet unclosed tags.
	 * 
	 * @throws IllegalStateException
	 *             if tags are not closed
	 */
	public void endDocument() {
		if (!stack.isEmpty()) {
			throw new IllegalStateException("Tags are not all closed. Possibly, '" + pop() + "' is unclosed. ");
		}
	}

	/**
	 * Returns the current depth of the element. Outside the root element, the
	 * depth is 0. The depth is incremented by 1 when startTag() is called. The
	 * depth is decremented after the call to endTag() event was observed.
	 * 
	 * <pre>
	 *        &lt;!-- outside --&gt;     0
	 *        &lt;root&gt;               1
	 *          sometext                 1
	 *            &lt;foobar&gt;         2
	 *            &lt;/foobar&gt;        2
	 *        &lt;/root&gt;              1
	 *        &lt;!-- outside --&gt;     0
	 * </pre>
	 * 
	 * @return the current level
	 */
	public int getDepth() {
		return stack.size();
	}

	/**
	 * Begin to output an entity.
	 * 
	 * @param name
	 *            name of entity.
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 */
	public XmlSerializer startTag(String name) throws IOException {
		if (!prologWritten) {
			startDocument(null, null);
		}
		closeOpeningTag();
		closed = false;
		indent();
		writer.write("<");
		writer.write(name);
		stack.addElement(name);
		empty = true;
		return this;
	}

	/**
	 * End the current entity silently without validating if correct tag is
	 * closed.
	 * 
	 * @return self
	 * @throws IOException
	 * @deprecated see {@link #endTag(String)}
	 */
	public XmlSerializer endTag() throws IOException {
		return endTag(null);
	}

	/**
	 * End the current entity. This will throw an exception if it is called when
	 * there is not a currently open entity.
	 * 
	 * @param aName
	 *            tag to close. This is used mostly for validation
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 * @throws IllegalStateException
	 *             if no entity waits to be closed
	 * @throws IllegalArgumentException
	 *             if expected element name is not the same
	 */
	public XmlSerializer endTag(String aName) throws IOException {

		if (stack.isEmpty()) {
			throw new IllegalStateException("Called endEntity too many times.");
		}
		String name = pop();
		if (name != null) {
			if (aName != null && !aName.equals(name)) {
				throw new IllegalArgumentException("Expected element name '" + name + "', not '" + aName + "'");
			}
			if (empty) {
				writeAttributes();
				writer.write("/>");
			} else {
				indent();
				writer.write("</");
				writer.write(name);
				writer.write(">");
			}
			empty = false;
		}
		return this;
	}

	/**
	 * Write an attribute out for the current entity. Any XML characters in the
	 * value are escaped. Calls to attribute() MUST follow a call to startTag()
	 * immediately.
	 * 
	 * @param localName
	 *            name of attribute.
	 * @param value
	 *            value of attribute.
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 * @throws IllegalStateException
	 *             if opening tag is closed
	 */
	public XmlSerializer attribute(String localName, String value) throws IOException {
		if (closed) {
			throw new IllegalStateException("Opening tag is already closed");
		}
		if (localName == null) {
			throw new IllegalArgumentException("Attribute name is null");
		}
		if (value == null)
			value = "";
		if (attrsWriter == null) {
			attrsWriter = new StringWriter();
			attrs = attrsWriter.getBuffer();
		}
		attrs.append(" ");
		attrs.append(localName);
		attrs.append("=\"");
		writeEscape(value, attrsWriter);
		attrs.append("\"");
		return this;
	}

	/**
	 * Output body text. Any XML characters are escaped.
	 * 
	 * @param text
	 *            the body text
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 */
	public XmlSerializer text(String text) throws IOException {
		closeOpeningTag();
		if (text != null && text.length() > 0) {
			indent();
			empty = false;
			writeEscape(text, writer);
		}
		return this;
	}

	/**
	 * Writes raw, CDATA section
	 * 
	 * @param text
	 *            the data
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 */
	public XmlSerializer cdsect(String text) throws IOException {
		closeOpeningTag();
		writer.write("<![CDATA[");
		writer.write(text);
		writer.write("]]>");
		return this;
	}

	/**
	 * Writes XML comment code
	 * 
	 * @param text
	 *            the comment
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 */
	public XmlSerializer comment(String text) throws IOException {
		closeOpeningTag();
		writer.write("<!--");
		writer.write(text);
		writer.write("-->");
		return this;
	}

	/**
	 * Writes DOCTYPE declaration. The document must be open prior calling that
	 * method.
	 * 
	 * @param text
	 *            declaration
	 * @return self
	 * @throws IOException
	 *             on I/O error
	 * @throws IllegalStateException
	 *             if document is not open or start tag is already started
	 */
	public XmlSerializer docdecl(String text) throws IOException {
		if (!prologWritten) {
			throw new IllegalStateException("Document is not open");
		}
		if (getDepth() != 0) {
			throw new IllegalStateException("Cannot add DOCTYPE after start tag has been opened!");
		}
		closeOpeningTag();
		writer.write("<!DOCTYPE");
		writer.write(text);
		writer.write(">");
		return this;
	}

	/**
	 * Enables/disables indentation
	 * 
	 * @param indent
	 *            an indentation string or <code>null</code> to disable
	 *            indentation.
	 */
	public void setIndent(boolean indent) {
		this.indentString = indent ? "\t" : null;
	}

	// close off the opening tag
	private void closeOpeningTag() throws IOException {
		if (!this.closed) {
			writeAttributes();
			closed = true;
			writer.write(">");
		}
	}

	// write out all current attributes
	private void writeAttributes() throws IOException {
		if (attrs != null) {
			writer.write(attrs.toString());
			attrs.setLength(0);
			empty = false;
		}
	}

	private final String pop() {
		int location = stack.size() - 1;
		String ret = (String) stack.elementAt(location);
		stack.removeElementAt(location);
		return ret;
	}

	private final void indent() throws IOException {
		if (indentString != null) {
			writer.write('\n');
			for (int i = 0; i < stack.size(); i++) {
				writer.write(indentString);
			}
		}
	}

	private final void writeEscape(String text, Writer out) throws IOException {
		// escape '<', '&', '>', ', "
		char[] buf = text.toCharArray();
		int len = buf.length;
		int pos = 0;
		for (int i = 0; i < len; i++) {
			final char ch = buf[i];

			if (ch == '&') {
				if (i > pos)
					out.write(buf, pos, i - pos);
				out.write("&amp;");
				pos = i + 1;
			} else if (ch == '<') {
				if (i > pos)
					out.write(buf, pos, i - pos);
				out.write("&lt;");
				pos = i + 1;
			} else if (ch == '>') {
				if (i > pos)
					out.write(buf, pos, i - pos);
				out.write("&gt;");
				pos = i + 1;
			} else if (ch == '"') {
				if (i > pos)
					out.write(buf, pos, i - pos);
				out.write("&guot;");
				pos = i + 1;
			} else if (ch == '\'') {
				if (i > pos)
					out.write(buf, pos, i - pos);
				out.write("&apos;");
				pos = i + 1;
			} else if (ch < 32) {
				// in XML 1.0 only legal character are #x9 | #xA | #xD
				if (ch == 9 || ch == 10 || ch == 13) {
					// pass through
				} else {
					throw new IllegalStateException("character " + ch + " (" + Integer.toString(ch) + ") is not allowed in output");
				}
			}
		}
		if (len > pos) {
			out.write(buf, pos, len - pos);
		}
	}

	/*
	 * public static void main(String[] args) throws IOException { XmlSerializer
	 * o = new XmlSerializer(); Writer x = new java.io.StringWriter();
	 * o.setIndent(true); o.setOutput(x); o.startDocument("UTF-8", null)//
	 * .startTag("test")// .startTag("inner")// .attribute("attribute",
	 * "value").text("text & ' < > \\\" ;[")// .comment("this is an embedded
	 * comment inside the inner body")// .endTag("inner")//
	 * .startTag("entry").cdsect("testing [] > < & ;").endTag("entry")//
	 * .endTag("test")// .endDocument(); System.out.println(x); }
	 */

}
