/*******************************************************************************
 *  Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests.registry.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.eclipse.core.runtime.ContributorFactorySimple;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.junit.Test;

/**
 * Tests merging static and dynamic contributions.
 *
 * @since 3.2
 */
public class MergeContributionTest extends BaseExtensionRegistryRun {
	@Test
	public void testMergeStaticDynamic() throws IOException {
		// Test with non-bundle contributor
		IContributor nonBundleContributor = ContributorFactorySimple.createContributor("ABC"); //$NON-NLS-1$
		String namespace = nonBundleContributor.getName();

		fillRegistryStatic(nonBundleContributor);
		checkRegistry(namespace, 3);
		fillRegistryDynamic(nonBundleContributor);
		checkRegistry(namespace, 6);

		stopRegistry();
		simpleRegistry = startRegistry();

		checkRegistry(namespace, 3);
		fillRegistryDynamic(nonBundleContributor);
		checkRegistry(namespace, 6);
	}

	private void fillRegistryStatic(IContributor contributor) throws IOException {
		processXMLContribution(contributor, getXML("MergeStatic.xml"), true); //$NON-NLS-1$
	}

	private void fillRegistryDynamic(IContributor contributor) throws IOException {
		processXMLContribution(contributor, getXML("MergeDynamic.xml"), false); //$NON-NLS-1$
	}

	private void checkRegistry(String namespace, int expectedExtensions) {
		IExtensionPoint extensionPoint = simpleRegistry.getExtensionPoint(qualifiedName(namespace, "MergeStatic")); //$NON-NLS-1$
		assertNotNull(extensionPoint);
		IExtension[] extensions = simpleRegistry.getExtensions(namespace);
		assertNotNull(extensions);
		assertEquals(expectedExtensions, extensions.length);
	}
}
