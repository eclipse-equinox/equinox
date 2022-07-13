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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.equinox.common.tests.registry.simple.utils.ExeExtensionStrategy;
import org.eclipse.equinox.common.tests.registry.simple.utils.ExecutableRegistryObject;
import org.junit.Test;

/**
 * Tests that executable extensions present in the simple registry actually gets
 * processed.
 *
 * @since 3.2
 */
public class XMLExecutableExtensionTest extends BaseExtensionRegistryRun {

	/**
	 * Provide own class loader to the registry executable element strategry
	 *
	 * @return - open extension registry
	 */
	@Override
	protected IExtensionRegistry startRegistry() {
		// use plugin's metadata directory to save cache data
		IPath userDataPath = getStateLocation();
		File[] registryLocations = new File[] { new File(userDataPath.toOSString()) };
		boolean[] readOnly = new boolean[] { false };
		RegistryStrategy registryStrategy = new ExeExtensionStrategy(registryLocations, readOnly);
		return RegistryFactory.createRegistry(registryStrategy, masterToken, userToken);
	}

	@Test
	public void testExecutableExtensionCreation() throws IOException, CoreException {
		// Test with non-bundle contributor
		IContributor nonBundleContributor = ContributorFactorySimple.createContributor("ABC"); //$NON-NLS-1$
		assertFalse(ExecutableRegistryObject.createCalled);

		fillRegistry(nonBundleContributor);
		assertFalse(ExecutableRegistryObject.createCalled);

		checkRegistry(nonBundleContributor.getName());
		assertTrue(ExecutableRegistryObject.createCalled);
	}

	private void fillRegistry(IContributor contributor) throws IOException {
		processXMLContribution(contributor, getXML("ExecutableExtension.xml")); //$NON-NLS-1$
	}

	private void checkRegistry(String namespace) throws CoreException {
		IConfigurationElement[] elements = simpleRegistry
				.getConfigurationElementsFor(qualifiedName(namespace, "XMLExecutableExtPoint")); //$NON-NLS-1$
		assertEquals(1, elements.length);
		for (IConfigurationElement element : elements) {
			Object object = element.createExecutableExtension("class"); //$NON-NLS-1$
			assertNotNull(object);
		}
	}
}
