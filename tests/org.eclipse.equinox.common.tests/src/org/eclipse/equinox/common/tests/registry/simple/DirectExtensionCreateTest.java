/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.common.tests.registry.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.internal.registry.spi.ConfigurationElementAttribute;
import org.eclipse.core.internal.registry.spi.ConfigurationElementDescription;
import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.junit.Test;

/**
 * Tests programmatic creation of extension and extension point by using direct
 * methods on the ExtensionRegistry.
 *
 * Note that in present those methods are internal, but might be exposed as
 * APIs in the future.
 *
 * @since 3.2
 */
public class DirectExtensionCreateTest extends BaseExtensionRegistryRun {

	@Test
	public void testExtensionPointAddition() {
		IContributor contributor = ContributorFactorySimple.createContributor("1"); //$NON-NLS-1$
		String extensionPointId = "DirectExtPoint"; //$NON-NLS-1$
		String extensionPointLabel = "Direct Extension Point"; //$NON-NLS-1$
		String extensionPointSchemaRef = "schema/ExtensionPointTest.exsd"; //$NON-NLS-1$

		/**********************************************************************************************
		 * Add extension point:
		 *
		 * <extension-point id="DirectExtPoint"
		 * 		name="Direct Extension Point"
		 * 		schema="schema/ExtensionPointTest.exsd"/>
		 *
		 *********************************************************************************************/

		((ExtensionRegistry) simpleRegistry).addExtensionPoint(extensionPointId, contributor, false, extensionPointLabel, extensionPointSchemaRef, userToken);

		String namespace = contributor.getName();
		IExtensionPoint extensionPoint = simpleRegistry.getExtensionPoint(qualifiedName(namespace, extensionPointId));
		assertNotNull(extensionPoint);
		assertTrue(extensionPointSchemaRef.equals(extensionPoint.getSchemaReference()));
		assertTrue(extensionPointLabel.equals(extensionPoint.getLabel()));

		// add second contribution in the same namespace
		String extensionPointAltId = "DirectExtPointAlt"; //$NON-NLS-1$
		String extensionPointAltLabel = "Second direct extension point"; //$NON-NLS-1$
		assertTrue(((ExtensionRegistry) simpleRegistry).addExtensionPoint(extensionPointAltId, contributor, false, extensionPointAltLabel, extensionPointSchemaRef, userToken));

		IExtensionPoint extensionPointAlt = simpleRegistry.getExtensionPoint(qualifiedName(namespace, extensionPointAltId));
		assertNotNull(extensionPointAlt);
		assertTrue(extensionPointSchemaRef.equals(extensionPointAlt.getSchemaReference()));
		assertTrue(extensionPointAltLabel.equals(extensionPointAlt.getLabel()));

		/**********************************************************************************************
		 * Add extension:
		 * <extension id="DirectExtensionID" name="Direct Extension" point="DirectExtPoint">
		 * 		<StorageDevice deviceURL="theShienneMountain">
		 * 			<BackupDevice backupURL="SkyLab"/>
		 * 			<BackupDevice backupURL="OceanFloor"/>
		 * 		</StorageDevice>
		 * </extension>
		 *********************************************************************************************/
		String extensionId = "DirectExtensionID"; //$NON-NLS-1$
		String extensionLabel = "Direct Extension"; //$NON-NLS-1$

		String nameChildDesc = "BackupDevice"; //$NON-NLS-1$
		String propNameChildDesc = "backupURL"; //$NON-NLS-1$
		String propValueChildDesc1 = "SkyLab"; //$NON-NLS-1$
		String propValueChildDesc2 = "OceanFloor"; //$NON-NLS-1$

		ConfigurationElementAttribute propChildDesc1 = new ConfigurationElementAttribute(propNameChildDesc, propValueChildDesc1);
		ConfigurationElementDescription childDesc1 = new ConfigurationElementDescription(nameChildDesc, propChildDesc1, null, null);

		ConfigurationElementAttribute propChildDesc2 = new ConfigurationElementAttribute(propNameChildDesc, propValueChildDesc2);
		ConfigurationElementDescription childDesc2 = new ConfigurationElementDescription(nameChildDesc, propChildDesc2, null, null);

		String extensionName = "StorageDevice"; //$NON-NLS-1$
		String extensionProrName1 = "deviceURL"; //$NON-NLS-1$
		String extensionPropValue1 = "theShienneMountain"; //$NON-NLS-1$
		String extensionProrName2 = "primary"; //$NON-NLS-1$
		String extensionPropValue2 = "true"; //$NON-NLS-1$
		ConfigurationElementAttribute prop1 = new ConfigurationElementAttribute(extensionProrName1, extensionPropValue1);
		ConfigurationElementAttribute prop2 = new ConfigurationElementAttribute(extensionProrName2, extensionPropValue2);
		String extensionValue = "SomeValue"; //$NON-NLS-1$

		ConfigurationElementDescription description = new ConfigurationElementDescription(extensionName, new ConfigurationElementAttribute[] {prop1, prop2}, extensionValue, new ConfigurationElementDescription[] {childDesc1, childDesc2});

		assertTrue(((ExtensionRegistry) simpleRegistry).addExtension(extensionId, contributor, false, extensionLabel, extensionPointId, description, userToken));

		IExtension[] namespaceExtensions = simpleRegistry.getExtensions(namespace);
		assertNotNull(namespaceExtensions);
		assertEquals(1, namespaceExtensions.length);
		IExtension[] extensions = extensionPoint.getExtensions();
		assertNotNull(extensions);
		assertEquals(1, extensions.length);
		for (IExtension extension : extensions) {
			String storedExtensionId = extension.getUniqueIdentifier();
			assertTrue(storedExtensionId.equals(qualifiedName(namespace, extensionId)));
			String extensionNamespace = extension.getNamespaceIdentifier();
			assertTrue(extensionNamespace.equals(namespace));
			String extensionContributor = extension.getContributor().getName();
			assertTrue(extensionContributor.equals(namespace));
			IConfigurationElement[] configElements = extension.getConfigurationElements();
			assertNotNull(configElements);
			for (IConfigurationElement configElement : configElements) {
				String configElementName = configElement.getName();
				assertTrue(configElementName.equals(extensionName));
				String configElementValue = configElement.getValue();
				assertTrue(configElementValue.equals(extensionValue));
				String[] attributeNames = configElement.getAttributeNames();
				assertEquals(2, attributeNames.length);
				IConfigurationElement[] configElementChildren = configElement.getChildren();
				assertEquals(2, configElementChildren.length);
			}
		}
	}
}
