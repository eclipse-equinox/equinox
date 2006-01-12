/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
	public Contribution createContribution(String contributorId, boolean isDynamic) {
		return new Contribution(contributorId, registry, isDynamic);
	}

	////////////////////////////////////////////////////////////////////////////
	// Extension point
	public ExtensionPoint createExtensionPoint(boolean isDynamic) {
		return new ExtensionPoint(registry, isDynamic);
	}

	public ExtensionPoint createExtensionPoint(int self, int[] children, int dataOffset, boolean isDynamic) {
		return new ExtensionPoint(self, children, dataOffset, registry, isDynamic);
	}

	////////////////////////////////////////////////////////////////////////////
	// Extension
	public Extension createExtension(boolean isDynamic) {
		return new Extension(registry, isDynamic);
	}

	public Extension createExtension(int self, String simpleId, String namespace, int[] children, int extraData, boolean isDynamic) {
		return new Extension(self, simpleId, namespace, children, extraData, registry, isDynamic);
	}

	////////////////////////////////////////////////////////////////////////////
	// Configuration element
	public ConfigurationElement createConfigurationElement(boolean isDynamic) {
		return new ConfigurationElement(registry, isDynamic);
	}

	public ConfigurationElement createConfigurationElement(int self, String contributorId, String name, String[] propertiesAndValue, int[] children, int extraDataOffset, int parent, byte parentType, boolean isDynamic) {
		return new ConfigurationElement(self, contributorId, name, propertiesAndValue, children, extraDataOffset, parent, parentType, registry, isDynamic);
	}
}
