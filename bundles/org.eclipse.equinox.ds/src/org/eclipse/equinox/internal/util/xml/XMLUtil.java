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

import org.eclipse.equinox.internal.util.string.CharBuffer;

/**
 *  Class with utility methods for extracting attributes from xml tags
 *  
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class XMLUtil {

	/**
	 * Coded chars placed in the xml content.
	 */
	public static String[] coded = {"&amp;", "&nbsp;", "&crlf;", "&tab;", "&lt;", "&gt;", "&quot;", "&apos;"};

	/**
	 * Corresponding decoded chars. First five of them cannot exist in plain
	 * (decoded) form in the xml content, but the rest can be eighter coded or
	 * decoded
	 */
	public static String[] decoded = {"&", " ", "\n", "\t", "<", ">", "\"", "'"};

	/**
	 * Replaces specified strings in a source string with corresponding strings
	 * 
	 * @param str
	 *            source string
	 * @param toBeReplaced
	 *            array of strings which will be replaced with the next
	 *            parameter
	 * @param replaceWith
	 *            array of the strings corresponding to "toBeReplaced"
	 *            parameter. the length should be equal
	 * @param length
	 *            specify how many parameters of the string array will be
	 *            replaced, the rest of "toBeReplaced" parameter will not.
	 * @return replaced string
	 */
	public static String replaceChars(String str, String[] toBeReplaced, String[] replaceWith, int length) {

		/*
		 * first 5 characters are not allowed to exists in such form in xml, the
		 * rest remains the same
		 */
		CharBuffer strBuf;
		for (int i = 0; i < length; i++) {
			strBuf = new CharBuffer();
			replace(str, toBeReplaced[i], replaceWith[i], strBuf);
			str = strBuf.toString();
		}
		return str;
	}

	/**
	 * Replaces a specified string in a source string with a corresponding
	 * string and adds the result in a buffer.
	 * 
	 * @param src
	 *            source string
	 * @param toBeReplaced
	 *            string which will be replaced with the next parameter
	 * @param toReplaceWith
	 *            the string corresponding to "toBeReplaced" parameter.
	 * @param strBuf
	 *            here is added the replaced source string.
	 */
	public static void replace(String src, String toBeReplaced, String toReplaceWith, CharBuffer strBuf) {

		int j;
		int pos = 0;
		int length = toBeReplaced.length();
		while (((j = src.indexOf(toBeReplaced, pos)) > -1)) {
			strBuf.append(src.substring(pos, j));
			strBuf.append(toReplaceWith);
			pos = j + length;
		}
		strBuf.append(src.substring(pos));
	}

	/**
	 * Converts special xml chars to valid string e.g. "&amp;lt;" becomes "<"
	 * and "&amp;gt;" becomes ">"
	 * 
	 * @param src
	 *            source string which is coded
	 * @return decoded string
	 */
	public static String getDecoded(String src) {
		int begin = src.indexOf('&');
		if (begin == -1) {
			/*
			 * improve speed when there is not any coded string (each coded
			 * string begins with '&')
			 */
			return src;
		}
		int end = src.indexOf(';', begin);
		if (end == -1) {
			/*
			 * improve speed when there is not any coded string (each coded
			 * string ends with ';')
			 */
			return src;
		}
		int lastBegin = 0;
		CharBuffer strBuf = new CharBuffer();
		while (begin != -1) {
			end = src.indexOf(';', begin); /*
													 * this is used on the second turn
													 * of the cycle
													 */
			if (end == -1) {
				strBuf.append(src.substring(begin));
				return strBuf.toString();
			}
			strBuf.append(src.substring(lastBegin, begin));
			String part = src.substring(begin, end + 1);
			boolean found = false;
			for (int i = 0; i < decoded.length; i++) {
				if (part.equals(coded[i])) {
					strBuf.append(decoded[i]);
					begin += part.length();
					lastBegin = begin;
					begin = src.indexOf('&', lastBegin);
					found = true;
					break; /*
											 * one word replaced, go on searching with rest of
											 * the src
											 */
				}
			}
			if (!found) {
				strBuf.append(part);
				begin += part.length();
				lastBegin = begin;
				begin = src.indexOf('&', begin);
			}
		}
		strBuf.append(src.substring(lastBegin));
		return strBuf.toString().intern();
	}

	/**
	 * Extracts the name of the tag, from string containing the name and
	 * attributes
	 * 
	 * @param tagAndAttributes
	 *            string containing the name and attributes
	 * @return tag name string separated by space from the tag attributes
	 * @deprecated use TagClass.getName() instead
	 */
	public static String getTagName(String tagAndAttributes) {
		int nameIndex = tagAndAttributes.indexOf(' ');
		if (nameIndex != -1) {
			int tabIndex = tagAndAttributes.indexOf('\t');
			if (tabIndex > -1 && tabIndex < nameIndex) {
				return tagAndAttributes.substring(0, tabIndex);
			}
			return tagAndAttributes.substring(0, nameIndex);
		}

		nameIndex = tagAndAttributes.indexOf('/');
		if (nameIndex != -1) {
			return tagAndAttributes.substring(0, nameIndex);
		}
		return tagAndAttributes;
	}

	public static boolean isEmptyTag(String tagName) {
		return '/' == tagName.charAt(tagName.length() - 1);
	}

	/**
	 * Checks whether the tag name string between &lt; and &gt;, ends with "/".
	 * 
	 * @param tagName
	 *            string between &lt; and &gt; chars
	 * @return true if and only if string ends with "/"
	 */

	protected static final String[] f_entities = {"amp", "nbsp", "crlf", "tab", "lt", "gt", "quot", "apos"};
	protected static final char[] f_ent_chars = {'&', ' ', '\n', '\t', '<', '>', '"', '\''};

	private static boolean substituteEntity(String ent, CharBuffer cb) {
		for (int j = 0; j < f_entities.length; j++) {
			if (f_entities[j].equals(ent)) {
				cb.append(f_ent_chars[j]);
				return true;
			}
		}
		return false;
	}

	/**
	 * Extracts an attribute value for a specified attribute name from tag
	 * string.
	 * 
	 * @param tag
	 *            whole tag name
	 * @param att
	 *            attribute name
	 * @return value of the attribute if exists otherwise return null
	 * @deprecated use TagClass.getAttrList() and TagClass.getAttribute(String
	 *             attr) instead
	 */
	public static String getAttributeValue(String tag, String att, int begin) {
		if (att.length() == 0)
			return null;

		int start = 0;
		while (start >= 0) {
			start = tag.indexOf(att, start);

			if (start >= 0) {
				// Checks if leading and trailing chars are valid attr_name
				// chars
				boolean b1 = (start > 0) ? XMLReader.isNameChar(tag.charAt(start - 1)) : true;
				boolean b2 = ((start + att.length() + 1) < tag.length()) ? XMLReader.isNameChar(tag.charAt(start + att.length())) : true;

				start += att.length();
				if (!b1 && !b2) {
					start = tag.indexOf('=', start) + 1;
					if (start <= 0)
						return null;

					while (start < tag.length() && Character.isWhitespace(tag.charAt(start))) {
						start++;
					}

					char quot = tag.charAt(start);
					int end = tag.indexOf(quot, start + 1);
					if ((start != -1) && (end > start)) {
						int pos = tag.indexOf('&', start + 1);
						if (pos < 0 || pos > end)
							return tag.substring(start + 1, end).intern();

						CharBuffer cb = new CharBuffer();

						char ch;
						for (int i = (start + 1); i < end; i++) {
							ch = tag.charAt(i);
							if (ch == '&') {
								pos = tag.indexOf(';', i);
								String ent = tag.substring(i + 1, pos);

								if (substituteEntity(ent, cb)) {
									i = pos;
									continue;
								}

								cb.append('&');
								cb.append(ent);
								cb.append(';');
								i = pos;
								continue;
							}
							cb.append(ch);
						}

						return cb.toString().intern();
					}
					start = end;
				}
			}
		}
		return null;
	}

	/**
	 * Extracts attribute names from tag string.
	 * 
	 * @param tag
	 *            whole tag name
	 * @return A vector with all attribute names
	 * @deprecated use TagClass.getAttrList() instead
	 */
	public static Vector getAttributeNames(String tag) {
		Vector names = new Vector();
		int start = tag.indexOf(' '); /* avoid name of this tag */
		int end;
		while (start != -1) {
			end = tag.indexOf('=', start + 1);
			if (end == -1) {
				break;
			}
			names.addElement(tag.substring(start + 1, end));
			start = tag.indexOf('"', end + 1);
			if (start == -1) {
				break;
			}
			start = tag.indexOf('"', start + 1);
			if (start == -1) {
				break;
			}
			start = tag.indexOf(' ', start + 1);
		}
		return names;
	}

	/**
	 * Extracts attribute values from tag string.
	 * 
	 * @param tag
	 *            whole tag name
	 * @return A vector with all attribute values
	 * @deprecated use TagClass.getAttrList() instead
	 */
	public static Vector getAttributeValues(String tag) {
		Vector values = new Vector();
		int start = tag.indexOf('=');
		int end;
		while (start != -1) {
			start = tag.indexOf('"', start + 1);
			end = tag.indexOf('"', start + 1);
			if ((start == -1) || (end == -1)) {
				break;
			}
			values.addElement(tag.substring(start + 1, end).intern());
			start = tag.indexOf('=', end + 1);
		}
		return values;
	}

	/**
	 * Returns content of the specified subtag.
	 * 
	 * @param tag
	 *            parent tag
	 * @param pos
	 *            position of the searched tag in child array of the parent tag
	 * @param name
	 *            name of the searched tag
	 * @return content of the specified subtag
	 * @exception IllegalArgumentException
	 *                if the specified name does not match the name of the tag
	 *                at specified position
	 * @deprecated use TagClass.getContent(int pos, String name) instead
	 */

	public static String getContent(TagClass tag, int pos, String name) throws IllegalArgumentException {
		TagClass subTag = tag.getTagAt(pos);
		if (subTag.getName().equals(name)) {
			return subTag.getContent();
		}
		throw new IllegalArgumentException("Missing subtag " + name + " in " + tag.getName());
	}
}
