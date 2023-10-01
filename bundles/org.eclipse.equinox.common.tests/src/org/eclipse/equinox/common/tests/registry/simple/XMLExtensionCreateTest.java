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

import java.io.IOException;

import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.equinox.common.tests.registry.simple.utils.SimpleRegistryListener;
import org.junit.Test;

/**
 * Tests addition of extension point and the extension to the registry via XML
 * contribution. Makes sure that items are actually added; checks listener
 * notification; reloads registry from cache and re-checks the data.
 *
 * @since 3.2
 */
public class XMLExtensionCreateTest extends BaseExtensionRegistryRun {

	@Test
	public void testExtensionPointAddition() throws IOException {
		SimpleRegistryListener listener = new SimpleRegistryListener();
		listener.register(simpleRegistry);

		// Test with non-bundle contributor
		IContributor nonBundleContributor = ContributorFactorySimple.createContributor("ABC"); //$NON-NLS-1$
		fillRegistry(nonBundleContributor);

		String namespace = nonBundleContributor.getName();
		checkListener(namespace, listener);
		checkRegistry(nonBundleContributor.getName());

		listener.unregister(simpleRegistry);

		// check the cache: stop -> re-start
		stopRegistry();
		startRegistry();
		checkRegistry(nonBundleContributor.getName());
	}

	private void fillRegistry(IContributor contributor) throws IOException {
		// Add extension point
		processXMLContribution(contributor, getXML("ExtensionPoint.xml")); //$NON-NLS-1$
		// Add extension
		processXMLContribution(contributor, getXML("Extension.xml")); //$NON-NLS-1$
	}

	private void checkRegistry(String namespace) {
		IExtensionPoint extensionPoint = simpleRegistry
				.getExtensionPoint(qualifiedName(namespace, "XMLDirectExtPoint")); //$NON-NLS-1$
		assertNotNull(extensionPoint);
		IExtension[] namespaceExtensions = simpleRegistry.getExtensions(namespace);
		assertNotNull(namespaceExtensions);
		assertEquals(1, namespaceExtensions.length);
		IExtension[] extensions = extensionPoint.getExtensions();
		assertNotNull(extensions);
		assertEquals(1, extensions.length);
		for (IExtension extension : extensions) {
			String extensionId = extension.getUniqueIdentifier();
			assertTrue(extensionId.equals(qualifiedName(namespace, "XMLDirectExtensionID"))); //$NON-NLS-1$
			String extensionNamespace = extension.getNamespaceIdentifier();
			assertTrue(extensionNamespace.equals(namespace));
			String extensionContributor = extension.getContributor().getName();
			assertTrue(extensionContributor.equals(namespace));
			IConfigurationElement[] configElements = extension.getConfigurationElements();
			assertNotNull(configElements);
			for (IConfigurationElement configElement : configElements) {
				String configElementName = configElement.getName();
				assertTrue(configElementName.equals("StorageDevice")); //$NON-NLS-1$
				String[] attributeNames = configElement.getAttributeNames();
				assertEquals(1, attributeNames.length);
				IConfigurationElement[] configElementChildren = configElement.getChildren();
				assertEquals(2, configElementChildren.length);
			}
		}
	}

	private void checkListener(String namespace, SimpleRegistryListener listener) {
		IRegistryChangeEvent event = listener.getEvent(5000);
		IExtensionDelta[] deltas = event.getExtensionDeltas();
		assertEquals(1, deltas.length); // only one notification
		for (IExtensionDelta delta : deltas) {
			assertEquals(delta.getKind(), IExtensionDelta.ADDED);
			IExtensionPoint theExtensionPoint = delta.getExtensionPoint();
			IExtension theExtension = delta.getExtension();
			String Id1 = theExtension.getExtensionPointUniqueIdentifier();
			String Id2 = theExtensionPoint.getUniqueIdentifier();
			assertTrue(Id1.equals(Id2)); // check connectivity
			assertTrue(Id1.equals(qualifiedName(namespace, "XMLDirectExtPoint"))); //$NON-NLS-1$
		}
	}
}
