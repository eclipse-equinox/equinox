/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.core.internal.registry;

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.core.runtime.*;

/**
 * This is the copy of the ExtensionPointHandle minus the getDeclaringPluginDescriptor()
 * method that was moved into compatibility plugin.
 *
 * This class should not be used directly. Use ExtensionPointHandle instead.
 *
 * @since org.eclipse.equinox.registry 3.2
 */
public class BaseExtensionPointHandle extends Handle implements IExtensionPoint {

	public BaseExtensionPointHandle(IObjectManager objectManager, int id) {
		super(objectManager, id);
	}

	@Override
	public IExtension[] getExtensions() {
		return (IExtension[]) objectManager.getHandles(getExtensionPoint().getRawChildren(), RegistryObjectManager.EXTENSION);
	}

	// This method is left for backward compatibility only
	@Override
	public String getNamespace() {
		return getContributor().getName();
	}

	@Override
	public String getNamespaceIdentifier() {
		return getExtensionPoint().getNamespace();
	}

	@Override
	public IContributor getContributor() {
		return getExtensionPoint().getContributor();
	}

	protected boolean shouldPersist() {
		return getExtensionPoint().shouldPersist();
	}

	@Override
	public IExtension getExtension(String extensionId) {
		if (extensionId == null)
			return null;
		for (int element : getExtensionPoint().getRawChildren()) {
			//	Here we directly get the object because it avoids the creation of garbage and because we'll need the object anyway to compare the value
			if (extensionId.equals(((Extension) objectManager.getObject(element, RegistryObjectManager.EXTENSION)).getUniqueIdentifier()))
				return (ExtensionHandle) objectManager.getHandle(element, RegistryObjectManager.EXTENSION);
		}
		return null;
	}

	@Override
	public IConfigurationElement[] getConfigurationElements() {
		//get the actual extension objects since we'll need to get the configuration elements information.
		Extension[] tmpExtensions = (Extension[]) objectManager.getObjects(getExtensionPoint().getRawChildren(), RegistryObjectManager.EXTENSION);
		if (tmpExtensions.length == 0)
			return ConfigurationElementHandle.EMPTY_ARRAY;

		ArrayList<Handle> result = new ArrayList<>();
		for (Extension tmpExtension : tmpExtensions) {
			result.addAll(Arrays.asList(objectManager.getHandles(tmpExtension.getRawChildren(), RegistryObjectManager.CONFIGURATION_ELEMENT)));
		}
		return result.toArray(new IConfigurationElement[result.size()]);
	}

	public String getLabelAsIs() {
		return getExtensionPoint().getLabelAsIs();
	}

	@Override
	public String getLabel() {
		return getExtensionPoint().getLabel();
	}

	@Override
	public String getLabel(String locale) {
		return getExtensionPoint().getLabel(locale);
	}

	@Override
	public String getSchemaReference() {
		return getExtensionPoint().getSchemaReference();
	}

	@Override
	public String getSimpleIdentifier() {
		return getExtensionPoint().getSimpleIdentifier();
	}

	@Override
	public String getUniqueIdentifier() {
		return getExtensionPoint().getUniqueIdentifier();
	}

	@Override
	RegistryObject getObject() {
		return getExtensionPoint();
	}

	protected ExtensionPoint getExtensionPoint() {
		return (ExtensionPoint) objectManager.getObject(getId(), RegistryObjectManager.EXTENSION_POINT);
	}

	@Override
	public boolean isValid() {
		try {
			getExtensionPoint();
		} catch (InvalidRegistryObjectException e) {
			return false;
		}
		return true;
	}
}
