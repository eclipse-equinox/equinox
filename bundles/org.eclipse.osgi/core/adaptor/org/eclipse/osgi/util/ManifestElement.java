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
 * This class represents a manifest element.
 */
public class ManifestElement {

	protected String value;
	protected Hashtable attributes;

	public ManifestElement() {
		this(null);
	}

	public ManifestElement(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String getAttribute(String key) {
		if (attributes == null) {
			return null;
		}
		return (String) attributes.get(key);
	}

	public Enumeration getKeys() {
		if (attributes == null) {
			return null;
		}
		return attributes.keys();
	}

	protected void setValue(String value) {
		this.value = value;
	}

	protected void addAttribute(String key, String value) {
		if (attributes == null) {
			attributes = new Hashtable(7);
		}
		String curValue = (String) attributes.get(key);
		if (curValue != null) {
			value = curValue + ";" + value;
		}
		attributes.put(key, value);
	}

	public static ManifestElement[] parseClassPath(String value) throws BundleException {
		if (value == null) {
			return (null);
		}
		Vector classpaths = new Vector(10);

		Tokenizer tokenizer = new Tokenizer(value);

		parseloop : while (true) {
			String path = tokenizer.getToken(",;");
			if (path == null) {
				throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_CLASSPATH, value));
			}

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.println("Classpath entry: " + path);
			}
			ManifestElement classpath = new ManifestElement(path);
			char c = tokenizer.getChar();
			attributeloop : while (c == ';') {
				String key = tokenizer.getToken("=");
				c = tokenizer.getChar();
				if (c == '=') {
					String val = tokenizer.getString(";,");
					if (val == null) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_NATIVECODE, value));
					}

					if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
						Debug.print(";" + key + "=" + val);
					}
					try {
						classpath.addAttribute(key, val);
					} catch (Exception e) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_CLASSPATH, value), e);
					}

					c = tokenizer.getChar();

					if (c == ';') /* more */ {
						break attributeloop;
					}

				} else {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_CLASSPATH, value));
				}
			}
			classpaths.addElement(classpath);

			if (c == ',') /* another path */ {
				continue parseloop;
			}

			if (c == '\0') /* end of value */ {
				break parseloop;
			}

			throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_CLASSPATH, value));
		}

		int size = classpaths.size();

		if (size == 0) {
			return (null);
		}

		ManifestElement[] result = new ManifestElement[size];
		classpaths.copyInto(result);

		return (result);
	}
	/**
	 * @param value The key to query the manifest for exported packages
	 * @return The Array of all ManifestElements that describe import or export
	 * package statements.
	 * @throws BundleException
	 */
	public static ManifestElement[] parsePackageDescription(String value) throws BundleException {
		if (value == null) {
			return (null);
		}

		Vector pkgvec = new Vector(10, 10);

		Tokenizer tokenizer = new Tokenizer(value);

		parseloop : while (true) {
			String pkgname = tokenizer.getToken(";,");
			if (pkgname == null) {
				throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
			}

			ManifestElement pkgdes = new ManifestElement(pkgname);
			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.print("PackageDescription: " + pkgname);
			}

			char c = tokenizer.getChar();

			while (c == ';') /* attributes */ {
				String key = tokenizer.getToken(";,=");
				if (key == null) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
				}

				c = tokenizer.getChar();

				if (c == '=') /* must be an attribute */ {
					String val = tokenizer.getString(";,");
					if (val == null) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
					}

					if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
						Debug.print(";" + key + "=" + val);
					}
					try {
						pkgdes.addAttribute(key, val);
					} catch (Exception e) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value), e);
					}

					c = tokenizer.getChar();
				} else /* error */ {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
				}
			}

			pkgvec.addElement(pkgdes);

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.println("");
			}

			if (c == ',') /* another description */ {
				continue parseloop;
			}

			if (c == '\0') /* end of value */ {
				break parseloop;
			}

			throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
		}

		int size = pkgvec.size();

		if (size == 0) {
			return (null);
		}

		ManifestElement[] result = new ManifestElement[size];
		pkgvec.copyInto(result);

		return (result);
	}

	public static ManifestElement[] parseBundleDescriptions(String value) throws BundleException {
		if (value == null) {
			return (null);
		}

		Vector bundlevec = new Vector(10, 10);

		Tokenizer tokenizer = new Tokenizer(value);

		parseloop : while (true) {
			String bundleUID = tokenizer.getToken(";,");
			if (bundleUID == null) {
				throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
			}

			ManifestElement bundledes = new ManifestElement(bundleUID);
			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.print("BundleDescription: " + bundleUID);
			}

			char c = tokenizer.getChar();

			while (c == ';') /* attributes */ {
				String key = tokenizer.getToken(";,=");
				if (key == null) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
				}

				c = tokenizer.getChar();

				if (c == '=') /* must be an attribute */ {
					String val = tokenizer.getString(";,");
					if (val == null) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
					}

					if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
						Debug.print(";" + key + "=" + val);
					}
					try {
						bundledes.addAttribute(key, val);
					} catch (Exception e) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value), e);
					}

					c = tokenizer.getChar();
				} else /* error */ {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
				}
			}

			bundlevec.addElement(bundledes);

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.println("");
			}

			if (c == ',') /* another description */ {
				continue parseloop;
			}

			if (c == '\0') /* end of value */ {
				break parseloop;
			}

			throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
		}

		int size = bundlevec.size();

		if (size == 0) {
			return (null);
		}

		ManifestElement[] result = new ManifestElement[size];
		bundlevec.copyInto(result);

		return (result);
	}

	public static ManifestElement[] parseNativeCodeDescription(String value) throws BundleException {
		if (value == null) {
			return (null);
		}

		Vector nativevec = new Vector(10, 10);

		Tokenizer tokenizer = new Tokenizer(value);

		parseloop : while (true) {
			String next = tokenizer.getToken(";,");
			if (next == null) {
				throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_NATIVECODE, value));
			}

			StringBuffer codepaths = new StringBuffer(next);

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.print("NativeCodeDescription: " + next);
			}

			char c = tokenizer.getChar();

			while (c == ';') {
				next = tokenizer.getToken(";,=");
				if (next == null) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_NATIVECODE, value));
				}

				c = tokenizer.getChar();

				if (c == ';') /* more */ {
					codepaths.append(";").append(next);

					if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
						Debug.print(";" + next);
					}
				}
			}

			ManifestElement nativedes = new ManifestElement(codepaths.toString());

			while (c == '=') {
				String val = tokenizer.getString(";,");
				if (val == null) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_NATIVECODE, value));
				}

				if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
					Debug.print(";" + next + "=" + val);
				}
				try {
					nativedes.addAttribute(next, val);
				} catch (Exception e) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_NATIVECODE, value), e);
				}

				c = tokenizer.getChar();

				if (c == ';') /* more */ {
					next = tokenizer.getToken("=");

					if (next == null) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_NATIVECODE, value));
					}

					c = tokenizer.getChar();
				}
			}

			nativevec.addElement(nativedes);

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.println("");
			}

			if (c == ',') /* another description */ {
				continue parseloop;
			}

			if (c == '\0') /* end of value */ {
				break parseloop;
			}

			throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", Constants.BUNDLE_NATIVECODE, value));
		}

		int size = nativevec.size();

		if (size == 0) {
			return (null);
		}

		ManifestElement[] result = new ManifestElement[size];
		nativevec.copyInto(result);

		return (result);
	}

	public static ManifestElement[] parseBasicCommaSeparation(String header, String value) throws BundleException {
		if (value == null) {
			return (null);
		}

		Vector bundlevec = new Vector(10, 10);

		Tokenizer tokenizer = new Tokenizer(value);

		parseloop : while (true) {
			String elementname = tokenizer.getToken(";,");
			if (elementname == null) {
				throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
			}

			ManifestElement element = new ManifestElement(elementname);
			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.print("ManifestElement: " + elementname);
			}

			char c = tokenizer.getChar();

			while (c == ';') /* attributes */ {
				String key = tokenizer.getToken(";,=");
				if (key == null) {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
				}

				c = tokenizer.getChar();

				if (c == '=') /* must be an attribute */ {
					String val = tokenizer.getString(";,");
					if (val == null) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value));
					}

					if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
						Debug.print(";" + key + "=" + val);
					}
					try {
						element.addAttribute(key, val);
					} catch (Exception e) {
						throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_PACKAGE_EXCEPTION", value), e);
					}

					c = tokenizer.getChar();
				} else /* error */ {
					throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", value));
				}
			}

			bundlevec.addElement(element);

			if (Debug.DEBUG && Debug.DEBUG_MANIFEST) {
				Debug.println("");
			}

			if (c == ',') /* another description */ {
				continue parseloop;
			}

			if (c == '\0') /* end of value */ {
				break parseloop;
			}

			throw new BundleException(Msg.formatter.getString("MANIFEST_INVALID_HEADER_EXCEPTION", value));
		}

		int size = bundlevec.size();

		if (size == 0) {
			return (null);
		}

		ManifestElement[] result = new ManifestElement[size];
		bundlevec.copyInto(result);

		return (result);

	}

}
