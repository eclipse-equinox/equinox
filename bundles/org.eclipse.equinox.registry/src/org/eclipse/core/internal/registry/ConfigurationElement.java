/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.util.Hashtable;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.registry.IExecutableExtension;
import org.eclipse.equinox.registry.IExecutableExtensionFactory;
import org.eclipse.osgi.util.NLS;

/**
 * An object which represents the user-defined contents of an extension
 * in a plug-in manifest.
 */
public class ConfigurationElement extends RegistryObject {
	static final ConfigurationElement[] EMPTY_ARRAY = new ConfigurationElement[0];

	//The id of the parent element. It can be a configuration element or an extension
	int parentId;
	byte parentType; //This value is only interesting when running from cache.

	//Store the properties and the value of the configuration element.
	//The format is the following: 
	//	[p1, v1, p2, v2, configurationElementValue]
	//If the array size is even, there is no "configurationElementValue (ie getValue returns null)".
	//The properties and their values are alternated (v1 is the value of p1). 
	private String[] propertiesAndValue;

	//The name of the configuration element
	private String name;

	//The ID of the namespace owner. 
	//This value can be null when the element is loaded from disk and the owner has been uninstalled.
	//This happens when the configuration is obtained from a delta containing removed extension.
	private String contributorId;

	protected ConfigurationElement(ExtensionRegistry registry, boolean persist) {
		super(registry, persist);
	}

	protected ConfigurationElement(int self, String contributorId, String name, String[] propertiesAndValue, int[] children, int extraDataOffset, int parent, byte parentType, ExtensionRegistry registry, boolean persist) {
		super(registry, persist);

		setObjectId(self);
		this.contributorId = contributorId;
		this.name = name;
		this.propertiesAndValue = propertiesAndValue;
		setRawChildren(children);
		setExtraDataOffset(extraDataOffset);
		parentId = parent;
		this.parentType = parentType;
	}

	void throwException(String message, Throwable exception) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, message, exception));
	}

	protected String getValue() {
		return getValueAsIs();
	}

	String getValueAsIs() {
		if (propertiesAndValue.length != 0 && propertiesAndValue.length % 2 == 1)
			return propertiesAndValue[propertiesAndValue.length - 1];
		return null;
	}

	public String getAttribute(String attrName) {
		return getAttributeAsIs(attrName);
	}

	String getAttributeAsIs(String attrName) {
		if (propertiesAndValue.length <= 1)
			return null;
		int size = propertiesAndValue.length - (propertiesAndValue.length % 2);
		for (int i = 0; i < size; i += 2) {
			if (propertiesAndValue[i].equals(attrName))
				return propertiesAndValue[i + 1];
		}
		return null;
	}

	protected String[] getAttributeNames() {
		if (propertiesAndValue.length <= 1)
			return RegistryObjectManager.EMPTY_STRING_ARRAY;

		int size = propertiesAndValue.length / 2;
		String[] result = new String[size];
		for (int i = 0; i < size; i++) {
			result[i] = propertiesAndValue[i * 2];
		}
		return result;
	}

	void setProperties(String[] value) {
		propertiesAndValue = value;
	}

	protected String[] getPropertiesAndValue() {
		return propertiesAndValue;
	}

	void setValue(String value) {
		if (propertiesAndValue.length == 0) {
			propertiesAndValue = new String[] {value};
			return;
		}
		if (propertiesAndValue.length % 2 == 1) {
			propertiesAndValue[propertiesAndValue.length - 1] = value;
			return;
		}
		String[] newPropertiesAndValue = new String[propertiesAndValue.length + 1];
		System.arraycopy(propertiesAndValue, 0, newPropertiesAndValue, 0, propertiesAndValue.length);
		newPropertiesAndValue[propertiesAndValue.length] = value;
		propertiesAndValue = newPropertiesAndValue;
	}

	void setNamespaceOwnerId(String namespaceOwnerId) {
		this.contributorId = namespaceOwnerId;
	}

	protected String getNamespaceOwnerId() {
		return contributorId;
	}

	public ConfigurationElement[] getChildren(String childrenName) {
		if (getRawChildren().length == 0)
			return ConfigurationElement.EMPTY_ARRAY;

		ConfigurationElement[] result = new ConfigurationElement[1]; //Most of the time there is only one match
		int idx = 0;
		RegistryObjectManager objectManager = registry.getObjectManager();
		for (int i = 0; i < children.length; i++) {
			ConfigurationElement toTest = (ConfigurationElement) objectManager.getObject(children[i], noExtraData() ? RegistryObjectManager.CONFIGURATION_ELEMENT : RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
			if (toTest.name.equals(childrenName)) {
				if (idx != 0) {
					ConfigurationElement[] copy = new ConfigurationElement[result.length + 1];
					System.arraycopy(result, 0, copy, 0, result.length);
					result = copy;
				}
				result[idx++] = toTest;
			}
		}
		if (idx == 0)
			result = ConfigurationElement.EMPTY_ARRAY;
		return result;
	}

	void setParentId(int objectId) {
		parentId = objectId;
	}

	protected String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	void setParentType(byte type) {
		parentType = type;
	}

	protected String getNamespace() {
		return registry.getNamespace(contributorId);
	}

	protected Object createExecutableExtension(String attributeName) throws CoreException {
		String prop = null;
		String executable;
		String contributorName = null;
		String className = null;
		Object initData = null;
		int i;

		if (attributeName != null)
			prop = getAttribute(attributeName);
		else {
			// property not specified, try as element value
			prop = getValue();
			if (prop != null) {
				prop = prop.trim();
				if (prop.equals("")) //$NON-NLS-1$
					prop = null;
			}
		}

		if (prop == null) {
			// property not defined, try as a child element
			ConfigurationElement[] exec;
			ConfigurationElement[] parms;
			ConfigurationElement element;
			Hashtable initParms;
			String pname;

			exec = getChildren(attributeName);
			if (exec.length != 0) {
				element = exec[0]; // assumes single definition
				contributorName = element.getAttribute("plugin"); //$NON-NLS-1$
				className = element.getAttribute("class"); //$NON-NLS-1$
				parms = element.getChildren("parameter"); //$NON-NLS-1$
				if (parms.length != 0) {
					initParms = new Hashtable(parms.length + 1);
					for (i = 0; i < parms.length; i++) {
						pname = parms[i].getAttribute("name"); //$NON-NLS-1$
						if (pname != null)
							initParms.put(pname, parms[i].getAttribute("value")); //$NON-NLS-1$
					}
					if (!initParms.isEmpty())
						initData = initParms;
				}
			} else {
				// specified name is not a simple attribute nor child element
				throwException(NLS.bind(RegistryMessages.exExt_extDefNotFound, attributeName), null);
			}
		} else {
			// simple property or element value, parse it into its components
			i = prop.indexOf(':');
			if (i != -1) {
				executable = prop.substring(0, i).trim();
				initData = prop.substring(i + 1).trim();
			} else
				executable = prop;

			i = executable.indexOf('/');
			if (i != -1) {
				contributorName = executable.substring(0, i).trim();
				className = executable.substring(i + 1).trim();
			} else
				className = executable;
		}

		// create a new instance
		Object result = null;

		// check if alternative processing strategy is present 
		result = registry.processExecutableExtension(contributorName, contributorId, getNamespace(), className, initData, attributeName, this);

		// Check if we have extension adapter and initialize;
		// Make the call even if the initialization string is null
		try {
			// We need to take into account both "old" and "new" style executable extensions 
			ConfigurationElementHandle confElementHandle = new ConfigurationElementHandle(registry.getObjectManager(), getObjectId());
			if (result instanceof IExecutableExtension) {
				((IExecutableExtension) result).setInitializationData(confElementHandle, attributeName, initData);
			}
			if (registry.compatibilityStrategy != null)
				registry.compatibilityStrategy.setInitializationData(result, confElementHandle, attributeName, initData);
		} catch (CoreException ce) {
			// user code threw exception
			throw ce;
		} catch (Exception te) {
			// user code caused exception
			throwException(NLS.bind(RegistryMessages.plugin_initObjectError, getNamespace(), className), te);
		}

		// Deal with executable extension factories.
		if (result instanceof IExecutableExtensionFactory) {
			result = ((IExecutableExtensionFactory) result).create();
		}
		if (registry.compatibilityStrategy != null)
			result = registry.compatibilityStrategy.create(result);

		return result;
	}

}
