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
 *    Lazar Kirchev		 	- bug.id = 320377
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.ds.Activator;
import org.eclipse.equinox.internal.ds.Messages;
import org.eclipse.equinox.internal.util.xml.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;

/**
 * Processes the parsing of the component description XMLs
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @author Teodor Bakardzhiev
 */
public class DeclarationParser implements ExTagListener {

	private static final String XMLNS_1_0 = "http://www.osgi.org/xmlns/scr/v1.0.0"; //$NON-NLS-1$
	private static final String XMLNS_1_1 = "http://www.osgi.org/xmlns/scr/v1.1.0"; //$NON-NLS-1$
	private static final String XMLNS_1_2 = "http://www.osgi.org/xmlns/scr/v1.2.0"; //$NON-NLS-1$
	private static final String ATTR_XMLNS = "xmlns"; //$NON-NLS-1$

	private static final String COMPONENT_TAG_NAME = "component"; //$NON-NLS-1$

	//component attributes
	private static final String ATTR_AUTOENABLE = "enabled"; //$NON-NLS-1$
	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	private static final String ATTR_FACTORY = "factory"; //$NON-NLS-1$
	private static final String ATTR_IMMEDIATE = "immediate"; //$NON-NLS-1$
	//component attributes according to schema v1.1
	private static final String ATTR_CONF_POLICY = "configuration-policy"; //$NON-NLS-1$
	private static final String ATTR_ACTIVATE = "activate"; //$NON-NLS-1$
	private static final String ATTR_DEACTIVATE = "deactivate"; //$NON-NLS-1$
	private static final String ATTR_MODIFIED = "modified"; //$NON-NLS-1$
	//component attributes according to schema v1.2
	private static final String ATTR_CONFIGURATION_PID = "configuration-pid"; //$NON-NLS-1$

	private static final String TAG_IMPLEMENTATION = "implementation"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

	private static final String TAG_PROPERTY = "property"; //$NON-NLS-1$
	private static final String ATTR_VALUE = "value"; //$NON-NLS-1$
	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$

	private static final String TAG_PROPERTIES = "properties"; //$NON-NLS-1$
	private static final String ATTR_ENTRY = "entry"; //$NON-NLS-1$

	private static final String TAG_SERVICE = "service"; //$NON-NLS-1$
	private static final String ATTR_SERVICEFACTORY = "servicefactory"; //$NON-NLS-1$
	private static final String TAG_PROVIDE = "provide"; //$NON-NLS-1$
	private static final String ATTR_INTERFACE = "interface"; //$NON-NLS-1$

	private static final String TAG_REFERENCE = "reference"; //$NON-NLS-1$
	private static final String ATTR_CARDINALITY = "cardinality"; //$NON-NLS-1$
	private static final String ATTR_POLICY = "policy"; //$NON-NLS-1$
	private static final String ATTR_TARGET = "target"; //$NON-NLS-1$
	private static final String ATTR_BIND = "bind"; //$NON-NLS-1$
	private static final String ATTR_UNBIND = "unbind"; //$NON-NLS-1$
	//reference attributes according to schema v1.2
	private static final String ATTR_UPDATED = "updated"; //$NON-NLS-1$
	private static final String ATTR_POLICY_OPTION = "policy-option"; //$NON-NLS-1$

	/** Constant for String property type */
	public static final int STRING = 1;
	/** Constant for Long property type */
	public static final int LONG = 2;
	/** Constant for Integer property type */
	public static final int INTEGER = 3;
	/** Constant for Short property type */
	public static final int SHORT = 4;
	/** Constant for Char property type */
	public static final int CHARACTER = 5;
	/** Constant for Byte property type */
	public static final int BYTE = 6;
	/** Constant for Double property type */
	public static final int DOUBLE = 7;
	/** Constant for Float property type */
	public static final int FLOAT = 8;
	/** Constant for Boolean property type */
	public static final int BOOLEAN = 11;

	public Vector components;

	private boolean throwErrors = false;

	private Bundle bundle;
	private BundleContext bc;
	private ServiceComponent currentComponent;
	private String closeTag;
	boolean immediateSet = false;
	private Hashtable namespaces = null;
	private boolean rootPassed = false;
	private String currentURL = null;

	public DeclarationParser() {
		this(false);
	}

	public DeclarationParser(boolean toThrowErrors) {
		this.throwErrors = toThrowErrors;
	}

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
	 * @param components sets the components Vector where the parsed components should be placed
	 * @param processingURL the URL of the XML which is being processed. Will be used for debugging purposes
	 * @throws Exception if any unhandled exception occurs during the parsing
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
		} catch (IllegalArgumentException iae) {
			currentComponent = null; //the component is bad - ignoring it
			closeTag = null;
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ERROR_PROCESSING_START_TAG, currentURL, bundle) + iae.getMessage(), null);
			if (Activator.DEBUG) {
				Activator.log.debug("[SCR] Tracing the last exception", iae); //$NON-NLS-1$
			}
			if (throwErrors) {
				throw new RuntimeException(NLS.bind(Messages.ERROR_PROCESSING_START_TAG, currentURL, bundle) + iae.getMessage(), iae);
			}
		} catch (Throwable e) {
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ERROR_PROCESSING_START_TAG, currentURL, bundle), e);
			if (throwErrors) {
				throw new RuntimeException(NLS.bind(Messages.ERROR_PROCESSING_START_TAG, currentURL, bundle) + e.getMessage(), e);
			}
		} finally {
			if (!rootPassed) {
				rootPassed = true;
			}
		}
	}

	private void doEndTag(Tag tag) throws InvalidSyntaxException {
		if (currentComponent != null) {
			String tagName = tag.getName().intern();
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
					// if unset, immediate attribute is false if service element is
					// specified or true otherwise
					// if component factory then immediate by default is false
					currentComponent.setImmediate(currentComponent.serviceInterfaces == null);
				}
				currentComponent.validate(tag.getLine(), getNamespace(tag.getName()));
				if (components == null) {
					components = new Vector(1, 1);
				}
				components.addElement(currentComponent);
				currentComponent = null;
				closeTag = null;
			} else {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.ILLEGAL_TAG_FOUND, tagName, Integer.toString(tag.getLine())));
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
		} catch (IllegalArgumentException iae) {
			currentComponent = null;
			closeTag = null;
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ERROR_PROCESSING_END_TAG, currentURL, bundle) + iae.getMessage(), null);
			if (Activator.DEBUG) {
				Activator.log.debug("[SCR] Tracing the last exception", iae); //$NON-NLS-1$
			}
			if (throwErrors) {
				throw new RuntimeException(NLS.bind(Messages.ERROR_PROCESSING_END_TAG, currentURL, bundle) + iae.getMessage(), iae);
			}
		} catch (Throwable e) {
			currentComponent = null;
			closeTag = null;
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ERROR_PROCESSING_END_TAG, currentURL, bundle), e);
			if (throwErrors) {
				throw new RuntimeException(NLS.bind(Messages.ERROR_PROCESSING_END_TAG, currentURL, bundle) + e.getMessage(), e);
			}
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
		if ("0..1".equals(value)) { //$NON-NLS-1$
			return ComponentReference.CARDINALITY_0_1;
		} else if ("0..n".equals(value)) { //$NON-NLS-1$
			return ComponentReference.CARDINALITY_0_N;
		} else if ("1..1".equals(value)) { //$NON-NLS-1$
			return ComponentReference.CARDINALITY_1_1;
		} else if ("1..n".equals(value)) { //$NON-NLS-1$
			return ComponentReference.CARDINALITY_1_N;
		} else {
			return -1;
		}
	}

	private void doReference(Tag tag) throws InvalidSyntaxException {
		String name = tag.getAttribute(ATTR_NAME);
		String iface = tag.getAttribute(ATTR_INTERFACE);
		if (iface == null) {
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.NO_INTERFACE_ATTR_IN_REFERENCE_TAG, Integer.toString(tag.getLine())));
			throw e;
		}

		String cardinalityS = tag.getAttribute(ATTR_CARDINALITY);
		int cardinality = ComponentReference.CARDINALITY_1_1; // default
		if (cardinalityS != null) {
			cardinality = getCardinality(cardinalityS);
			if (cardinality < 0) {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_CARDINALITY_ATTR, cardinalityS, Integer.toString(tag.getLine())));
				throw e;
			}
		} // if null - default cardinality is already initialized in
			// constructor

		String policyS = tag.getAttribute(ATTR_POLICY);
		int policy = ComponentReference.POLICY_STATIC; // default
		if (policyS != null) {
			// verify the policy attribute values
			if (policyS.equals("dynamic")) { //$NON-NLS-1$
				policy = ComponentReference.POLICY_DYNAMIC;
			} else if (policyS.equals("static")) { //$NON-NLS-1$
				policy = ComponentReference.POLICY_STATIC;
			} else {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_POLICY_ATTR, policyS, Integer.toString(tag.getLine())));
				throw e;
			}
		} // if null - default policy is already initialized in constructor

		String bind = tag.getAttribute(ATTR_BIND);
		String unbind = tag.getAttribute(ATTR_UNBIND);
		if (bind != null) {
			if (bind.equals("")) { //$NON-NLS-1$
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_REFERENCE_TAG__BIND_ATTR_EMPTY, Integer.toString(tag.getLine())));
				throw e;
			}
			if (bind.equals(unbind)) {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_REFERENCE_TAG__BIND_EQUALS_UNBIND, Integer.toString(tag.getLine())));
				throw e;
			}
		}
		if (unbind != null && unbind.equals("")) { //$NON-NLS-1$
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_REFERENCE_TAG__UNBIND_ATTR_EMPTY, Integer.toString(tag.getLine())));
			throw e;
		}

		String updated = tag.getAttribute(ATTR_UPDATED);
		if (updated != null && updated.equals("")) { //$NON-NLS-1$
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_REFERENCE_TAG__UPDATED_ATTR_EMPTY, Integer.toString(tag.getLine())));
			throw e;
		}

		String policyOption = tag.getAttribute(ATTR_POLICY_OPTION);
		int policyOptionProcessed = ComponentReference.POLICY_OPTION_RELUCTANT; //default
		if (policyOption != null) {
			if (policyOption.equals("greedy")) { //$NON-NLS-1$
				policyOptionProcessed = ComponentReference.POLICY_OPTION_GREEDY;
			} else if (policyOption.equals("reluctant")) { //$NON-NLS-1$
				policyOptionProcessed = ComponentReference.POLICY_OPTION_RELUCTANT;
			} else {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_POLICY_OPTION_ATTR, policyOption, Integer.toString(tag.getLine())));
				throw e;
			}
		}

		//check if the current component namespace is suitable for the latest reference attributes
		if (!isNamespaceAtLeast12(closeTag)) {
			if (updated != null) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_TAG_ACCORDING_TO_NAMESPACE1_2, ATTR_UPDATED, Integer.toString(tag.getLine())));
			}
			if (policyOption != null) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_TAG_ACCORDING_TO_NAMESPACE1_2, ATTR_POLICY_OPTION, Integer.toString(tag.getLine())));
			}
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
		ref.updated = updated;
		ref.policy_option = policyOptionProcessed;
		ref.target = tag.getAttribute(ATTR_TARGET);
		// validate the target filter
		if (ref.target != null) {
			Activator.createFilter(ref.target);
		}
	}

	private void doImplementation(Tag tag) {
		if (currentComponent.implementation != null) {
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_COMPONENT_TAG__MULTIPLE_IMPL_ATTRIBS, Integer.toString(tag.getLine())));
			throw e;
		}
		String tmp = tag.getAttribute(ATTR_CLASS);
		if (tmp == null) {
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_COMPONENT_TAG__NO_CLASS_ATTR, Integer.toString(tag.getLine())));
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
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_SERVICE_TAG__NO_PROVIDE_TAG, Integer.toString(tag.getLine())));
			throw e;
		}
		if (currentComponent.serviceInterfaces != null) {
			//there are already defined service interfaces. The service tag seems to be duplicated
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.DUPLICATED_SERVICE_TAGS, Integer.toString(tag.getLine())));
			throw e;
		}
		for (int i = 0; i < size; i++) {
			Tag p = tag.getTagAt(i);
			String pName = p.getName().intern();
			if (pName == TAG_PROVIDE) {
				String iFace = p.getAttribute(ATTR_INTERFACE);
				if (iFace == null) {
					IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_PROVIDE_TAG__NO_INTERFACE_ATTR, Integer.toString(tag.getLine())));
					throw e;
				}
				if (currentComponent.serviceInterfaces == null) {
					currentComponent.serviceInterfaces = new Vector(size);
				}
				currentComponent.serviceInterfaces.addElement(iFace);
			} else {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.ILLEGAL_ELEMENT_IN_SERVICE_TAG, pName, Integer.toString(tag.getLine())));
				throw e;
			}
		}
	}

	private void doProperty(Tag tag) {
		String name = null;
		try {
			name = tag.getAttribute(ATTR_NAME);
			if (name == null) {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_PROPERTY_TAG__NO_NAME_ATTR, Integer.toString(tag.getLine())));
				throw e;
			}

			String type = tag.getAttribute(ATTR_TYPE);
			int mtType;
			if (type == null || "String".equals(type)) { //$NON-NLS-1$
				mtType = STRING;
			} else if ("Boolean".equals(type)) { //$NON-NLS-1$
				mtType = BOOLEAN;
			} else if ("Integer".equals(type)) { //$NON-NLS-1$
				mtType = INTEGER;
			} else if ("Long".equals(type)) { //$NON-NLS-1$
				mtType = LONG;
			} else if ("Char".equals(type) || "Character".equals(type)) { //$NON-NLS-1$ //$NON-NLS-2$
				mtType = CHARACTER;
			} else if ("Double".equals(type)) { //$NON-NLS-1$
				mtType = DOUBLE;
			} else if ("Float".equals(type)) { //$NON-NLS-1$
				mtType = FLOAT;
			} else if ("Byte".equals(type)) { //$NON-NLS-1$
				mtType = BYTE;
			} else if ("Short".equals(type)) { //$NON-NLS-1$
				mtType = SHORT;
			} else {
				IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_PROPERTY_TYPE, type, Integer.toString(tag.getLine())));
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
					IllegalArgumentException e = new IllegalArgumentException(Messages.INVALID_PROPERTY_TAG__NO_BODY_CONTENT);
					throw e;
				}
				StringTokenizer tok = new StringTokenizer(value, "\n\r"); //$NON-NLS-1$
				Vector el = new Vector(10);
				while (tok.hasMoreTokens()) {
					String next = tok.nextToken().trim();
					if (next.length() > 0) {
						el.addElement(next);
					}
				}
				if (el.size() == 0) {
					IllegalArgumentException e = new IllegalArgumentException(Messages.INVALID_PROPERTY_TAG__NO_BODY_CONTENT);
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
		} catch (IllegalArgumentException iae) {
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ERROR_PROCESSING_PROPERTY, name, currentURL) + iae.getMessage(), null);
			if (Activator.DEBUG) {
				Activator.log.debug("[SCR] Tracing the last exception", iae); //$NON-NLS-1$
			}
			if (throwErrors) {
				throw new RuntimeException(NLS.bind(Messages.ERROR_PROCESSING_PROPERTY, name, currentURL) + iae.getMessage(), iae);
			}
		} catch (Throwable e) {
			//logging unrecognized exception
			Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ERROR_PROCESSING_PROPERTY, name, currentURL), e);
			if (throwErrors) {
				throw new RuntimeException(NLS.bind(Messages.ERROR_PROCESSING_PROPERTY, name, currentURL) + e.getMessage(), e);
			}
		}
	}

	private void doProperties(Tag tag) {
		String fileEntry = tag.getAttribute(ATTR_ENTRY);
		if (fileEntry == null) {
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_PROPERTIES_TAG__NO_ENTRY_ATTR, Integer.toString(tag.getLine())));
			throw e;
		}

		InputStream is = null;
		try {
			URL resource = bundle.getEntry(fileEntry);
			is = resource != null ? resource.openStream() : null;
		} catch (IOException ignore) {
			//ignore
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
				Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, Messages.ERROR_LOADING_PROPERTIES_FILE, e);
				if (throwErrors) {
					throw new RuntimeException(Messages.ERROR_LOADING_PROPERTIES_FILE + e, e);
				}
			}
		}

		if (invalid) {
			IllegalArgumentException e = new IllegalArgumentException(NLS.bind(Messages.INVALID_PROPERTIES_TAG__INVALID_ENTRY_VALUE, Integer.toString(tag.getLine()), fileEntry));
			throw e;
		}
	}

	private void doCorrectComponentTag(Tag tag, String tagName) {
		closeTag = tagName.intern();
		String tmp = tag.getAttribute(ATTR_NAME);
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
		if (isNamespaceAtLeast11(tagName)) {
			//processing attribute configuration-policy
			tmp = tag.getAttribute(ATTR_CONF_POLICY);
			if (tmp != null && tmp.length() == 0) {
				tmp = null;
			}
			if (tmp != null) {
				currentComponent.configurationPolicy = tmp.intern();
			}
			//processing attribute activate
			tmp = tag.getAttribute(ATTR_ACTIVATE);
			if (tmp != null && tmp.length() == 0) {
				tmp = null;
			}
			if (tmp != null) {
				currentComponent.activateMethodName = tmp;
				currentComponent.activateMethodDeclared = true;
			}
			//processing attribute deactivate
			tmp = tag.getAttribute(ATTR_DEACTIVATE);
			if (tmp != null && tmp.length() == 0) {
				tmp = null;
			}
			if (tmp != null) {
				currentComponent.deactivateMethodName = tmp;
				currentComponent.deactivateMethodDeclared = true;
			}
			//processing attribute modified
			tmp = tag.getAttribute(ATTR_MODIFIED);
			if (tmp != null && tmp.length() == 0) {
				tmp = null;
			}
			if (tmp != null) {
				currentComponent.modifyMethodName = tmp;
			}
		} else {
			if (tag.getAttribute(ATTR_CONF_POLICY) != null) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_TAG_ACCORDING_TO_NAMESPACE1_0, ATTR_CONF_POLICY, Integer.toString(tag.getLine())));
			}
			if (tag.getAttribute(ATTR_ACTIVATE) != null) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_TAG_ACCORDING_TO_NAMESPACE1_0, ATTR_ACTIVATE, Integer.toString(tag.getLine())));
			}
			if (tag.getAttribute(ATTR_DEACTIVATE) != null) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_TAG_ACCORDING_TO_NAMESPACE1_0, ATTR_DEACTIVATE, Integer.toString(tag.getLine())));
			}
			if (tag.getAttribute(ATTR_MODIFIED) != null) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_TAG_ACCORDING_TO_NAMESPACE1_0, ATTR_MODIFIED, Integer.toString(tag.getLine())));
			}
		}

		if (isNamespaceAtLeast12(tagName)) {
			//processing attribute configuration-policy
			tmp = tag.getAttribute(ATTR_CONFIGURATION_PID);
			if (tmp != null && tmp.length() == 0) {
				tmp = null;
			}
			if (tmp != null) {
				currentComponent.configurationPID = tmp;
			}
		} else {
			if (tag.getAttribute(ATTR_CONFIGURATION_PID) != null) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_TAG_ACCORDING_TO_NAMESPACE1_2, ATTR_CONFIGURATION_PID, Integer.toString(tag.getLine())));
			}
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
			return namespace.length() == 0 || namespace.equals(XMLNS_1_1) || namespace.equals(XMLNS_1_2) || namespace.equals(XMLNS_1_0);
		}
		// not a root element
		return namespace.equals(XMLNS_1_1) || namespace.equals(XMLNS_1_2) || namespace.equals(XMLNS_1_0);
	}

	/**
	 * Creates an object from a <code>String</code> value and a type, as defined by the objectType parameter.
	 *
	 * @param string The <code>String</code> value representation of the object. 
	 * @param objectType Defines the type of the object to be generated
	 *
	 * @return an Object, which is of a type, corresponding to the given,
	 * and value - got from the string parameter.
	 *
	 * @exception IllegalArgumentException if a proper object can not be created 
	 * due to incompatibility of objectType and value or if the parameters are not correct
	 * (e.g. objectType is not a valid constant).
	 */
	public static Object makeObject(String string, int objectType) throws IllegalArgumentException {
		try {
			switch (objectType) {
				case STRING : {
					return string;
				}
				case INTEGER : {
					return new Integer(string);
				}
				case LONG : {
					return new Long(string);
				}
				case FLOAT : {
					return new Float(string);
				}
				case DOUBLE : {
					return new Double(string);
				}
				case BYTE : {
					return new Byte(string);
				}
				case SHORT : {
					return new Short(string);
				}
				case CHARACTER : {
					if (string.length() == 0) {
						throw new IllegalArgumentException(Messages.MISSING_CHARACTER);
					}
					return new Character(string.charAt(0));
				}
				case BOOLEAN : {
					return Boolean.valueOf(string);
				}
				default : {
					throw new IllegalArgumentException(NLS.bind(Messages.UNSUPPORTED_TYPE, String.valueOf(objectType)));
				}
			}
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(NLS.bind(Messages.INCORRECT_VALUE_TYPE, string));
		}
	}

	/**
	 * Makes an array from the string array value and the object type.
	 *
	 * @param array <code>String</code> array representation of an array.
	 * @param objectType The array's object type.
	 *
	 * @return an array of primitives or objects, whose component type corresponds to <code>objectType</code>,
	 * and value build from the string array passed.
	 *
	 * @exception IllegalArgumentException  if any of the elements in the string array
	 * can not be converted to a proper object or primitive, or if the <code>objectType</code> is not a valid constant.
	 */
	public static Object makeArr(String[] array, int objectType) throws IllegalArgumentException {
		switch (objectType) {
			case STRING : {
				return array;
			}
			case INTEGER : {
				int[] ints = new int[array.length];
				for (int i = 0; i < array.length; i++) {
					ints[i] = Integer.parseInt(array[i]);
				}
				return ints;
			}
			case LONG : {
				long[] longs = new long[array.length];
				for (int i = 0; i < array.length; i++) {
					longs[i] = Long.parseLong(array[i]);
				}
				return longs;
			}
			case FLOAT : {
				float[] floats = new float[array.length];
				for (int i = 0; i < array.length; i++) {
					floats[i] = Float.valueOf(array[i]).floatValue();
				}
				return floats;
			}
			case DOUBLE : {
				double[] doubles = new double[array.length];
				for (int i = 0; i < array.length; i++) {
					doubles[i] = Double.valueOf(array[i]).doubleValue();
				}
				return doubles;
			}
			case BYTE : {
				byte[] bytes = new byte[array.length];
				for (int i = 0; i < array.length; i++) {
					bytes[i] = Byte.parseByte(array[i]);
				}
				return bytes;
			}
			case SHORT : {
				short[] shorts = new short[array.length];
				for (int i = 0; i < array.length; i++) {
					shorts[i] = Short.parseShort(array[i]);
				}
				return shorts;
			}
			case CHARACTER : {
				char[] chars = new char[array.length];
				for (int i = 0; i < array.length; i++) {
					chars[i] = array[i].charAt(0);
				}
				return chars;
			}
			case BOOLEAN : {
				boolean[] booleans = new boolean[array.length];
				for (int i = 0; i < array.length; i++) {
					booleans[i] = Boolean.valueOf(array[i]).booleanValue();
				}
				return booleans;
			}
			default : {
				throw new IllegalArgumentException(NLS.bind(Messages.UNSUPPORTED_TYPE, String.valueOf(objectType)));
			}
		}
	}

	private int getNamespace(String tagName) {
		String qualifier = getNamespaceQualifier(tagName);
		String namespace = getCurrentNamespace(qualifier);
		if (!rootPassed) { // this is the root element
			if (namespace.length() == 0) {
				return ServiceComponent.NAMESPACE_1_0;
			}
		}
		// not a root element
		if (namespace.equals(XMLNS_1_0)) {
			return ServiceComponent.NAMESPACE_1_0;
		} else if (namespace.equals(XMLNS_1_1)) {
			return ServiceComponent.NAMESPACE_1_1;
		} else if (namespace.equals(XMLNS_1_2)) {
			return ServiceComponent.NAMESPACE_1_2;
		}

		//namespace is not known
		return -1;
	}

	private boolean isNamespaceAtLeast11(String tagName) {
		int namespace = getNamespace(tagName);
		return (namespace >= ServiceComponent.NAMESPACE_1_1);
	}

	private boolean isNamespaceAtLeast12(String tagName) {
		int namespace = getNamespace(tagName);
		return (namespace >= ServiceComponent.NAMESPACE_1_2);
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
			return ""; //$NON-NLS-1$
		}
		Stack stack = (Stack) namespaces.get(qualifier);
		if (stack == null || stack.empty()) {
			return ""; //$NON-NLS-1$
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
			return ""; //$NON-NLS-1$
		}
		return name.substring(0, index);
	}
}
