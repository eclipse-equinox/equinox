/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
package org.eclipse.equinox.common.tests.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NamespaceTest {

	@Test
	public void testNamespaceBasic() throws IOException, BundleException {
		//test the addition of an extension point
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		Bundle bundle01 = BundleTestingHelper.installBundle("Plugin", bundleContext, "Plugin_Testing/registry/testNamespace/1");
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {bundle01});

		// Extension point and extension should be present
		IExtensionPoint extpt = RegistryFactory.getRegistry().getExtensionPoint("org.abc.xptNS1");
		assertNotNull(extpt);
		assertTrue(extpt.getNamespaceIdentifier().equals("org.abc"));
		assertTrue(extpt.getContributor().getName().equals("testNamespace1"));
		assertTrue(extpt.getSimpleIdentifier().equals("xptNS1"));
		assertTrue(extpt.getUniqueIdentifier().equals("org.abc.xptNS1"));

		IExtension ext = RegistryFactory.getRegistry().getExtension("org.abc.extNS1");
		assertNotNull(ext);
		assertTrue(ext.getNamespaceIdentifier().equals("org.abc"));
		assertTrue(ext.getContributor().getName().equals("testNamespace1"));
		assertTrue(ext.getSimpleIdentifier().equals("extNS1"));
		assertTrue(ext.getUniqueIdentifier().equals("org.abc.extNS1"));

		// Check linkage extension <-> extension point
		assertTrue(ext.getExtensionPointUniqueIdentifier().equals(extpt.getUniqueIdentifier()));
		IExtension[] extensions = extpt.getExtensions();
		assertEquals(1, extensions.length);
		assertTrue(extensions[0].equals(ext));

		// Exactly one extension and one extension point in the "org.abc" namespace
		IExtensionPoint[] namespaceExtensionPoints = RegistryFactory.getRegistry().getExtensionPoints("org.abc");
		assertEquals(1, namespaceExtensionPoints.length);
		assertTrue(namespaceExtensionPoints[0].equals(extpt));
		IExtension[] namespaceExtensions = RegistryFactory.getRegistry().getExtensions("org.abc");
		assertEquals(1, namespaceExtensions.length);
		assertTrue(namespaceExtensions[0].equals(ext));

		// There should not be extension points or extensions in the default namespace
		IExtensionPoint[] defaultExtensionPoints = RegistryFactory.getRegistry().getExtensionPoints("testNamespace1");
		assertEquals(0, defaultExtensionPoints.length);
		IExtension[] defaultExtensions = RegistryFactory.getRegistry().getExtensions("testNamespace1");
		assertEquals(0, defaultExtensions.length);

		// remove the first bundle
		bundle01.uninstall();
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {bundle01});
	}

	@Test
	public void testNamespaceDynamic() throws BundleException, IOException {

		// add another bundle
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		Bundle anotherNamespaceBundle = BundleTestingHelper.installBundle("Plugin", bundleContext, "Plugin_Testing/registry/testNamespace/2");
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {anotherNamespaceBundle});

		// all elements from the first bundle should be gone
		IExtensionPoint extpt_removed = RegistryFactory.getRegistry().getExtensionPoint("org.abc.xptNS1");
		assertNull(extpt_removed);
		IExtension ext_removed = RegistryFactory.getRegistry().getExtension("org.abc.extNS1");
		assertNull(ext_removed);

		// all elements from the second bundle should still be present
		IExtensionPoint extpt2 = RegistryFactory.getRegistry().getExtensionPoint("org.abc.xptNS2");
		assertNotNull(extpt2);
		IExtension ext2 = RegistryFactory.getRegistry().getExtension("org.abc.extNS2");
		assertNotNull(ext2);

		// Exactly one extension and one extension point in the "org.abc" namespace
		IExtensionPoint[] namespaceExtensionPoints2 = RegistryFactory.getRegistry().getExtensionPoints("org.abc");
		assertEquals(1, namespaceExtensionPoints2.length);
		assertTrue(namespaceExtensionPoints2[0].equals(extpt2));
		IExtension[] namespaceExtensions2 = RegistryFactory.getRegistry().getExtensions("org.abc");
		assertEquals(1, namespaceExtensions2.length);
		assertTrue(namespaceExtensions2[0].equals(ext2));

		// remove the second bundle
		anotherNamespaceBundle.uninstall();
		BundleTestingHelper.refreshPackages(bundleContext, new Bundle[] {anotherNamespaceBundle});
	}

}
