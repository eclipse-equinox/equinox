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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.equinox.common.tests.registry.simple.utils.HiddenLogRegistryStrategy;
import org.junit.Test;

/**
 * Tests addition of extensions and extension points with duplicate IDs.
 * The duplicate extension points should be ignored.
 * The duplicate extensions should be added.
 * The rest of the XML contribution should not be affected.
 *
 * @since 3.2
 */
public class DuplicatePointsTest extends BaseExtensionRegistryRun {

	private final static String errMsg1 = "Error:  Ignored duplicate extension point \"testDuplicates.duplicateExtensionPoint\" supplied by \"2\"." + "Warning:  Extensions supplied by \"2\" and \"1\" have the same Id: \"testDuplicates.duplicateExtension\".";
	private final static String errMsg2 = "Error:  Ignored duplicate extension point \"testSame.duplicateExtensionPointSame\" supplied by \"3\"." + "Warning:  Extensions supplied by \"3\" and \"3\" have the same Id: \"testSame.duplicateExtensionSame\".";

	/**
	 * Use registry strategy with modified logging
	 * @return - open extension registry
	 */
	@Override
	protected IExtensionRegistry startRegistry() {
		// use plugin's metadata directory to save cache data
		IPath userDataPath = getStateLocation();
		File[] registryLocations = new File[] {new File(userDataPath.toOSString())};
		boolean[] readOnly = new boolean[] {false};
		RegistryStrategy registryStrategy = new HiddenLogRegistryStrategy(registryLocations, readOnly);
		return RegistryFactory.createRegistry(registryStrategy, masterToken, userToken);
	}

	@Test
	public void testDuplicates() throws IOException {
		HiddenLogRegistryStrategy.output = ""; //$NON-NLS-1$
		IContributor contributor1 = ContributorFactorySimple.createContributor("1"); //$NON-NLS-1$
		processXMLContribution(contributor1, getXML("DuplicatePoints1.xml")); //$NON-NLS-1$

		IContributor contributor2 = ContributorFactorySimple.createContributor("2"); //$NON-NLS-1$
		processXMLContribution(contributor2, getXML("DuplicatePoints2.xml")); //$NON-NLS-1$

		checkRegistryDifferent("testDuplicates"); //$NON-NLS-1$

		HiddenLogRegistryStrategy.output = ""; //$NON-NLS-1$
		IContributor contributor3 = ContributorFactorySimple.createContributor("3"); //$NON-NLS-1$
		processXMLContribution(contributor3, getXML("DuplicatePointsSame.xml")); //$NON-NLS-1$

		checkRegistrySame("testSame"); //$NON-NLS-1$
	}

	private void checkRegistryDifferent(String namespace) {
		assertTrue(errMsg1.equals(HiddenLogRegistryStrategy.output));

		IExtensionPoint[] extensionPoints = simpleRegistry.getExtensionPoints(namespace);
		assertEquals(2, extensionPoints.length);

		IExtension[] extensions = simpleRegistry.getExtensions(namespace);
		assertEquals(3, extensions.length);

		IExtension extension = simpleRegistry.getExtension(qualifiedName(namespace, "nonDuplicateExtension")); //$NON-NLS-1$
		assertNotNull(extension);
	}

	private void checkRegistrySame(String namespace) {
		assertTrue(errMsg2.equals(HiddenLogRegistryStrategy.output));

		IExtensionPoint[] extensionPoints = simpleRegistry.getExtensionPoints(namespace);
		assertEquals(1, extensionPoints.length);

		IExtension[] extensions = simpleRegistry.getExtensions(namespace);
		assertEquals(2, extensions.length);
	}
}
