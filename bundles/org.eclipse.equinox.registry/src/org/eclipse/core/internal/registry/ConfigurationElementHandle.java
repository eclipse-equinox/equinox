/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import org.eclipse.core.runtime.*;

/**
 * @since 3.1 
 */
public class ConfigurationElementHandle extends Handle implements IConfigurationElement {
	static final ConfigurationElementHandle[] EMPTY_ARRAY = new ConfigurationElementHandle[0];

	public ConfigurationElementHandle(IObjectManager objectManager, int id) {
		super(objectManager, id);
	}

	protected ConfigurationElement getConfigurationElement() {
		return (ConfigurationElement) objectManager.getObject(getId(), RegistryObjectManager.CONFIGURATION_ELEMENT);
	}

	protected boolean shouldPersist() {
		return getConfigurationElement().shouldPersist();
	}

	public String getAttribute(String propertyName) {
		return getConfigurationElement().getAttribute(propertyName);
	}

	public String getAttribute(String attrName, String locale) {
		return getConfigurationElement().getAttribute(attrName, locale);
	}

	public String[] getAttributeNames() {
		return getConfigurationElement().getAttributeNames();
	}

	public IConfigurationElement[] getChildren() {
		ConfigurationElement actualCe = getConfigurationElement();
		if (actualCe.noExtraData()) {
			return (IConfigurationElement[]) objectManager.getHandles(actualCe.getRawChildren(), RegistryObjectManager.CONFIGURATION_ELEMENT);
		}
		return (IConfigurationElement[]) objectManager.getHandles(actualCe.getRawChildren(), RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
	}

	public Object createExecutableExtension(String propertyName) throws CoreException {
		try {
			return getConfigurationElement().createExecutableExtension(propertyName);
		} catch (InvalidRegistryObjectException e) {
			Status status = new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, "Invalid registry object", e); //$NON-NLS-1$
			if (objectManager instanceof RegistryObjectManager)
				((RegistryObjectManager) objectManager).getRegistry().log(status);
			throw new CoreException(status);
		}
	}

	public String getAttributeAsIs(String name) {
		return getConfigurationElement().getAttributeAsIs(name);
	}

	public IConfigurationElement[] getChildren(String name) {
		ConfigurationElement actualCE = getConfigurationElement();
		ConfigurationElement[] children = (ConfigurationElement[]) objectManager.getObjects(actualCE.getRawChildren(), actualCE.noExtraData() ? RegistryObjectManager.CONFIGURATION_ELEMENT : RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
		if (children.length == 0)
			return ConfigurationElementHandle.EMPTY_ARRAY;

		IConfigurationElement[] result = new IConfigurationElement[1];
		int idx = 0;
		for (int i = 0; i < children.length; i++) {
			if (children[i].getName().equals(name)) {
				if (idx != 0) {
					IConfigurationElement[] copy = new IConfigurationElement[result.length + 1];
					System.arraycopy(result, 0, copy, 0, result.length);
					result = copy;
				}
				result[idx++] = (IConfigurationElement) objectManager.getHandle(children[i].getObjectId(), actualCE.noExtraData() ? RegistryObjectManager.CONFIGURATION_ELEMENT : RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
			}
		}
		if (idx == 0)
			return ConfigurationElementHandle.EMPTY_ARRAY;
		return result;
	}

	public IExtension getDeclaringExtension() {
		Object result = this;
		while (!((result = ((ConfigurationElementHandle) result).getParent()) instanceof ExtensionHandle)) { /*do nothing*/
		}
		return (IExtension) result;
	}

	public String getName() {
		return getConfigurationElement().getName();
	}

	public Object getParent() {
		ConfigurationElement actualCe = getConfigurationElement();
		return objectManager.getHandle(actualCe.parentId, actualCe.parentType);
	}

	public String getValue() {
		return getConfigurationElement().getValue();
	}

	public String getValue(String locale) {
		return getConfigurationElement().getValue(locale);
	}

	public String getValueAsIs() {
		return getConfigurationElement().getValueAsIs();
	}

	RegistryObject getObject() {
		return getConfigurationElement();
	}

	// Method left for backward compatibility only
	public String getNamespace() {
		return getContributor().getName();
	}

	public String getNamespaceIdentifier() {
		// namespace name is determined by the contributing extension
		return getDeclaringExtension().getNamespaceIdentifier();
	}

	public IContributor getContributor() {
		return getConfigurationElement().getContributor();
	}

	public boolean isValid() {
		try {
			getConfigurationElement();
		} catch (InvalidRegistryObjectException e) {
			return false;
		}
		return true;
	}
}
