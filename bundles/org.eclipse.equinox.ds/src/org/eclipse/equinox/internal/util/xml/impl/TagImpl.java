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
package org.eclipse.equinox.internal.util.xml.impl;

import java.io.*;
import java.util.*;
import org.eclipse.equinox.internal.util.string.CharBuffer;
import org.eclipse.equinox.internal.util.xml.Tag;

/**
 * @author Ivan Dimitrov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class TagImpl implements Tag {

	private CharBuffer fContent = null;
	private String fContentString = null;
	private String fName = null;
	private Hashtable fAttributes = null;
	private Vector fTags = null;
	private int fLine = -1;
	protected boolean fInline = false;

	/**
	 * A hashtable enumerator class for empty hash tables, specializes the
	 * general Enumerator
	 */
	private static Enumeration fEmptyEnumeration = new Enumeration() {
		public boolean hasMoreElements() {
			return false;
		}

		public Object nextElement() {
			throw new NoSuchElementException("Hashtable Enumerator");
		}
	};

	/**
	 * 
	 */
	public TagImpl() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getAttribute(java.lang.String)
	 */
	public String getAttribute(String aAttrName) {
		return (String) ((fAttributes != null) ? fAttributes.get(aAttrName) : null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getAttributes()
	 */
	public Enumeration getAttributeNames() {
		return (fAttributes != null) ? fAttributes.keys() : fEmptyEnumeration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getAttributeValues()
	 */
	public Enumeration getAttributeValues() {
		return (fAttributes != null) ? fAttributes.elements() : fEmptyEnumeration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getName()
	 */
	public String getName() {
		return fName;
	}

	/**
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getContent() note: The content that
	 *      is to be returned will be trimmed first
	 */
	public String getContent() {
		if (fContentString != null) {
			return fContentString;
		} else if (fContent != null) {
			fContentString = fContent.trim();
			fContent = null;
			return fContentString;
		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getLine()
	 */
	public int getLine() {
		return fLine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#size()
	 */
	public int size() {
		return (fTags != null) ? fTags.size() : 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getTagAt(int)
	 */
	public Tag getTagAt(int aPosition) {
		return (Tag) fTags.elementAt(aPosition);
	}

	protected void setName(String aName) {
		fName = aName;
	}

	protected void addAttribute(String aAttrName, String aAttrValue) {
		if (fAttributes == null) {
			fAttributes = new Hashtable(1);
		}
		fAttributes.put(aAttrName, aAttrValue);
	}

	protected void addTag(Tag aTag) {
		if (fTags == null) {
			fTags = new Vector(2, 3);
		}
		fTags.addElement(aTag);
	}

	protected void appendContent(CharBuffer toAppend) {
		if (fContent == null) {
			fContent = new CharBuffer(toAppend.length());
		}
		fContent.append(toAppend.getValue());
	}

	protected void appendContent(String toAppend) {
		if (fContent == null) {
			fContent = new CharBuffer(toAppend.length());
		}
		fContent.append(toAppend);
	}

	protected CharBuffer getContentBuffer() {
		if (fContent == null) {
			fContent = new CharBuffer(5);
		}
		return fContent;
	}

	protected void setLine(int aLine) {
		fLine = aLine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.Tag#getContent(int, java.lang.String)
	 */
	public String getContent(int aPos, String aName) {
		Tag child = getTagAt(aPos);
		if (child == null)
			throw new NullPointerException("There is no such a tag. [Parent tag name] = [" + aName + "], [child tag index] = " + aPos + ", [child tag name] = [" + aName + ']');

		if (child.getName().equals(aName))
			return child.getContent();

		throw new IllegalArgumentException("There is no such a tag. [Parent tag name] = [" + aName + "], [child tag index] = " + aPos + ", [child tag name] = [" + aName + ']');
	}

	/* begin serialization see FR [ 13325 ] XML: Make Tag externalizable */

	/**
	 * @see org.eclipse.equinox.internal.util.io.Externalizable#readObject(java.io.InputStream)
	 */
	public void readObject(InputStream is) throws Exception {
		if (!(is instanceof ObjectInput)) {
			is = new ObjectInputStream(is);
		}
		readExternal((ObjectInput) is);
	}

	/**
	 * @see org.eclipse.equinox.internal.util.io.Externalizable#writeObject(java.io.OutputStream)
	 */
	public void writeObject(OutputStream os) throws Exception {
		if (!(os instanceof ObjectOutput)) {
			os = new ObjectOutputStream(os);
		}
		writeExternal((ObjectOutput) os);
		os.flush();
	}

	/**
	 * @param input
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		fName = input.readUTF();
		/* read content */
		boolean hasContent = input.readBoolean();
		if (hasContent) {
			fContentString = input.readUTF();
		}
		int size;
		/* read attributes */
		size = input.readInt();
		if (size > 0) {
			fAttributes = new Hashtable(size, 1);
			for (int i = 0; i < size; i++) {
				String key = input.readUTF();
				String val = input.readUTF();
				fAttributes.put(key, val);
			}
		}
		/* read elements */
		size = input.readInt();
		if (size > 0) {
			fTags = new Vector(size);
			for (int i = 0; i < size; i++) {
				TagImpl tag = new TagImpl();
				tag.readExternal(input);
				fTags.addElement(tag);
			}
		}
	}

	/**
	 * @param output
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeUTF(fName);
		/* write content */
		String content = null;
		if (fContentString != null) {
			content = fContentString;
		} else if (fContent != null) {
			content = fContent.trim();
		}
		if (content != null) {
			output.writeBoolean(true);
			output.writeUTF(content);
		} else {
			output.writeBoolean(false);
		}
		/* write attributes */
		if (fAttributes != null) {
			output.writeInt(fAttributes.size());
			for (Enumeration e = fAttributes.keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				String val = (String) fAttributes.get(key);
				output.writeUTF(key);
				output.writeUTF(val);
			}
		} else {
			output.writeInt(0);
		}
		/* write elements */
		if (fTags != null) {
			output.writeInt(fTags.size());
			for (int i = 0, size = fTags.size(); i < size; i++) {
				TagImpl current = (TagImpl) fTags.elementAt(i);
				current.writeExternal(output);
			}
		} else {
			output.writeInt(0);
		}
		output.flush();
	}

	/* begin simple, utility method for debugging */

	/**
	 * Writes the tag as formatted XML into the given writer.
	 * 
	 * @param out
	 *            the output writer, where tag is serialized to XML
	 * @param level
	 *            the initial level. If set to negative value, the indent
	 *            /formatting/ of the XML is disable, and it is written on a
	 *            single line. Otherwise, the level is used to calculate the
	 *            offset from the left margin.
	 * @throws IOException
	 *             on I/O error
	 */
	public void writeXml(Writer out, int level) throws IOException {
		boolean indent = level >= 0;
		for (int i = 0; indent && i < level; i++)
			out.write(' '); /* indent */
		out.write('<');
		out.write(fName);
		if (fAttributes != null) {
			for (Enumeration e = fAttributes.keys(); e.hasMoreElements();) {
				out.write(' ');
				String key = (String) e.nextElement();
				String val = (String) fAttributes.get(key);
				out.write(key);
				out.write("=\"");
				out.write(val);
				out.write('"');
			}
		}
		/* don't use getContent() - this will demolish the buffer */
		String content = null;
		if (fContentString != null) {
			content = fContentString;
		} else if (fContent != null) {
			content = fContent.trim();
		}
		boolean isShort = content == null && fTags == null;
		if (isShort) {
			/* close immediately */
			out.write("/>");
			if (indent)
				out.write('\n');
		} else {
			out.write('>');
			if (indent)
				out.write('\n');
			/* write elements */
			for (int i = 0; fTags != null && i < fTags.size(); i++) {
				TagImpl child = (TagImpl) fTags.elementAt(i);
				child.writeXml(out, level < 0 ? -1 : (level + 1));
			}
			/* write contents */
			if (content != null) {
				for (int i = 0; indent && i < level + 1; i++)
					out.write(' '); /* indent */
				out.write(content);
				if (indent)
					out.write('\n');
			}
			/* write closing tag */
			for (int i = 0; indent && i < level; i++)
				out.write(' '); /* indent */
			out.write("</");
			out.write(fName);
			out.write('>');
			if (indent)
				out.write('\n');
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringWriter out = new StringWriter();
		try {
			writeXml(out, 0);
		} catch (IOException e) {
			/* this will never happen */
		}
		return out.toString();
	}

}
