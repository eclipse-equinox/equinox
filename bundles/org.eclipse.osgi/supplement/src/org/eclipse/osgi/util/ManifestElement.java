/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.internal.util.SupplementDebug;
import org.eclipse.osgi.internal.util.Tokenizer;
import org.osgi.framework.BundleException;

/**
 * This class represents a single manifest element.  A manifest element must consist of a single
 * {@link String} value.  The {@link String} value may be split up into component values each
 * separated by a semi-colon (';').  A manifest element may optionally have a set of
 * attribute and directive values associated with it. The general syntax of a manifest element is as follows:
 * <pre>
 * ManifestElement ::= component (';' component)* (';' parameter)*
 * component ::= ([^;,:="\#x0D#x0A#x00])+ | quoted-string
 * quoted-string::= '"' ( [^"\#x0D#x0A#x00] | '\"'| '\\')* '"'
 * parameter ::= directive | attribute
 * directive ::= token ':=' argument
 * attribute ::= token '=' argument
 * argument ::= extended  | quoted-string
 * token ::= ( alphanum | '_' | '-' )+
 * extended ::= ( alphanum | '_' | '-' | '.' )+
 * </pre>
 * <p>
 * For example, the following is an example of a manifest element to the <code>Export-Package</code> header:
 * </p>
 * <pre>
 * org.osgi.framework; specification-version="1.2"; another-attr="examplevalue"
 * </pre>
 * <p>
 * This manifest element has a value of <code>org.osgi.framework</code> and it has two attributes,
 * <code>specification-version</code> and <code>another-attr</code>.
 * </p>
 * <p>
 * The following manifest element is an example of a manifest element that has multiple
 * components to its value:
 * </p>
 * <pre>
 * code1.jar;code2.jar;code3.jar;attr1=value1;attr2=value2;attr3=value3
 * </pre>
 * <p>
 * This manifest element has a value of <code>code1.jar;code2.jar;code3.jar</code>.
 * This is an example of a multiple component value.  This value has three
 * components: <code>code1.jar</code>, <code>code2.jar</code>, and <code>code3.jar</code>.
 * </p>
 * <p>
 * If components contain delimiter characters (e.g ';', ',' ':' "=") then it must be
 * a quoted string.  For example, the following is an example of a manifest element
 * that has multiple components containing delimiter characters:
 * </p>
 * <pre>
 * "component ; 1"; "component , 2"; "component : 3"; attr1=value1; attr2=value2; attr3=value3
 * </pre>
 * <p>
 * This manifest element has a value of <code>"component ; 1"; "component , 2"; "component : 3"</code>.
 * This value has three components: <code>"component ; 1"</code>, <code>"component , 2"</code>, <code>"component : 3"</code>.
 * </p>
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ManifestElement {

	/**
	 * The value of the manifest element.
	 */
	private final String mainValue;

	/**
	 * The value components of the manifest element.
	 */
	private final String[] valueComponents;

	/**
	 * The table of attributes for the manifest element.
	 */
	private HashMap<String, Object> attributes;

	/**
	 * The table of directives for the manifest element.
	 */
	private HashMap<String, Object> directives;

	/**
	 * Constructs an empty manifest element with no value or attributes.
	 */
	private ManifestElement(String value, String[] valueComponents) {
		this.mainValue = value;
		this.valueComponents = valueComponents;
	}

	/**
	 * Returns the value of the manifest element.  The value returned is the
	 * complete value up to the first attribute or directive.  For example, the
	 * following manifest element:
	 * <pre>
	 * test1.jar;test2.jar;test3.jar;selection-filter="(os.name=Windows XP)"
	 * </pre>
	 * <p>
	 * This manifest element has a value of <code>test1.jar;test2.jar;test3.jar</code>
	 * </p>
	 *
	 * @return the value of the manifest element.
	 */
	public String getValue() {
		return mainValue;
	}

	/**
	 * Returns the value components of the manifest element. The value
	 * components returned are the complete list of value components up to
	 * the first attribute or directive.
	 * For example, the following manifest element:
	 * <pre>
	 * test1.jar;test2.jar;test3.jar;selection-filter="(os.name=Windows XP)"
	 * </pre>
	 * <p>
	 * This manifest element has the value components array
	 * <code>{ "test1.jar", "test2.jar", "test3.jar" }</code>
	 * Each value component is delemited by a semi-colon (<code>';'</code>).
	 * </p>
	 *
	 * @return the String[] of value components
	 */
	public String[] getValueComponents() {
		return valueComponents;
	}

	/**
	 * Returns the value for the specified attribute or <code>null</code> if it does
	 * not exist.  If the attribute has multiple values specified then the last value
	 * specified is returned. For example the following manifest element:
	 * <pre>
	 * elementvalue; myattr="value1"; myattr="value2"
	 * </pre>
	 * <p>
	 * specifies two values for the attribute key <code>myattr</code>.  In this case <code>value2</code>
	 * will be returned because it is the last value specified for the attribute
	 * <code>myattr</code>.
	 * </p>
	 *
	 * @param key the attribute key to return the value for
	 * @return the attribute value or <code>null</code>
	 */
	public String getAttribute(String key) {
		return getTableValue(attributes, key);
	}

	/**
	 * Returns an array of values for the specified attribute or
	 * <code>null</code> if the attribute does not exist.
	 *
	 * @param key the attribute key to return the values for
	 * @return the array of attribute values or <code>null</code>
	 * @see #getAttribute(String)
	 */
	public String[] getAttributes(String key) {
		return getTableValues(attributes, key);
	}

	/**
	 * Returns an enumeration of attribute keys for this manifest element or
	 * <code>null</code> if none exist.
	 *
	 * @return the enumeration of attribute keys or null if none exist.
	 */
	public Enumeration<String> getKeys() {
		return getTableKeys(attributes);
	}

	/**
	 * Add an attribute to this manifest element.
	 *
	 * @param key the key of the attribute
	 * @param value the value of the attribute
	 */
	private void addAttribute(String key, String value) {
		attributes = addTableValue(attributes, key, value);
	}

	/**
	 * Returns the value for the specified directive or <code>null</code> if it
	 * does not exist.  If the directive has multiple values specified then the
	 * last value specified is returned. For example the following manifest element:
	 * <pre>
	 * elementvalue; mydir:="value1"; mydir:="value2"
	 * </pre>
	 * <p>
	 * specifies two values for the directive key <code>mydir</code>.  In this case <code>value2</code>
	 * will be returned because it is the last value specified for the directive <code>mydir</code>.
	 * </p>
	 *
	 * @param key the directive key to return the value for
	 * @return the directive value or <code>null</code>
	 */
	public String getDirective(String key) {
		return getTableValue(directives, key);
	}

	/**
	 * Returns an array of string values for the specified directives or
	 * <code>null</code> if it does not exist.
	 *
	 * @param key the directive key to return the values for
	 * @return the array of directive values or <code>null</code>
	 * @see #getDirective(String)
	 */
	public String[] getDirectives(String key) {
		return getTableValues(directives, key);
	}

	/**
	 * Return an enumeration of directive keys for this manifest element or
	 * <code>null</code> if there are none.
	 *
	 * @return the enumeration of directive keys or <code>null</code>
	 */
	public Enumeration<String> getDirectiveKeys() {
		return getTableKeys(directives);
	}

	/**
	 * Add a directive to this manifest element.
	 *
	 * @param key the key of the attribute
	 * @param value the value of the attribute
	 */
	private void addDirective(String key, String value) {
		directives = addTableValue(directives, key, value);
	}

	/*
	 * Return the last value associated with the given key in the specified table.
	 */
	private String getTableValue(HashMap<String, Object> table, String key) {
		if (table == null) {
			return null;
		}
		Object result = table.get(key);
		if (result == null) {
			return null;
		}
		if (result instanceof String)
			return (String) result;

		@SuppressWarnings("unchecked")
		List<String> valueList = (List<String>) result;
		//return the last value
		return valueList.get(valueList.size() - 1);
	}

	/*
	 * Return the values associated with the given key in the specified table.
	 */
	private String[] getTableValues(HashMap<String, Object> table, String key) {
		if (table == null) {
			return null;
		}
		Object result = table.get(key);
		if (result == null) {
			return null;
		}
		if (result instanceof String)
			return new String[] {(String) result};
		@SuppressWarnings("unchecked")
		List<String> valueList = (List<String>) result;
		return valueList.toArray(new String[valueList.size()]);
	}

	/*
	 * Return an enumeration of table keys for the specified table.
	 */
	private Enumeration<String> getTableKeys(HashMap<String, Object> table) {
		if (table == null)
			return null;
		return Collections.enumeration(table.keySet());
	}

	/*
	 * Add the given key/value association to the specified table. If an entry already exists
	 * for this key, then create an array list from the current value (if necessary) and
	 * append the new value to the end of the list.
	 */
	@SuppressWarnings("unchecked")
	private HashMap<String, Object> addTableValue(HashMap<String, Object> table, String key, String value) {
		if (table == null) {
			table = new HashMap<>(7);
		}
		Object curValue = table.get(key);
		if (curValue != null) {
			List<String> newList;
			// create a list to contain multiple values
			if (curValue instanceof List) {
				newList = (List<String>) curValue;
			} else {
				newList = new ArrayList<>(5);
				newList.add((String) curValue);
			}
			newList.add(value);
			table.put(key, newList);
		} else {
			table.put(key, value);
		}
		return table;
	}

	/**
	 * Parses a manifest header value into an array of ManifestElements.  Each
	 * ManifestElement returned will have a non-null value returned by getValue().
	 *
	 * @param header the header name to parse.  This is only specified to provide error messages
	 * 	when the header value is invalid.
	 * @param value the header value to parse.
	 * @return the array of ManifestElements that are represented by the header value; null will be
	 * 	returned if the value specified is null or if the value does not parse into
	 * 	one or more ManifestElements.
	 * @throws BundleException if the header value is invalid
	 */
	public static ManifestElement[] parseHeader(String header, String value) throws BundleException {
		if (value == null)
			return (null);
		List<ManifestElement> headerElements = new ArrayList<>(10);
		Tokenizer tokenizer = new Tokenizer(value);
		parseloop: while (true) {
			String next = tokenizer.getString(";,"); //$NON-NLS-1$
			if (next == null)
				throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR);
			List<String> headerValues = new ArrayList<>();
			StringBuilder headerValue = new StringBuilder(next);
			headerValues.add(next);

			if (SupplementDebug.STATIC_DEBUG_MANIFEST)
				System.out.print("parseHeader: " + next); //$NON-NLS-1$
			boolean directive = false;
			char c = tokenizer.getChar();
			// Header values may be a list of ';' separated values.  Just append them all into one value until the first '=' or ','
			while (c == ';') {
				next = tokenizer.getString(";,=:"); //$NON-NLS-1$
				if (next == null)
					throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR);
				c = tokenizer.getChar();
				while (c == ':') { // may not really be a :=
					c = tokenizer.getChar();
					if (c != '=') {
						String restOfNext = tokenizer.getToken(";,=:"); //$NON-NLS-1$
						if (restOfNext == null)
							throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR);
						next += ":" + c + restOfNext; //$NON-NLS-1$
						c = tokenizer.getChar();
					} else
						directive = true;
				}
				if (c == ';' || c == ',' || c == '\0') /* more */ {
					headerValues.add(next);
					headerValue.append(";").append(next); //$NON-NLS-1$
					if (SupplementDebug.STATIC_DEBUG_MANIFEST)
						System.out.print(";" + next); //$NON-NLS-1$
				}
			}
			// found the header value create a manifestElement for it.
			ManifestElement manifestElement = new ManifestElement(headerValue.toString(), headerValues.toArray(new String[headerValues.size()]));

			// now add any attributes/directives for the manifestElement.
			while (c == '=' || c == ':') {
				while (c == ':') { // may not really be a :=
					c = tokenizer.getChar();
					if (c != '=') {
						String restOfNext = tokenizer.getToken("=:"); //$NON-NLS-1$
						if (restOfNext == null)
							throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR);
						next += ":" + c + restOfNext; //$NON-NLS-1$
						c = tokenizer.getChar();
					} else
						directive = true;
				}
				// determine if the attribute is the form attr:List<type>
				String preserveEscapes = null;
				if (!directive && next.indexOf("List") > 0) { //$NON-NLS-1$
					Tokenizer listTokenizer = new Tokenizer(next);
					String attrKey = listTokenizer.getToken(":"); //$NON-NLS-1$
					if (attrKey != null && listTokenizer.getChar() == ':' && "List".equals(listTokenizer.getToken("<"))) { //$NON-NLS-1$//$NON-NLS-2$
						// we assume we must preserve escapes for , and "
						preserveEscapes = "\\,"; //$NON-NLS-1$
					}
				}
				String val = tokenizer.getString(";,", preserveEscapes); //$NON-NLS-1$
				if (val == null)
					throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR);

				if (SupplementDebug.STATIC_DEBUG_MANIFEST)
					System.out.print(";" + next + "=" + val); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					if (directive)
						manifestElement.addDirective(next, val);
					else
						manifestElement.addAttribute(next, val);
					directive = false;
				} catch (Exception e) {
					throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR, e);
				}
				c = tokenizer.getChar();
				if (c == ';') /* more */ {
					next = tokenizer.getToken("=:"); //$NON-NLS-1$
					if (next == null)
						throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR);
					c = tokenizer.getChar();
				}
			}
			headerElements.add(manifestElement);
			if (SupplementDebug.STATIC_DEBUG_MANIFEST)
				System.out.println(""); //$NON-NLS-1$
			if (c == ',') /* another manifest element */
				continue parseloop;
			if (c == '\0') /* end of value */
				break parseloop;
			throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_HEADER_EXCEPTION, header, value), BundleException.MANIFEST_ERROR);
		}
		int size = headerElements.size();
		if (size == 0)
			return (null);

		ManifestElement[] result = headerElements.toArray(new ManifestElement[size]);
		return (result);
	}

	/**
	 * Returns the result of converting a list of comma-separated tokens into an array.
	 *
	 * @return the array of string tokens or <code>null</code> if there are none
	 * @param stringList the initial comma-separated string
	 */
	public static String[] getArrayFromList(String stringList) {
		String[] result = getArrayFromList(stringList, ","); //$NON-NLS-1$
		return result.length == 0 ? null : result;
	}

	/**
	 * Returns the result of converting a list of tokens into an array.  The tokens
	 * are split using the specified separator.
	 *
	 * @return the array of string tokens.  If there are none then an empty array
	 * is returned.
	 * @param stringList the initial string list
	 * @param separator the separator to use to split the list into tokens.
	 * @since 3.2
	 */
	public static String[] getArrayFromList(String stringList, String separator) {
		if (stringList == null || stringList.trim().length() == 0)
			return new String[0];
		List<String> list = new ArrayList<>();
		StringTokenizer tokens = new StringTokenizer(stringList, separator);
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (token.length() != 0)
				list.add(token);
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Parses a bundle manifest and puts the header/value pairs into the supplied Map.
	 * Only the main section of the manifest is parsed (up to the first blank line).  All
	 * other sections are ignored.  If a header is duplicated then only the last
	 * value is stored in the map.
	 * <p>
	 * The supplied input stream is consumed by this method and will be closed.
	 * If the supplied Map is null then a Map is created to put the header/value pairs into.
	 * </p>
	 * @param manifest an input stream for a bundle manifest.
	 * @param headers a map used to put the header/value pairs from the bundle manifest.  This value may be null.
	 * @throws BundleException if the manifest has an invalid syntax
	 * @throws IOException if an error occurs while reading the manifest
	 * @return the map with the header/value pairs from the bundle manifest
	 */
	public static Map<String, String> parseBundleManifest(InputStream manifest, Map<String, String> headers) throws IOException, BundleException {
		if (headers == null)
			headers = new HashMap<>();

		manifest = new BufferedInputStream(manifest);
		try {

			ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
			while (true) {
				String line = readLine(manifest, buffer);
				/* The java.util.jar classes in JDK 1.3 use the value of the last
				 * encountered manifest header. So we do the same to emulate
				 * this behavior. We no longer throw a BundleException
				 * for duplicate manifest headers.
				 */

				if ((line == null) || (line.length() == 0)) /* EOF or empty line */
				{
					break; /* done processing main attributes */
				}

				int colon = line.indexOf(':');
				if (colon == -1) /* no colon */
				{
					throw new BundleException(NLS.bind(Msg.MANIFEST_INVALID_LINE_NOCOLON, line), BundleException.MANIFEST_ERROR);
				}
				String header = line.substring(0, colon).trim();
				String value = line.substring(colon + 1).trim();
				// intern the header here because they likely have constants for them anyway
				headers.put(header.intern(), value);
			}
		} finally {
			try {
				manifest.close();
			} catch (IOException ee) {
				// do nothing
			}
		}
		return headers;
	}

	private static String readLine(InputStream input, ByteArrayOutputStream buffer) throws IOException {
		// Read a header 'line'
		// A header line may span multiple lines with line continuations using a beginning space.
		// This method reads all the line continuations into a single string.
		// Care must be taken for cases where double byte UTF characters are split
		// across line continuations.
		// This is why BufferedReader.readLine is not used here.  We must process the
		// CR LF chars ourselves
		lineLoop: while (true) {
			int c = input.read();
			if (c == '\n') { // LF
				// next char is either a continuation (space) char or the first char of the next header
				input.mark(1);
				c = input.read();
				if (c != ' ') {
					// This first char of the next header, reset so we don't loose the char
					input.reset();
					break lineLoop;
				}
				// This is a continuation, skip the space and read the next char
				c = input.read();
			} else if (c == '\r') { // CR
				// next char is either a continuation (space) char, LF or the first char of the next header
				input.mark(1);
				c = input.read();
				if (c == '\n') { // LF
					// next char is either a continuation (space) char or the first char of the next header
					input.mark(1);
					c = input.read();
				}
				if (c != ' ') {
					// This first char of the next header, reset so we don't loose the char
					input.reset();
					break lineLoop;
				}
				c = input.read();
			}
			if (c == -1) {
				break lineLoop;
			}
			buffer.write(c);
		}
		String result = buffer.toString("UTF8"); //$NON-NLS-1$
		buffer.reset();
		return result;
	}

	@Override
	public String toString() {
		Enumeration<String> attrKeys = getKeys();
		Enumeration<String> directiveKeys = getDirectiveKeys();
		if (attrKeys == null && directiveKeys == null)
			return mainValue;
		StringBuilder result = new StringBuilder(mainValue);
		if (attrKeys != null) {
			while (attrKeys.hasMoreElements()) {
				String key = attrKeys.nextElement();
				addValues(false, key, getAttributes(key), result);
			}
		}
		if (directiveKeys != null) {
			while (directiveKeys.hasMoreElements()) {
				String key = directiveKeys.nextElement();
				addValues(true, key, getDirectives(key), result);
			}
		}
		return result.toString();
	}

	private void addValues(boolean directive, String key, String[] values, StringBuilder result) {
		if (values == null)
			return;
		for (String value : values) {
			result.append(';').append(key);
			if (directive)
				result.append(':');
			result.append("=\"").append(value).append('\"'); //$NON-NLS-1$
		}
	}
}
