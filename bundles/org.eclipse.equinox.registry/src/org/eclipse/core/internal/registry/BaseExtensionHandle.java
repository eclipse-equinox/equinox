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
 * This is the copy of the ExtensionHandle minus the getDeclaringPluginDescriptor()
 * method that was moved into compatibility plugin.
 * 
 * This class should not be used directly. Use ExtensionHandle instead.
 * 
 * @since org.eclipse.equinox.registry 3.2 
 */
public class BaseExtensionHandle extends Handle implements IExtension {

	public BaseExtensionHandle(IObjectManager objectManager, int id) {
		super(objectManager, id);
	}

	protected Extension getExtension() {
		return (Extension) objectManager.getObject(getId(), RegistryObjectManager.EXTENSION);
	}

	protected boolean shouldPersist() {
		return getExtension().shouldPersist();
	}

	// Method left for backward compatiblity only
	public String getNamespace() {
		return getContributor().getName();
	}

	public String getNamespaceIdentifier() {
		return getExtension().getNamespaceIdentifier();
	}

	public IContributor getContributor() {
		return getExtension().getContributor();
	}

	String getContributorId() {
		return getExtension().getContributorId();
	}

	public String getExtensionPointUniqueIdentifier() {
		return getExtension().getExtensionPointIdentifier();
	}

	public String getLabel() {
		return getExtension().getLabel();
	}

	public String getLabelAsIs() {
		return getExtension().getLabelAsIs();
	}

	public String getLabel(String locale) {
		return getExtension().getLabel(locale);
	}

	public String getSimpleIdentifier() {
		return getExtension().getSimpleIdentifier();
	}

	public String getUniqueIdentifier() {
		return getExtension().getUniqueIdentifier();
	}

	public IConfigurationElement[] getConfigurationElements() {
		return (IConfigurationElement[]) objectManager.getHandles(getExtension().getRawChildren(), RegistryObjectManager.CONFIGURATION_ELEMENT);
	}

	RegistryObject getObject() {
		return getExtension();
	}

	public boolean isValid() {
		try {
			getExtension();
		} catch (InvalidRegistryObjectException e) {
			return false;
		}
		return true;
	}
}
