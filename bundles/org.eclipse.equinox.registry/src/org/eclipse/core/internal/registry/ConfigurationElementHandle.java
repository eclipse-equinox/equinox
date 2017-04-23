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

	@Override
	public String getAttribute(String propertyName) {
		return getConfigurationElement().getAttribute(propertyName);
	}

	@Override
	public String getAttribute(String attrName, String locale) {
		return getConfigurationElement().getAttribute(attrName, locale);
	}

	@Override
	public String[] getAttributeNames() {
		return getConfigurationElement().getAttributeNames();
	}

	@Override
	public IConfigurationElement[] getChildren() {
		ConfigurationElement actualCe = getConfigurationElement();
		if (actualCe.noExtraData()) {
			return (IConfigurationElement[]) objectManager.getHandles(actualCe.getRawChildren(), RegistryObjectManager.CONFIGURATION_ELEMENT);
		}
		return (IConfigurationElement[]) objectManager.getHandles(actualCe.getRawChildren(), RegistryObjectManager.THIRDLEVEL_CONFIGURATION_ELEMENT);
	}

	@Override
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

	@Override
	public String getAttributeAsIs(String name) {
		return getConfigurationElement().getAttributeAsIs(name);
	}

	@Override
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

	@Override
	public IExtension getDeclaringExtension() {
		Object result = this;
		while (!((result = ((ConfigurationElementHandle) result).getParent()) instanceof ExtensionHandle)) { /*do nothing*/
		}
		return (IExtension) result;
	}

	@Override
	public String getName() {
		return getConfigurationElement().getName();
	}

	@Override
	public Object getParent() {
		ConfigurationElement actualCe = getConfigurationElement();
		return objectManager.getHandle(actualCe.parentId, actualCe.parentType);
	}

	@Override
	public String getValue() {
		return getConfigurationElement().getValue();
	}

	@Override
	public String getValue(String locale) {
		return getConfigurationElement().getValue(locale);
	}

	@Override
	public String getValueAsIs() {
		return getConfigurationElement().getValueAsIs();
	}

	@Override
	RegistryObject getObject() {
		return getConfigurationElement();
	}

	// Method left for backward compatibility only
	@Override
	public String getNamespace() {
		return getContributor().getName();
	}

	@Override
	public String getNamespaceIdentifier() {
		// namespace name is determined by the contributing extension
		return getDeclaringExtension().getNamespaceIdentifier();
	}

	@Override
	public IContributor getContributor() {
		return getConfigurationElement().getContributor();
	}

	@Override
	public boolean isValid() {
		try {
			getConfigurationElement();
		} catch (InvalidRegistryObjectException e) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ConfigurationElementHandle ["); //$NON-NLS-1$
		sb.append("name: "); //$NON-NLS-1$
		sb.append(getName());
		String id = getAttribute("id"); //$NON-NLS-1$
		if (id != null && id.length() > 0) {
			sb.append(", id: ").append(id); //$NON-NLS-1$
		}
		String value = getValue();
		if (value != null) {
			sb.append(", value: ").append(value); //$NON-NLS-1$
		}
		sb.append(", handle id: ").append(hashCode()); //$NON-NLS-1$
		sb.append(", namespace: "); //$NON-NLS-1$
		sb.append(getNamespaceIdentifier());
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

}
