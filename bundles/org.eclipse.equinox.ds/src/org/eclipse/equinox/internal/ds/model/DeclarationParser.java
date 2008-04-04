/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.ds.Activator;
import org.eclipse.equinox.internal.util.xml.*;
import org.osgi.framework.*;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * ComponentParser.java
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @author Teodor Bakardzhiev
 * @version 1.0
 */

public class DeclarationParser implements ExTagListener {

	private static final String XMLNS = "http://www.osgi.org/xmlns/scr/v1.0.0";
	private static final String ATTR_XMLNS = "xmlns";

	private static final String COMPONENT_TAG_NAME = "component";

	private static final String ATTR_AUTOENABLE = "enabled";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_FACTORY = "factory";
	private static final String ATTR_IMMEDIATE = "immediate";

	private static final String TAG_IMPLEMENTATION = "implementation";
	private static final String ATTR_CLASS = "class";

	private static final String TAG_PROPERTY = "property";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_TYPE = "type";

	private static final String TAG_PROPERTIES = "properties";
	private static final String ATTR_ENTRY = "entry";

	private static final String TAG_SERVICE = "service";
	private static final String ATTR_SERVICEFACTORY = "servicefactory";
	private static final String TAG_PROVIDE = "provide";
	private static final String ATTR_INTERFACE = "interface";

	private static final String TAG_REFERENCE = "reference";
	private static final String ATTR_CARDINALITY = "cardinality";
	private static final String ATTR_POLICY = "policy";
	private static final String ATTR_TARGET = "target";
	private static final String ATTR_BIND = "bind";
	private static final String ATTR_UNBIND = "unbind";

	public Vector components;

	private Bundle bundle;
	private BundleContext bc;
	private ServiceComponent currentComponent;
	private String closeTag;
	boolean immediateSet = false;
	private Hashtable namespaces = null;
	private boolean rootPassed = false;
	private String currentURL = null;

	/**
	 * This method parses an XML file read from the given stream and converts
	 * the components definitions into a java object.
	 * <p>
	 * 
	 * It also performs a full XML verification of the definition.�
	 * 
	 * @param in
	 *            the input stream to the XML declaration file
	 * @param bundle
	 *            this is used to load the 'properties' tag
	 */
	public void parse(InputStream in, Bundle bundle, Vector components, String processingURL) throws Exception {
		this.components = components;
		this.bundle = bundle;
		this.bc = bundle.getBundleContext();
		this.currentURL = processingURL;
		rootPassed = false;
		XMLParser.parseXML(in, this, -1);

		// release temporary objects
		this.bundle = null;
		this.bc = null;
		this.currentComponent = null;
		this.currentURL = null;
		this.closeTag = null;
		this.namespaces = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.ExTagListener#startTag(org.eclipse.equinox.internal.util.xml.Tag)
	 */
	public final void startTag(Tag tag) {
		try {
			processNamespacesEnter(tag);
			String tagName = tag.getName();
			if (isCorrectComponentTag(tagName)) {
				doCorrectComponentTag(tag, tagName);
			}
		} catch (Throwable e) {
			Activator.log.error("[SCR] Error occured while processing start tag of XML '" + currentURL + "' in bundle " + bundle, e);
		} finally {
			if (!rootPassed) {
				rootPassed = true;
			}
		}
	}

	private void doEndTag(Tag tag) throws InvalidSyntaxException {
		String tagName = tag.getName().intern();
		if (currentComponent != null) {
			if (tagName == TAG_IMPLEMENTATION) {
				doImplementation(tag);
			} else if (tagName == TAG_PROPERTY) {
				doProperty(tag);
			} else if (tagName == TAG_PROPERTIES) {
				doProperties(tag);
			} else if (tagName == TAG_SERVICE) {
				doService(tag);
			} else if (tagName == TAG_REFERENCE) {
				doReference(tag);
			} else if (tagName == TAG_PROVIDE) {
				// empty - this tag is processed within the service tag
			} else if (tagName == closeTag) {
				// the component is completed - we can now fully validate it!

				if (!immediateSet && (currentComponent.factory == null)) {
					// if unset, immediate attribute is false if service element
					// is
					// specified or true otherwise
					// if component factory then immediate by default is false
					currentComponent.setImmediate(currentComponent.serviceInterfaces == null);
				}
				currentComponent.validate(tag.getLine());
				if (components == null) {
					components = new Vector(1, 1);
				}
				components.addElement(currentComponent);
				currentComponent = null;
				closeTag = null;
			} else {
				IllegalArgumentException e = new IllegalArgumentException("Found illegal tag named '" + tagName + "' in component XML, at line " + tag.getLine());
				throw e;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.xml.ExTagListener#endTag(org.eclipse.equinox.internal.util.xml.Tag)
	 */
	public final void endTag(Tag tag) {
		try {
			doEndTag(tag);
			processNamespacesLeave(tag);
		} catch (Throwable e) {
			currentComponent = null;
			closeTag = null;
			Activator.log.error("[SCR] Error occured while processing end tag of XML '" + currentURL + "' in bundle " + bundle, e);
		}
	}

	/**
	 * This method will return convert a string to a cardinality constant
	 * 
	 * @param value
	 *            the input string
	 * @return the cardinality or -1 to indicate error
	 */
	private int getCardinality(String value) {
		if ("0..1".equals(value)) {
			return ComponentReference.CARDINALITY_0_1;
		} else if ("0..n".equals(value)) {
			return ComponentReference.CARDINALITY_0_N;
		} else if ("1..1".equals(value)) {
			return ComponentReference.CARDINALITY_1_1;
		} else if ("1..n".equals(value)) {
			return ComponentReference.CARDINALITY_1_N;
		} else {
			return -1;
		}
	}

	private void doReference(Tag tag) throws InvalidSyntaxException {
		String name = tag.getAttribute(ATTR_NAME);
		if (name == null) {
			IllegalArgumentException e = new IllegalArgumentException("The 'reference' tag must have 'name' attribute, at line " + tag.getLine());
			throw e;
		}

		String iface = tag.getAttribute(ATTR_INTERFACE);
		if (iface == null) {
			IllegalArgumentException e = new IllegalArgumentException("The 'reference' tag must have 'interface' attribute, at line " + tag.getLine());
			throw e;
		}

		String cardinalityS = tag.getAttribute(ATTR_CARDINALITY);
		int cardinality = ComponentReference.CARDINALITY_1_1; // default
		if (cardinalityS != null) {
			cardinality = getCardinality(cardinalityS);
			if (cardinality < 0) {
				IllegalArgumentException e = new IllegalArgumentException("The 'cardinality' attribute has invalid value '" + cardinalityS + "' at line " + tag.getLine());
				throw e;
			}
		} // if null - default cardinality is already initialized in
		// constructor

		String policyS = tag.getAttribute(ATTR_POLICY);
		int policy = ComponentReference.POLICY_STATIC; // default
		if (policyS != null) {
			// verify the policy attribute values
			if (policyS.equals("static")) {
				policy = ComponentReference.POLICY_STATIC;
			} else if (policyS.equals("dynamic")) {
				policy = ComponentReference.POLICY_DYNAMIC;
			} else {
				IllegalArgumentException e = new IllegalArgumentException("The 'policy' attribute has invalid value '" + policyS + "' at line " + tag.getLine());
				throw e;
			}
		} // if null - default policy is already initialized in constructor

		String bind = tag.getAttribute(ATTR_BIND);
		String unbind = tag.getAttribute(ATTR_UNBIND);
		if ((bind != null && ((unbind == null) || bind.equals("") || bind.equals(unbind))) || (unbind != null && ((bind == null) || unbind.equals("") || unbind.equals(bind)))) {
			IllegalArgumentException e = new IllegalArgumentException("The 'reference' tag at line " + tag.getLine() + " is invalid: you must specify both but different 'bind' and 'unbind' attributes!");
			throw e;
		}

		// the reference is autoadded in the ServiceComponent's list of
		// references
		// in its constructor
		ComponentReference ref = new ComponentReference(currentComponent);
		ref.name = name;
		ref.interfaceName = iface;
		ref.cardinality = cardinality;
		ref.policy = policy;
		ref.bind = bind;

		ref.unbind = unbind;
		ref.target = tag.getAttribute(ATTR_TARGET);
		// validate the target filter
		if (ref.target != null) {
			Activator.createFilter(ref.target);
		}
	}

	private void doImplementation(Tag tag) {
		if (currentComponent.implementation != null) {
			IllegalArgumentException e = new IllegalArgumentException("The 'component' tag must have exactly one 'implementation' attribute at line " + tag.getLine());
			throw e;
		}
		String tmp = tag.getAttribute(ATTR_CLASS);
		if (tmp == null) {
			IllegalArgumentException e = new IllegalArgumentException("The 'implementation' element must have 'class' attribute set at line " + tag.getLine());
			throw e;
		}
		currentComponent.implementation = tmp;
	}

	private void doService(Tag tag) {
		String tmp = tag.getAttribute(ATTR_SERVICEFACTORY);
		if (tmp != null) {
			currentComponent.serviceFactory = Boolean.valueOf(tmp).booleanValue();
		}
		int size = tag.size();
		if (size == 0) {
			IllegalArgumentException e = new IllegalArgumentException("The 'service' tag must have one 'provide' tag set at line " + tag.getLine());
			Activator.log.error("[SCR] " + e.getMessage(), e);
			throw e;
		}
		for (int i = 0; i < size; i++) {
			Tag p = tag.getTagAt(i);
			String pName = p.getName().intern();
			if (pName == TAG_PROVIDE) {
				String iFace = p.getAttribute(ATTR_INTERFACE);
				if (iFace == null) {
					IllegalArgumentException e = new IllegalArgumentException("The 'provide' tag must have 'interface' attribute set at line " + tag.getLine());
					Activator.log.error("[SCR] " + e.getMessage(), e);
					throw e;
				}
				if (currentComponent.serviceInterfaces == null) {
					currentComponent.serviceInterfaces = new Vector(size);
				}
				currentComponent.serviceInterfaces.addElement(iFace);
			} else {
				IllegalArgumentException e = new IllegalArgumentException("Found illegal element '" + pName + "' for tag 'service' at line " + tag.getLine());
				Activator.log.error("[SCR] " + e.getMessage(), e);
				throw e;
			}
		}
	}

	private void doProperty(Tag tag) {
		String name = null;
		try {
			name = tag.getAttribute(ATTR_NAME);
			if (name == null) {
				IllegalArgumentException e = new IllegalArgumentException("The 'property' tag must have 'name' attribute set at line " + tag.getLine());
				throw e;
			}

			String type = tag.getAttribute(ATTR_TYPE);
			int mtType;
			if (type == null || "String".equals(type)) {
				mtType = AttributeDefinition.STRING;
			} else if ("Boolean".equals(type)) {
				mtType = AttributeDefinition.BOOLEAN;
			} else if ("Integer".equals(type)) {
				mtType = AttributeDefinition.INTEGER;
			} else if ("Long".equals(type)) {
				mtType = AttributeDefinition.LONG;
			} else if ("Char".equals(type) || "Character".equals(type)) {
				mtType = AttributeDefinition.CHARACTER;
			} else if ("Double".equals(type)) {
				mtType = AttributeDefinition.DOUBLE;
			} else if ("Float".equals(type)) {
				mtType = AttributeDefinition.FLOAT;
			} else if ("Byte".equals(type)) {
				mtType = AttributeDefinition.BYTE;
			} else if ("Short".equals(type)) {
				mtType = AttributeDefinition.SHORT;
			} else {
				IllegalArgumentException e = new IllegalArgumentException("Illegal property type '" + type + "' on line " + tag.getLine());
				throw e;
			}

			String value = tag.getAttribute(ATTR_VALUE);
			Object _value;
			if (value != null) {
				_value = makeObject(value, mtType);
			} else {
				// body must be specified
				value = tag.getContent();
				if (value == null) {
					IllegalArgumentException e = new IllegalArgumentException("The 'property' tag must have body content if 'value' attribute is not specified!");
					throw e;
				}
				StringTokenizer tok = new StringTokenizer(value, "\n\r");
				Vector el = new Vector(10);
				while (tok.hasMoreTokens()) {
					String next = tok.nextToken().trim();
					if (next.length() > 0) {
						el.addElement(next);
					}
				}
				if (el.size() == 0) {
					IllegalArgumentException e = new IllegalArgumentException("The 'property' tag must have body content if 'value' attribute is not specified!");
					throw e;
				}
				String[] values = new String[el.size()];
				el.copyInto(values);
				_value = makeArr(values, mtType);
			}
			if (currentComponent.properties == null) {
				currentComponent.properties = new Properties();
			}
			currentComponent.properties.put(name, _value);
		} catch (Throwable e) {
			Activator.log.error("[SCR - DeclarationParser.doProperty()] Error while processing property '" + name + "' in XML " + currentURL, e);
		}
	}

	private void doProperties(Tag tag) {
		String fileEntry = tag.getAttribute(ATTR_ENTRY);
		if (fileEntry == null) {
			IllegalArgumentException e = new IllegalArgumentException("The 'properties' tag must include 'entry' attribute, at line " + tag.getLine());
			throw e;
		}

		InputStream is = null;
		try {
			URL resource = bundle.getResource(fileEntry);
			is = resource != null ? resource.openStream() : null;
		} catch (IOException ignore) {
		}

		boolean invalid = true;
		// FIXME: this will not work on properties defined like
		// com.prosyst.bla.component.properties
		// in this case the path should be converted as
		// com/prosyst/bla/component.properties
		if (is != null) {
			if (currentComponent.properties == null) {
				currentComponent.properties = new Properties();
			}
			try {
				currentComponent.properties.load(is);
				invalid = false;
			} catch (IOException e) {
				Activator.log.error("[SCR - DeclarationParser.doProperties()] Error while loading properties file", e);
			}
		}

		if (invalid) {
			IllegalArgumentException e = new IllegalArgumentException("The specified 'entry' for the 'properties' tag at line " + tag.getLine() + " doesn't contain valid reference to a property file: " + fileEntry);
			Activator.log.error("[SCR] " + e.getMessage(), e);
			throw e;
		}
	}

	private void doCorrectComponentTag(Tag tag, String tagName) {
		closeTag = tagName.intern();
		String tmp = tag.getAttribute(ATTR_NAME);
		if (tmp == null || tmp.equals("")) {
			throw new IllegalArgumentException("The 'component' tag MUST have 'name' tag set, at line " + tag.getLine());
		}
		immediateSet = false;

		currentComponent = new ServiceComponent();
		// make sure that the bundle attribute is set - it is required further
		currentComponent.bundle = bundle;
		currentComponent.bc = bc;
		currentComponent.name = tmp;
		tmp = tag.getAttribute(ATTR_AUTOENABLE);
		if (tmp != null) {
			currentComponent.autoenable = Boolean.valueOf(tmp).booleanValue();
		}
		tmp = tag.getAttribute(ATTR_FACTORY);
		if (tmp != null && tmp.length() == 0) {
			tmp = null;
		}
		currentComponent.factory = tmp;
		tmp = tag.getAttribute(ATTR_IMMEDIATE);
		if (tmp != null && tmp.length() == 0) {
			tmp = null;
		}
		if (tmp != null) {
			currentComponent.immediate = Boolean.valueOf(tmp).booleanValue();
			immediateSet = true;
		}
	}

	private boolean isCorrectComponentTag(String tagName) {
		String qualifier = getNamespaceQualifier(tagName);
		String localTagName = (qualifier.length() == 0) ? tagName : tagName.substring(qualifier.length() + 1); // + one char for ':'
		if (!localTagName.equals(COMPONENT_TAG_NAME)) {
			return false;
		}
		String namespace = getCurrentNamespace(qualifier);
		if (!rootPassed) { // this is the root element
			return namespace.length() == 0 || namespace.equals(XMLNS);
		} else { // not a root element
			return namespace.equals(XMLNS);
		}
	}

	/**
	 * Creates an object from a <code>String</code> value and a type, as
	 * returned by the corresponding {@link AttributeDefinition#getType()}
	 * method.
	 * 
	 * @param string
	 *            The <code>String</code> value representation of the object.
	 * @param syntax
	 *            The object's type as defined by
	 *            <code>AttributeDefinition</code>.
	 * 
	 * @return an Object, which is of a type, corresponding to the given, and
	 *         value - got from the string parameter. E.g. if syntax is equal to
	 *         <code>AttributeDefinition.INTEGER</code> and string is "1",
	 *         then the value returned should be Integer("1").
	 * 
	 * @exception IllegalArgumentException
	 *                if a proper object can not be created due to
	 *                incompatibility of syntax and value or if the parameters
	 *                are not correct (e.g. syntax is not a valid
	 *                <code>AttributeDefinition</code> constant).
	 */
	public static Object makeObject(String string, int syntax) throws IllegalArgumentException {
		try {
			switch (syntax) {
				case AttributeDefinition.STRING : {
					return string;
				}
				case AttributeDefinition.INTEGER : {
					return new Integer(string);
				}
				case AttributeDefinition.LONG : {
					return new Long(string);
				}
				case AttributeDefinition.FLOAT : {
					return new Float(string);
				}
				case AttributeDefinition.DOUBLE : {
					return new Double(string);
				}
				case AttributeDefinition.BYTE : {
					return new Byte(string);
				}
				case AttributeDefinition.SHORT : {
					return new Short(string);
				}
				case AttributeDefinition.CHARACTER : {
					if (string.length() == 0) {
						throw new IllegalArgumentException("Missing character ");
					}
					return new Character(string.charAt(0));
				}
				case AttributeDefinition.BOOLEAN : {
					return Boolean.valueOf(string);
				}
				default : {
					throw new IllegalArgumentException("Unsupported type: ".concat(String.valueOf(syntax)));
				}
			}
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Value: " + string + " does not fit its type!");
		}
	}

	/**
	 * Makes an array from the string array value and the syntax.
	 * 
	 * @param array
	 *            <code>String</code> array representation of an array, which
	 *            follows the rules defined by <code>AttributeDefinition</code>.
	 * @param syntax
	 *            The array's type as defined by
	 *            <code>AttributeDefinition</code>.
	 * 
	 * @return an arary of primitives or objects, whose component type
	 *         corresponds to <code>syntax</code>, and value build from the
	 *         string array passed.
	 * 
	 * @exception IllegalArgumentException
	 *                if any of the elements in the string array can not be
	 *                converted to a proper object or primitive, or if the
	 *                <code>syntax</code> is not a valid
	 *                <code>AttributeDefinition</code> type constant.
	 */
	public static Object makeArr(String[] array, int syntax) throws IllegalArgumentException {
		switch (syntax) {
			case AttributeDefinition.STRING : {
				return array;
			}
			case AttributeDefinition.INTEGER : {
				int[] ints = new int[array.length];
				for (int i = 0; i < array.length; i++) {
					ints[i] = Integer.parseInt(array[i]);
				}
				return ints;
			}
			case AttributeDefinition.LONG : {
				long[] longs = new long[array.length];
				for (int i = 0; i < array.length; i++) {
					longs[i] = Long.parseLong(array[i]);
				}
				return longs;
			}
			case AttributeDefinition.FLOAT : {
				float[] floats = new float[array.length];
				for (int i = 0; i < array.length; i++) {
					floats[i] = Float.valueOf(array[i]).floatValue();
				}
				return floats;
			}
			case AttributeDefinition.DOUBLE : {
				double[] doubles = new double[array.length];
				for (int i = 0; i < array.length; i++) {
					doubles[i] = Double.valueOf(array[i]).doubleValue();
				}
				return doubles;
			}
			case AttributeDefinition.BYTE : {
				byte[] bytes = new byte[array.length];
				for (int i = 0; i < array.length; i++) {
					bytes[i] = Byte.parseByte(array[i]);
				}
				return bytes;
			}
			case AttributeDefinition.SHORT : {
				short[] shorts = new short[array.length];
				for (int i = 0; i < array.length; i++) {
					shorts[i] = Short.parseShort(array[i]);
				}
				return shorts;
			}
			case AttributeDefinition.CHARACTER : {
				char[] chars = new char[array.length];
				for (int i = 0; i < array.length; i++) {
					chars[i] = array[i].charAt(0);
				}
				return chars;
			}
			case AttributeDefinition.BOOLEAN : {
				boolean[] booleans = new boolean[array.length];
				for (int i = 0; i < array.length; i++) {
					booleans[i] = Boolean.valueOf(array[i]).booleanValue();
				}
				return booleans;
			}
			default : {
				throw new IllegalArgumentException("Unsupported type: ".concat(String.valueOf(syntax)));
			}
		}
	}

	private void processNamespacesEnter(Tag tag) {
		Enumeration en = tag.getAttributeNames();
		while (en.hasMoreElements()) {
			String attrName = (String) en.nextElement();
			if (attrName.startsWith(ATTR_XMLNS)) {
				String qualifier = attrName.substring(ATTR_XMLNS.length());
				if ((qualifier.length() == 0) || // <- default namespace
						((qualifier.charAt(0) == ':') ? (qualifier = qualifier.substring(1)).length() > 0 : false)) {

					if (namespaces == null) {
						namespaces = new Hashtable();
					}
					Stack stack = null;
					if ((stack = (Stack) namespaces.get(qualifier)) == null) {
						stack = new Stack();
						namespaces.put(qualifier, stack);
					}
					stack.push(tag.getAttribute(attrName));
				}
			}
		}
	}

	private String getCurrentNamespace(String qualifier) {
		if (namespaces == null || qualifier == null) {
			return "";
		}
		Stack stack = (Stack) namespaces.get(qualifier);
		if (stack == null || stack.empty()) {
			return "";
		}

		return (String) stack.peek();
	}

	private void processNamespacesLeave(Tag tag) {
		if (namespaces == null) {
			return;
		}
		Enumeration en = tag.getAttributeNames();
		while (en.hasMoreElements()) {
			String attrName = (String) en.nextElement();
			if (attrName.startsWith(ATTR_XMLNS)) {
				String qualifier = attrName.substring(ATTR_XMLNS.length());
				if ((qualifier.length() == 0) || // <- default namespace
						((qualifier.charAt(0) == ':') ? (qualifier = qualifier.substring(1)).length() > 0 : false)) {

					Stack stack = (Stack) namespaces.get(qualifier);
					if (stack != null) {
						if (!stack.empty()) {
							stack.pop();
						}
						if (stack.empty()) {
							namespaces.remove(qualifier);
						}
					}
				}
			}
		}
	}

	private String getNamespaceQualifier(String name) {
		int index = name.indexOf(':');
		if (index < 0) {
			return "";
		}
		return name.substring(0, index);
	}
}
