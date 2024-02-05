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
import java.net.URL;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.ContributorFactoryOSGi;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Check dynamic contribution into the Eclipse registry itself.
 * 
 * @since 3.2
 */
public class XMLExtensionCreateEclipseTest extends BaseExtensionRegistryRun {
	@Test
	public void testDynamicContribution() throws IllegalArgumentException, IOException {
		// specify this bundle as a contributor
		Bundle thisBundle = FrameworkUtil.getBundle(getClass());
		IContributor thisContributor = ContributorFactoryOSGi.createContributor(thisBundle);
		fillRegistry(thisContributor);
		checkRegistry(thisContributor.getName());
	}

	private void fillRegistry(IContributor contributor) throws IllegalArgumentException, IOException {
		Object userKey = ((ExtensionRegistry) RegistryFactory.getRegistry()).getTemporaryUserToken();
		URL xmlURL = getXML("DynamicExtension.xml"); //$NON-NLS-1$
		RegistryFactory.getRegistry().addContribution(xmlURL.openStream(), contributor, false, xmlURL.getFile(), null,
				userKey);
	}

	private void checkRegistry(String namespace) {
		IExtensionRegistry eclipseRegistry = RegistryFactory.getRegistry();
		String uniqueId = qualifiedName(namespace, "XMLDirectExtPoint"); //$NON-NLS-1$
		IExtensionPoint dynamicExtensionPoint = eclipseRegistry.getExtensionPoint(uniqueId);
		assertNotNull(dynamicExtensionPoint);
		IConfigurationElement[] elements = eclipseRegistry.getConfigurationElementsFor(uniqueId);
		assertEquals(1, elements.length);
		for (IConfigurationElement element : elements) {
			assertTrue("org.eclipse.equinox.common.tests.registry.simple.utils.ExecutableRegistryObject"
					.equals(element.getAttribute("class")));
		}
	}
}
