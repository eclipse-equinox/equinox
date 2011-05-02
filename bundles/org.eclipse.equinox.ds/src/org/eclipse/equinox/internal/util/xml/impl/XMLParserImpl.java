/*******************************************************************************
 * Copyright (c) 1997, 2010 by ProSyst Software GmbH
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
import org.eclipse.equinox.internal.ds.Activator;
import org.eclipse.equinox.internal.util.string.CharBuffer;
import org.eclipse.equinox.internal.util.xml.ExTagListener;

/**
 * <p>
 * Class used for reading of xml files, creating tree structure of 'TagImpl' for
 * each xml tag. When reader reaches a closed tag it notifies a given
 * 'TagListener' and sends the last tag to it. If closing tag does not
 * correspond with the last open IllegalArgumentException is thrown. There is a
 * debug property 'xml.debug' used to dump an Exceptions occurred while
 * operation is running.
 * </p>
 * 
 * <p>
 * The parser, in general, is a simple XML parser that implements
 * &quot;Recursive descent&quot; parsing method.
 * 
 * Known limitations:<br>
 * 
 * <pre>
 *  Currently this XMLParser does not support the following special tags:
 *  1. &lt;?TAG_NAME ..... ?&gt;   or also &quot;Processing Instructions&quot;
 *  2. &lt;!DOCTYPE .... &gt;
 *  3. &lt;!ELEMENT .... &gt;
 *  4. &lt;!ATTLIST .... &gt;
 *  5. &lt;!ENTITY .... &gt;
 * </pre>
 * 
 * <br>
 * The parser skips these tags (it searches for '>' symbol and closes the
 * 'special' tag).<br>
 * 
 * @author Ivan Dimitrov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class XMLParserImpl {

	private static final String DEBUG = "xml.debug";
	private static final String INTERN_ATTRIBUTES = "xml.intern.attributes";

	private static final String CDATA = "CDATA";
	private static final String XML = "xml";
	private static final String VERSION = "version";
	private static final String ENCODING = "encoding";
	private static final String STANDALONE = "standalone";

	private static final String ERR_EOS = "End-of-stream reached before the end of XML.";
	private static final String ERR_ENTITY_EXPECTED = "Entity reference or Character reference expected.";
	private static final String ERR_EQUAL_EXPECTED = "'=' expected.";
	private static final String ERR_QUOT_EXPECTED = "''' or '\"' expected.";
	private static final String ERR_GT_EXPECTED = "'>' expected.";
	private static final String ERR_LT_EXPECTED = "'<' expected.";
	private static final String ERR_CLOSE_TAG1_EXPECTED = "'/' or tag name expected.";
	private static final String ERR_CLOSE_TAG2_EXPECTED = "'>', '/>' or more attributes expected.";
	private static final String ERR_CLOSE_TAG3_EXPECTED = "'?>' expected.";
	private static final String ERR_CONTENT_EXPECTED = "Content data, new tag or closing tag expected.";
	private static final String ERR_QUESTIONMARK_EXPECTED = "'?' expected.";
	private static final String ERR_ILLEGAL_CHARACTER = "Illegal character.";
	private static final String ERR_TAGNAME_EXPECTED = "Tag name expected.";
	private static final String ERR_TAGNAME2_EXPECTED = "Tag name, '?' or '!' expected.";
	private static final String ERR_DASH_EXPECTED = "'-' expected.";
	private static final String ERR_COMMENT_CLOSE_EXPECTED = "'-->' expected.";
	private static final String ERR_CDATA_EXPECTED = "'CDATA' expected.";
	private static final String ERR_OPENSQBRACKET_EXPECTED = "'[' expected.";
	private static final String ERR_CLOSE_CDATA_EXPECTED = "']]>' expected.";
	private static final String ERR_SEMICOLON_EXPECTED = "';' expected.";
	private static final String ERR_XMLPROLOG_EXPECTED = "XML prolog '<?xml' is not expected at this position.";
	private static final String ERR_VERSION_EXPECTED = "'version' attribute expected.";
	private static final String ERR_ENCODING_STANDALONE_EXPECTED = "'encoding', 'standalone' or '?>' expected.";
	private static final String ERR_STANDALONE_EXPECTED = "'standalone' attribute expected.";

	protected static final boolean fDebug = Activator.getBoolean(DEBUG);
	protected static final boolean fInternAttributes = Activator.getBoolean(INTERN_ATTRIBUTES);

	private String fDefaultEncoding = "UTF-8";

	// private CharBuffer c;
	private CharBuffer temp = new CharBuffer();
	private CharBuffer temp2 = null;

	protected Reader fReader = null;
	protected InputStream fStream = null;

	protected char currentChar = 0;
	protected ExTagListener fExTagListener;

	protected int fLine = 1;
	protected int fPos = 0;

	protected int fLevel = -1;
	protected int fCurrentLevel = 1;

	private String fVersion = "1.0";
	private String fEncoding = "UTF-8";
	private String fStandalone = "yes";

	/**
	 * An empty default constructor
	 * 
	 */
	public XMLParserImpl() {
		//
	}

	/**
	 * Constructs a new XMLReader. <br>
	 * <br>
	 * <b>Note: The XMLReader does not close the passed Reader or InputStream
	 * 
	 * @param aInputStream
	 *            an InputStream to read the XML file from
	 * @param aListener
	 *            ExTagListener that will be notified on tag-open or tag-close
	 *            events
	 * @throws IOException
	 */
	public XMLParserImpl(InputStream aInputStream, ExTagListener aListener) {
		fStream = aInputStream;
		fExTagListener = aListener;
	}

	/**
	 * Constructs a new XMLReader. <br>
	 * <br>
	 * <b>Note: The XMLReader does not close the passed Reader or InputStream
	 * 
	 * @param aReader
	 *            a Reader to read the XML file from
	 * @param aListener
	 *            ExTagListener that will be notified on tag-open or tag-close
	 *            events
	 */
	public XMLParserImpl(Reader aReader, ExTagListener aListener) {
		fReader = aReader;
		fExTagListener = aListener;
	}

	/**
	 * Sets the level of tags bellow which the listener will be notified for.
	 * For internal use only.
	 * 
	 * @param aLevel
	 */
	public void setLevel(int aLevel) {
		fLevel = aLevel;
	}

	/**
	 * Sets the parser's encoding. If there is a current encoding associated
	 * with the parser the method returns immediately
	 * 
	 * @param aEncoding
	 *            new encoding to be set
	 * @throws UnsupportedEncodingException
	 *             if the encoding is not supported.
	 */
	protected void setEncoding(String aEncoding) {
		if (fReader == null) {
			try {
				fReader = new InputStreamReader(fStream, aEncoding);
			} catch (Exception e) {
				if (fDebug) {
					System.err.println("[XMLParserImpl] Failed setting the encoding \"" + aEncoding + "\", continue parsing with the default one.");
				}
				fReader = new InputStreamReader(fStream);
			}
		}
	}

	/* A helper function to reuse a temp CharBuffers without recreating it */
	protected CharBuffer getCharBuffer() {
		if (temp.length() <= 0) {
			return temp;
		} else if (temp2 == null) {
			temp2 = new CharBuffer(0);
			return temp2;
		} else if (temp2.length() <= 0) {
			return temp2;
		}
		return new CharBuffer(0);
	}

	protected char prev_char = 0;
	private char[] fBuffer = new char[4096];
	private int fBufferLen = 0;
	private int fBufferPos = 0;

	/**
	 * Reads the next char from the input stream and sets it to private field
	 * <code>currentChar</code>
	 * 
	 * @return true if the next char is successfully read of false if
	 *         End-Of-Stream is reached
	 * @throws IOException
	 *             if some error occurs during reading the character or if the
	 *             caller tries to read beyond the End-Of-Stream.
	 */
	protected boolean getNextChar() throws IOException {
		// // Reading characters without buffering
		// int ichar = 0;
		// int count = 0;
		// while (ichar == 0 && count < 100) {
		// ichar = fReader.read();
		// count++;
		// }
		//    
		// if (ichar == 0)
		// throw new IOException("Failed to read from the input file.");
		//    
		// if (ichar < 0 && prev_char == 0)
		// throw new IOException(ERR_EOS);
		//    
		// char ch = (char) ichar;

		char ch;

		if (fReader == null) { // If there is no associated reader,
			int ach = fStream.read(); // then reads from the InputStream until
			if (ach < 0) { // the rigth encoding is recognized
				ach = 0;
			}

			ch = (char) ach;
			if (ch == 0 && prev_char == 0) {
				throw new IOException(ERR_EOS);
			}
		} else {
			// Buffered reading
			if (fBufferLen < 0) {
				throw new IOException(ERR_EOS);
			}

			if ((fBufferPos) >= fBufferLen) {
				// Refetch the buffer
				fBufferLen = 0;
				fBufferPos = 0;
				int count = 0;
				while (fBufferLen == 0 && count < 100) {
					fBufferLen = fReader.read(fBuffer);
					count++;
				}

				ch = (fBufferLen > 0) ? fBuffer[fBufferPos++] : 0;

				if (fBufferLen == 0) {
					fBufferLen = -1;
				}
			} else {
				ch = fBuffer[fBufferPos++];
			}
		}

		prev_char = currentChar;
		currentChar = ch;
		fPos++;

		switch (ch) {
			case '\n' :
				if (prev_char != '\r') {
					fLine++;
				}
				fPos = 0;
				break;
			case '\r' :
				fPos = 0;
				fLine++;
				break;
		}
		return (currentChar != 0);
	}

	/**
	 * Parses the attribute value and if it's successful then adds it to the
	 * CharBuffer. If there are EntityReferences of CharReferences in the
	 * attribute value, they will be turned to their equivalent symbols.<br>
	 * attr_value ::= (acceptable_char | EntityRef | CharRef)* - quot_symbol
	 * 
	 * @see parse_attr
	 * @see parse_CharRef
	 * @see parse_EntityRef
	 */
	protected void parse_attr_value(CharBuffer sb, char quot) throws IOException {
		while (currentChar != quot && currentChar != '<') {
			if (accept_char('&')) {
				if (!parse_CharRef(sb)) {
					if (!parse_EntityRef(sb)) {
						err(fPos - 1, ERR_ENTITY_EXPECTED);
					}
				}
			} else {
				sb.append(currentChar);

				if (!getNextChar()) {
					break;
				}
			}
		}
	}

	/**
	 * Parses an attribute with the given simplified grammar:<br>
	 * 
	 * <pre>
	 * attribute ::= S* + attr_name + S* + '=' + S* + ('\'' + (attr_value - '\'') + '\'')) | ('&quot;' + (attr_value - '&quot;') + '&quot;'))
	 * attr_value ::= (acceptable_char | EntityRef | CharRef)*
	 * attr_name ::= identifier
	 * </pre>
	 * 
	 * @param aParent
	 *            the parent tag where the correctly parsed attribute will be
	 *            added
	 * @throws IOException
	 * @see parse_identifier
	 * @see parse_attr_value
	 */
	protected boolean parse_attr(TagImpl aParent) throws IOException {
		clearWhiteSpaces();

		String attrName = parse_identifier();

		if (attrName != null) {
			clearWhiteSpaces();

			if (!accept_char('=')) {
				err(ERR_EQUAL_EXPECTED);
			}

			clearWhiteSpaces();

			char quot = 0;
			if (accept_char('"')) {
				quot = '"';
			} else if (accept_char('\'')) {
				quot = '\'';
			} else {
				err(ERR_QUOT_EXPECTED);
			}

			CharBuffer sb = getCharBuffer();
			parse_attr_value(sb, quot);

			if (!accept_char(quot)) {
				err("'" + quot + "' expected.");
			}

			String attrValue = sb.toString();

			if (fInternAttributes) {
				attrValue = attrValue.intern();
			}

			aParent.addAttribute(attrName, attrValue);
			sb.setLength(0);
			return true;
		}
		return false;
	}

	/**
	 * Parses a tag attribute list with the following simplified grammar:
	 * 
	 * <pre>
	 * attr_list ::= attribute*
	 * @param aParent the parent tag that the parsed attributes will be added to
	 * @return true if at least one attribute is parsed correctly and false otherwise
	 * @throws IOException
	 * @see parse_attr
	 * 
	 */
	protected boolean parse_attr_list(TagImpl aParent) throws IOException {
		boolean result = false;
		while (parse_attr(aParent)) {
			result = true;
		}
		return result;
	}

	private static final char bA = 'A' - 1;
	private static final char aZ = 'Z' + 1;
	private static final char ba = 'a' - 1;
	private static final char az = 'z' + 1;
	private static final char b0 = '0' - 1;
	private static final char a9 = '9' + 1;

	/**
	 * This method returns true is the passed character may be used as starting
	 * character for tag name and attribute name
	 * 
	 * @param ch
	 *            the tested character
	 * @return true if the character could be used as starting character for a
	 *         tag name and an attribute name and false otherwise
	 */
	protected final boolean isNameStartChar(char ch) {
		return (ch > bA && ch < aZ) || (ch > ba && ch < az) || (ch == ':') || (ch == '_') || (ch > 0xBF && ch < 0xD7) || (ch > 0xD7 && ch < 0xF7) || (ch > 0xF7 && ch < 0x300) || (ch > 0x36F && ch < 0x37E) || (ch > 0x37E && ch < 0x2000) || (ch > 0x200B && ch < 0x200E) || (ch > 0x206F && ch < 0x2190) || (ch > 0x2BFF && ch < 0x2FF0) || (ch > 0x3000 && ch < 0xD800) || (ch > 0xF900 && ch < 0xFDD0) || (ch > 0xFDEF && ch < 0xFFFE) || (ch > 0x0FFFF && ch < 0xF0000);
	}

	/**
	 * This method returns true if the passed characted may be used as part of a
	 * tag name or an attribute name
	 * 
	 * @param ch
	 *            the tested character
	 * @return true is the characted could be used as part of a tag name or an
	 *         attribute name and false otherwise
	 */
	protected final boolean isNameChar(char ch) {
		return (ch == '-') || (ch == '.') || (ch == 0xB7) || (ch > b0 && ch < a9) || isNameStartChar(ch) || (ch > 0x02FF && ch < 0x0370) || (ch > 0x203E && ch < 0x2041);
	}

	/**
	 * Parses an identifier.
	 * 
	 * @return an identifier if it is parsed successfully and null otherwise
	 * @throws IOException
	 */
	protected String parse_identifier() throws IOException {
		if (isNameStartChar(currentChar)) {
			CharBuffer sb = getCharBuffer();

			while (isNameChar(currentChar)) {
				sb.append(currentChar);

				if (!getNextChar()) {
					break;
				}
			}
			String result = sb.toString().intern();
			sb.setLength(0);
			return result;
		}
		return null;
	}

	/**
	 * Parses a tag name and if it is successfully parsed the method sets it as
	 * a name of the parent tag
	 * 
	 * @param aParent
	 *            parent tag
	 * @return true if the name is parsed successfully and false otherwise
	 * @throws IOException
	 * @see parse_identifier
	 */
	protected boolean parse_tag_name(TagImpl aParent) throws IOException {
		String name = parse_identifier();
		if (name != null) {
			aParent.setName(name);
		}
		return name != null;
	}

	/**
	 * Helper function that notify listeners depending on certain conditions
	 * such as if the tag event is on-close or on-open
	 * 
	 * @param aTag
	 *            The tag that the notification event is valid for.
	 * @param isStart
	 *            true if the event is on-open and false if it is on-close
	 */
	protected void notifyListeners(TagImpl aTag, boolean isStart) {
		try {
			if (isStart) {
				fExTagListener.startTag(aTag);
			} else {
				fExTagListener.endTag(aTag);
			}
		} catch (RuntimeException re) {
			if (fDebug) {
				System.err.println("An outside exception occurred while processing a tag on line " + aTag.getLine() + ", the tag name is: " + aTag.getName());
				re.printStackTrace(System.err);
			}
			throw re;
		}
	}

	/**
	 * Parses a normal tag. There are two cases - (1) the tag has separate open
	 * and close tag elements and (2) the tag is simple suchas &lt;tag_name ...
	 * /&gt;
	 * 
	 * @param aParent
	 *            The parent tag that this tag will be added to if the parsing
	 *            is successful
	 * @return true on success and false otherwise
	 * @throws IOException
	 * @see clearWhiteSpaces
	 * @see parse_tag_name
	 * @see parse_attr_list
	 * @see notifyListeners
	 * @see accept_char
	 * @see accept_seq
	 * @see parse_PCDATA
	 */
	protected boolean parse_tag_normal(TagImpl aParent) throws IOException {
		// Looking for a tag_name (identifier)
		if (isNameStartChar(currentChar)) {
			TagImpl tag = new TagImpl();
			tag.setLine(fLine);

			parse_tag_name(tag);
			parse_attr_list(tag);

			clearWhiteSpaces();

			if (accept_char('/')) {
				if (!accept_char('>')) {
					err(ERR_GT_EXPECTED);
				}
				aParent.addTag(tag);

				if ((fLevel <= 0 || fLevel == fCurrentLevel)) {
					notifyListeners(tag, true);
					notifyListeners(tag, false);
				}

				return true;
			} else if (accept_char('>')) {

				notifyListeners(tag, true);

				while (true) {
					clearWhiteSpaces();
					int pos = fPos;
					if (currentChar == '<') { // Normal tag, Special tag or
						// closing tag
						if (!parse_tag(tag)) { // It may be a special tag.
							if (!accept_char('/')) {
								err(pos + 1, ERR_CLOSE_TAG1_EXPECTED);
							}

							// trying to accept: tag_name + S* + '>'
							pos = fPos;
							if (!accept_seq(tag.getName())) {
								err(pos, '\'' + tag.getName() + "' string expected.");
							}

							clearWhiteSpaces();
							if (!accept_char('>')) {
								err(ERR_GT_EXPECTED);
							}

							aParent.addTag(tag);

							if (fLevel <= 0 || fLevel == fCurrentLevel) {
								notifyListeners(tag, false);
							}

							return true;
						}
					} else {
						if (!parse_PCDATA(tag)) {
							break;
						}
					}
				}
				err(ERR_CONTENT_EXPECTED);
			} else {
				err(ERR_CLOSE_TAG2_EXPECTED);
			}
		}
		return false;
	}

	/**
	 * Returns true if the specified attribute is already parsed and false
	 * otherwise
	 * 
	 * @param aTag
	 *            the which attribute will be examined.
	 * @param attrName
	 *            an attribute name
	 * @return true if the specified attribute is already parsed and false
	 *         otherwise
	 */
	protected boolean accept_attr(TagImpl aTag, String attrName) {
		return aTag.getAttribute(attrName) != null;
	}

	/**
	 * Parses the XML prolog tag, i.e.<br>
	 * <code> &lt;?xml version="..." encoding="..." standalone="..." ?&gt; </code><br>
	 * 
	 * @param parent
	 *            the parent tag (in this case this is the root "fake" tag,
	 *            which the listeners will never be informed for...)
	 * @throws IOException
	 *             if an exception occurs during read operations from the Reader
	 *             or the InputStream
	 */
	protected boolean parse_xml_prolog(TagImpl parent) throws IOException {
		if (accept_char('?')) {
			TagImpl tag = new TagImpl();

			if (parse_tag_name(tag)) {
				if (tag.getName().equalsIgnoreCase(XML)) {
					clearWhiteSpaces();

					int pos = fPos;

					if (parse_attr(tag)) {
						String s = tag.getAttribute(VERSION);

						if (s == null) {
							err(pos, ERR_VERSION_EXPECTED);
						}

						fVersion = s;

						clearWhiteSpaces();
						pos = fPos;
						if (parse_attr(tag)) {
							clearWhiteSpaces();

							if (accept_attr(tag, ENCODING)) {
								fEncoding = tag.getAttribute(ENCODING);

								pos = fPos;
								if (parse_attr(tag)) {
									clearWhiteSpaces();
									if (accept_attr(tag, STANDALONE)) {
										fStandalone = tag.getAttribute(STANDALONE);
									} else {
										err(pos, ERR_STANDALONE_EXPECTED);
									}
								}
							} else if (accept_attr(tag, STANDALONE)) {
								fStandalone = tag.getAttribute(STANDALONE);
							} else {
								err(pos, ERR_ENCODING_STANDALONE_EXPECTED);
							}
						}

						clearWhiteSpaces();
						pos = fPos;
						if (!accept_seq("?>")) {
							err(pos, ERR_CLOSE_TAG3_EXPECTED);
						}
					} else {
						err(pos, ERR_VERSION_EXPECTED);
					}
					clearWhiteSpaces();
					return true;
				}

				char prevCh = 0;

				while (true) {
					if (currentChar == '>') {
						if (prevCh == '?') {
							accept_char('>');
							clearWhiteSpaces();

							return true;
						}
						err(ERR_QUESTIONMARK_EXPECTED);
					} else if (currentChar == '<') {
						err(ERR_ILLEGAL_CHARACTER + " ('<')");
					}
					prevCh = currentChar;
					getNextChar();
				}
			}
		}
		return false;
	}

	/**
	 * Parses special tags, such that begins with:<br>
	 * 
	 * <pre><code>
	 * &lt;!--         comments
	 * &lt;!tag_name   Parsing instructions
	 * &lt;![          CDATA element
	 * &lt;?           DOCTYPE, etc.
	 * </code></pre>
	 * 
	 * @param aParent
	 *            The parent tag that this tag will be added to if the parsing
	 *            is successful
	 * @return true on success and false otherwise
	 * @throws IOException
	 * @see accept_char
	 * @see clearWhiteSpaces
	 * @see parse_tag_CDATA
	 * @see parse_tag_name
	 * @see parse_comment
	 */
	protected boolean parse_tag_special(TagImpl aParent) throws IOException {
		if (accept_char('!')) {

			TagImpl tag = new TagImpl();

			if (parse_tag_name(tag)) {
				clearWhiteSpaces();

				while (true) {
					if (accept_char('>')) {
						clearWhiteSpaces();
						return true;
					}
					getNextChar();
				}
			} else if (parse_tag_CDATA(aParent)) { // parse CDATA tag
				return true;
			} else if (parse_comment(tag)) {
				return true;
			}
		} else if (accept_char('?')) {
			TagImpl tag = new TagImpl();

			int pos = fPos;
			if (parse_tag_name(tag)) {
				if (tag.getName().equals(XML)) {
					err(pos - 2, ERR_XMLPROLOG_EXPECTED);
				}

				char prevCh = 0;
				while (true) {
					if (currentChar == '>') {
						if (prevCh == '?') {
							accept_char('>');
							clearWhiteSpaces();
							return true;
						}
					}
					prevCh = currentChar;
					getNextChar();
				}

			} else {
				err(pos, ERR_TAGNAME_EXPECTED);
			}
		}
		return false;
	}

	/**
	 * Parses a comment. The grammar is:<br>
	 * Comment ::= '&lt;!--' ((Char - '-') | ('-' (Char - '-')))* '--&gt;'<br>
	 * Note that the grammar does not allow a comment ending in ---&gt;. The
	 * following example is not well-formed.<br>
	 * <code>
	 * &lt;!-- B+, B, or B---&gt;</code>
	 * 
	 * @param aParent
	 *            The parent tag
	 * @return true on success and false otherwise
	 * @throws IOException
	 * @see accept_char
	 */
	protected boolean parse_comment(TagImpl aParent) throws IOException {
		if (accept_char('-')) {
			if (!accept_char('-')) {
				err(ERR_DASH_EXPECTED);
			}

			while (true) {
				if (accept_char('-')) {
					if (accept_char('-')) {
						if (accept_char('>')) {
							break;
						}
						err(ERR_GT_EXPECTED);
					}
				}

				if (!getNextChar()) {
					err(ERR_COMMENT_CLOSE_EXPECTED);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Parses either normal or special tag
	 * 
	 * @param aParent
	 *            The parent tag that the successfully parsed tag will (if it is
	 *            normal tag or CDATA element) be added
	 * @return true on success and false otherwise
	 * @throws IOException
	 * @see accept_cahr
	 * @see parse_tag_normal
	 * @see parse_tag_special
	 * @see clearWhiteSpaces
	 */
	protected boolean parse_tag(TagImpl aParent) throws IOException {
		clearWhiteSpaces();
		try {
			fCurrentLevel++;

			if (accept_char('<')) {
				if (parse_tag_normal(aParent) || parse_tag_special(aParent)) {
					return true;
				}
			}
			return false;
		} finally {
			fCurrentLevel--;
		}
	}

	/**
	 * Parses the content of the tag (including sub-tags and sub-elements)
	 * 
	 * @param aParent
	 *            The parent tag that the content and tags will be added to
	 * @return true on success and false otherwise
	 * @throws IOException
	 * @see parse_PCDATA
	 * @see parse_tag
	 */
	protected boolean parse_content(TagImpl aParent) throws IOException {
		return (parse_PCDATA(aParent) || parse_tag(aParent));
	}

	/**
	 * Parses a CDATA tag (or CDATA content element).
	 * 
	 * @param aParent
	 *            The parent tag that the content will be added to
	 * @return true on success and false otherwise
	 * @throws IOException
	 */
	protected boolean parse_tag_CDATA(TagImpl aParent) throws IOException {
		if (accept_char('[')) {
			int pos = fPos;

			if (!accept_seq(CDATA)) {
				err(pos, ERR_CDATA_EXPECTED);
			}

			if (!accept_char('[')) {
				err(ERR_OPENSQBRACKET_EXPECTED);
			}

			do {
				if (currentChar != '>') {
					aParent.getContentBuffer().append(currentChar);
				} else {
					CharBuffer sb = aParent.getContentBuffer();
					int l = sb.length();

					if (l >= 2) {
						if (sb.charAt(l - 1) == ']' && sb.charAt(l - 2) == ']') {
							sb.setLength(l - 2); // Truncates the extra "]]"
							// symbols appended at the
							// end

							getNextChar();
							return true;
						}
					}
					sb.append(currentChar);
				}
			} while (getNextChar());

			err(fPos - 1, ERR_CLOSE_CDATA_EXPECTED);
		}
		return false;
	}

	/**
	 * Parses PCDATA content (Parseable Content DATA). The EntityRefs and
	 * CharRefs that are parsed will be turned to its symbol equivalent.
	 * 
	 * @param aParent
	 *            The parent tag that the PCDATA will be added to
	 * @return true on success and false otherwise
	 * @throws IOException
	 * @see accept_char
	 * @see parse_CharRef
	 * @see parse_EntityRef
	 */
	protected boolean parse_PCDATA(TagImpl aParent) throws IOException {
		boolean result = false;
		while (currentChar != '<') {
			result = true;

			CharBuffer sbContent = aParent.getContentBuffer();

			if (accept_char('&')) {
				int pos = fPos;
				if (!parse_CharRef(sbContent)) {
					if (!parse_EntityRef(sbContent)) {
						err(pos - 1, ERR_ENTITY_EXPECTED);
					}
				}
			} else {
				sbContent.append(currentChar);

				if (!getNextChar()) {
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Accepts one character from the input stream and if it's successful moves
	 * one character forward.
	 * 
	 * @param ch
	 *            The character that should be accepted
	 * @return true on success and false otherwise
	 * @throws IOException
	 */
	protected boolean accept_char(char ch) throws IOException {
		if (currentChar == ch) {
			getNextChar();
			return true;
		}
		return false;
	}

	/**
	 * Accepts a sequence of characters given by seq parameter. If the sequence
	 * is accepted successfully then the currentChar field will contain the
	 * character immediately after the accepted sequence.
	 * 
	 * @param seq
	 *            The character sequence that should be accepted
	 * @return true on success and false otherwise
	 * @throws IOException
	 */
	protected boolean accept_seq(String seq) throws IOException {
		for (int i = 0; i < seq.length(); i++) {
			if (!accept_char(seq.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	protected static final String[] entities = {"amp", "apos", "lt", "gt", "quot"};
	protected static final char[] ent_chars = {'&', '\'', '<', '>', '"'};

	/**
	 * <code>
	 * EntityRef ::= '&' + EntityValue + ';'<br>
	 * EntityValue ::= 'amp' | 'quot' | 'apos' | 'gt' | 'lt' | identifier
	 * </code>
	 * 
	 * @param sb
	 *            The string buffer that the recognized entity will be appended
	 *            to
	 * @throws IOException
	 * @return true on success and false otherwise
	 * @see parse_identifier
	 * @see accept_char
	 */
	protected boolean parse_EntityRef(CharBuffer sb) throws IOException {
		String ent = parse_identifier();

		if (!accept_char(';')) {
			err(ERR_SEMICOLON_EXPECTED);
		}

		int length = entities.length;
		for (int i = 0; i < length; i++) {
			if (entities[i] == ent) { // 'ent' is interned by
				// parse_identifier() function
				sb.append(ent_chars[i]);
				return true;
			}
		}

		sb.append('&');

		if (ent != null && ent.length() > 0) {
			sb.append(ent);
		}

		sb.append(';');

		return true;
	}

	/**
	 * Parses a CharReference and if it is successful then appends it to the
	 * passed CharBuffer
	 * 
	 * @param sb
	 *            CharBuffer that the parsed CharReference will be added to
	 * @return true on success and false otherwise
	 * @throws IOException
	 */
	protected boolean parse_CharRef(CharBuffer sb) throws IOException {
		if (accept_char('#')) {
			// TODO - Postponed...
			while (currentChar != ';') {
				getNextChar();
			}

			if (!accept_char(';')) {
				err(fPos - 1, ERR_SEMICOLON_EXPECTED);
			}

			return true;
		}
		return false;
	}

	/**
	 * Clears the white spaces starting from the current position
	 * 
	 * @throws IOException
	 */
	protected void clearWhiteSpaces() throws IOException {
		while (Character.isWhitespace(currentChar)) {
			if (!getNextChar()) {
				break;
			}
		}
	}

	/**
	 * Throws an IOException with a given message. The current line number and
	 * line position are appended to the error message
	 * 
	 * @param message
	 *            The message of the exception
	 * @throws IOException
	 */
	protected void err(String message) throws IOException {
		err(fPos, message);
	}

	/**
	 * Throws an IOException with the given message for the given line position.
	 * The current line number and position (pos) are appended to the exception
	 * message
	 * 
	 * @param pos
	 *            The line position that the error will be reported for
	 * @param message
	 * @throws IOException
	 */
	protected void err(int pos, String message) throws IOException {
		throw new IOException("[Line: " + fLine + ", Pos: " + pos + "]  " + message);
	}

	/**
	 * Initiates parsing of the XML file given through aInputStream or aReader
	 * in the given constructor when creating XMLReader object.
	 * 
	 * @throws IOException
	 *             if an error occurs during reading the XML file or if a
	 *             parsing error eccurs.
	 */
	public void parseXML() throws IOException {
		TagImpl rootTag = new TagImpl();

		try {
			getNextChar();
			clearWhiteSpaces();

			boolean start = false;

			while (accept_char('<')) {
				start = true;
				int pos = fPos;

				if (fPos == 2 && fLine == 1) {
					if (parse_xml_prolog(rootTag)) {
						// System.out.println("XML Prolog found.");
						// System.out.println("XML Version: " + fVersion + ",
						// encoding: " + fEncoding);
						setEncoding(fEncoding);
						clearWhiteSpaces();
						continue;
					}
				} else {
					setEncoding(fDefaultEncoding);
				}

				if (!parse_tag_special(rootTag)) {
					if (parse_tag_normal(rootTag)) {
						// TODO da se proveri dali e dostignat kraja na file-a,
						// ako ne e - togava ot tuk natatuk moje da ima samo
						// komentari.
						return;
					}
					err(pos, ERR_TAGNAME2_EXPECTED);
				}

				clearWhiteSpaces();
			}

			if (!start) {
				err(ERR_LT_EXPECTED);
			}
		} catch (IOException ioe) {
			if (fDebug) {
				ioe.printStackTrace(System.err);
			}

			throw ioe;
		}
	}

	/**
	 * Parses a XML file given through aInputStream and during the parsing
	 * notifies aListener for close-tag and open-tag events <br>
	 * <br>
	 * <b>Note: The XMLReader does not close the passed Reader or InputStream
	 * 
	 * @param aInputStream
	 *            an InputStream to read the XML file from
	 * @param aListener
	 *            ExTagListener that will be notified on close-tag and open-tag
	 *            events
	 * @param aLevel
	 *            indicates the tag level that the listener will be invoked for.
	 *            For example if the XML is:<br>
	 * 
	 * <pre>
	 *  &lt;a&gt;
	 *    &lt;b&gt;
	 *      &lt;c /&gt;
	 *    &lt;/b&gt;
	 *  &lt;/a&gt;
	 * </pre>
	 * 
	 * <br>
	 *            and the passed aLevel is 2 then the listener will be invoked
	 *            only for tags that have level 2, i.e. in our example the
	 *            listener will be invoked only for tag &lt;b&gt;<br>
	 *            <ul>
	 *            <li>Value less than 0 indicates &quot;invoke listener for all
	 *            tags no matter what are their levels&quot;</li>
	 *            <li>Value of 0 indicates that the listener must not be
	 *            invoked in general no matter what is the tag level</li>
	 *            <li>Value greater than 0 indicates the tag level that the
	 *            listener will be invoked for</li>
	 *            description
	 * @throws IOException
	 *             if some IO error occurs when reading the XML file or if a
	 *             parser error occurs.
	 */
	public static void parseXML(InputStream aInputStream, ExTagListener aListener, int aLevel) throws IOException {
		XMLParserImpl xml = new XMLParserImpl(aInputStream, aListener);
		xml.setLevel(aLevel);
		xml.parseXML();
	}

	/**
	 * Parses a XML file given through aReader and during the parsing notifies
	 * aListener for close-tag and open-tag events <br>
	 * <br>
	 * <b>Note: The XMLReader does not close the passed Reader or InputStream
	 * 
	 * @param aReader
	 *            aReader to read the XML file from
	 * @param aListener
	 *            ExTagListener that will be notified on close-tag and open-tag
	 *            events
	 * @param aLevel
	 *            see parseXML(Reader aReader, ExTagListener aListener, int
	 *            aLevel description
	 * @throws IOException
	 *             if some IO error occurs when reading the XML file or if a
	 *             parser error occurs.
	 */
	public static void parseXML(Reader aReader, ExTagListener aListener, int aLevel) throws IOException {
		XMLParserImpl xml = new XMLParserImpl(aReader, aListener);
		xml.setLevel(aLevel);
		xml.parseXML();
	}

	/**
	 * Returns the XML version attribute
	 * 
	 * @return the XML file version attribute
	 */
	public String getVersion() {
		return fVersion;
	}

	/**
	 * Returns the XML encoding attribute
	 * 
	 * @return the XML encoding attribute
	 */
	public String getEncoding() {
		return fEncoding;
	}

	/**
	 * Returns the value of XML standalone attribute
	 * 
	 * @return the value of XML standalone attribute
	 */
	public String getStandalone() {
		return fStandalone;
	}
}
