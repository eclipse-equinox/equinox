/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

/**
 * A factory method for the creation of the registry objects.
 */
public class RegistryObjectFactory {

	// The extension registry that this element factory works in
	protected ExtensionRegistry registry;

	public RegistryObjectFactory(ExtensionRegistry registry) {
		this.registry = registry;
	}

	////////////////////////////////////////////////////////////////////////////
	// Contribution
	public Contribution createContribution(String contributorId, boolean persist) {
		return new Contribution(contributorId, registry, persist);
	}

	////////////////////////////////////////////////////////////////////////////
	// Extension point
	public ExtensionPoint createExtensionPoint(boolean persist) {
		return new ExtensionPoint(registry, persist);
	}

	public ExtensionPoint createExtensionPoint(int self, int[] children, int dataOffset, boolean persist) {
		return new ExtensionPoint(self, children, dataOffset, registry, persist);
	}

	////////////////////////////////////////////////////////////////////////////
	// Extension
	public Extension createExtension(boolean persist) {
		return new Extension(registry, persist);
	}

	public Extension createExtension(int self, String simpleId, String namespace, int[] children, int extraData, boolean persist) {
		return new Extension(self, simpleId, namespace, children, extraData, registry, persist);
	}

	////////////////////////////////////////////////////////////////////////////
	// Configuration element
	public ConfigurationElement createConfigurationElement(boolean persist) {
		return new ConfigurationElement(registry, persist);
	}

	public ConfigurationElement createConfigurationElement(int self, String contributorId, String name, String[] propertiesAndValue, int[] children, int extraDataOffset, int parent, byte parentType, boolean persist) {
		return new ConfigurationElement(self, contributorId, name, propertiesAndValue, children, extraDataOffset, parent, parentType, registry, persist);
	}
}
