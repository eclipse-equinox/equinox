/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.util;

import java.util.*;

import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.osgi.framework.util.Tokenizer;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * This class represents a single manifest element.  A manifest element must consist of a single
 * String value and may optionally have a set of attribute values associated with it. The
 * general syntax of a manifest element is the following <p>
 * <pre>
 * ManifestElement ::= headervalues (';' attribute)*
 * headervalues ::= headervalue (';' headervalue)*
 * headervalue ::= <any string value that does not have ';'>
 * attribute ::= key ’=’ value
 * key ::= token
 * value ::= token | quoted-string
 * </pre>
 * 
 * <p>
 * For example, The following is an example of a manifest element to the Export-Package header: <p>
 * <pre>
 * org.osgi.framework; specification-version="1.2"; another-attr="examplevalue"
 * </pre>
 * <p>
 * This manifest element has a value of org.osgi.framework and it has two attributes, specification-version
 * and another-attr. <p>
 * 
 * 
 */
public class ManifestElement {

	/**
	 * The value of the manifest element.
	 */
	protected String value;
	/**
	 * The table of attributes for the manifest element.
	 */
	protected Hashtable attributes;

	/**
	 * Constructs an empty manifest element with no value or attributes.
	 *
	 */
	public ManifestElement() {
		this(null);
	}

	/**
	 * Constructs a manifest element with the specified value an no attributes.
	 * @param value The value of the manifest element.
	 */
	public ManifestElement(String value) {
		this.value = value;
	}

	/**
	 * Returns the value of the manifest element.
	 * @return the value of the manifest element.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns the value for the specified attribute.  If the attribute
	 * has multiple values specified then the last value specified is returned.
	 * For example the following manifest element: <p>
	 * <pre>
	 * elementvalue; myattr="value1"; myattr="value2"
	 * </pre>
	 * <p>
	 * specifies two values for the attribute key myattr.  In this case value2
	 * will be returned because it is the last value specified for the attribute
	 * myattr.
	 * @param key the attribute key to return the value for.
	 * @return the attribute value or null if the attribute does not exist.
	 */
	public String getAttribute(String key) {
		if (attributes == null)
			return null;
		Object value = attributes.get(key);
		if (value == null)
			return null;
		if (value instanceof String)
			return (String) value;

		ArrayList valueList = (ArrayList) value;
		//return the last attribute value
		return (String) valueList.get(valueList.size() - 1);
	}

	/**
	 * Returns an array of values for the specified attribute.
	 * @param key the attribute key to return the values for.
	 * @return the array of attribute values or null if the attribute does not exist. 
	 */
	public String[] getAttributes(String key) {
		if (attributes == null)
			return null;
		Object value = attributes.get(key);
		if (value == null)
			return null;
		if (value instanceof String)
			return new String[] {(String) value};

		ArrayList valueList = (ArrayList) value;
		return (String[]) valueList.toArray(new String[valueList.size()]);
	}

	/**
	 * Returns the Enumeration of attribute keys for this manifest element.
	 * @return the Enumeration of attribute keys or null if none exist.
	 */
	public Enumeration getKeys() {
		if (attributes == null) {
			return null;
		}
		return attributes.keys();
	}

	/**
	 * Sets the value of this manifest element.
	 * @param value the value to set this manifest element to.
	 */
	protected void setValue(String value) {
		this.value = value;
	}

	/**
	 * Adds an attribute to this manifest element.
	 * @param key the key of the attribute
	 * @param value the value of the attribute
	 */
	protected void addAttribute(String key, String value) {
		if (attributes == null) {
			attributes = new Hashtable(7);
		}
		Object curValue = attributes.get(key);
		if (curValue != null) {
			ArrayList newList;
			// create a list to contain multiple values
			if (curValue instanceof ArrayList) {
				newList = (ArrayList) curValue;
			}
			else {
				newList = new ArrayList(5);
				newList.add(curValue);
			}
			newList.add(value);
			attributes.put(key, newList);
		}
		else {
			attributes.put(key, value);
		}
	}

	/**
	 * @deprecated Use parseHeader(String header, String value) instead.
	 */
	public static ManifestElement[] parseClassPath(String value) throws BundleException {
		return parseHeader(Constants.BUNDLE_CLASSPATH,value);
	}

	/**
	 * @deprecated Use parseHeader(String header, String value) instead.
	 */
	public static ManifestElement[] parsePackageDescription(String value) throws BundleException {
		return parseHeader("PACKAGE_DESCRIPTION",value);
	}

	/**
	 * @deprecated Use parseHeader(String header, String value) instead.
	 */
	public static ManifestElement[] parseBundleDescriptions(String value) throws BundleException {
		return parseHeader("BUNDLE_DESCRIPTION",value);
	}

	/**
	 * @deprecated Use parseHeader(String header, String value) instead.
	 */
	public static ManifestElement[] parseNativeCodeDescription(String value) throws BundleException {
		return parseHeader(Constants.BUNDLE_NATIVECODE, value);
	}

	/**
	 * @deprecated Use parseHeader(String header, String value) instead.
	 */
	public static ManifestElement[] parseBasicCommaSeparation(String header, String value) throws BundleException {
		return parseHeader(header,value);
	}

	/**
	 * Parses a manifest header value into an array of ManifestElements.  Each
	 * ManifestElement returned will have a non-null value returned by getValue().
	 * @param header the header name to parse.  This is only specified to provide error messages
	 * when the header value is invalid.
	 * @param value the header value to parse.
	 * @return the array of ManifestElements that are represented by the header value.
	 * @throws BundleException if the header value is invalid.
	 */
	public static ManifestElement[] parseHeader(String header, String value) throws BundleException {
		if (value == null) {
			return (null);
		}

		Vector headerElements = new Vector(10, 10);

		Tokenizer tokenizer = new Tokenizer(value);

		parseloop : while (true) {
			String next = tokenizer.getToken(";,");
			if (next == null) {
				throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", header, value));
			}

			StringBuffer headerValue = new StringBuffer(next);

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.print("paserHeader: " + next);
			}

			char c = tokenizer.getChar();

			// Header values may be a list of ';' separated values.  Just append them all into one value until the first '=' or ','
			while (c == ';') {
				next = tokenizer.getToken(";,=");
				if (next == null) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", header, value));
				}

				c = tokenizer.getChar();

				if (c == ';') /* more */ {
					headerValue.append(";").append(next);

					if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
						Debug.print(";" + next);
					}
				}
			}

			// found the header value create a manifestElement for it.
			ManifestElement manifestElement = new ManifestElement(headerValue.toString());

			// now add any attributes for the manifestElement.
			while (c == '=') {
				String val = tokenizer.getString(";,");
				if (val == null) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", header, value));
				}

				if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
					Debug.print(";" + next + "=" + val);
				}
				try {
					manifestElement.addAttribute(next, val);
				} catch (Exception e) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", header, value), e);
				}

				c = tokenizer.getChar();

				if (c == ';') /* more */ {
					next = tokenizer.getToken("=");

					if (next == null) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", header, value));
					}

					c = tokenizer.getChar();
				}
			}

			headerElements.addElement(manifestElement);

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.println("");
			}

			if (c == ',') /* another manifest element */ {
				continue parseloop;
			}

			if (c == '\0') /* end of value */ {
				break parseloop;
			}

			throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", header, value));
		}

		int size = headerElements.size();

		if (size == 0) {
			return (null);
		}

		ManifestElement[] result = new ManifestElement[size];
		headerElements.copyInto(result);

		return (result);
	}
}
