/******************************************************************************* 
 * Copyright (c) 2000, 2003 IBM Corporation and others. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/cpl-v10.html 
 * 
 * Contributors: 
 *     IBM Corporation - initial API and implementation 
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;
import java.util.*;
import javax.xml.parsers.SAXParserFactory;
import org.osgi.framework.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class PluginParser extends DefaultHandler implements IModel {
	private PluginInfo manifestInfo = new PluginInfo();
	private BundleContext context;

	public class PluginInfo implements IPluginInfo {
		private String schemaVersion;
		private String pluginId;
		private String version;
		private String vendor;
 
		// an ordered list of library path names.
		private ArrayList libraryPaths;
		// TODO Should get rid of the libraries map and just have a
		// list of library export statements instead.  Library paths must
		// preserve order.
		private Map libraries; //represent the libraries and their
										  // export statement
		private ArrayList requires;
		private String pluginClass;
		private String masterPluginId;
		private String masterVersion;
		private Set filters;
		private String pluginName;
		public boolean isFragment() {
			return masterPluginId != null;
		}
		public String toString() {
			return "plugin-id: " + pluginId + "  version: " + version + " libraries: " + libraries + " class:" + pluginClass + " master: " + masterPluginId + " master-version: " + masterVersion + " requires: " + requires; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		}
		public Map getLibraries() {
			if (libraries == null)
				return new HashMap(0);
			return libraries;
		}
		public String[] getRequires() {
			if (requires == null)
				return new String[]{"org.eclipse.core.runtime.compatibility"};
			if (schemaVersion == null) {
				//Add elements on the requirement list of ui and help.
				for (int i = 0; i < requires.size(); i++) {
					if ("org.eclipse.ui".equals(requires.get(i))) { //$NON-NLS-1$ 
						requires.add(i + 1, "org.eclipse.ui.workbench.texteditor;" + Constants.OPTIONAL_ATTRIBUTE + "=" + "true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						requires.add(i + 1, "org.eclipse.jface.text;" + Constants.OPTIONAL_ATTRIBUTE + "=" + "true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						requires.add(i + 1, "org.eclipse.ui.editors;" + Constants.OPTIONAL_ATTRIBUTE + "=" + "true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						requires.add(i + 1, "org.eclipse.ui.views;" + Constants.OPTIONAL_ATTRIBUTE + "=" + "true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						requires.add(i + 1, "org.eclipse.ui.ide;" + Constants.OPTIONAL_ATTRIBUTE + "=" + "true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
					} else if ("org.eclipse.help".equals(requires.get(i))) { //$NON-NLS-1$ 
						requires.add(i + 1, "org.eclipse.help.base;" + Constants.OPTIONAL_ATTRIBUTE + "=" + "true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
					}
				}
			}
			if (!requires.contains("org.eclipse.core.runtime.compatibility"))
				requires.add("org.eclipse.core.runtime.compatibility");
			String[] requireBundles = new String[requires.size()];
			requires.toArray(requireBundles);
			return requireBundles;
		}
		public String getMasterId() {
			return masterPluginId;
		}
		public String getMasterVersion() {
			return masterVersion;
		}
		public String getPluginClass() {
			return pluginClass;
		}
		public String getUniqueId() {
			return pluginId;
		}
		public String getVersion() {
			return version;
		}
		public Set getPackageFilters() {
			return filters;
		}
		public String[] getLibrariesName() {
			if (libraryPaths == null)
				return new String[0];
			return (String[]) libraryPaths.toArray(new String[libraryPaths.size()]);
		}
		public String getPluginName() {
			return pluginName;
		}
		public String getProviderName() {
			return vendor;
		}
	}
	// File name for this plugin or fragment
	// This to help with error reporting
	String locationName = null;

	// Current State Information
	Stack stateStack = new Stack();

	// Current object stack (used to hold the current object we are populating in this plugin info
	Stack objectStack = new Stack();
	Locator locator = null;

	// Valid States
	private static final int IGNORED_ELEMENT_STATE = 0;
	private static final int INITIAL_STATE = 1;
	private static final int PLUGIN_STATE = 2;
	private static final int PLUGIN_RUNTIME_STATE = 3;
	private static final int PLUGIN_REQUIRES_STATE = 4;
	private static final int PLUGIN_EXTENSION_POINT_STATE = 5;
	private static final int PLUGIN_EXTENSION_STATE = 6;
	private static final int RUNTIME_LIBRARY_STATE = 7;
	private static final int LIBRARY_EXPORT_STATE = 8;
	private static final int LIBRARY_PACKAGES_STATE = 12;
	private static final int PLUGIN_REQUIRES_IMPORT_STATE = 9;
	private static final int CONFIGURATION_ELEMENT_STATE = 10;
	private static final int FRAGMENT_STATE = 11;
	private ServiceReference parserReference;

	public PluginParser(BundleContext context) {
		super();
		this.context = context;
	}
	/**
	 * Receive a Locator object for document events.
	 * 
	 * <p>
	 * By default, do nothing. Application writers may override this method in
	 * a subclass if they wish to store the locator for use with other document
	 * events.
	 * </p>
	 * 
	 * @param locator A locator for all SAX document events.
	 * @see org.xml.sax.ContentHandler#setDocumentLocator
	 * @see org.xml.sax.Locator
	 */
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}
	public void endDocument() {
	}
	public void endElement(String uri, String elementName, String qName) {
		switch (((Integer) stateStack.peek()).intValue()) {
			case IGNORED_ELEMENT_STATE :
				stateStack.pop();
				break;
			case INITIAL_STATE :
				// shouldn't get here
				// internalError(Policy.bind("parse.internalStack", elementName)); //$NON-NLS-1$
				break;
			case PLUGIN_STATE :
			case FRAGMENT_STATE :
				break;
			case PLUGIN_RUNTIME_STATE :
				if (elementName.equals(RUNTIME)) {
					stateStack.pop();
				}
				break;
			case PLUGIN_REQUIRES_STATE :
				if (elementName.equals(PLUGIN_REQUIRES)) {
					stateStack.pop();
					objectStack.pop();
				}
				break;
			case PLUGIN_EXTENSION_POINT_STATE :
				break;
			case PLUGIN_EXTENSION_STATE :
				break;
			case RUNTIME_LIBRARY_STATE :
				if (elementName.equals(LIBRARY)) {
					String curLibrary = (String) objectStack.pop();
					Vector exportsVector = (Vector) objectStack.pop();
					if (manifestInfo.libraries == null){
						manifestInfo.libraries = new HashMap(3);
						manifestInfo.libraryPaths = new ArrayList(3);
					}
					manifestInfo.libraries.put(curLibrary, exportsVector);
					manifestInfo.libraryPaths.add(curLibrary);
					stateStack.pop();
				}
				break;
			case LIBRARY_EXPORT_STATE :
				if (elementName.equals(LIBRARY_EXPORT)) {
					stateStack.pop();
				}
				break;
			case PLUGIN_REQUIRES_IMPORT_STATE :
				if (elementName.equals(PLUGIN_REQUIRES_IMPORT)) {
					stateStack.pop();
				}
				break;
			case CONFIGURATION_ELEMENT_STATE :
				break;
		}
	}
	public void error(SAXParseException ex) {
		logStatus(ex);
	}
	public void fatalError(SAXParseException ex) throws SAXException {
		logStatus(ex);
		throw ex;
	}
	public void handleExtensionPointState(String elementName, Attributes attributes) {
		// We ignore all elements under extension points (if there are any)
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		// internalError(Policy.bind("parse.unknownElement", EXTENSION_POINT,elementName)); //$NON-NLS-1$
	}
	public void handleExtensionState(String elementName, Attributes attributes) {
		stateStack.push(new Integer(CONFIGURATION_ELEMENT_STATE));
	}
	public void handleInitialState(String elementName, Attributes attributes) {
		if (elementName.equals(PLUGIN)) {
			stateStack.push(new Integer(PLUGIN_STATE));
			parsePluginAttributes(attributes);
		} else if (elementName.equals(FRAGMENT)) {
			stateStack.push(new Integer(FRAGMENT_STATE));
			parseFragmentAttributes(attributes);
		} else {
			stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
			//	internalError(Policy.bind("parse.unknownTopElement", elementName)); //$NON-NLS-1$
		}
	}
	public void handleLibraryExportState(String elementName, Attributes attributes) {
		// All elements ignored.
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		// internalError(Policy.bind("parse.unknownElement", LIBRARY_EXPORT, elementName)); //$NON-NLS-1$
	}
	public void handleLibraryState(String elementName, Attributes attributes) {
		if (elementName.equals(LIBRARY_EXPORT)) {
			// Change State
			stateStack.push(new Integer(LIBRARY_EXPORT_STATE));
			// The top element on the stack much be a library element
			String currentLib = (String) objectStack.peek();
			if (attributes == null)
				return;
			String maskValue = attributes.getValue("", LIBRARY_EXPORT_MASK); //$NON-NLS-1$
			// pop off the library - already in currentLib
			objectStack.pop();
			Vector exportMask = (Vector) objectStack.peek();
			// push library back on
			objectStack.push(currentLib);
			//Split the export upfront
			if (maskValue != null) {
				StringTokenizer tok = new StringTokenizer(maskValue, ","); //$NON-NLS-1$
				while (tok.hasMoreTokens()) {
					String value = tok.nextToken();
					if (!exportMask.contains(maskValue))
						exportMask.addElement(value);
				}
			}
			return;
		}
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		return;
	}
	public void handlePluginState(String elementName, Attributes attributes) {
		if (elementName.equals(RUNTIME)) {
			// We should only have one Runtime element in a plugin or fragment
			Object whatIsIt = objectStack.peek();
			if ((whatIsIt instanceof PluginInfo) && ((PluginInfo) objectStack.peek()).libraries != null) {
				// This is at least the 2nd Runtime element we have hit. Ignore it.
				stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
				return;
			}
			stateStack.push(new Integer(PLUGIN_RUNTIME_STATE));
			// Push a new vector to hold all the library entries objectStack.push(new Vector());
			return;
		}
		if (elementName.equals(PLUGIN_REQUIRES)) {
			stateStack.push(new Integer(PLUGIN_REQUIRES_STATE));
			// Push a new vector to hold all the prerequisites
			objectStack.push(new Vector());
			parseRequiresAttributes(attributes);
			return;
		}
		if (elementName.equals(EXTENSION_POINT)) {
			stateStack.push(new Integer(PLUGIN_EXTENSION_POINT_STATE));
			return;
		}
		if (elementName.equals(EXTENSION)) {
			stateStack.push(new Integer(PLUGIN_EXTENSION_STATE));
			return;
		}
		// If we get to this point, the element name is one we don't currently accept.
		// Set the state to indicate that this element will be ignored
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
	}
	public void handleRequiresImportState(String elementName, Attributes attributes) {
		// All elements ignored.
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
	}
	public void handleRequiresState(String elementName, Attributes attributes) {
		if (elementName.equals(PLUGIN_REQUIRES_IMPORT)) {
			parsePluginRequiresImport(attributes);
			return;
		}
		// If we get to this point, the element name is one we don't currently accept.
		// Set the state to indicate that this element will be ignored
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
	}
	public void handleRuntimeState(String elementName, Attributes attributes) {
		if (elementName.equals(LIBRARY)) {
			// Change State
			stateStack.push(new Integer(RUNTIME_LIBRARY_STATE));
			// Process library attributes
			parseLibraryAttributes(attributes);
			return;
		}
		// If we get to this point, the element name is one we don't currently accept.
		// Set the state to indicate that this element will be ignored
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
	}
	private void logStatus(SAXParseException ex) {
		String name = ex.getSystemId();
		if (name == null)
			name = locationName;
		if (name == null)
			name = ""; //$NON-NLS-1$ 
		else
			name = name.substring(1 + name.lastIndexOf("/")); //$NON-NLS-1$ 
		String msg;
		if (name.equals("")) //$NON-NLS-1$ 
			msg = "parse.error";//Policy.bind("parse.error",
											 // ex.getMessage()); //$NON-NLS-1$
		else
			msg = "parse.errorNameLineColumn";
		//Policy.bind("parse.errorNameLineColumn", //$NON-NLS-1$
		//                new String[] { name, Integer.toString(ex.getLineNumber()),
		// Integer.toString(ex.getColumnNumber()), ex.getMessage()});
		//                factory.error(new Status(IStatus.WARNING, Platform.PI_RUNTIME,
		// Platform.PARSE_PROBLEM, msg, ex));
	}
	synchronized public PluginInfo parsePlugin(String in) throws Exception {
		SAXParserFactory factory = acquireXMLParsing();
		if (factory == null)
			return null; // TODO we log an error
		try {
			factory.setNamespaceAware(true);
			factory.setFeature("http://xml.org/sax/features/string-interning", true); //$NON-NLS-1$ 
			factory.setValidating(false);
			factory.newSAXParser().parse(in, this);
			return manifestInfo;
		} finally {
			releaseXMLParsing();
		}
	}
	private SAXParserFactory acquireXMLParsing() {
		if (context == null) {
			return SAXParserFactory.newInstance();
		}
		parserReference = context.getServiceReference("javax.xml.parsers.SAXParserFactory"); //$NON-NLS-1$ 
		if (parserReference == null)
			return null;
		return (SAXParserFactory) context.getService(parserReference);
	}
	private void releaseXMLParsing() {
		if (parserReference != null)
			context.ungetService(parserReference);
	}
	public void parseFragmentAttributes(Attributes attributes) {
		// process attributes
		objectStack.push(manifestInfo);
		int len = attributes.getLength();
		for (int i = 0; i < len; i++) {
			String attrName = attributes.getLocalName(i);
			String attrValue = attributes.getValue(i).trim();
			if (attrName.equals(FRAGMENT_ID))
				manifestInfo.pluginId = attrValue;
			else if (attrName.equals(FRAGMENT_NAME))
				manifestInfo.pluginName = attrValue;
			else if (attrName.equals(FRAGMENT_VERSION))
				manifestInfo.version = attrValue;
			else if (attrName.equals(FRAGMENT_PROVIDER))
				manifestInfo.vendor = attrValue;
			else if (attrName.equals(FRAGMENT_PLUGIN_ID))
				manifestInfo.masterPluginId = attrValue;
			else if (attrName.equals(FRAGMENT_PLUGIN_VERSION))
				manifestInfo.masterVersion = attrValue;
		}
	}
	public void parseLibraryAttributes(Attributes attributes) {
		// Push a vector to hold the export mask
		objectStack.push(new Vector());
		String current = attributes.getValue("", LIBRARY_NAME); //$NON-NLS-1$ 
		objectStack.push(current);
	}
	public void parsePluginAttributes(Attributes attributes) {
		// process attributes
		objectStack.push(manifestInfo);
		int len = attributes.getLength();
		for (int i = 0; i < len; i++) {
			String attrName = attributes.getLocalName(i);
			String attrValue = attributes.getValue(i).trim();
			if (attrName.equals(PLUGIN_ID))
				manifestInfo.pluginId = attrValue;
			else if (attrName.equals(PLUGIN_NAME))
				manifestInfo.pluginName = attrValue;
			else if (attrName.equals(PLUGIN_VERSION))
				manifestInfo.version = attrValue;
			else if (attrName.equals(PLUGIN_VENDOR) || (attrName.equals(PLUGIN_PROVIDER)))
				manifestInfo.vendor = attrValue;
			else if (attrName.equals(PLUGIN_CLASS))
				manifestInfo.pluginClass = attrValue;
		}
	}
	public void parsePluginRequiresImport(Attributes attributes) {
		if (manifestInfo.requires == null) {
			manifestInfo.requires = new ArrayList();
			// to avoid cycles
			if (!manifestInfo.pluginId.equals("org.eclipse.core.runtime"))  //$NON-NLS-1$
				manifestInfo.requires.add("org.eclipse.core.runtime"); //$NON-NLS-1$
		}
		// process attributes
		String plugin = attributes.getValue("", PLUGIN_REQUIRES_PLUGIN); //$NON-NLS-1$ 
		if (plugin == null)
			return;
		if (plugin.equals("org.eclipse.core.boot") || plugin.equals("org.eclipse.core.runtime"))  //$NON-NLS-1$//$NON-NLS-2$
			return;
		String version = attributes.getValue("", PLUGIN_REQUIRES_PLUGIN_VERSION); //$NON-NLS-1$ 
		String optional = attributes.getValue("", PLUGIN_REQUIRES_OPTIONAL); //$NON-NLS-1$ 
		String export = attributes.getValue("", PLUGIN_REQUIRES_EXPORT); //$NON-NLS-1$ 
		String match = attributes.getValue("", PLUGIN_REQUIRES_MATCH); //$NON-NLS-1$ 
		String modImport = plugin;
		if (version != null) {
			modImport += "; " + Constants.BUNDLE_VERSION_ATTRIBUTE + "=" + version; //$NON-NLS-1$ //$NON-NLS-2$ 
		}
		if (export != null) {
			modImport += "; " + Constants.PROVIDE_PACKAGES_ATTRIBUTE + "=" + export;//$NON-NLS-1$ //$NON-NLS-2$ 
		}
		if (optional != null) {
			modImport += ";" + Constants.OPTIONAL_ATTRIBUTE + "=" + "true";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		}
		if (match != null) {
			modImport += ";" + Constants.VERSION_MATCH_ATTRIBUTE + "="; //$NON-NLS-1$ //$NON-NLS-2$ 
			if (match.equalsIgnoreCase(PLUGIN_REQUIRES_MATCH_PERFECT)) {
				modImport += Constants.VERSION_MATCH_PERFECT;
			} else if (match.equalsIgnoreCase(PLUGIN_REQUIRES_MATCH_EQUIVALENT)) {
				modImport += Constants.VERSION_MATCH_EQUIVALENT;
			} else if (match.equalsIgnoreCase(PLUGIN_REQUIRES_MATCH_COMPATIBLE)) {
				modImport += Constants.VERSION_MATCH_COMPATIBLE;
			} else if (match.equalsIgnoreCase(PLUGIN_REQUIRES_MATCH_GREATER_OR_EQUAL)) {
				modImport += Constants.VERSION_MATCH_GREATERTHANOREQUAL;
			}
		}
		manifestInfo.requires.add(modImport);
	}
	public void parseRequiresAttributes(Attributes attributes) {
	}
	static String replace(String s, String from, String to) {
		String str = s;
		int fromLen = from.length();
		int toLen = to.length();
		int ix = str.indexOf(from);
		while (ix != -1) {
			str = str.substring(0, ix) + to + str.substring(ix + fromLen);
			ix = str.indexOf(from, ix + toLen);
		}
		return str;
	}
	public void startDocument() {
		stateStack.push(new Integer(INITIAL_STATE));
	}
	public void startElement(String uri, String elementName, String qName, Attributes attributes) {
		switch (((Integer) stateStack.peek()).intValue()) {
			case INITIAL_STATE :
				handleInitialState(elementName, attributes);
				break;
			case FRAGMENT_STATE :
			case PLUGIN_STATE :
				handlePluginState(elementName, attributes);
				break;
			case PLUGIN_RUNTIME_STATE :
				handleRuntimeState(elementName, attributes);
				break;
			case PLUGIN_REQUIRES_STATE :
				handleRequiresState(elementName, attributes);
				break;
			case PLUGIN_EXTENSION_POINT_STATE :
				handleExtensionPointState(elementName, attributes);
				break;
			case PLUGIN_EXTENSION_STATE :
			case CONFIGURATION_ELEMENT_STATE :
				handleExtensionState(elementName, attributes);
				break;
			case RUNTIME_LIBRARY_STATE :
				handleLibraryState(elementName, attributes);
				break;
			case LIBRARY_EXPORT_STATE :
				handleLibraryExportState(elementName, attributes);
				break;
			case PLUGIN_REQUIRES_IMPORT_STATE :
				handleRequiresImportState(elementName, attributes);
				break;
			default :
				stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		//internalError(Policy.bind("parse.unknownTopElement", elementName));
		// //$NON-NLS-1$
		}
	}
	public void warning(SAXParseException ex) {
		logStatus(ex);
	}
	private void internalError(String message) {
		//                if (locationName != null)
		//                        factory.error(new Status(IStatus.WARNING, Platform.PI_RUNTIME,
		// Platform.PARSE_PROBLEM, locationName + ": " + message, null));
		// //$NON-NLS-1$
		//                else
		//                        factory.error(new Status(IStatus.WARNING, Platform.PI_RUNTIME,
		// Platform.PARSE_PROBLEM, message, null));
	}
	public void processingInstruction(String target, String data) throws SAXException {
		// Since 3.0, a processing instruction of the form <?eclipse version="3.0"?> at
		// the start of the manifest file is used to indicate the plug-in manifest
		// schema version in effect. Pre-3.0 (i.e., 2.1) plug-in manifest files do not
		// have one of these, and this is how we can distinguish the manifest of a
		// pre-3.0 plug-in from a post-3.0 one (for compatibility tranformations).
		if (target.equalsIgnoreCase("eclipse")) { //$NON-NLS-1$ 
			// just the presence of this processing instruction indicates that this
			// plug-in is at least 3.0
			manifestInfo.schemaVersion = "3.0"; //$NON-NLS-1$ 
			StringTokenizer tokenizer = new StringTokenizer(data, "=\""); //$NON-NLS-1$ 
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.equalsIgnoreCase("version")) { //$NON-NLS-1$ 
					if (!tokenizer.hasMoreTokens()) {
						break;
					}
					manifestInfo.schemaVersion = tokenizer.nextToken();
					break;
				}
			}
		}
	}
}