/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

/**
 * A factory method for the creation of the registry objects for registries
 * supporting multiple languages.
 */
public class RegistryObjectFactoryMulti extends RegistryObjectFactory {

	public RegistryObjectFactoryMulti(ExtensionRegistry registry) {
		super(registry);
	}

	@Override
	public ExtensionPoint createExtensionPoint(boolean persist) {
		return new ExtensionPointMulti(registry, persist);
	}

	@Override
	public ExtensionPoint createExtensionPoint(int self, int[] children, int dataOffset, boolean persist) {
		return new ExtensionPointMulti(self, children, dataOffset, registry, persist);
	}

	@Override
	public Extension createExtension(boolean persist) {
		return new ExtensionMulti(registry, persist);
	}

	@Override
	public Extension createExtension(int self, String simpleId, String namespace, int[] children, int extraData,
			boolean persist) {
		return new ExtensionMulti(self, simpleId, namespace, children, extraData, registry, persist);
	}

	@Override
	public ConfigurationElement createConfigurationElement(boolean persist) {
		return new ConfigurationElementMulti(registry, persist);
	}

	@Override
	public ConfigurationElement createConfigurationElement(int self, String contributorId, String name,
			String[] propertiesAndValue, int[] children, int extraDataOffset, int parent, byte parentType,
			boolean persist) {
		return new ConfigurationElementMulti(self, contributorId, name, propertiesAndValue, children, extraDataOffset,
				parent, parentType, registry, persist);
	}
}
