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

import java.util.Vector;
import org.eclipse.equinox.internal.ds.Activator;
import org.eclipse.equinox.internal.util.string.CharBuffer;

/**
 * Class corresponding to one XML tag, containing its tag name, attributes,
 * content string and tree of the sub tags.
 * 
 * @author Ivan Dimitrov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class TagClass {

	private static final String INTERN_TAGCONTENT = "xml.intern.tagcontent";
	private static boolean fInternTagContent = Activator.getBoolean(INTERN_TAGCONTENT);

	private CharBuffer fContent = null;
	private String fName = null;
	private Vector fTags = null;
	String fAttributes = null;
	private int fLine = -1;
	protected boolean fInline = false;

	/**
	 * Creates an empty tag node
	 */
	public TagClass() {
		// Do nothing here
	}

	/**
	 * Creates tag node with specified name
	 * 
	 * @param name
	 *            tag name
	 */
	public TagClass(String name) {
		fName = name;
	}

	/**
	 * Creates tag node with specified name
	 * 
	 * @param line
	 *            the line in the XML file where the tags begin.
	 * @param name
	 *            tag name
	 */
	public TagClass(int line, String name) {
		fLine = line;
		fName = name;
	}

	/**
	 * Creates tag node with specified name and content string
	 * 
	 * @param name
	 *            tag name
	 * @param contentStr
	 *            content string of the tag
	 */
	public TagClass(String name, String contentStr) {
		setup(-1, name, contentStr);
	}

	/**
	 * Creates tag node with specified name and content string
	 * 
	 * @param line
	 *            the line in the XML file where the tags begin.
	 * @param name
	 *            tag name
	 * @param contentStr
	 *            content string of the tag
	 */
	public TagClass(int line, String name, String contentStr) {
		setup(line, name, contentStr);
	}

	private void setup(int line, String name, String contentStr) {
		fLine = line;
		fName = name;
		if (contentStr != null && contentStr.length() > 0) {
			fContent = new CharBuffer(contentStr.length());
			fContent.append(contentStr);
		}
	}

	/**
	 * Appends the 'toAppend' CharBuffer content to the content buffer
	 * 
	 * @param toAppend
	 *            CharBuffer content to append
	 */
	protected void appendContent(CharBuffer toAppend) {
		if (fContent == null) {
			fContent = new CharBuffer();
		}
		fContent.append(toAppend.getValue());
	}

	/**
	 * Appends the 'toAppend' string to the content buffer
	 * 
	 * @param toAppend
	 *            string to append
	 */
	protected void appendContent(String toAppend) {
		if (fContent == null) {
			fContent = new CharBuffer();
		}
		fContent.append(toAppend);
	}

	/**
	 * Returns content string of the tag
	 * 
	 * @return tag content note: The content that is to be returned will be
	 *         trimmed first
	 */
	/*
	 * Implementation Note: The content string is not suitable to be interned
	 * with String().intern() because it is more often to be unique and the
	 * benefit will be much smaller than expected.
	 */
	public String getContent() {
		String result = "";
		if (fContent != null) {
			result = fContent.trim();
			if (fInternTagContent) {
				result = result.intern();
			}
		}
		return result;
		// TODO -oIvan: Should not it be null instead of empty string?...
		// Plamen: Yes it have to be null insted of "" because the old one used
		// to return null.
		// Ivan Dimitrov: The metatyping code relies on the fact that the
		// content is never null
	}

	/**
	 * Returns the content of a child tag with index position equals to aPos but
	 * only if its name is equal to aName. Otherwise the method throws an
	 * exception
	 * 
	 * @param aPos
	 *            index of the tag in its parent list
	 * @param aName
	 *            the name of the tag
	 * @return tag content
	 * @throws NullPointerException
	 *             if there is no tag on the given position
	 * @throws IllegalArgumentException
	 *             if the name of the tag on the given position does not match
	 *             the requested name
	 */
	public String getContent(int aPos, String aName) {
		TagClass child = getTagAt(aPos);
		if (child == null)
			throw new NullPointerException("There is no such a tag. [Parent tag name] = [" + aName + "], [child tag index] = " + aPos + ", [child tag name] = [" + aName + ']');

		if (child.getName().equals(aName))
			return child.getContent();

		throw new IllegalArgumentException("There is no such a tag. [Parent tag name] = [" + aName + "], [child tag index] = " + aPos + ", [child tag name] = [" + aName + ']');
	}

	/**
	 * Returns the internal content buffer
	 * 
	 * @return internal content buffer as CharBuffer
	 */
	protected CharBuffer getContentBuffer() {
		if (fContent == null) {
			fContent = new CharBuffer();
		}
		return fContent;
	}

	/**
	 * Returns name of the tag
	 * 
	 * @return tag name
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Sets the name of the tag
	 * 
	 * @param aName
	 *            name of the tag
	 */
	protected void setName(String aName) {
		fName = aName;
	}

	/**
	 * Returns string with all attribute names and values of the tag, as is in
	 * the XML content.
	 * 
	 * @return tag name and its attributes
	 */
	public String getAttributes() {
		return (fAttributes != null) ? fAttributes : "";
	}

	/**
	 * Returns an attribute value for an attribute with a given name (attrName)
	 * 
	 * @param attrName
	 *            the name of attribute
	 * @return Attribute value for attribute with name given by attrName or null
	 *         if there is no such an attribute
	 * @throws NullPointerException
	 *             if attrName is null
	 */
	public String getAttribute(String attrName) {
		return XMLUtil.getAttributeValue(fAttributes, attrName, 0);
	}

	/**
	 * Adds a child tag to this one
	 * 
	 * @param aTag
	 *            a tag to be added as a child tag
	 */
	public void addTag(TagClass aTag) {
		if (fTags == null) {
			fTags = new Vector(5, 5);
		}
		fTags.addElement(aTag);
	}

	/**
	 * Returns sub tag of this one at a given position
	 * 
	 * @param aPos
	 *            position of the searched tag
	 * @return a child tag at a given position in the child-tags set or null if
	 *         there is no such a tag
	 */
	public TagClass getTagAt(int aPos) {
		return (fTags == null) ? null : (TagClass) fTags.elementAt(aPos);
	}

	/**
	 * Returns children tags count
	 * 
	 * @return sub tags count
	 */
	public int size() {
		return (fTags == null) ? 0 : fTags.size();
	}

	/**
	 * Returns the line in the XML file where the tags begin.
	 * 
	 * @return Returns the line.
	 */
	public int getLine() {
		return fLine;
	}

	/**
	 * Sets the tag line number (the line in the XML file where the current tag
	 * starts)
	 * 
	 * @param aLine
	 *            current tag line number
	 */
	protected void setLine(int aLine) {
		fLine = aLine;
	}

	protected void setInline() {
		fInline = true;
	}
}
