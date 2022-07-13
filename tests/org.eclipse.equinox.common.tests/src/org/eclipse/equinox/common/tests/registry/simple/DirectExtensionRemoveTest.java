/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.equinox.common.tests.registry.simple.utils.SimpleRegistryListener;
import org.junit.Test;

/**
 * Tests removal APIs using a simple registry.
 * @since 3.2
 */
public class DirectExtensionRemoveTest extends BaseExtensionRegistryRun {

	private final static String pointA = "PointA"; //$NON-NLS-1$
	private final static String pointB = "PointB"; //$NON-NLS-1$

	private final static String extensionA1 = "TestExtensionA1"; //$NON-NLS-1$
	private final static String extensionA2 = "TestExtensionA2"; //$NON-NLS-1$


	// Fill the registry; remove half; check listener; check what's left
	@Test
	public void testExtensionPointAddition() throws IOException {
		IContributor nonBundleContributor = ContributorFactorySimple.createContributor("DirectRemoveProvider"); //$NON-NLS-1$
		String namespace = nonBundleContributor.getName();
		fillRegistry(nonBundleContributor);
		checkRegistryFull(namespace);

		SimpleRegistryListener listener = new SimpleRegistryListener();
		listener.register(simpleRegistry);
		remove(namespace);

		checkListener(listener);
		checkRegistryRemoved(namespace);
		listener.unregister(simpleRegistry);
	}

	/**
	 * Tests that configuration elements associated with the removed extension are
	 * removed.
	 *
	 * @throws IOException
	 */
	@Test
	public void testAssociatedConfigElements() throws IOException {
		IContributor nonBundleContributor = ContributorFactorySimple.createContributor("CETest"); //$NON-NLS-1$
		String namespace = nonBundleContributor.getName();
		processXMLContribution(nonBundleContributor, getXML("CERemovalTest.xml")); //$NON-NLS-1$

		IExtensionPoint extensionPointA = simpleRegistry.getExtensionPoint(qualifiedName(namespace, "PointA")); //$NON-NLS-1$
		assertNotNull(extensionPointA);
		IExtension[] extensionsA = extensionPointA.getExtensions();
		assertEquals(2, extensionsA.length);

		// check first extension
		IExtension ext1 = extensionPointA.getExtension(qualifiedName(namespace, "TestExtensionA1")); //$NON-NLS-1$
		assertNotNull(ext1);
		IConfigurationElement[] ces11 = ext1.getConfigurationElements(); // this will be used later
		assertNotNull(ces11);
		assertEquals(1, ces11.length);
		String[] attrs1 = ces11[0].getAttributeNames();
		assertNotNull(attrs1);
		assertEquals(1, attrs1.length);
		assertEquals("class", attrs1[0]); //$NON-NLS-1$
		IConfigurationElement[] ces12 = ces11[0].getChildren(); // this will be used later
		assertNotNull(ces12);
		assertEquals(1, ces12.length);
		String[] attrs2 = ces12[0].getAttributeNames();
		assertNotNull(attrs2);
		assertEquals(1, attrs2.length);
		assertEquals("value", attrs2[0]); //$NON-NLS-1$

		// check second extension
		IExtension ext2 = extensionPointA.getExtension(qualifiedName(namespace, "TestExtensionA2")); //$NON-NLS-1$
		assertNotNull(ext2);
		IConfigurationElement[] ces21 = ext2.getConfigurationElements(); // this will be used later
		IConfigurationElement[] ces22 = ces21[0].getChildren(); // this will be used later
		String[] attrs22 = ces22[0].getAttributeNames();
		assertNotNull(attrs22);
		assertEquals(1, attrs22.length);
		assertEquals("value", attrs22[0]); //$NON-NLS-1$

		// remove extension1
		// listener to verify that valid CEs are included in the notification
		IRegistryChangeListener listener = event -> {
			IExtensionDelta[] deltas = event.getExtensionDeltas();
			assertEquals(1, deltas.length);
			for (IExtensionDelta delta : deltas) {
				assertEquals(IExtensionDelta.REMOVED, delta.getKind());
				IExtension extension = delta.getExtension();
				assertNotNull(extension);

				IConfigurationElement[] l_ces11 = extension.getConfigurationElements();
				assertNotNull(l_ces11);
				assertEquals(1, l_ces11.length);
				String[] l_attrs1 = l_ces11[0].getAttributeNames();
				assertNotNull(l_attrs1);
				assertEquals(1, l_attrs1.length);
				assertEquals("class", l_attrs1[0]); //$NON-NLS-1$
				IConfigurationElement[] l_ces12 = l_ces11[0].getChildren();
				assertNotNull(l_ces12);
				assertEquals(1, l_ces12.length);
				String[] l_attrs2 = l_ces12[0].getAttributeNames();
				assertNotNull(l_attrs2);
				assertEquals(1, l_attrs2.length);
				assertEquals("value", l_attrs2[0]); //$NON-NLS-1$
			}
		};

		//SimpleRegistryListener listener = new SimpleRegistryListener() {};
		simpleRegistry.addRegistryChangeListener(listener);
		try {
			simpleRegistry.removeExtension(ext1, userToken);
		} finally {
			simpleRegistry.removeRegistryChangeListener(listener);
		}

		// basic checks
		IExtension[] extensionsRemoved = extensionPointA.getExtensions();
		assertEquals(1, extensionsRemoved.length);

		// re-check configuration elements
		assertThrows(InvalidRegistryObjectException.class, () -> ces11[0].getAttributeNames()); // should produce an

		assertThrows(InvalidRegistryObjectException.class, () -> ces12[0].getAttributeNames()); // should produce an
		// the non-removed extension CEs should still be valid
		String[] attrs22removed = ces22[0].getAttributeNames();
		assertNotNull(attrs22removed);
		assertEquals(1, attrs22removed.length);
		assertEquals("value", attrs22removed[0]); //$NON-NLS-1$
	}

	private void fillRegistry(IContributor contributor) throws IOException {
		processXMLContribution(contributor, getXML("RemovalTest.xml")); //$NON-NLS-1$
	}

	private void checkRegistryFull(String namespace) {
		IExtensionPoint extensionPointA = simpleRegistry.getExtensionPoint(qualifiedName(namespace, pointA));
		assertNotNull(extensionPointA);
		IExtensionPoint extensionPointB = simpleRegistry.getExtensionPoint(qualifiedName(namespace, pointB));
		assertNotNull(extensionPointB);
		IExtension[] extensionsA = extensionPointA.getExtensions();
		assertEquals(2, extensionsA.length);
		IExtension[] extensionsB = extensionPointB.getExtensions();
		assertEquals(2, extensionsB.length);
	}

	private void remove(String namespace) {
		IExtensionPoint extensionPointB = simpleRegistry.getExtensionPoint(qualifiedName(namespace, pointB));
		assertTrue(simpleRegistry.removeExtensionPoint(extensionPointB, userToken));

		IExtension extension = simpleRegistry.getExtension(qualifiedName(namespace, extensionA1));
		assertTrue(simpleRegistry.removeExtension(extension, userToken));
	}

	private void checkRegistryRemoved(String namespace) {
		IExtensionPoint extensionPointA = simpleRegistry.getExtensionPoint(qualifiedName(namespace, pointA));
		assertNotNull(extensionPointA);
		IExtensionPoint extensionPointB = simpleRegistry.getExtensionPoint(qualifiedName(namespace, pointB));
		assertNull(extensionPointB);
		IExtension[] extensionsA = extensionPointA.getExtensions();
		assertEquals(1, extensionsA.length);
		String Id = extensionsA[0].getUniqueIdentifier();
		assertTrue(qualifiedName(namespace, extensionA2).equals(Id));
	}

	private void checkListener(SimpleRegistryListener listener) {
		IRegistryChangeEvent event = listener.getEvent(5000);
		IExtensionDelta[] deltas = event.getExtensionDeltas();
		assertEquals(2, deltas.length);
		for (IExtensionDelta delta : deltas) {
			assertEquals(IExtensionDelta.REMOVED, delta.getKind());
			assertNotNull(delta.getExtension());
			assertNotNull(delta.getExtensionPoint());
		}
	}
}
